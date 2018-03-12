package samcl;

import java.util.ArrayList;
import java.util.List;

import util.grid.Grid;
import util.robot.Pose;
import util.robot.RobotState;

import com.beust.jcommander.JCommander;

public class Main {
	
	public static void main(String[] args)  {
		//for debug mode
		if(args.length==0){
			String[] targs = {
//					"-i","file:///Users/ihsumlee/Jolly/jpg/sim_map.jpg"
//					"-i","file:///home/wuser/backup/jpg/test6.jpg"
					//"-i","file:///Users/Jolly/git/Cloud-based MCL/jpg/simmap.jpg"
					"-i","file:///Users/Jolly/git/Cloud-based MCL/jpg/map_8590.jpg"
//					,"-rl","true"
					,"-rx","30"
					,"-ry","30"
					,"-a", "0.7"
//					,"-rh","50"
//					,"-n","50"
//					,"-p","20"
					,"-D","false"
					,"-c","true"
//					,"--ignore", "true"
					,"--showparticles", "true"
//					,"--period","50"
					,"--logfile", "true"
					,"-d","0.001"
					,"-x","0.05"
//					,"-t","test6.18.split"
//					,"-cl"
					,"--visualization", "true"
					,"--showmeasurements","true"
					,"-o","12"
					,"-lares","30"
					,"-lrmax","50"
					};
			args = targs;
		}
		SAMCL samcl = null;
		RobotState robot = null;
		JCommander jc =null;
		try{
			
			
			
			/**
			 * First step:
			 * to create the localization algorithm
			 * and setup the listener for SAMCL
			 */
//			samcl = new SAMCL(
//					(float) 0.005, //delta energy
//					100, //total particle
//					(float) 0.001, //threshold xi
//					(float) 0.6, //rate of population
//					10);//competitive strength
			samcl = new SAMCL();
			jc = new JCommander();
			jc.setAcceptUnknownOptions(true);
			jc.addObject(samcl);
			jc.parse(args);
			if(samcl.help){
				jc.usage();
				System.exit(0);
			}
			
			Grid grid = new Grid();
			jc = new JCommander();
			jc.setAcceptUnknownOptions(true);
			jc.addObject(grid);
			jc.parse(args);
			samcl.setupMCL(grid);
//			if(!samcl.onCloud){
				System.out.println("start to pre-caching");
				samcl.preCaching(grid);
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
			robot = new RobotState(/*150, 150, 0, */path);
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
			int counter = 0;
			while(!samcl.isClosing()){
				System.out.println(counter + " times mcl.");
//				robot.goStraight();
				samcl.run(robot, grid);
				robot.robotStartOver();
			}
			
		}
		catch (Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}
