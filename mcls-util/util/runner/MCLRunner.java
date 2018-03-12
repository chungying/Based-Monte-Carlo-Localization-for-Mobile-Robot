package util.runner;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import samcl.SAMCL;
import util.grid.Grid;
import util.recorder.Record;
import util.robot.Pose;
import util.robot.RobotState;

public class MCLRunner {
	@Parameter(names = {"--runTimes"}, description = "the number of runs, default is infinite.", required = false, arity = 1)
	private int runTimes = 0;
	
	@Parameter(names = {"--className"}, description = "the fullpath of class name of MCL, default is \"mcl.MCL\"", required = false, arity = 1)
	private String fullPathOfClassName = "mcl.MCL";
	
	@Parameter(names = {"--forwardDist"}, 
			description = "a positive distance the robot move forward. "
					+ "If the distance is negative, this function is disable."
					+ " Default is -1", 
			required = false, 
			arity = 1)
	private double forwardDist = -1;
	
	public void run(String[] args) throws Exception {
		Grid grid = new Grid();
		SAMCL mcl = null;
		RobotState robot = null;
		try{
			JCommander jc = null;
		
			/**
			 * First step:
			 * setup the map
			 * */
			
			jc = new JCommander();
			jc.setAcceptUnknownOptions(true);
			jc.addObject(grid);
			jc.parse(args);
			grid.setupGrid();
			
			/**
			 * Second step:
			 * setup MCL
			 */
			mcl = (SAMCL)Class.forName(fullPathOfClassName).newInstance();
		//		SAMCL mcl = new MCL();
			jc = new JCommander();
			jc.setAcceptUnknownOptions(true);
			jc.addObject(mcl);
			jc.parse(args);
			if(mcl.help){
				jc.usage();
				System.exit(0);
			}
			mcl.setupMCL(grid);
			
			/**
			 * Third step:
			 * setup Robot
			 */
			robot = new RobotState();
			jc = new JCommander();
			jc.setAcceptUnknownOptions(true);
			jc.addObject(robot);
			jc.parse(args);
			robot.setupSimulationRobot(grid);
			if(this.forwardDist >=0 ){
				robot.setPath(new Pose(robot.X + this.forwardDist, robot.Y, robot.H));
			}
			grid.setupCloseableObjs(robot/*, mcl, grid*/);
			new Thread(robot).start();
			
			int counter = 0;		
			while((this.runTimes==0 || counter<this.runTimes) 
					&& ( !mcl.hasClosed() && !robot.isRobotClosing()) ){
				counter++;
				System.out.println(counter + " times mcl.");
				mcl.run(robot, grid);
				robot.robotStartOver();
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			System.out.println("finally closing");
			if(mcl!=null)
				mcl.close();
			if(robot!=null)
				robot.close();
			grid.close();
		}

		Record.statistics();
	}

}
