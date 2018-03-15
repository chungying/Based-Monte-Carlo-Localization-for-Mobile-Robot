package util.imageprocess;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.swing.JFrame;

import org.apache.hadoop.hbase.util.Bytes;
/**
 * Thanks for jxue providing this class online.
 * https://john.cs.olemiss.edu/~jxue/teaching/csci112_S11/notes/hw1/PgmImage.java
 * 
 * This class handles simple processing for PGM images 
 * The codes can load and display a gray-scale image in PGM format
 * It also contains an implemented image processing method which 
 *   flips the image horizontally.
 *  
 *  @author jxue
 *  @version 0.2011-1-24
 *  
 */
public class PgmImage extends Component {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// image buffer for graphical display
	public BufferedImage img;
	// image buffer for plain gray-scale pixel values
	private int[][] pixels;
	
	// translating raw gray scale pixel values to buffered image for display
	private void pix2img(){
		int g;
		img = new BufferedImage( pixels[0].length, pixels.length, BufferedImage.TYPE_INT_ARGB );
		// copy the pixels values
		for(int row=0; row<pixels.length; ++row)
			for(int col=0; col<pixels[row].length; ++col){
				g = pixels[row][col];
				img.setRGB(col, row, ((255<<24) | (g << 16) | (g <<8) | g));		
			}
	}

	// default constructor with a 3 by 4 image
	public PgmImage(){
		int[][] defaultPixels = {{0, 1, 2, 3}, {4, 5, 6, 7},{8, 9, 10, 11}};
		pixels = new int[defaultPixels.length][defaultPixels[0].length];
		for(int row=0; row < pixels.length; ++row)
			for(int col=0; col < pixels[0].length; ++col)
				pixels[row][col] = (int)(defaultPixels[row][col] * 255.0/12);
		pix2img();
	}
	
	// constructor that loads pgm image from a file
	public PgmImage(String filename) {
		this(filename, "");
	}
	
	public PgmImage(String filename, String outputFile) {
		pixels = null;
		DataInputStream dis=null;
		try {
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
			readPGM(dis);
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if(dis!=null)
				try {
					dis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		
		if(outputFile.length()>0)
			savePGM(outputFile);
	}
	
	public PgmImage(DataInputStream dis){
		if(dis != null)
			readPGM(dis);
	}

	ArrayList<Byte> bytes = new ArrayList<Byte>();
	int headerSize;
	Rectangle rect = new Rectangle(0,0,0,0);
	int cols,rows, maxValue;
	private void savePGM(String filename) {
		try{
			HashMap<Integer,Integer> histogram = new HashMap<Integer,Integer>();
			int blackThresh= (int)Math.round((1-0.65)*maxValue);
			for(int row = 0 ; row < rows ; row++){
				for(int col = 0 ; col < cols ; col++){
					int index = row*cols+col + headerSize;
					int g = bytes.get(index) & 0xFF;
					if(g<blackThresh ){
						if((col-rect.x+1)>rect.width){
							rect.width = col-rect.x+1;
						}
						if((row-rect.y+1)>rect.height){
							rect.height = row-rect.y+1;
						}
					}
					if(histogram.get(g)==null)
						histogram.put(g, 0);
					histogram.put(g, histogram.get(g)+1);
				}
			}
			
			//System.out.println("histogram info");
			//for(Entry<Integer,Integer> entry : histogram.entrySet())
			//	System.out.println(entry.getKey() + " -> " + entry.getValue());
			//System.out.println(rect.toString());

			//output the data in the rectangle into a pgm file.
			FileOutputStream fis = new FileOutputStream(filename);
	    	BufferedOutputStream bis = new BufferedOutputStream(fis);
	    	DataOutputStream dis = new DataOutputStream(bis);
	    	int idx = 0;
	    	for(int lines = 1 ; lines <=4 ; lines++){
	    		//copy 1st line of magic number 
	    		//copy 2nd line of comment
	    		//3rd line is width and height.
	    		//4th line is as same as the original.
	    		for(;;){
		    		int b = bytes.get(idx)&0xFF;
		    		if(lines !=3)
		    			dis.writeByte(b);
		    		idx++;
		    		if(b==10)//if b is newLine.
		    			break;
		    	}
	    		if(lines ==3)
	    			dis.writeBytes(String.valueOf(rect.width) + " " + String.valueOf(rect.height) + "\n");
	    	}
	    	//System.out.println("byte size of meta data:" + idx);
	    	for(int Y = 0; Y < (rect.height); Y++)
	    		for(int X = 0; X < (rect.width); X++){
	    			int index = (rect.y+Y)*cols+rect.x + X + headerSize;
	    			dis.writeByte(bytes.get(index));
	    		}
	    	dis.close();
	    	//System.out.println("the image is cut.");
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	// load gray scale pixel values from a PGM format image
	public void readPGM(DataInputStream dis){
		
		try {
			// process the top 4 header lines
	   	   	String newline = System.getProperty("line.separator");
	   	   	ArrayList<Byte> linebytes = new ArrayList<Byte>();
	   	   	int lineCount = 0;
	   	   	String[] lines = new String[4];
	   	   	headerSize = 0;
	   	   	for(;;){
	   	   		Byte b = dis.readByte();
	   	   		headerSize++;
	   	   		this.bytes.add(b);
	   	   		String ch = new String(new byte[] {b});
	   	   		if(ch.contains(newline) || ch.contains("\n")){
	   	   			byte[] bs = new byte[linebytes.size()];
	   	   			int j = 0;
	   	   			for(Byte tempb: linebytes)
	   	   				bs[j++] = tempb.byteValue();
	   	   			linebytes.clear();
	   	   			lines[lineCount++] = new String(bs);
	   	   		}
	   	   		else
	   	   			linebytes.add(b);
	   	   		if(lineCount>3){
	   	   			//System.out.println("stop reading header at the forth line.");
	   	   			break;
	   	   		}
	   	   	}
	   	   	String filetype = lines[0];
	   	   	cols = Integer.valueOf(lines[2].split(" ")[0]);
	   	   	rows = Integer.valueOf(lines[2].split(" ")[1]);
	   	   	maxValue = Integer.valueOf(lines[3]);
	   	   	//System.out.println("PGM file type:" + lines[0] + ",cols: " +cols + ", rows: " + rows + ", maxValues: " + maxValue);
	   	   	//System.out.println("comment is : " + lines[1]);
	   	   	
	   	   	/**
	   	   	 * it is able to read P5 and P2 format
	   	   	 */
	   	   	if(filetype.equalsIgnoreCase("p5")){
		    	//this.bytes.clear();
		    	int total = cols*rows;
		    	
		    	try{
		    		int progress = total/100;
		    		for(;;){
		    			for(int count = 0 ; count < progress; count++){
		    				byte b = dis.readByte();
		    				this.bytes.add(b);
		    			}
		    			int p = (int) Math.round((double)this.bytes.size()/(double)total*100.0);
		    			//if(p%10==0)
		    			//	System.out.println("Has read " + p + "%");
	    			}
		    	}catch(EOFException e){
		    	}
		    	
		    	int whiteThresh= (int)Math.round((1-0.196)*maxValue);
		    	int blackThresh= (int)Math.round((1-0.65)*maxValue);
				img = new BufferedImage( cols, rows, BufferedImage.TYPE_INT_ARGB );
				rect.setLocation(cols, rows);;
				// copy the pixels values
				int pastP = -1;
				int index = 0;
				for(int row=0; row<rows; row++){
					for(int col=0; col<cols; col++){
						index = row*cols+col + headerSize;
						Byte b = this.bytes.get(index);
						int g = b & 0xFF;
						assert(g<256 && g>=0);
						if(g<blackThresh){
							img.setRGB(col, row, Color.BLACK.getRGB());
							if(col<rect.x){
								rect.x = col;
							}
							if(row<rect.y){
								rect.y = row;
							}

						}else if(g>whiteThresh){
							img.setRGB(col, row, Color.WHITE.getRGB());
						}else{
							img.setRGB(col, row, Color.GRAY.getRGB());
						}
						
					}
					int p = (int) Math.round((double)(index)/(double)bytes.size()*100.0);
	    			int pp = p%10;
					if(pp==0 && p!=pastP){
	    				//System.out.println("Has converted " + p + "% of PMG into BuffuredImage");
	    				pastP = p;
	    			}
					
				}

		    }else if(filetype.equalsIgnoreCase("p2")){
		    	Scanner infile = new Scanner(dis);
		    	pixels = new int[rows][cols];	   	       
		   	   	// process the rest lines that hold the actual pixel values
		   	   	for (int r=0; r<rows; r++) 
		   	   		for (int c=0; c<cols; c++)
		   	   			pixels[r][c] = (int)(infile.nextInt()*255.0/maxValue);
		   	   	infile.close();
				if (pixels != null)
					pix2img();
		    }else{
		    	System.out.println("[readPGM]Cannot load the image type of "+filetype);
		    }        
	   	   	
	    } catch (Exception e) {
	    	System.out.println(e.toString() + " caught in readPPM.");
	    	e.printStackTrace();
	    }

	}
	// overrides the paint method of Component class
	public void paint(Graphics g) {
		// simply draw the buffered image
		g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
	}
	
	/**
	 * This main will load and process a pgm image, and display the result.
	 * @param args
	 */
	public static void main(String[] args) {
		// instantiate the PgmImage object according to the 
		//  command line argument
		PgmImage img;
		String filename ="default";
		String outputFile = "output.pgm";
		if (args.length==1){
			filename = args[0];
			img = new PgmImage(filename);
		}else if(args.length==2){
			filename = args[0];
			outputFile = args[1];
			img = new PgmImage(filename, outputFile);
		}
		else { 
			img = new PgmImage();
			filename = "default";
		}

		// set up the GUI for display the PgmImage object 
		JFrame f = new JFrame("PGM Image: "+filename);
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		f.add(img);
		f.pack();
		f.setVisible(true);
	}
}
