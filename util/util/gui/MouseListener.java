/**
 * 
 */
package util.gui;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.ImageIcon;

/**
 * @author w514
 *
 */
@SuppressWarnings("serial")
public class MouseListener extends ImageIcon {
	
	/**
	 * @param image
	 */
	@SuppressWarnings("unused")
	public MouseListener(Image image) {
		super(image);
		MouseMotionListener mouse = new MouseMotionListener(){

			@Override
			public void mouseDragged(MouseEvent e) {
				
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				
			}
			
		};
		
		
		
	}	
}
