package samcl;

import java.util.ArrayList;
import java.util.List;

import robot.Pose;
import robot.RobotState;
import util.gui.RobotController;
import util.gui.Window;

import com.beust.jcommander.JCommander;
import com.google.protobuf.ServiceException;

public class Main {
	
	public static void main(String[] args) throws ServiceException, Throwable {
		//for debug mode
		if(args.length==0){
			String[] targs = {/*"-cl",*/
//					"-i","file:///Users/ihsumlee/Jolly/jpg/sim_map.jpg"
					"-i","file:///home/w514/jpg/map.jpg"
					,"-o","4"
//					,"-rl","true"
					,"-rx","100"
					,"-ry","100"
//					,"-p","10"
					,"-cl"
					,"-t map.512.4.split"
					};
			args = targs;
		}
		
		/**
		 * First step:
		 * to create the localization algorithm
		 * and setup the listener for SAMCL
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
		JCommander jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(samcl);
		jc.parse(args);
		samcl.setup();
		if(!samcl.onCloud){
			System.out.println("start to pre-caching");
			samcl.Pre_caching();
		}	
		
		/**
		 * Second step:
		 * to create a robot
		 * setup the listener of Robot
		 * */
		List<Pose> path = new ArrayList<Pose>();
		path.add(new Pose(400,150,0));
		path.add(new Pose(400,150,90));
		path.add(new Pose(400,400,90));
		path.add(new Pose(400,400,180));
		path.add(new Pose(350,400,180));
		path.add(new Pose(350,400,90));
		path.add(new Pose(350,550,90));
		path.add(new Pose(350,550,180));
		path.add(new Pose(150,550,180));
		path.add(new Pose(150,550,270));
		path.add(new Pose(150,150,270));
		path.add(new Pose(150,150,0));
		RobotState robot = new RobotState(150, 150, 0, /*null*/samcl.grid, /*null*/"map.512.4.split", path);
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(robot);
		jc.parse(args);
		//TODO setup robot
		robot.setInitModel(robot.getUt());
		robot.setInitPose(robot.getPose());
		@SuppressWarnings("unused")
		RobotController robotController = new RobotController("robot controller", robot,samcl);
		Thread t = new Thread(robot);
		t.start();
		/**
		 * Third step:
		 * start to run samcl
		 */
		//TODO WINDOW
		Window window = new Window("samcl image", samcl,robot);
		
		//TODO test 2014/06/19
		int counter = 0;
		while(true){
			window.setTitle("samcl image:"+String.valueOf(counter));
			robot.goStraight();
			samcl.run(robot, window);
			robot.lock();
			robot.initRobot();
		}
		
		/*
		//below is for test.
		Panel panel = new Panel(new BufferedImage(samcl.grid.width,samcl.grid.height, BufferedImage.TYPE_INT_ARGB));
		samcl_window.add(panel);
		samcl_window.setVisible(true);
		Graphics2D grap = panel.img.createGraphics();
		
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
			grap.drawImage(samcl.grid.map_image, null, 0, 0);
			//System.out.println(robot.toString());
//			px = robot.getX();
//			py = robot.getY();
			time = System.currentTimeMillis()/1000 - time;
			rx = robot.getX();
			ry = robot.getY();
			rh = robot.getHead();
			for(Particle p : parts){
				//System.out.println("drawing particles");
				
//				if(i<10000000){
					p.setX(rx);
					p.setY(ry);
					p.setTh(rh);
//				}
				
				Distribution.Motion_sampling(p,samcl.orientation, robot.getUt(), time);
				Tools.drawPoint(grap,  p.getX(), p.getY(), p.getTh(), 4, Color.BLUE);
				System.out.println(p.toString());
			}
			
			Tools.drawRobot(grap,  robot.getX(),  robot.getY(), robot.getHead(), 20, Color.ORANGE);
			
//			Tools.drawRobot(grap, 250, 250, robot.getHead(), 10, Color.RED);
			panel.repaint();
			//System.out.println(robot.toString());
			
		}*/
	}
	
}
