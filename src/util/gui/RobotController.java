package util.gui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import robot.RobotState;

@SuppressWarnings("serial")
public class RobotController extends JFrame implements ActionListener{
	
	public String S[] = {
			/*0*/"Pause/Continue",	/*1*/"Stop",				/*2*/"",
			/*3*/"",							/*4*/"Forward",		/*5*/"",
			/*6*/"TurnLeft",				/*7*/"Backward",	/*8*/"TurnRight"
	};
	@Override
	public void actionPerformed(ActionEvent e) {
		//System.out.println("action!!");
		Button btn = (Button) e.getSource();
		
		if(btn==B[0]){
			//Pause/Continue
			this.robot.reverseLock();
		}
		else if(btn==B[1]){
			//Stop
			System.out.println("Stop");
			this.robot.setVt(0);
			this.robot.setWt(0);
		}
		else if(btn==B[2]){
			//
		}
		else if(btn==B[3]){
			//
		}
		else if(btn==B[4]){//Forward
			System.out.println("Forward");
			this.robot.setVt(this.robot.getVt() + 1);
		}
		else if(btn==B[5]){
			//
		}
		else if(btn==B[6]){
			//Turnleft
			System.out.println("Turnleft");
			this.robot.setWt(this.robot.getWt() - 1);
		}
		else if(btn==B[7]){
			//Backward
			System.out.println("Backward");
			this.robot.setVt(this.robot.getVt() - 1);
		}
		else if(btn==B[8]){
			//Turnright
			System.out.println("Turnright");
			this.robot.setWt(this.robot.getWt() + 1);
		}
	}


	RobotState robot;
	JPanel control_panel = new JPanel(new GridLayout(3,3));
	Button[] B = new Button[9];
	
	/**
	 * @param title
	 * @throws HeadlessException
	 */
	public RobotController(String title) throws HeadlessException {
		super(title);
		BorderLayout boarder = new BorderLayout(3,3);
		this.setLayout(boarder);
		
		//set up contorl panel
		System.out.println("initial "+ this.getTitle());
		for (int i = 0; i < this.S.length; i++) {
			B[i] = new Button(S[i]);
			control_panel.add(B[i]);
			B[i].addActionListener(this);
		}
		this.add(control_panel, BorderLayout.NORTH);
		
				
		this.pack();
		this.setVisible(true);
		
	}
	
	public RobotController(String title, RobotState robot) {
		this(title);
		this.robot = robot;
	}
}
