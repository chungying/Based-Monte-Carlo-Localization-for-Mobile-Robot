package util.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
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

public class VariablesController extends JFrame {
	public static final int DEFAULT_WIDTH = 350;
	public static final int DEFAULT_HEIGHT = 450;
	
	private double[] al;
	private List<ChangeListener> listeners;
	private List<JTextField> texts;
	private List<JSlider> sliders;
	private JPanel sliderPanel;
	class Listener implements ChangeListener{
		double[] al;
		int index;
		JTextField text;
		public Listener(JTextField text, double[] al, int index){
			this.text = text;
			this.al = al;
			this.index = index;
		}
		@Override
		public void stateChanged(ChangeEvent e) {
			JSlider slider = (JSlider)e.getSource();
			al[index] = ((double)slider.getValue());
			//al[index+1] = ((double)slider.getValue())/1000;
			text.setText(String.valueOf(al[index]));
		}
	}
	public VariablesController(double[] al){
		
		setTitle("SliderTest");
	    setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		
	    sliderPanel = new JPanel();
	    sliderPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	    
	    this.texts = new ArrayList<JTextField>();
	    this.listeners = new ArrayList<ChangeListener>();
	    this.sliders = new ArrayList<JSlider>();
	    
		this.al = al;
		for(int i = 0 ; i < al.length ; i++){
			final JTextField text = new JTextField(String.valueOf(al[i]),4);
			texts.add(text);
			listeners.add( new Listener(text, this.al,i));
		}
		
		
		Dictionary<Integer, Component> labelTable = new Hashtable<Integer, Component>();
		labelTable.put(0, new JLabel("0"));
		labelTable.put(50000, new JLabel("50000"));
//		labelTable.put(4000, new JLabel("4000"));
//		labelTable.put(6000, new JLabel("6000"));
//		labelTable.put(8000, new JLabel("8000"));
		labelTable.put(100000, new JLabel("100000"));
		JSlider slider;
		for(int i = 0 ; i<listeners.size(); i++){
			slider = new JSlider(0,100000,(int)al[i]);
			slider.setPaintLabels(true);
			slider.setPaintTicks(true);
			slider.setMajorTickSpacing(200);
			slider.setMinorTickSpacing(5);
			slider.setLabelTable(labelTable);
			slider.addChangeListener(listeners.get(i));
			sliders.add(slider);
			JPanel panel = new JPanel();
		    panel.add(slider);
			panel.add(new JLabel("alpha"+i));
			panel.add(texts.get(i));
			sliderPanel.add(panel);
		}
		
		add(sliderPanel,BorderLayout.CENTER);
		this.pack();
		this.setVisible(true);
	}




}
