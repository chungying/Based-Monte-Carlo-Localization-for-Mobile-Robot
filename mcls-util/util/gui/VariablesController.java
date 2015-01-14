package util.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
	private List<ActionListener> textlisteners;
	private List<ChangeListener> sliderlisteners;
	private List<JTextField> texts;
	private List<JSlider> sliders;
	private JPanel sliderPanel;
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
	
	public VariablesController(double[] al){
		
		setTitle("SliderTest");
	    setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		
	    sliderPanel = new JPanel();
	    sliderPanel.setLayout(new GridLayout(3,2));

	    this.textlisteners = new ArrayList<ActionListener>();
	    this.texts = new ArrayList<JTextField>();
	    this.sliderlisteners = new ArrayList<ChangeListener>();
	    this.sliders = new ArrayList<JSlider>();
	    
		this.al = al;
		for(int i = 0 ; i < al.length ; i++){
			final JTextField text = new JTextField(String.valueOf(al[i]),4);
			texts.add(text);
			sliderlisteners.add( new SliderListener(text, this.al,i));
		}
		
		
		Dictionary<Integer, Component> labelTable = new Hashtable<Integer, Component>();
		labelTable.put(0, new JLabel("0"));
		labelTable.put(50000, new JLabel("50000"));
//		labelTable.put(4000, new JLabel("4000"));
//		labelTable.put(6000, new JLabel("6000"));
//		labelTable.put(8000, new JLabel("8000"));
		labelTable.put(100000, new JLabel("100000"));
		JSlider slider;
		for(int i = 0 ; i<sliderlisteners.size(); i++){
			slider = new JSlider(0,100000,(int)al[i]);
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
		}
		
		for(int i = 0 ; i < al.length;i++){
			textlisteners.add(new TextFieldListener(sliders.get(i),this.al,i));
			texts.get(i).addActionListener(textlisteners.get(i));
		}
		
		add(sliderPanel,BorderLayout.CENTER);
		this.pack();
		this.setVisible(true);
	}




}
