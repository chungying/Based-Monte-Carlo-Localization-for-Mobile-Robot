package samclroe;

import java.util.ArrayList;
import java.util.List;

import util.gui.RobotController;
import util.gui.VariablesController;
import util.gui.Window;
import util.robot.Pose;
import util.robot.RobotState;

import com.beust.jcommander.JCommander;
import com.google.protobuf.ServiceException;

public class Main {

	public static void main(String[] args) throws ServiceException, Throwable {
		//for debug mode
		if(args.length==0){
			String[] targs = {
//					"-i","file:///Users/ihsumlee/Jolly/jpg/sim_map.jpg"
//					"-i","file:///home/wuser/backup/jpg/test6.jpg"
					"-i","file:///Users/Jolly/git/Cloud-based MCL/jpg/map_8590.jpg"
//					,"-rl","true"
					,"-rx","30"
					,"-ry","30"
//					,"-rh","50"
//					,"-n","50"
//					,"-p","20"
					,"-D","false"
					,"--converge","true"
//					,"--ignore", "true"
					,"--visualization","true"
					,"--showparticles","true"
					,"--showmeasurements","true"
//					,"--period","500"
					,"--logfile","true"
					,"-d","0.001"
					,"-x","0.05"
					,"-o","18"
					,"-lares","20"
					,"-lrmax","-1"
					,"-max_dist", "-1"
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
		RobotState robot = new RobotState(150, 150, 0, path);
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(robot);
		jc.parse(args);
		
		/**
		 * First step:
		 * to create the localization algorithm
		 * and setup the listener for SAMCL
		 */
		final SAMCLROE samclroe = new SAMCLROE(
//				18, //orientation
				(float) 0.005, //delta energy
				100, //total particle
				(float) 0.001, //threshold xi
				(float) 0.6, //rate of population
				10);//competitive strength
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(samclroe);
		jc.parse(args);
		if(samclroe.help){
			jc.usage();
			System.exit(0);
		}
		
		samclroe.setupGrid(robot.laser);
		if(!samclroe.onCloud){
			System.out.println("start to pre-caching");
			samclroe.preCaching();
		}	
		

		robot.setupSimulationRobot(samclroe.grid);
		
		//TODO setup robot
		RobotController robotController = new RobotController("robot controller", robot,samclroe);
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(robotController);
		jc.parse(args);
		robotController.setVisible(robotController.visualization);
		VariablesController vc = new VariablesController(samclroe);
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
		Window window = new Window("samcl image", samclroe,robot);
		
		//TODO test 2014/06/19
		int counter = 0;
		while(!samclroe.isClosing()){
			counter++;
			window.setTitle("samcl image:"+String.valueOf(counter));
			robot.goStraight();
			samclroe.run(robot, window);
			robot.setRobotLock(true);
			robot.robotStartOver();
		}
		samclroe.close();
		robot.close();
	}
}

