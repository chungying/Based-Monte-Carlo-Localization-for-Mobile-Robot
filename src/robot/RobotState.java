package robot;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.hbase.client.HTable;

import samcl.Grid;
import util.gui.RobotListener;
import util.metrics.Transformer;

public class RobotState implements Runnable,Closeable{
	private double x;
	private double y;
	private double head;
	private VelocityModel ut = new VelocityModel();
	private PathPlan pp = null;
public VelocityModel getUt() {
		return ut;
	}

	//	private double Vt;
//	private double Wt;
	private float[] measurements;
	
	private boolean onCloud;
	private Grid grid = null;
	private HTable table = null;
	

	public RobotState(int x, int y, double head) throws IOException {
		this(x, y, head, null, null);
	}
	
	public RobotState(int x, int y, double head, Grid grid) throws IOException {
		this(x, y, head, grid, null);
	}
	
	/**
	 * @param x
	 * @param y
	 * @param head
	 * @param vt
	 * @param wt
	 * @throws IOException 
	 */
	public RobotState(int x, int y, double head, Grid grid, String tableName) throws IOException {
		super();
		System.out.println("initial robot");
		this.x = x;
		this.y = y;
		this.head = Transformer.checkHeadRange(head);
		ut.setVelocity(0);
		ut.setAngular_velocity(0);
		//Vt = 0;
		//Wt = 0;
		
		//TODO setup Grid and onCloud?
		this.grid = grid;
		//TODO table name
		if(tableName!=null)
			this.table = this.grid.getTable(tableName);
	}
	
	public void update(double t) throws IOException{
		this.x = this.x +  ( ut.getVelocity() * t * Math.cos( Math.toRadians(head) ) ) /*+ (int)(Math.round(Wt))*/;
		this.y = this.y +  ( ut.getVelocity() * t * Math.sin( Math.toRadians(head) ) ) /*+ (int)(Math.round(Wt))*/;
		this.head = Transformer.checkHeadRange((ut.getAngular_velocity() * t) + this.head);
		//System.out.println(this.toString());
	}

	private void updateSensor() throws IOException {
		this.setMeasurements(this.grid.getMeasurements(this.table , onCloud, getX(), getY(), getHead()));
	}

	@Override
	public void run(){
		long time = 0;
		long duration = 0;
		List<Pose> path = new ArrayList<Pose>();
		Pose pose1 = new Pose(40,30,0);
		path.add(pose1);
		Pose pose2 = new Pose(40,30,90);
		path.add(pose2);
		int target = 0;
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
/*				if(pp!=null){
					pp.nextPose(this);
					//System.out.println(pp.currentP);
					if(this.getPose().eauals(pp.currentP)){
						System.out.println("!!!!!!!!!!!");
						System.out.println(this.getPose());
						System.out.println(pp.currentP);
					}
				}*/
				if(target<path.size()){
					if(this.getPose().equal(path.get(target))){
						if(target==0){
							this.setPose(path.get(target));
							this.setVt(0);
							this.setWt(15);
						}else if(target==1){
							this.setPose(path.get(target));
							this.setVt(10);
							this.setWt(0);
						}
						target++;
					}
				}
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
		return ut.velocity;
	}

	public void setVt(double vt) {
		ut.setVelocity(vt);
	}

	/**
	 * @return the angular velocity in degree/times 
	 */
	public double getWt() {
		return ut.getAngular_velocity();
	}

	/**
	 * @param wt in degree/times
	 */
	public void setWt(double wt) {
		ut.setAngular_velocity(wt);
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
				+ ut.getVelocity() + "\t," + ut.getAngular_velocity() + "\t]\n"+Arrays.toString(measurements);
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
		
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		robot.setVt(-1);
		robot.setWt(1);
		
	}

	@Override
	public void close() throws IOException {
		if(this.table!=null) 
			table.close();
		
	}

	public Pose getPose() {
		return new Pose(this.x,this.y,this.head);
	}

	public void stop() {
		this.ut.reset(0.0, 0.0);
	}

	public void setVelocityModel(VelocityModel u) {
		this.ut.setVelocity(u.getVelocity());
		this.ut.setAngular_velocity(u.getAngular_velocity());
		
	}

	public void setPose(Pose pose) {
		this.x = pose.X;
		this.y = pose.Y;
		this.head = pose.H;
	}

	public PathPlan getPp() {
		return pp;
	}

	public void setPp(PathPlan pp) {
		this.pp = pp;
	}
	
}
