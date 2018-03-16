package samclroe;

import java.util.ArrayList;
import java.util.List;

import util.grid.Grid;
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
					};
			args = targs;
		}
		JCommander jc = null;
		
		
		
		
		/**
		 * First step:
		 * to create the localization algorithm
		 * and setup the listener for SAMCL
		 */
		SAMCLROE samclroe = new SAMCLROE();
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(samclroe);
		jc.parse(args);
		//if(samclroe.help){
		//	jc.usage();
		//	System.exit(0);
		//}
		
		Grid grid = new Grid();
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(grid);
		jc.parse(args);
		samclroe.setupMCL(grid);
//		if(!samclroe.onCloud){
			System.out.println("start to pre-caching");
			samclroe.preCaching(grid);
//		}	
		
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
		RobotState robot = new RobotState(/*150, 150, 0, */path);
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(robot);
		jc.parse(args);
		robot.setupSimulationRobot(grid);
		
		Thread t = new Thread(robot);
		t.start();

		
		int counter = 0;
		while(!samclroe.isClosing()){
			System.out.println(counter + " times mcl.");
			counter++;
//			robot.goStraight();
			samclroe.run(robot, grid);
			robot.robotStartOver();
		}
	}
}

