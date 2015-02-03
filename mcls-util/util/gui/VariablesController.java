package util.gui;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
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

public class VariablesController extends JFrame {
	
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
		this.setVisible(true);
	}
	
	public VariablesController(SAMCL mcl){
		this(mcl.al);
		this.setVisible(false);
		setTitle("Variables Controller with all variables");
		
		
		this.mcl = mcl;
		
		JPanel checkPanel = new JPanel();
		checkPanel.setLayout(new GridLayout(1,2));
		
		Checkbox checkParticles = new Checkbox("show particles",null,mcl.ifShowParticles);
		checkParticles.addItemListener(new ShowParticlesListener(this.mcl,checkParticles));
		checkPanel.add(checkParticles);
		
		Checkbox checkSER = new Checkbox("show SER",null,mcl.ifShowSER);
		checkSER.addItemListener(new ShowSERListener(this.mcl, checkSER));
		checkPanel.add(checkSER);
		
		add(checkPanel,BorderLayout.SOUTH);
		
		this.pack();
		this.setVisible(true);
	}




}
