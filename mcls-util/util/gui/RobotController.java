package util.gui;

import java.awt.Button;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;

import javax.swing.JFrame;
import javax.swing.JPanel;

import util.robot.RobotState;

public class RobotController extends JFrame implements Closeable, ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9182253906123273095L;
	private Button[] v1Buttons = new Button[8];
	private Label[] v1StatusLavel = new Label[2];
	private TextField[] v1TextFields = new TextField[5];
	private Label[] v1TextLabels = new Label[5];
	private final static String S1[] = {
			/*0*/"Pause/Continue",	
			/*1*/"Initialize",	
			/*2*/"Stop",		/*3*/"SetPose",
			/*4*/"Forward",		/*5*/"Backward",	
			/*6*/"TurnLeft",	/*7*/"TurnRight"
	};
	/* S1
	 * __________________
	 * |Button	|Label	|
	 * |Pause	|Status	|
	 * |________|_______|
	 * |Button	|Label	|
	 * |Initial	|Initial|
	 * |________|_______|
	 * |Button	|Button	|
	 * |Stop	|SetPose|
	 * |________|_______|
	 * |Button	|Button	|
	 * |Forward	|Back	|
	 * |________|_______|
	 * |Button	|Button	|
	 * |Left	|Right	|
	 * |________|_______|
	 * |Label	|Text	|
	 * |X		|		|
	 * |________|_______|
	 * |Label	|Text	|
	 * |Y		|		|
	 * |________|_______|
	 * |Label	|Text	|
	 * |H		|		|
	 * |________|_______|
	 * |Label	|Text	|
	 * |V		|		|
	 * |________|_______|
	 * |Label	|Text	|
	 * |W		|		|
	 * |________|_______|
	 *  
	 */	
	private void setupAllComponents(){
		controlPanel.setLayout(new GridLayout( 10, 2));

		String[] strs = RobotController.S1; 
		v1Buttons = new Button[strs.length];
		for (int i = 0; i < strs.length; i++) {
			v1Buttons[i] = new Button(strs[i]);
			v1Buttons[i].addActionListener(this);
		}
		
		v1StatusLavel[0] = new Label(
				String.format("%s", robot.isMotorLocked()?"Lock":"Unlock"));
		v1StatusLavel[1] = new Label(
				String.format("%.2f %.2f %.2f %.2f %.2f", 
						robot.initRobot.X,
						robot.initRobot.X,
						robot.initRobot.X,
						robot.initModel.velocity,
						robot.initModel.angular_velocity));
				
		v1TextLabels[0] = new Label(
				String.format("%.2f", robot.X));
		v1TextLabels[1] = new Label(
				String.format("%.2f", robot.Y));
		v1TextLabels[2] = new Label(
				String.format("%.2f", robot.H));
		v1TextLabels[3] = new Label(
				String.format("%.2f", robot.getVt()));
		v1TextLabels[4] = new Label(
				String.format("%.2f", robot.getWt()));
		
		v1TextFields[0] = new TextField();
		v1TextFields[0].setText(String.format("%.2f", robot.X));
		v1TextFields[0].addActionListener(this);
		v1TextFields[1] = new TextField();
		v1TextFields[1].setText(String.format("%.2f", robot.Y));
		v1TextFields[1].addActionListener(this);
		v1TextFields[2] = new TextField();
		v1TextFields[2].setText(String.format("%.2f", robot.H));
		v1TextFields[2].addActionListener(this);
		v1TextFields[3] = new TextField();
		v1TextFields[3].setText(String.valueOf(this.robot.getVt()));
		v1TextFields[3].addActionListener(this);
		v1TextFields[4] = new TextField();
		v1TextFields[4].setText(String.valueOf(this.robot.getWt()));
		v1TextFields[4].addActionListener(this);
		
		controlPanel.add(v1Buttons[0]);		controlPanel.add(v1StatusLavel[0]);
		controlPanel.add(v1Buttons[1]);		controlPanel.add(v1StatusLavel[1]);
		controlPanel.add(v1Buttons[2]);		controlPanel.add(v1Buttons[3]);
		controlPanel.add(v1Buttons[4]);		controlPanel.add(v1Buttons[5]);
		controlPanel.add(v1Buttons[6]);		controlPanel.add(v1Buttons[7]);
		controlPanel.add(v1TextLabels[0]);	controlPanel.add(v1TextFields[0]);
		controlPanel.add(v1TextLabels[1]);	controlPanel.add(v1TextFields[1]);
		controlPanel.add(v1TextLabels[2]);	controlPanel.add(v1TextFields[2]);
		controlPanel.add(v1TextLabels[3]);	controlPanel.add(v1TextFields[3]);
		controlPanel.add(v1TextLabels[4]);	controlPanel.add(v1TextFields[4]);
	}
	
	private void buttonActions(Button btn){
		if(btn==v1Buttons[0]){
			System.out.println();
			//Pause/Continue
			this.robot.reverseMotorLock();
		}
		else if(btn==v1Buttons[1]){
			//Initialize
			//System.out.println("Initialize robot");
			this.robot.robotStartOver();
		}
		else if(btn==v1Buttons[2]){
			//Stop
			//System.out.println("Stop");
			this.robot.setVt(0);
			this.robot.setWt(0);
			
		}
		else if(btn==v1Buttons[3]){
			//Set Initial Robot Pose
			//System.out.println("Set Initial Robot Pose");
			robot.setInitState(robot);
		}
		else if(btn==v1Buttons[4]){
			//Forward
			//System.out.println("Forward");
			this.robot.setVt(this.robot.getVt() + 1);
		}
		else if(btn==v1Buttons[5]){
			//Backward
			//System.out.println("Backward");
			this.robot.setVt(this.robot.getVt() - 1);
		}
		else if(btn==v1Buttons[6]){
			//Turnleft
			//System.out.println("Turnleft");
			this.robot.setWt(this.robot.getWt() - 1);
		}
		else if(btn==v1Buttons[7]){
			//Turnright
			//System.out.println("Turnright");
			this.robot.setWt(this.robot.getWt() + 1);
		}
	}
	
	private void textAction(TextField text){
		if(text==v1TextFields[0]){
			//System.out.println("update robot X");
			if(text.getText().length()!=0)
				this.robot.setX(Double.parseDouble(text.getText()));
		}
		else if(text==v1TextFields[1]){
			//System.out.println("update robot Y");
			if(text.getText().length()!=0)
				this.robot.setY(Double.parseDouble(text.getText()));
		}
		else if(text==v1TextFields[2]){
			//System.out.println("update robot Head");
			if(text.getText().length()!=0)
				this.robot.setHead(Double.parseDouble(text.getText()));
		}
		else if(text==v1TextFields[3]){
			//System.out.println("update velocity");
			if(text.getText().length()!=0)
				this.robot.setVt(Double.parseDouble(text.getText()));
		}
		else if(text==v1TextFields[4]){
			//System.out.println("update angular velocity");
			if(text.getText().length()!=0)
				this.robot.setWt(Double.parseDouble(text.getText()));
		}
		
	}
	
	private void labelUpdate(){
		if(!robot.isRobotLocked()){
			v1TextLabels[0].setText(
					String.format("%.2f", robot.X));
			v1TextLabels[1].setText(
					String.format("%.2f", robot.Y));
			v1TextLabels[2].setText(
					String.format("%.2f", robot.H));
			v1TextLabels[3].setText(
					String.format("%.2f", robot.getVt()));
			v1TextLabels[4].setText(
					String.format("%.2f", robot.getWt()));
			v1StatusLavel[0].setText(
					String.format("%s", robot.isMotorLocked()?"Lock":"Unlock"));
			v1StatusLavel[1].setText(
					String.format("%.2f %.2f %.2f %.2f %.2f", 
							robot.initRobot.X,
							robot.initRobot.Y,
							robot.initRobot.H,
							robot.initModel.velocity,
							robot.initModel.angular_velocity));
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object object = e.getSource();
		if(object instanceof Button){
			buttonActions((Button) object);
			
		}else if(object instanceof TextField){
			textAction((TextField) object);
		}
	}

	@Override
	public void close(){
		updateThtread.interrupt();
//		this.dispose();
	}

	Thread updateThtread = new Thread(){
		public boolean closing = false;
		@Override
		public void run() {
			try {
				while(!closing){
					labelUpdate();
					Thread.sleep(33);
				}
			} catch (InterruptedException e) {
//				e.printStackTrace();
			}
		}
	};
	
	private RobotState robot;
	private JPanel controlPanel = new JPanel();
	
	public RobotController(){
		super("robot controller");
	}
	public void setupRobot(RobotState robot){
		this.robot = robot;
		//set up contorl panel
		setupAllComponents();
		this.add(controlPanel);
		
		//execute monitor thread
		if(this.robot!=null){
			this.updateThtread.start();
		}
		
		this.pack();
		this.setVisible(true);
	}
}
