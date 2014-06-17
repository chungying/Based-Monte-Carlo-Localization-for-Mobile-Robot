package util.gui;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
	// ...
@SuppressWarnings("serial")
public class Panel extends JPanel{
	BufferedImage img;
	
	/**
	 * 
	 */
	public Panel(BufferedImage image) {
		super();
		this.img = image;
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(img, 0, 0, null);

	}

}
