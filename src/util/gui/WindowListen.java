package util.gui;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

public class WindowListen implements WindowListener{
	public boolean isClosing; 
	
	public boolean isClosing() {
		return isClosing;
	}

	public JFrame frm;
	/**
	 * @param isClosing
	 */
	public WindowListen(JFrame frm, boolean isClosing) {
		super();
		this.isClosing = isClosing;
		this.frm = frm;
	}

	@Override
	public void windowActivated(WindowEvent e) {
		
	}

	@Override
	public void windowClosed(WindowEvent e) {

		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		this.frm.dispose();
		this.isClosing = true;
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		
	}

	@Override
	public void windowOpened(WindowEvent e) {
		
	}

}
