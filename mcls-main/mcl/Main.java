package mcl;

import java.util.ArrayList;
import java.util.List;

import util.gui.RobotController;
import util.gui.VariablesController;
import util.gui.Window;
import util.robot.Pose;
import util.robot.RobotState;

import com.beust.jcommander.JCommander;

public class Main {
	
	public static void main(String[] args) throws Throwable{
		//for debug mode
		if(args.length==0){
			String[] targs = {
					//"-i","file:///Users/ihsumlee/Jolly/jpg/sim_map.jpg"
					//"-i","file:///home/wuser/backup/jpg/test6.jpg"
					"-i"
					//,"file:///Users/Jolly/git/Cloud-based MCL/jpg/simmap.jpg"
					,"file:///Users/Jolly/workspace/dataset/intel_lab/intel-map.png"
					
					,"-rl","true"
					,"-rx","60"
					,"-ry","60"
//					,"-rh","50"
//					,"-n","50"
//					,"-p","20"
					,"-D","false"
					//,"-c","true"//forcing initial convergence of particles.
					,"--ignore", "true"
					,"--showparticles", "true"
					,"--period","50"
					,"--logfile", "true"
					,"--visualization", "true"
					,"--showmeasurements", "true"
					,"-o","72"
					,"-lares","5"
					,"-lrmax","100"
					,"-max_dist", "100"
					};
			args = targs;
		}
		JCommander jc = null;
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
		RobotState robot = new RobotState(60, 60, 0, path);
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(robot);
		jc.parse(args);
		
		/**
		 * First step:
		 * to create the localization algorithm
		 * and setup the listener for SAMCL
		 */
		//TODO parameter 
		final MCL mcl = new MCL(false,
				18, //orientation
				100, //total particle
				10);//competitive stress of tournament selection
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(mcl);
		jc.parse(args);
		if(mcl.help){
			jc.usage();
			System.exit(0);
		}
		mcl.setupGrid(robot.laser);
		
		robot.setupSimulationRobot(mcl.grid);
		
	
		/**
		 * Third step:
		 * Setting up controllers
		 */
		RobotController robotController = new RobotController("robot controller", robot,mcl);
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(robotController);
		jc.parse(args);
		robotController.setVisible(robotController.visualization);
		VariablesController vc = new VariablesController(mcl);
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(vc);
		jc.parse(args);
		vc.setVisible(vc.visualization);
		
		
		Thread t = new Thread(robot);
		t.start();
		/**
		 * Third step:
		 * start to run samcl
		 */
		//TODO WINDOW
		Window window = new Window("mcl image", mcl,robot);
		
		//TODO test 2014/06/19
		int counter = 0;
		while(!mcl.isClosing()){
			counter++;
			window.setTitle("mcl image:"+String.valueOf(counter));
			robot.goStraight();
			mcl.run(robot, window);
			robot.setRobotLock(true);
			robot.robotStartOver();
		}	
		mcl.close();
		robot.close();
	}
	
}
