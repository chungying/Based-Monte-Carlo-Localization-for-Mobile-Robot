package util.gui;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
	// ...
@SuppressWarnings("serial")
public class ImagePanel extends JPanel{
	public BufferedImage img;
	
	/**
	 * 
	 */
	public ImagePanel(BufferedImage image) {
		super();
		this.img = image;
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(img, 0, 0, null);

	}

}
