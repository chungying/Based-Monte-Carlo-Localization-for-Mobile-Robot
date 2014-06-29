package util.gui;

import java.awt.BorderLayout;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JFrame;

import samcl.SAMCL;

@SuppressWarnings("serial")
public class SamclListener extends JFrame implements  AdjustmentListener{
	/**
	 * 
	 */
	Scrollbar scrollbar = new Scrollbar();
	SAMCL samcl;
	float delta_energy;
	
	public SamclListener(String title,  SAMCL samcl) {
		this(title);;
		this.samcl = samcl;
		this.delta_energy = samcl.delta_energy;
	}
	
	
	public SamclListener(String title) {
		super(title);
		BorderLayout boarder = new BorderLayout(3,3);
		this.setLayout(boarder);
		
		//set up scroll bar
		this.scrollbar.addAdjustmentListener(this);
		this.scrollbar.setValues(500, 100, 0, 1100);
		this.scrollbar.setOrientation(Scrollbar.HORIZONTAL);
		this.add(scrollbar, BorderLayout.SOUTH);
		
		this.setSize(300, 50);
		
		this.setVisible(true);
		
	}


	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		int value = scrollbar.getValue();
		System.out.println(value);
		float tune = converter(value);
		this.samcl.delta_energy = this.delta_energy + (tune-0.5f)*0.01f;
		//System.out.println("max:"+this.scrollbar.getMaximum()+",min:"+this.scrollbar.getMinimum());
		//System.out.println(converter(value)*0.01);
	}

	private float converter(int value) {
		return (float)value/(this.scrollbar.getMaximum()-this.scrollbar.getMinimum()-this.scrollbar.getVisibleAmount());
	}
}
