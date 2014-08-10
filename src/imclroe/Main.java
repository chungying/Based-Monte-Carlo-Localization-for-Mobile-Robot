package imclroe;

import java.util.ArrayList;
import java.util.List;

import robot.Pose;
import robot.RobotState;
import util.gui.RobotController;
import util.gui.Window;

import com.beust.jcommander.JCommander;

public class Main {

	public static void main(String[] args) throws Throwable {
		//for debug mode
		if(args.length==0){
			String[] targs = {
					//"-i","file:///Users/ihsumlee/Jolly/jpg/white.jpg"
					"-i","file:///home/w514/jpg/map.jpg"
					,"-o","18"
					,"-rl","true"
					,"-rx","100"
					,"-ry","100"
					,"-p","10"
					,"-cl"
					,"-t", "map.512.4.split"
					};
			args = targs;
		}
		
		/**
		 * First step:
		 * to create the localization algorithm
		 * and setup the listener for SAMCL
		 */
		//TODO parameter
		final IMCLROE imclroe = new IMCLROE(false,
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
		jc.addObject(imclroe);
		jc.parse(args);
		imclroe.setup();
//			if(!imclroe.onCloud){
//				System.out.println("start to pre-caching");
//				imclroe.Pre_caching();
//			}	
		
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
		RobotState robot = new RobotState(150, 150, 0, /*null*/imclroe.grid, /*null*/"map.512.4.split", path);
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(robot);
		jc.parse(args);
		//TODO setup robot
		robot.setInitModel(robot.getUt());
		robot.setInitPose(robot.getPose());
		@SuppressWarnings("unused")
		RobotController robotController = new RobotController("robot controller", robot,imclroe);
		Thread t = new Thread(robot);
		t.start();
		/**
		 * Third step:
		 * start to run samcl
		 */
		//TODO WINDOW
		Window window = new Window("samcl image", imclroe,robot);
		
		//TODO test 2014/06/19
		for(int i = 0; i < 10; i ++){
			window.setTitle("samcl image:"+String.valueOf(i));
			robot.goStraight();
			imclroe.run(robot, window);
			robot.lock();
			robot.initRobot();
			robot.unlock();
		}		
	}
	
}
