package util.gui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Label;
import java.awt.TextComponent;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import samcl.SAMCL;
import util.robot.RobotState;

@SuppressWarnings("serial")
public class RobotController extends JFrame implements ActionListener{
	
	public String S[] = {
			/*0*/"Pause/Continue",	/*1*/"Stop",		/*2*/"Terminate",
			/*3*/"",				/*4*/"Forward",		/*5*/"Initialize",
			/*6*/"TurnLeft",		/*7*/"Backward",	/*8*/"TurnRight"
	};
	@Override
	public void actionPerformed(ActionEvent e) {
		this.labelU.setText(
				"v:"+Double.toString(this.robot.getVt())+
				",w:"+Double.toString(this.robot.getWt()));
		//System.out.println("action!!");
		Object object = e.getSource();
		if(object instanceof Button){
			Button btn = (Button) object;
			System.out.println("Action!!!");
			
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
				//terminate
				System.out.println("terminate the localization method");
				this.samcl.setTerminated(true);
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
				System.out.println("Initialize robot");
				this.robot.lock();
				this.robot.initRobot();
				
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
		}else if(object instanceof TextField){
			TextField text = (TextField) object;
			if(text==textU[0]){
				System.out.println("update velocity");
				if(text.getText().length()!=0)
					this.robot.setVt(Double.parseDouble(text.getText()));
//				this.labelU.setText(
//						"v:"+Double.toString(this.robot.getVt())+
//						",w:"+Double.toString(this.robot.getWt()));
			}
			else if(text==textU[1]){
				System.out.println("update angular velocity");
				if(text.getText().length()!=0)
					this.robot.setWt(Double.parseDouble(text.getText()));
//				this.labelU.setText(
//						"v:"+Double.toString(this.robot.getVt())+
//						",w:"+Double.toString(this.robot.getWt()));
			}
		}
	}


	private RobotState robot;
	private SAMCL samcl;
	
	
	/*
	 * _________________________
	 * |Button	|Button	|Button	|
	 * |________________________|
	 * |Button	|Button	|Button	|
	 * |________________________|
	 * |Button	|Button	|Button	|
	 * |________________________|
	 * |Text	|Button	|Label	|
	 * |________________________|
	 * 
	 * 
	 */
	JPanel control_panel = new JPanel(new GridLayout(4,3));
	Button[] B = new Button[9];
	TextField[] textU = new TextField[2];
	Label labelU = new Label();
	/**
	 * @param title
	 * @throws HeadlessException
	 */
	public RobotController(String title) throws HeadlessException {
		this(title, null, null);
	}
	
	public RobotController(String title, RobotState robot) {
		this(title, robot, null);
	}
	
	public RobotController(String title, RobotState robot, SAMCL samcl){
		super(title);
		this.robot = robot;
		this.samcl = samcl;

		BorderLayout boarder = new BorderLayout(3,3);
		this.setLayout(boarder);
		
		//set up contorl panel
		System.out.println("initial "+ this.getTitle());
		for (int i = 0; i < this.S.length; i++) {
			B[i] = new Button(S[i]);
			control_panel.add(B[i]);
			B[i].addActionListener(this);
		}
		labelU = new Label(
				"v:"+Double.toString(this.robot.getVt())+
				",w:"+Double.toString(this.robot.getWt()));
		
		control_panel.add(labelU);
		
		textU[0] = new TextField();
		textU[0].setText(String.valueOf(this.robot.getVt()));
		control_panel.add(textU[0]);
		textU[0].addActionListener(this);
		textU[1] = new TextField();
		textU[1].setText(String.valueOf(this.robot.getWt()));
		control_panel.add(textU[1]);
		textU[1].addActionListener(this);
		
		
		this.add(control_panel, BorderLayout.NORTH);
		
				
		this.pack();
		this.setVisible(true);
	}
}
