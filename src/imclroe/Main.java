package imclroe;

import java.io.IOException;

import robot.RobotState;
import util.gui.RobotController;
import util.gui.Window;

import com.beust.jcommander.JCommander;

public class Main {

	public static void main(String[] args) throws Throwable {
		//for debug mode
				if(args.length==0){
					String[] targs = {/*"-cl",*/
							//"-i","file:///Users/ihsumlee/Jolly/jpg/white.jpg"
							"-i","file:///home/w514/jpg/map.jpg"
							,"-o","4"
							,"-rl","true"
//							,"-rx","30"
//							,"-ry","30"
							,"-p","10"
//							,"-cl"
							};
					args = targs;
				}
				
				/**
				 * First step:
				 * to create the localization algorithm
				 * and setup the listener for SAMCL
				 */
				final IMCLROE samcl = new IMCLROE(false,
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
				RobotState robot = new RobotState(100, 100, 0, /*null*/samcl.precomputed_grid, /*null*/"map.512.4.split", null);
				jc = new JCommander();
				jc.setAcceptUnknownOptions(true);
				jc.addObject(robot);
				jc.parse(args);
				//TODO setup robot
				
				robot.setVt(0);
				robot.setWt(0);
				robot.setInitModel(robot.getUt());
				robot.setInitPose(robot.getPose());
				RobotController robotController = new RobotController("robot controller", robot,samcl);
				Thread t = new Thread(robot);
				t.start();
				/**
				 * Third step:
				 * start to run samcl
				 */
				//TODO WINDOW
				Window samcl_window = new Window("samcl image", samcl,robot);
				
				//TODO test 2014/06/19
				for(int i = 0; i < 10; i ++){
					samcl_window.setTitle("samcl image:"+String.valueOf(i));
					samcl.run(robot, samcl_window);
					robot.lock();
					robot.initRobot();
					robot.unlock();
				}
				
	}

}
