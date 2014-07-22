package robot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import samcl.SAMCL;
import samcl.SAMCLROE;
import util.gui.Panel;
import util.gui.RobotListener;
import util.gui.SamclListener;
import util.gui.Tools;
import util.metrics.Particle;
import util.metrics.Transformer;

import com.beust.jcommander.JCommander;
import com.google.protobuf.ServiceException;

public class PathPlan {
	public static final double standardAngularVelocity = 15;// degree/second
	public static final double standardVelocity = 20;// pixel/second
	
	private List<Pose> path = null;
	
	
	public Pose currentPose;
	private int currentTarget;
	private int nextTarget;
	
	public int nextPose(Pose robot){
		return currentTarget;
	}
	
	
	public boolean nextPose(RobotState robot){
		
		
		
		//System.out.println("next target"+path.get(currentPose+1));
		if( path.get(currentTarget+1).equals(robot.getPose()) ){
			robot.stop();
			currentTarget++;
			currentPose = path.get(currentTarget);
			robot.setPose(currentPose);
			this.setVelocityModel(robot, path.get(currentTarget+1));
			return true;
		}
		else{
			return false;
		}
	}
	
	private void setVelocityModel(RobotState robot, Pose dst){
		double dd = Pose.compareToDistance(robot.getPose(), dst);
		if(dd!=0.0){
			if(dd>0)
				robot.setVt(PathPlan.standardVelocity);
			else
				robot.setVt(0-PathPlan.standardVelocity);
		}
		else {
			double dH = Pose.compareToHead(robot.getPose(), dst);
			if(dH>0){
				robot.setWt(PathPlan.standardVelocity);
			}
			else if(dH < 0){
				robot.setWt(0-PathPlan.standardAngularVelocity);
			}else
				robot.stop();
		}
	}
	
	public void setPath(Pose robot, List<Pose> path){
		List<Pose> newPath = new ArrayList<Pose>();
		if(!robot.equal(path.get(0))){
			Pose e = new Pose(robot.X, robot.Y, robot.H + Pose.compareToOrientation(robot, path.get(0)) );
			newPath.add(e);
			Pose d = new Pose(path.get(0).X,path.get(0).Y,e.H);
			newPath.add(d);
		}
		
		Pose src = null;
		Pose dst = null;
		for(int i = 0 ; i < path.size() ; i++){
			//if last newPath pose equals path[i]
			src = newPath.get(newPath.size()-1);
			dst = path.get(i);
			if( src.equal(dst) ){
				//do nothing
			}
			//if last newPath pose does not equal to path[i]
			else{
				//if distance differ
				if(!src.equalsPose(dst)){
					//add next Pose to turn to the orientation face the path[i]
					Pose e = new Pose( dst.X, dst.Y, src.H + Pose.compareToOrientation(src, dst));
					newPath.add(e);
					src = e;
					if(!src.equalsHead(dst)){
						//add the path[i] to newPath
						Pose d = new Pose(dst.X, dst.Y, src.H + Pose.compareToHead(src, dst));
						newPath.add(d);
					}
				}else{
					if(!src.equalsHead(dst)){
						//add the path[i] to newPath
						Pose d = new Pose(dst.X, dst.Y, src.H + Pose.compareToHead(src, dst));
						newPath.add(d);
					}
				}
				
			}
		}
		this.path = newPath;
		this.currentTarget = 0;
		this.currentPose = newPath.get(0);
	}
	
	
	public static void main(String[] args) throws ServiceException, Throwable{
		final SAMCLROE samcl = new SAMCLROE(
				18, //orientation
				//"file:///home/w514/map.jpg",//map image file
				//TODO file name
				"hdfs:///user/eeuser/map1024.jpeg",
				(float) 0.00005, //delta energy
				100, //total particle
				(float) 0.01, //threshold xi
				(float) 0.6, //rate of population
				10);//competitive strength
		if(args.length==0){
			String[] targs = {/*"-cl",*/
					"-i","file:///home/w514/jpg/map.jpg"
					//"-i","file:///Users/ihsumlee/Jolly/jpg/map.jpg"
					,"-o","18"
					};
			args = targs;
		}
		
		new JCommander(samcl, args);
		
		if(!samcl.onCloud){			
			samcl.setup();
			System.out.println("start to pre-caching");
			samcl.Pre_caching();
		}else
			samcl.setup();
		
		/**
		 * Second step:
		 * to create a robot
		 * setup the listener of Robot
		 * */
		RobotState robot = new RobotState(220, 60, 0, /*null*/samcl.precomputed_grid, null/*"map.512.4.split"*/);
		robot.setVt(0);
		robot.setWt(0);
		robot.setOnCloud(samcl.onCloud);
		PathPlan plan = new PathPlan();
		List<Pose> path = new ArrayList<Pose>();
		path.add(new Pose(60,30,90));
		path.add(new Pose(60,40,180));
		path.add(new Pose(30,40,270));
		plan.setPath(robot.getPose(), path);
		
		robot.setPp(plan);
		for(Pose p : plan.getPath()){
			System.out.println(p.toString());
		}
		SamclListener samclListener = new SamclListener("samcl tuner", samcl);
		RobotListener robotListener = new RobotListener("robot controller", robot);
		Thread t = new Thread(robot);
		t.start();
		/**
		 * Third step:
		 * start to run samcl
		 */
		final JFrame samcl_window = new JFrame("samcl image");
		Panel panel = new Panel(new BufferedImage(samcl.precomputed_grid.width,samcl.precomputed_grid.height, BufferedImage.TYPE_INT_ARGB));
		samcl_window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		samcl_window.add(panel);
		samcl_window.setSize(panel.img.getWidth(), panel.img.getHeight());
		samcl_window.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("close table!!!!!!!!!!!!!!!!!!!!!!!");
				if (JOptionPane.showConfirmDialog(samcl_window,
						"Are you sure to close this window?", "Really Closing?", 
			            JOptionPane.YES_NO_OPTION,
			            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
					try {
						if(samcl.onCloud)
							samcl.precomputed_grid.closeTable();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					System.exit(0);
				}
			}
		
		});
		
		samcl_window.setVisible(true);
		Graphics2D grap = panel.img.createGraphics();
		Vector<Particle> robots = new Vector<Particle>();
		robots.add(new Particle(robot.getX(),robot.getY(),Transformer.th2Z(robot.getHead(),samcl.orientation, samcl.precomputed_grid.orientation_delta_degree)));
		
		
		
		
		//TODO test 2014/06/19
		samcl.run(robot, samcl_window);

		int i = 0;
		double time = System.currentTimeMillis()/1000;
		while(true){
			i++;
			Thread.sleep(3);
			grap.drawImage(samcl.precomputed_grid.map_image, null, 0, 0);
			time = System.currentTimeMillis()/1000 - time;
			Tools.drawRobot(grap, robot.getX(), robot.getY(), robot.getHead(), 10, Color.RED);
			panel.repaint();
			//System.out.println(robot.toString());
			if(i==2000){
				System.out.println("start up ");
				robot.setVt(PathPlan.standardVelocity);
			}
			
			
		}
	}

	public List<Pose> getPath() {
		return path;
	}
	
}
