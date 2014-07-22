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
	
	public static final double standardAngularVelocity = 15;// degree/second
	public static final double standardVelocity = 20;// pixel/second
	
	public static void main(String[] args) throws IOException{
		//test
		RobotState robot = new RobotState(220,60,0);
		//robot.setWt(1);
		//robot.setVt(1);
		Thread t = new Thread(robot);
		t.start();
		
		RobotListener lstn = new RobotListener("test", robot);
		
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		robot.setVt(10);
		//robot.setWt(1);
		
	}

	@Override
		public void run(){
			long time = 0;
			long duration = 0;
			
			while(true){
				System.out.println(this.toString());
				System.out.println("target:" +target);
				//delay
				time = System.currentTimeMillis();
				
				try {
					
					updateTarget(path);
					
					Thread.sleep(10);
					
					this.update(duration/1000.0);
					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				//next iteration
				duration = System.currentTimeMillis() - time;
			}
		}

	private void updateTarget(List<Pose> path) {
		
		
		if(!path.isEmpty()){//do update target
			
			if(ifArrive(this.getPose(), path.get(target))){//if arrive the target, update target and VelocityModel
				this.stop();
				if(target < path.size() -1 ){//if it is not finished, do update target
					//update robot pose to current target
					this.setPose(path.get(target));
					//update target
					this.target++;
					//update VelocityModel
					updateVelocityModel(this.getPose(), path.get(target));
				}else if(target == path.size() - 1){
					//update robot pose to current target
					this.setPose(path.get(target));
				}else//finished then stop moving
					this.stop();
				
			}else//else do nothing to keep moving
				;
		}
		else//do nothing
			;
	}

	private void updateVelocityModel(Pose current, Pose goal) {
		if(Pose.compareToDistance(current, goal)>0){
			this.goStraight();
		}
		double headError = Pose.compareToHead(current, goal);
		if(headError>0){
			this.turnRight();
		}else if (headError<0){
			this.turnLeft();
		}
		//if distance <= 0 or head error == 0
		//do nothing
		
	}

	private void turnLeft() {
		this.ut.setAngular_velocity(0-standardAngularVelocity);
		this.ut.setVelocity(0);
	}

	private void turnRight() {
		this.ut.setAngular_velocity(standardAngularVelocity);
		this.ut.setVelocity(0);
	}

	private void goStraight() {
		this.ut.setAngular_velocity(0);
		this.ut.setVelocity(standardVelocity);
	}

	private boolean ifArrive(Pose current, Pose goal) {
		//distance error
		if(Pose.compareToDistance(current, goal)>Pose.ERROR)
			return false;
		//degree error
		if(Pose.compareToHead(current, goal)>Pose.ERROR)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RobotState (" + x + "\t," + y + "\t," + head + "\t),\n["
				+ ut.getVelocity() + "\t," + ut.getAngular_velocity() + "\t]\n"+Arrays.toString(measurements);
	}

	private double x;
	private double y;
	private double head;
	private VelocityModel ut = new VelocityModel();
	List<Pose> path = null;
	//current Target
	private static int target = 0;
	private PathPlan pp = null;
	private float[] measurements;
	private boolean onCloud;
	private Grid grid = null;
	private HTable table = null;
	

	/**
	 * @param x
	 * @param y
	 * @param head
	 * @throws IOException
	 *  Grid and HTable are null.
	 */
	public RobotState(int x, int y, double head) throws IOException {
		this(x, y, head, null, null);
	}
	
	/**
	 * @param x
	 * @param y
	 * @param head
	 * @param grid
	 * @throws IOException
	 * HTable is null.
	 */
	public RobotState(int x, int y, double head, Grid grid) throws IOException {
		this(x, y, head, grid, null);
	}
	
	/**
	 * @param x initial pose of robot.
	 * @param y initial pose of robot.
	 * @param head initial head of robot.
	 * @param grid where the sensor data from.
	 * @param tableName 
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
		if(tableName!=null){
			this.table = this.grid.getTable(tableName);
			this.setOnCloud(true);
		}
		
		//current Target
		target = 0;
		//Path
		path = new ArrayList<Pose>();
		Pose pose1 = new Pose(440,60,0);
		path.add(pose1);
		Pose pose2 = new Pose(440,60,90);
		path.add(pose2);
		Pose pose3 = new Pose(440,320,90);
		path.add(pose3);
		Pose pose4 = new Pose(440,320,180);
		path.add(pose4);
		Pose pose5 = new Pose(220,320,180);
		path.add(pose5);
		
		if(path==null)
			throw new NullPointerException();
	}
	
	
	public void update(double t) throws IOException{
		//update pose
		this.x = this.x +  ( ut.getVelocity() * t * Math.cos( Math.toRadians(head) ) ) /*+ (int)(Math.round(Wt))*/;
		this.y = this.y +  ( ut.getVelocity() * t * Math.sin( Math.toRadians(head) ) ) /*+ (int)(Math.round(Wt))*/;
		this.head = Transformer.checkHeadRange((ut.getAngular_velocity() * t) + this.head);
		
		//update sensor data 
		if(this.grid!=null){
			this.updateSensor();
		}	
	}

	private void updateSensor() throws IOException {
		this.setMeasurements(this.grid.getMeasurements(this.table , onCloud, getX(), getY(), getHead()));
	}

	public VelocityModel getUt() {
		return ut;
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
