package util.gui;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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

import com.beust.jcommander.Parameter;

import samcl.SAMCL;


public class VariablesController extends JFrame {
	@Parameter(names = "--visualization", help = false)
	public boolean visualization;
	/**
	 * 
	 */
	private static final long serialVersionUID = -6900761245686028716L;


	private SAMCL mcl;
	
	public static final int DEFAULT_WIDTH = 350;
	public static final int DEFAULT_HEIGHT = 450;
	/*
	 * the first block is for sliders
	 * 	
	*/
	private double[] al;
	
	class SliderListener implements ChangeListener{
		JTextField text;
		double[] al;
		int index;
		public SliderListener(JTextField text, double[] al, int index){
			this.text = text;
			this.al = al;
			this.index = index;
		}
		@Override
		public void stateChanged(ChangeEvent e) {
			System.out.println("changed!!!");
			JSlider slider = (JSlider)e.getSource();
			al[index] = ((double)slider.getValue());
			text.setText(String.valueOf(al[index]));
		}
	}
	
	class TextFieldListener implements ActionListener{
		JSlider slider;
		double[] al;
		int index;
		public TextFieldListener(JSlider slider, double[] al, int index){
			this.slider = slider;
			this.al = al;
			this.index = index;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			System.out.println("action!!!");
			JTextField text = (JTextField)e.getSource();
			al[index] = Double.parseDouble(text.getText());
			slider.setValue((int)al[index]);
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
	
	public VariablesController(double[] inputAl){
		setTitle("Variables Controller with alpha only");
	    setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		
		
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
	    
	    this.al = inputAl;
	    
	    for(int i = 0 ; i < al.length ; i++){
			final JTextField text = new JTextField(String.valueOf(this.al[i]),4);
			texts.add(text);
			sliderlisteners.add( new SliderListener(text, this.al,i));
		}
	    int min = 0;
		int stage = 2;
		int max = 10000;
		Dictionary<Integer, Component> labelTable = new Hashtable<Integer, Component>();
		labelTable.put(min, new JLabel(String.valueOf(min)));
		labelTable.put(min+max/stage, new JLabel(String.valueOf(min+max/stage)));
		labelTable.put(max, new JLabel(String.valueOf(max)));
		JSlider slider;
		for(int i = 0 ; i<sliderlisteners.size(); i++){
			slider = new JSlider(min,max,(int)al[i]);
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
			
			textlisteners.add(new TextFieldListener(sliders.get(i),this.al,i));
			texts.get(i).addActionListener(textlisteners.get(i));
		}
		
		add(sliderPanel,BorderLayout.CENTER);
		
		this.pack();
		this.setVisible(this.visualization);
	}
	
	public VariablesController(SAMCL mcl){
		this(mcl.al);
		this.setVisible(false);
		setTitle("Variables Controller with all variables");
		
		
		this.mcl = mcl;
		//layout grid
		JPanel checkPanel = new JPanel();
		checkPanel.setLayout(new GridLayout(2,2));
		
		//add showing particles
		Checkbox checkParticles = new Checkbox("show particles",null,mcl.ifShowParticles);
		checkParticles.addItemListener(new ShowParticlesListener(this.mcl,checkParticles));
		checkPanel.add(checkParticles);
		//add showing SER
		Checkbox checkSER = new Checkbox("show SER",null,mcl.ifShowSER);
		checkSER.addItemListener(new ShowSERListener(this.mcl, checkSER));
		checkPanel.add(checkSER);
		
		//...
		PopulationListener pl = new PopulationListener(mcl, new TextField());
		checkPanel.add(pl);
		
		DeltaListener dl = new DeltaListener(mcl, new TextField());
		checkPanel.add(dl);
		//
		
		add(checkPanel,BorderLayout.SOUTH);
		this.pack();
		this.setVisible(this.visualization);
	}




}
