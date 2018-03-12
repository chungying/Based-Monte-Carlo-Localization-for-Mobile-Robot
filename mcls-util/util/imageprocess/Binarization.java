package util.imageprocess;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import util.gui.ImagePanel;

public class Binarization {
	BufferedImage map_image;
	String map_filename = "";
	String folder = "";
	JFrame frame;
	Graphics2D grap;
	int black = 255;
	int red = 255;
	int green = 255;
	int blue = 255;
	ExtendedMouseMotionListener mml;
	class ExtendedMouseMotionListener implements MouseListener,MouseMotionListener{
		public ExtendedMouseMotionListener(ImagePanel panel, BufferedImage map_image) {
			this.panel = panel;
			this.map_image = map_image;
		}
		ImagePanel panel;
		BufferedImage map_image;
		@Override
		public void mouseDragged(MouseEvent e) {
			
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			int color_val = map_image.getRGB(e.getX(), e.getY());
			System.out.print("MouseListener is moving at: ["+ e.getX()+", "+e.getY()+"]. ");
			System.out.print("sRGB is "+color_val+". ");
			
			System.out.println("aRGB:["+(int)((color_val>>24)&0xFF)+" "+
										(int)((color_val>>16)&0xFF)+" "+
										(int)((color_val>>8)&0xFF)+" "+
										(int)((color_val)&0xFF)+"].");
			
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			
			
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			Graphics2D grap = map_image.createGraphics();
			grap.drawImage(map_image, null, 0, 0);
			grap.setColor(Color.BLACK);
			grap.drawOval(e.getX(), e.getY(), 1, 1);
			panel.repaint();
			
		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}
	}
	class ExtendedWindowAdapter extends WindowAdapter{
		private String folder;
		BufferedImage map_image;
		public ExtendedWindowAdapter(String folder, BufferedImage map_image){
			this.folder = folder;
			this.map_image = map_image;
		}
		@Override
		public void windowClosing(WindowEvent e) {
			try {
				ImageIO.write(map_image, "BMP", new File(this.folder+"filename.bmp"));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			super.windowClosing(e);
		}
	}
	
	private void addOne(HashMap<String,Integer> map, String str){
		if(map.get(str)==null){
			map.put(str, 1);
		}else{
			map.put(str, map.get(str)+1);
		}
	}
	
	public Binarization(String folder, String filename){
		try {
			this.map_filename = filename;
			this.folder = folder;
			this.map_image = ImageIO.read(new URL("file://"+folder+map_filename));
			
			HashMap<String,Integer> mapA = new HashMap<String,Integer>();
			HashMap<String,Integer> mapR = new HashMap<String,Integer>();
			HashMap<String,Integer> mapG = new HashMap<String,Integer>();
			HashMap<String,Integer> mapB = new HashMap<String,Integer>();
			int countD = 0;

			black = black <<24;
			red = red<<8;
			red+=255;
			red = red<<16;
			green = green<<16;
			green+=255;
			green = green<<8;
			blue = blue<<24;
			blue+=255;
			for(int x = 0 ; x<map_image.getWidth();x++){
				for(int y = 0 ; y < map_image.getHeight();y++){
					int rgb = map_image.getRGB(x, y);
					int a = (int)((rgb>>24)&0xFF);
					int r = (int)((rgb>>16)&0xFF);
					int g = (int)((rgb>>8)&0xFF);
					int b = (int)((rgb)&0xFF);
					addOne(mapA,String.valueOf(a));
					addOne(mapR,String.valueOf(r));
					addOne(mapG,String.valueOf(g));
					addOne(mapB,String.valueOf(b));
					if(r!=g || r!=b || g!=b){
						countD++;
					}
					
					grap = map_image.createGraphics(); 
					
					if(r==0&&g==0&&b==0){//black
						//map_image.setRGB(x, y, new Color(127,127,127).getRGB());
					}else if(r==255&&g==255&&b==255){//white
						//map_image.setRGB(x, y, red);
					}else if(r>127&&g<127&&b<127){//red
						//map_image.setRGB(x, y, new Color(255,255,255).getRGB());
					}else if(r<127&&g>127&&b<127){//green
						//map_image.setRGB(x, y, new Color(255,255,255).getRGB());
					}else if(r<127&&g<127&&b>127){//blue
						//map_image.setRGB(x, y, new Color(0,0,0).getRGB());
					}else if(r>127||g>127||b>127){//darker gray
						map_image.setRGB(x, y, new Color(0,0,0).getRGB());
					}else{//normal gray
						map_image.setRGB(x, y, new Color(127,127,127).getRGB());
					}
						
				}
			}
			System.out.println("Total: "+map_image.getWidth()*map_image.getHeight()+" pixels.");
			System.out.println("Total: "+mapA.size()+" different A(s)");
			System.out.println("Total: "+countD+" color pixels.");
			System.out.println("Total: "+mapR.size()+" red values among 256.");
			System.out.println("Total: "+mapG.size()+" green values among 256.");
			System.out.println("Total: "+mapB.size()+" blue values among 256.");
			System.out.println("Total: "+mapR.get(String.valueOf(0))+" black(0) pixels.");
			System.out.println("Total: "+mapR.get(String.valueOf(127))+" grey(127) pixels.");
			System.out.println("Total: "+mapR.get(String.valueOf(254))+" white(254) pixels.");
			System.out.println("mapR");
			for(Entry<String, Integer> en:mapR.entrySet()){
				System.out.println(en.getKey()+", "+en.getValue());
			}
			System.out.println("mapG");
			for(Entry<String, Integer> en:mapG.entrySet()){
				System.out.println(en.getKey()+", "+en.getValue());
			}
			System.out.println("mapB");
			for(Entry<String, Integer> en:mapB.entrySet()){
				System.out.println(en.getKey()+", "+en.getValue());
			}
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//Frame
		this.frame = new JFrame();
		frame.setSize(map_image.getWidth(), map_image.getHeight());
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		ImagePanel panel = new ImagePanel(map_image);
		frame.getContentPane().add(panel);
		mml = new ExtendedMouseMotionListener(panel, this.map_image);
		frame.addMouseMotionListener(mml);
		frame.addMouseListener(mml);
		frame.addWindowListener(new ExtendedWindowAdapter(this.folder,this.map_image));
		frame.setVisible(true);
	}
	

	@SuppressWarnings("unused")
	public static void main(String[] args){
		Binarization bin = new Binarization("/Users/Jolly/workspace/dataset/dataset_fr079/maps/generated_metric_maps/","filename2.bmp");
		
	}
}
