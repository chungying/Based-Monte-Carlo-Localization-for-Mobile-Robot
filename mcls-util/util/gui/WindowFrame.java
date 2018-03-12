package util.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import util.grid.Grid;
//TODO implement Window.class with Runnable interface in order to independently monitoring.
public class WindowFrame extends JFrame implements Runnable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public List<Closeable> closeList = new ArrayList<Closeable>();
	private CloseManager manager = null;
	public WindowFrame(String name, Grid grid){
		super(name);
		this.setSize(grid.width, grid.height);
		manager = new CloseManager(grid);
		this.addWindowListener(manager);
	}
	
	public void setupCloseList(Closeable... closeObjs){
		manager.setupCloseList(closeObjs);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}	
	
	public class CloseManager extends WindowAdapter{
		
		public List<Closeable> closeList = new ArrayList<Closeable>();
		Grid grid = null;
		CloseManager(Grid grid){
			this.grid = grid;
		}
		
		public void setupCloseList(Closeable... closeObjs){
			for(Closeable o: closeObjs){
				this.closeList.add(o);
			}
		}
		
		@Override
		public void windowClosing(WindowEvent e) {
			System.out.println("window frame closing");
			try {
				for(Object o:closeList){
					if(o!=null){
						((Closeable)o).close();
					}
				}
				this.grid.windowClosedFlag = true;
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		
	}

	
	
}
