package util.gui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.beust.jcommander.Parameter;

import samcl.SAMCL;
import util.robot.RobotState;

@SuppressWarnings("serial")
public class RobotController extends JFrame implements ActionListener{
	@Parameter(names = "--visualization", help = false)
	public boolean visualization;
	public String S[] = {
			/*0*/"Pause/Continue",	/*1*/"Stop",		/*2*/"Terminate",
			/*3*/"Converge",		/*4*/"Forward",		/*5*/"Initialize",
			/*6*/"TurnLeft",		/*7*/"Backward",	/*8*/"TurnRight"
	};
	@Override
	public void actionPerformed(ActionEvent e) {
		
		//System.out.println("action!!");
		Object object = e.getSource();
		if(object instanceof Button){
			Button btn = (Button) object;
//			System.out.println("Action!!!");
			
			if(btn==B[0]){
				//Pause/Continue
				this.robot.reverseLock2();
			}
			else if(btn==B[1]){
				//Stop
				//System.out.println("Stop");
				this.robot.setVt(0);
				this.robot.setWt(0);
			}
			else if(btn==B[2]){
				//terminate
				//System.out.println("terminate the localization method");
				this.samcl.setTerminating(true);
			}
			else if(btn==B[3]){
				//force converge
				samcl.forceConverge();
			}
			else if(btn==B[4]){//Forward
				//System.out.println("Forward");
				this.robot.setVt(this.robot.getVt() + 1);
			}
			else if(btn==B[5]){
				//Initialize
				//System.out.println("Initialize robot");
				this.robot.lock2();
				this.robot.initRobot();
				
			}
			else if(btn==B[6]){
				//Turnleft
				//System.out.println("Turnleft");
				this.robot.setWt(this.robot.getWt() - 1);
			}
			else if(btn==B[7]){
				//Backward
				//System.out.println("Backward");
				this.robot.setVt(this.robot.getVt() - 1);
			}
			else if(btn==B[8]){
				//Turnright
				//System.out.println("Turnright");
				this.robot.setWt(this.robot.getWt() + 1);
			}
		}else if(object instanceof TextField){
			TextField text = (TextField) object;
			if(text==textU[0]){
				//System.out.println("update velocity");
				if(text.getText().length()!=0)
					this.robot.setVt(Double.parseDouble(text.getText()));
			}
			else if(text==textU[1]){
				//System.out.println("update angular velocity");
				if(text.getText().length()!=0)
					this.robot.setWt(Double.parseDouble(text.getText()));
			}
			else if(text==textPose[0]){
				//System.out.println("update robot X");
				if(text.getText().length()!=0)
					this.robot.setX(Double.parseDouble(text.getText()));
			}
			else if(text==textPose[1]){
				//System.out.println("update robot Y");
				if(text.getText().length()!=0)
					this.robot.setY(Double.parseDouble(text.getText()));
			}
			else if(text==textPose[2]){
				//System.out.println("update robot Head");
				if(text.getText().length()!=0)
					this.robot.setHead(Double.parseDouble(text.getText()));
			}
		}
	}


	Thread updateThtread = new Thread(){
		@Override
		public void run() {
			try {
				while(true){
					if(!robot.isLock2()){
						labelU.setText(
								"v:"+Double.toString(robot.getVt())+
								",w:"+Double.toString(robot.getWt()));
						label[0].setText(
								String.format("%.2f", robot.X));
						label[1].setText(
								String.format("%.2f", robot.Y));
						label[2].setText(
								String.format("%.2f", robot.H));
					}
					Thread.sleep(33);
				}
			} catch (InterruptedException e) {
				System.out.println("updateThread in RobotController");
				e.printStackTrace();
			}
		}
	};
	
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
	 * |Label	|Label	|Label	|
	 * |________________________|
	 * |Text	|Text	|Text	|
	 * |________________________|
	 * 
	 * 
	 */
	JPanel control_panel = new JPanel(new GridLayout(6,3));
	Button[] B = new Button[9];
	TextField[] textU = new TextField[2];
	Label labelU = new Label();
	TextField[] textPose = new TextField[3];
	Label[] label = new Label[3];
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
		this.setLayout(new BorderLayout(3,3));
		
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
		
		
		label[0] = new Label(
				Double.toString(this.robot.X));
		label[1] = new Label(
				Double.toString(this.robot.Y));
		label[2] = new Label(
				Double.toString(this.robot.H));
		control_panel.add(label[0]);
		control_panel.add(label[1]);
		control_panel.add(label[2]);
		textPose[0] = new TextField();
		textPose[0].setText(String.format("%.2f", robot.X));
		control_panel.add(textPose[0]);
		textPose[0].addActionListener(this);
		textPose[1] = new TextField();
		textPose[1].setText(String.format("%.2f", robot.Y));
		control_panel.add(textPose[1]);
		textPose[1].addActionListener(this);
		textPose[2] = new TextField();
		textPose[2].setText(String.format("%.2f", robot.H));
		control_panel.add(textPose[2]);
		textPose[2].addActionListener(this);
		
		
		this.add(control_panel, BorderLayout.NORTH);
		
		if(this.robot!=null){
			this.updateThtread.start();
		}
		
		this.pack();
		//this.setVisible(this.visualization);
	}
}
