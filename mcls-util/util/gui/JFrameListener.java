package util.gui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import samcl.SAMCL;
import util.robot.RobotState;
@Deprecated
@SuppressWarnings("serial")
public class JFrameListener extends JFrame implements ActionListener, AdjustmentListener{
	SAMCL samcl;
	RobotState robot;
	JPanel control_panel = new JPanel(new GridLayout(3,3));
	Button[] B = new Button[9];
	Scrollbar scrollbar = new Scrollbar();
	float delta_energy;
	
	public String S[] = {
			/*0*/"",			/*1*/"Forward",	/*2*/"",
			/*3*/"TurnLeft",	/*4*/"Stop",	/*5*/"TurnRight",
			/*6*/"",			/*7*/"Backward",	/*8*/""
	};
	
	/**
	 * @param title
	 * @throws HeadlessException
	 */
	public JFrameListener(String title) throws HeadlessException {
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
		
		//set up scroll bar
		this.scrollbar.addAdjustmentListener(this);
		this.scrollbar.setValues(500, 100, 0, 1100);
		this.scrollbar.setOrientation(Scrollbar.HORIZONTAL);
		this.add(scrollbar, BorderLayout.SOUTH);
		
		this.pack();
		this.setVisible(true);
		
	}
	
	public JFrameListener(String title, RobotState robot, SAMCL samcl) {
		this(title);
		this.robot = robot;
		this.samcl = samcl;
		this.delta_energy = samcl.deltaEnergy;
	}
	

	@Override
	public void actionPerformed(ActionEvent arg0) {
		//System.out.println("action!!");
		Button btn = (Button) arg0.getSource();
		
		if(btn==B[1])//Forward
		{
			System.out.println("Forward");
			this.robot.setVt(this.robot.getVt() + 1);
		}
		else if(btn==B[7])//Backward
		{
			System.out.println("Backward");
			this.robot.setVt(this.robot.getVt() - 1);
		}
		else if(btn==B[3])//Turnleft
		{
			System.out.println("Turnleft");
			this.robot.setWt(this.robot.getWt() - 1);
		}
		else if(btn==B[5])//Turnright
		{
			System.out.println("Turnright");
			this.robot.setWt(this.robot.getWt() + 1);
		}
		else if(btn==B[4])//Stop
		{
			System.out.println("Stop");
			this.robot.setVt(0);
			this.robot.setWt(0);
		}
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		int value = scrollbar.getValue();
		System.out.println(value);
		float tune = converter(value);
		this.samcl.deltaEnergy = this.delta_energy + (tune-0.5f)*0.01f;
		//System.out.println("max:"+this.scrollbar.getMaximum()+",min:"+this.scrollbar.getMinimum());
		//System.out.println(converter(value)*0.01);
	}

	private float converter(int value) {
		return (float)value/(this.scrollbar.getMaximum()-this.scrollbar.getMinimum()-this.scrollbar.getVisibleAmount());
	}

}
