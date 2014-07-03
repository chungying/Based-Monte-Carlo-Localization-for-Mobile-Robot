package samcl;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import robot.RobotState;
import util.gui.Panel;
import util.gui.RobotListener;
import util.gui.SamclListener;
import util.gui.Tools;
import util.metrics.Particle;
import util.metrics.Transformer;

import com.beust.jcommander.JCommander;

public class Main {
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException, InterruptedException {
		/**
		 * First step:
		 * to create the localization algorithm
		 * and setup the listener for S	AMCL
		 */
		final SAMCL samcl = new SAMCL(
				18, //orientation
				//"file:///home/w514/map.jpg",//map image file
				//TODO file name
				"hdfs:///user/eeuser/map1024.jpeg",
				(float) 0.005, //delta energy
				100, //total particle
				(float) 0.001, //threshold xi
				(float) 0.6, //rate of population
				10);//competitive strength
		if(args.length==0){
			String[] targs = {"-cl",
					"-i","file:///home/w514/jpg/map.jpg"
					,"-o","4"
					};
			args = targs;
		}
		
		new JCommander(samcl, args);
		SamclListener samclListener = new SamclListener("samcl tuner", samcl);
		
		if(!samcl.onCloud){
			if (!Arrays.asList(args).contains("-i") && !Arrays.asList(args).contains("--image")) {
				String filepath = "file://" + System.getProperty("user.home") + "/test6.jpg";
				System.out.println(filepath);
				samcl.map_filename = filepath;
				
			}
			
			samcl.setup();
			System.out.println("start to pre-caching");
			samcl.Pre_caching();
		}else
			samcl.setup();
	
		
		/**
		 * Second step:
		 * to create a robot
		 * setup the listener of Robot
		 * */
		RobotState robot = new RobotState(70, 70, 180, samcl.precomputed_grid, "map.512.4.split");
		robot.setOnCloud(samcl.onCloud);
		RobotListener robotListener = new RobotListener("robot controller", robot);
		Thread t = new Thread(robot);
		t.start();
		/**
		 * Third step:
		 * start to run samcl
		 */
		final JFrame samcl_window = new JFrame("samcl image");
		Panel panel = new Panel(new BufferedImage(samcl.precomputed_grid.width,samcl.precomputed_grid.height, BufferedImage.TYPE_INT_ARGB));
		samcl_window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		samcl_window.add(panel);
		samcl_window.setSize(panel.img.getWidth(), panel.img.getHeight());
		samcl_window.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("close table!!!!!!!!!!!!!!!!!!!!!!!");
				if (JOptionPane.showConfirmDialog(samcl_window,
						"Are you sure to close this window?", "Really Closing?", 
			            JOptionPane.YES_NO_OPTION,
			            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
					try {
						if(samcl.onCloud)
							samcl.precomputed_grid.closeTable();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					System.exit(0);
				}
			}
		
		});
		
		samcl_window.setVisible(true);
		Graphics2D grap = panel.img.createGraphics();
		Vector<Particle> robots = new Vector<Particle>();
		robots.add(new Particle(robot.getX(),robot.getY(),Transformer.th2Z(robot.getHead(),samcl.orientation, samcl.precomputed_grid.orientation_delta_degree)));
		
		
		
		
		//TODO test 2014/06/19
		samcl.run(robot, samcl_window);
		
		while(true){
			Thread.sleep(33);
			grap.drawImage(samcl.precomputed_grid.map_image, null, 0, 0);
			Tools.drawRobot(grap, robot.getX(), robot.getY(), robot.getHead(), 10, Color.RED);
			panel.repaint();
			System.out.println(robot.toString());
		}
	}
	
	
	/*public class WindowAdpter extends WindowAdapter{
		@Override
		public void windowClosing(WindowEvent e) {
			System.out.println("close table!!!!!!!!!!!!!!!!!!!!!!!");
			if (JOptionPane.showConfirmDialog(samcl_window,
					"Are you sure to close this window?", "Really Closing?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
				//samcl.precomputed_grid.closeTable();
				System.exit(0);
			}
		}
	
	}*/
	
	
}
