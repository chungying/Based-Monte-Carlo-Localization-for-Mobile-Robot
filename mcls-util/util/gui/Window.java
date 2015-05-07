package util.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import samcl.SAMCL;
import util.robot.RobotState;
//TODO implement Window.class with Runnable interface in order to independently monitoring.
public class Window extends JFrame implements Runnable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public SAMCL samcl = null;
	public RobotState robot = null;
	public Window(String name, SAMCL samcl, RobotState robot){
		super(name);
		this.samcl = samcl;
		this.robot = robot;
		this.setSize(samcl.grid.width, samcl.grid.height);
		this.addWindowListener(new CustomAdapter());
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	
	public class CustomAdapter extends WindowAdapter{
		@Override
		public void windowClosing(WindowEvent e) {
			System.out.println("close table!!!!!!!!!!!!!!!!!!!!!!!");
			if (JOptionPane.showConfirmDialog(Window.this,
					"Are you sure to close this window?", "Really Closing?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
				try {
					samcl.setTerminating(true);
					samcl.setClosing(true);
					
//					samcl.close();
//					robot.close();
					
				} catch (Exception e1) {
					e1.printStackTrace();
				}
//				System.exit(0);
			}
			
			super.windowClosing(e);
		}
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
}
