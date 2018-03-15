package util.runner;

import java.lang.StringBuilder;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import samcl.SAMCL;
import util.grid.Grid;
import util.recorder.Record;
import util.robot.Pose;
import util.robot.RobotState;

public class MCLRunner {
	@Parameter(names = {"--help","-h"}, help = true)
	public boolean help = false;
	
	@Parameter(names = {"--className"}, description = "the fullpath of class name of MCL, default is \"mcl.MCL\"", required = true, arity = 1)
	private String fullPathOfClassName = "mcl.MCL";

	@Parameter(names = {"--runTimes"}, description = "the number of runs, default is infinite.", required = false, arity = 1)
	private int runTimes = 0;
	
	@Parameter(names = {"--forwardDist"}, 
			description = "a positive distance the robot move forward. "
					+ "If the distance is negative, this function is disable."
					+ " Default is -1", 
			required = false, 
			arity = 1)
	private double forwardDist = -1;
	
	public void run(String[] args) throws Exception {
		Grid grid = null;
		SAMCL mcl = null;
		RobotState robot = null;
		try{
			grid = new Grid();
			mcl = (SAMCL)Class.forName(fullPathOfClassName).newInstance();
			robot = new RobotState();
			JCommander gridjc = new JCommander();
			JCommander mcljc = new JCommander();
			JCommander robotjc = new JCommander();
			try{
				gridjc.setAcceptUnknownOptions(true);
				gridjc.addObject(grid);
				gridjc.parse(args);
		        
				mcljc.setAcceptUnknownOptions(true);
				mcljc.addObject(mcl);
				mcljc.parse(args);
                                
				robotjc.setAcceptUnknownOptions(true);
				robotjc.addObject(robot);
				robotjc.parse(args);
			}catch(ParameterException e){
				System.out.println(e.getMessage());
				System.out.println("please type --help for more information");
				System.exit(1);
			}

			if(this.help){
				StringBuilder gsb = new StringBuilder();
				StringBuilder rsb = new StringBuilder();
				StringBuilder msb = new StringBuilder();
				gridjc.usage(gsb);
				robotjc.usage(rsb);
				mcljc.usage(msb);
				System.out.println("Grid usage\n"+gsb.toString());
				System.out.println("Robot usage\n"+rsb.toString());
				System.out.println("MCL usage\n"+msb.toString());
				System.exit(0);
			}

			/**
			 * First step:
			 * setup the map
			 * */
			
			grid.setupGrid();
			
			/**
			 * Second step:
			 * setup MCL
			 */
			mcl.setupMCL(grid);
			
			/**
			 * Third step:
			 * setup Robot
			 */
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
