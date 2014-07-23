package samcl;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import robot.RobotState;
import util.gui.Panel;
import util.gui.RobotListener;
import util.gui.Tools;
import util.metrics.Distribution;
import util.metrics.Particle;
import util.metrics.Transformer;

import com.beust.jcommander.JCommander;
import com.google.protobuf.ServiceException;

public class Main {
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws ServiceException, Throwable {
		/**
		 * First step:
		 * to create the localization algorithm
		 * and setup the listener for S	AMCL
		 */
		final SAMCL samcl = new SAMCL(
				18, //orientation
				//"file:///home/w514/map.jpg",//map image file
				"hdfs:///user/eeuser/map1024.jpeg",
				(float) 0.005, //delta energy
				100, //total particle
				(float) 0.001, //threshold xi
				(float) 0.6, //rate of population
				10);//competitive strength
		if(args.length==0){
			String[] targs = {/*"-cl",*/
					"-i","file:///Users/ihsumlee/Jolly/jpg/test6.jpg"
					//"-i","file:///Users/ihsumlee/Jolly/jpg/map.jpg"
					,"-o","36"
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
		RobotState robot = new RobotState(20, 20, 0, /*null*/samcl.precomputed_grid, null/*"map.512.4.split"*/, null);
		robot.setVt(0);
		robot.setWt(0);
		robot.setOnCloud(samcl.onCloud);
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
		samcl_window.setSize(samcl.precomputed_grid.width, samcl.precomputed_grid.height);
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
		
		
		List<Particle> parts = new ArrayList<Particle>();
		
		for(int i = 0 ; i < 1000; i++){
			parts.add(new Particle(0, 0, 0,samcl.orientation));
		}
		int rx=0, ry=0,px=0, py=0;
		double rh=0.0;
		int i = 0;
		double time = System.currentTimeMillis()/1000;
		while(true){
			i++;
			Thread.sleep(33);
			grap.drawImage(samcl.precomputed_grid.map_image, null, 0, 0);
			
			px = robot.getX();
			py = robot.getY();
			time = System.currentTimeMillis()/1000 - time;
			for(Particle p : parts){
				//System.out.println("drawing particles");
				
				if(i<10000000){
					p.setX(rx);
					p.setY(ry);
					p.setTh(rh);
				}
				
				Distribution.Motion_sampling(p, robot.getUt(), time);
				Tools.drawPoint(grap, 250 + px - p.getX(), 250 + py - p.getY(), p.getTh(), 4, Color.BLUE);
			}
			
			Tools.drawRobot(grap, 250 + robot.getX()-rx, 250 + robot.getY() - ry, rh, 20, Color.ORANGE);
			rx = robot.getX();
			ry = robot.getY();
			rh = robot.getHead();
			Tools.drawRobot(grap, 250/*robot.getX()*/, 250/*robot.getY()*/, robot.getHead(), 10, Color.RED);
			panel.repaint();
			//System.out.println(robot.toString());
			
			
			
		}
	}
	
	
	/*public class WindowAdpter extends WindowAdapter{
		@Override
		public void windowClosing(WindowEvent e) {
			System.out.println("close table!!!!!!!!!!!!!!!!!!!!!!!");
			if (JOptionPane.showConfirmDialog(samcl_window,
					"Are you sure to close this window?", "Really Closing?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
				//samcl.precomputed_grid.closeTable();
				System.exit(0);
			}
		}
	
	}*/
	
	
}
