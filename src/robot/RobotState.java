package robot;

import java.io.IOException;
import java.util.Arrays;

import samcl.Grid;
import util.gui.RobotListener;
import util.metrics.Transformer;

public class RobotState implements Runnable{
	private double x;
	private double y;
	private double head;
	private double Vt;
	private double Wt;
	private float[] measurements;
	
	private boolean onCloud;
	private Grid grid = null;
	

	public RobotState(int x, int y, double head) {
		super();
		System.out.println("initial robot");
		this.x = x;
		this.y = y;
		this.head = Transformer.checkHeadRange(head);
		Vt = 0;
		Wt = 0;
		
		
		//TODO setup Grid and onCloud?
		this.grid = null;
	}
	
	/**
	 * @param x
	 * @param y
	 * @param head
	 * @param vt
	 * @param wt
	 */
	public RobotState(int x, int y, double head, Grid grid) {
		super();
		System.out.println("initial robot");
		this.x = x;
		this.y = y;
		this.head = Transformer.checkHeadRange(head);
		Vt = 0;
		Wt = 0;
		
		//TODO setup Grid and onCloud?
		this.grid = grid;
	}
	
	public void update(double t) throws IOException{
		this.x = this.x +  ( Vt * t * Math.cos( Math.toRadians(head) ) ) /*+ (int)(Math.round(Wt))*/;
		this.y = this.y +  ( Vt * t * Math.sin( Math.toRadians(head) ) ) /*+ (int)(Math.round(Wt))*/;
		this.head = Transformer.checkHeadRange((this.Wt * t) + this.head);
		//System.out.println(this.toString());
	}

	private void updateSensor() throws IOException {
		this.setMeasurements(this.grid.getMeasurements(onCloud, getX(), getY(), getHead()));
	}

	@Override
	public void run(){
		long time = 0;
		long duration = 0;
		while(true){
			time = System.currentTimeMillis();
			//delay
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//System.out.print(time+ "\t");
			try {
				//update kinematic model
				this.update(duration/1000.0);
				
				//update sensor data 
				if(this.grid!=null){
					this.updateSensor();
				}	
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			//next iteration
			duration = System.currentTimeMillis() - time;
		}
	}

	public double getVt() {
		return Vt;
	}

	public void setVt(double vt) {
		Vt = vt;
	}

	/**
	 * @return the angular velocity in degree/times 
	 */
	public double getWt() {
		return Wt;
	}

	/**
	 * @param wt in degree/times
	 */
	public void setWt(double wt) {
		Wt = wt;
	}

	public int getX() {
		return (int)Math.round(x);
	}

	public int getY() {
		return (int)Math.round(y);
	}
	
	public double getHead() {
		return head;
	}

	public float[] getMeasurements() {
		return measurements;
	}

	public void setMeasurements(float[] measurements) {
		this.measurements = measurements;
	}

	public boolean isOnCloud() {
		return onCloud;
	}

	public void setOnCloud(boolean onCloud) {
		this.onCloud = onCloud;
	}

	@Override
	public String toString() {
		return "RobotState (" + x + "\t," + y + "\t," + head + "\t),\n["
				+ Vt + "\t," + Wt + "\t]\n"+Arrays.toString(measurements);
	}

	
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException{
		//test
		RobotState robot = new RobotState(25,25,90);
		//robot.setWt(1);
		robot.setVt(1);
		Thread t = new Thread(robot);
		t.start();
		RobotListener lstn = new RobotListener("test", robot);
		
		
	}
	
}
