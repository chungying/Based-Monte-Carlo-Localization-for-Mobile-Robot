package samcl;


import java.io.IOException;
import java.util.Arrays;

import robot.RobotState;
import util.gui.RobotListener;
import util.gui.SamclListener;

import com.beust.jcommander.JCommander;

public class Main {
	
	public static void main(String[] args) throws IOException {
		/**
		 * First step:
		 * to create the localization algorithm
		 * and setup the listener for SAMCL
		 */
		SAMCL samcl = new SAMCL(
				18, //orientation
				//"file:///home/w514/map.jpg",//map image file
				//TODO file name
				"hdfs:///user/eeuser/map1024.jpeg",
				(float) 0.005, //delta energy
				100, //total particle
				(float) 0.001, //threshold xi
				(float) 0.6, //rate of population
				10);//competitive strength
		
		new JCommander(samcl, args);
		SamclListener samclListener = new SamclListener("samcl tuner", samcl);
		
		if(!samcl.onCloud){
			if (!Arrays.asList(args).contains("-i") && !Arrays.asList(args).contains("--image")) {
				String filepath = "file://" + System.getProperty("user.home") + "/sim_map.jpg";
				System.out.println(filepath);
				samcl.map_filename = filepath;
				
			}
			
			samcl.setup();
			System.out.println("start to pre-caching");
			//samcl.Pre_caching();
		}else
			samcl.setup();
		
		/**
		 * Second step:
		 * to create a robot
		 * setup the listener of Robot
		 * */
		RobotState robot = new RobotState(0, 0, 0, samcl.precomputed_grid);
		RobotListener robotListener = new RobotListener("robot controller", robot);
		
		/**
		 * Third step:
		 * start to run samcl
		 */
		samcl.run(robot);
//		int counter = 0;
//		while(true){
//			counter++;
//			samcl.run();
//			System.out.println("end.....");
//		}
		
	}
}
