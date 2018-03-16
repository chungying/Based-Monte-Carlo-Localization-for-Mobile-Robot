package imclroe;

import java.util.ArrayList;
import java.util.List;

import util.grid.Grid;
import util.robot.Pose;
import util.robot.RobotState;

import com.beust.jcommander.JCommander;

/**
 * IMCLROE Class is based on cloud computing so there is only one version relying on cloud servers. 
 * @author Jolly
 *
 */
public class Main {

	public static void main(String[] args) throws Throwable {
		//for debug mode
		if(args.length==0){
			String[] targs = {
//					"-i","file:///Users/ihsumlee/Jolly/jpg/sim_map.jpg"
					"-i","file:///home/wuser/backup/jpg/test6.jpg"
//					"-i","file:///Users/Jolly/git/Cloud-based MCL/jpg/map_8590.jpg"
//					,"-rl","true"
					,"-rx","80"
					,"-ry","50"
//					,"-rh","50"
//					,"-n","50"
//					,"-p","20"
					,"-D","false"
					,"--converge","true"
//					,"--ignore", "true"
					,"--showparticles", "true"
//					,"--period","500"
					,"--logfile", "true"
					,"-d","0.001"
					,"-x","0.05"
					,"-t","test6.18.split"
					,"-cl"
					,"-E","2"
					,"-o","18"
					,"-lares","20"
					,"-lrmax","-1"
					};
			args = targs;
		}
		JCommander jc =null;
		
		
		/**
		 * First step:
		 * to create the localization algorithm
		 * and setup the listener for SAMCL
		 */
		//TODO parameter
		final IMCLROE imclroe = new IMCLROE(
//				false,//cloud
//				18, //orientation
//				(float) 0.005, //delta energy
//				100, //total particle
//				(float) 0.001, //threshold xi
//				(float) 0.6, //rate of population
//				10//competitive strength
				);
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(imclroe);
		jc.parse(args);
		//if(imclroe.help){
		//	jc.usage();
		//	System.exit(0);
		//}
		
		Grid grid = new Grid();
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(grid);
		jc.parse(args);
		imclroe.setupMCL(grid);
//		if(!imclroe.onCloud){
			System.out.println("start to pre-caching");
			imclroe.preCaching(grid);
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
		RobotState robot = new RobotState(path);
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(robot);
		jc.parse(args);
		robot.setupSimulationRobot(grid);
		
		Thread t = new Thread(robot);
		t.start();
		/**
		 * Third step:
		 * start to run samcl
		 */
		
		//TODO test 2014/06/19
		int counter = 0;
		while(!imclroe.isClosing()){
			System.out.println(counter + " times mcl.");
			counter++;
//			robot.goStraight();
			imclroe.run(robot, grid);
			robot.robotStartOver();
		}
	}
	
}
