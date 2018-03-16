package util.gui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import samcl.SAMCL;
import util.pf.sensor.laser.LaserModel;
import util.pf.sensor.odom.callbackfunc.MCLMotionModel;


public class VariablesController extends JFrame implements Closeable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6900761245686028716L;

	private SAMCL mcl;
	
	public static final int DEFAULT_WIDTH = 350;
	public static final int DEFAULT_HEIGHT = 450;

	private MCLMotionModel odomModel;
	class AlphaSliderListener implements ChangeListener{
		JTextField text;
		MCLMotionModel odomModel;
		int index;
		public AlphaSliderListener(JTextField text, MCLMotionModel odomModel, int index){
			this.text = text;
			this.odomModel = odomModel;
			this.index = index;
		}
		@Override
		public void stateChanged(ChangeEvent e) {
			System.out.println("changed!!!");
			JSlider slider = (JSlider)e.getSource();
			odomModel.setAlpha(index,((double)slider.getValue()));
			text.setText(String.valueOf(odomModel.getAlpha(index)));
		}
	}
	
	String[] sliderNames = {"zhit", "zshort", "zmax", "zrand", "sigmahit", "lamdashort"};
	/**
	 * 
	 */
	class ZSliderListener implements ChangeListener{
		LaserModel laserModel;
		int idx;
		public ZSliderListener(LaserModel model, int idx){
			this.laserModel = model;
			this.idx = idx;
		}
		@Override
		public void stateChanged(ChangeEvent e) {
			JSlider slider = (JSlider)e.getSource();
			float v = (float)slider.getValue()/(slider.getMaximum() - slider.getMinimum());
			switch(this.idx){
			case 0:
//				this.laserModel.z_hit = v;
				break;
			case 1:
//				this.laserModel.z_short = v;
				break;
			case 2:
//				this.laserModel.z_max = v;
				break;
			case 3:
//				this.laserModel.z_rand = v;
				break;
			case 4:
				break;
			case 5:
				break;
			}
			
		}
		
	}
	
	class TextFieldListener implements ActionListener{
		JSlider slider;
		MCLMotionModel odomModel;
		int index;
		public TextFieldListener(JSlider slider, MCLMotionModel odomModel, int index){
			this.slider = slider;
			this.odomModel = odomModel;
			this.index = index;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			System.out.println("action!!!");
			JTextField text = (JTextField)e.getSource();
			odomModel.setAlpha(index,Double.parseDouble(text.getText()));
			slider.setValue((int)odomModel.getAlpha(index));
		}
		
	}
	
	/*
	 * the second block is for check boxes
	 * */
	
	class ShowParticlesListener implements ItemListener{
		int index;
		Checkbox check;
		SAMCL mcl;
		@Override
		public void itemStateChanged(ItemEvent e) {
			this.mcl.ifShowParticles = check.getState();
		}
		
		ShowParticlesListener(SAMCL mcl, Checkbox check){
			this.mcl = mcl;
			this.check = check;
		}
		
	}
	
	class ShowSERListener implements ItemListener{
		int index;
		Checkbox check;
		SAMCL mcl;
		@Override
		public void itemStateChanged(ItemEvent e) {
			this.mcl.ifShowSER = check.getState();
			
		}
		
		ShowSERListener(SAMCL mcl, Checkbox check){
			this.mcl = mcl;
			this.check = check;
		}
		
	}
	
	@SuppressWarnings("serial")
	class PopulationListener extends JPanel implements ActionListener{
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Object obj = e.getSource();
			if(obj instanceof TextField){
				TextField tf = (TextField)obj;
				int nt = Integer.parseInt(tf.getText());
				if(nt>0 && nt <=10000){
					mcl.Nt = nt;
					this.lb2.setText(String.valueOf(mcl.Nt));
					System.out.println("Population is set to\t"+nt);
				}else{
					tf.setText(String.valueOf(mcl.Nt));
				}
			}
		}

		private SAMCL mcl;
		TextField tf;
		Label lb;
		Label lb2;
		public PopulationListener(SAMCL mcl, TextField tf){
			super();
			this.mcl = mcl;
			
			this.setLayout(new GridLayout(1,3));
			
			this.lb = new Label("particle NO.:");
			this.add(lb);
			this.lb2 = new Label(String.valueOf(mcl.Nt));
			this.add(lb2);
			this.tf = tf;
			this.tf.addActionListener(this);
			this.add(tf);
		}
		
	}
	
	@SuppressWarnings("serial")
	class DeltaListener extends JPanel implements ActionListener{
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Object obj = e.getSource();
			if(obj instanceof TextField){
				TextField tf = (TextField)obj;
				float delta = Float.parseFloat(tf.getText());
				if(delta>0 && delta <=1.0f){
					mcl.deltaEnergy = delta;
					this.lb2.setText(String.format("%.6f",delta));
					System.out.println("Energy delta is set to\t"+delta);
				}else{
					tf.setText(String.format("%.6f",mcl.deltaEnergy));
				}
			}
		}

		private SAMCL mcl;
		TextField tf;
		Label lb;
		Label lb2;
		public DeltaListener(SAMCL mcl, TextField tf){
			super();
			this.mcl = mcl;
			
			this.setLayout(new GridLayout(1,3));
			
			this.lb = new Label("delta:");
			this.add(lb);
			this.lb2 = new Label(String.format("%.6f",mcl.deltaEnergy));
			this.add(lb2);
			this.tf = tf;
			this.tf.addActionListener(this);
			this.add(tf);
		}
		
	}
	
	class ButtonListener extends JPanel implements ActionListener{
		/**
		 * 
		 */
		private static final long serialVersionUID = 2523953561425125807L;
		private SAMCL samcl = null;
		public final String buttonString[] = {
				/*0*/"Converge", /*1*/"Restart localization", /*2*/"Global Localization"
		};
		
		private static final int itemCount = 3;
		
		Button[] buttons = new Button[itemCount];
		
		ButtonListener(SAMCL samcl){
			super();
			this.samcl = samcl;
			this.setLayout(new GridLayout(1,itemCount));
			for(int i = 0 ; i < buttons.length ; i++){
				buttons[i] = new Button(buttonString[i]);
				this.add(buttons[i]);
				buttons[i].addActionListener(this);
			}
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Object object = e.getSource();
			if(object instanceof Button){
				Button btn = (Button) object;
				if(btn==buttons[0]){
					///*0*/"Converge"
					if(samcl!=null)
						samcl.forceConverge();
				}
				else if(btn==buttons[1]){
					///*1*/"Restart localization"
					if(samcl!=null)
						this.samcl.setTerminating(true);
				}
				else if(btn==buttons[2]){
					///*2*/"Global Localization"
					if(samcl!=null)
						System.out.println("TODO Global localization button");
				}
			}
			
		}
		
	}
	
	public VariablesController(){
		this.setVisible(false);
	}
	
	private JPanel setupAlphaVariables(){
		this.odomModel = mcl.odomModel;
	    
		List<ActionListener> textlisteners;
		List<ChangeListener> sliderlisteners;
		List<JTextField> texts;
		List<JSlider> sliders;
		JPanel sliderPanel;
		sliderPanel = new JPanel();
		sliderPanel.setLayout(new GridLayout(3,2));
		textlisteners = new ArrayList<ActionListener>();
		texts = new ArrayList<JTextField>();
		sliderlisteners = new ArrayList<ChangeListener>();
		sliders = new ArrayList<JSlider>();
	    
		for(int i = 0 ; i < 6; i++){
			final JTextField text = new JTextField(String.valueOf(this.odomModel.getAlpha(i)),4);
			texts.add(text);
			sliderlisteners.add( new AlphaSliderListener(text, this.odomModel, i));
		}
		int min = 0;
		int stage = 2;
		int max = 100;
		Dictionary<Integer, Component> labelTable = new Hashtable<Integer, Component>();
		labelTable.put(min, new JLabel(String.valueOf(min)));
		labelTable.put(min+max/stage, new JLabel(String.valueOf(min+max/stage)));
		labelTable.put(max, new JLabel(String.valueOf(max)));
		JSlider slider;
		for(int i = 0 ; i<sliderlisteners.size(); i++){
			slider = new JSlider(min,max,(int)odomModel.getAlpha(i));
			slider.setPaintLabels(true);
			slider.setPaintTicks(true);
			slider.setMajorTickSpacing(200);
			slider.setMinorTickSpacing(5);
			slider.setLabelTable(labelTable);
			slider.addChangeListener(sliderlisteners.get(i));
			sliders.add(slider);
			JPanel panel = new JPanel();
		    panel.add(slider);
			panel.add(new JLabel("alpha"+i));
			panel.add(texts.get(i));
			sliderPanel.add(panel);
			textlisteners.add(new TextFieldListener(sliders.get(i), this.odomModel, i));
			texts.get(i).addActionListener(textlisteners.get(i));
		}
		
		return sliderPanel;
	}

	private JPanel setupCheckBoxes(){
		//layout grid
		JPanel checkPanel = new JPanel();
		checkPanel.setLayout(new GridLayout(2,2));
		
		//add showing particles
		Checkbox checkParticles = new Checkbox("show particles",null,mcl.ifShowParticles);
		checkParticles.addItemListener(new ShowParticlesListener(mcl,checkParticles));
		checkPanel.add(checkParticles);
		//add showing SER
		Checkbox checkSER = new Checkbox("show SER",null,mcl.ifShowSER);
		checkSER.addItemListener(new ShowSERListener(mcl, checkSER));
		checkPanel.add(checkSER);
		
		//...
		PopulationListener pl = new PopulationListener(mcl, new TextField());
		checkPanel.add(pl);
		
		DeltaListener dl = new DeltaListener(mcl, new TextField());
		checkPanel.add(dl);
		//
		return checkPanel;
		
	}
	
	public void setInstance(SAMCL mcl){
		this.setVisible(false);
		this.mcl = mcl;
		setTitle("Variables Controller with all variables");
		//setting north layout
		add(setupAlphaVariables(),BorderLayout.NORTH);
		
		//setting center layout
		add(setupCheckBoxes(),BorderLayout.CENTER);
		
		//setting south layout
		add(new ButtonListener(mcl), BorderLayout.SOUTH);
		
		this.pack();
	}

	@Override
	public void close(){
//		this.dispose();
	}
}
