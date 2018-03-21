package util.robot;

import java.awt.Point;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import util.Transformer;
import util.grid.Grid;
import util.grid.GridTools;
import util.gui.FrameOwner;
import util.gui.RobotController;
import util.pf.sensor.data.LaserScanData;
import util.pf.sensor.laser.LaserSensor;
import util.recorder.PoseWithTimestamp; 
public class RobotState extends Pose implements Runnable, Closeable, FrameOwner{
	
	private boolean robotLock = false;
	@Parameter(names = {"-rl","--motorlock"}, description = "initial state of motor's lock", required = false, arity = 1)
	private boolean motorLock = false;
	private boolean sensorUpdated = false;
	private boolean closing = false;
	public boolean isRobotClosing(){
		return closing;
	}
	public boolean isAllTasksOver(){
		return (this.targetID==-1?true:false);
	}
	//current Target
	private int targetID = 0;
	
	
	private List<Pose> path = new ArrayList<Pose>();;
	private LaserScanData scanData = null;
	private List<Float> measurements = null;
	private List<Point> measurement_true_points = null;
	private Grid grid = null;
	
	@ParametersDelegate
	private VelocityModel ut = new VelocityModel();
	
	private LaserSensor laser = new LaserSensor();
	public Pose initRobot = null;
	private Pose lastTarget = null;
	public VelocityModel initModel = null;
	private RobotController controller = null;
	public static final double standardAngularVelocity = 3;// degree/second
	public static final double standardVelocity = 1;// pixel/second

	public void reverseMotorLock(){
		this.setMotorLock(!this.isMotorLocked());
	}
	
	public boolean isMotorLocked() {
		return motorLock;
	}
	
	public void setMotorLock(boolean lock) {
		this.motorLock = lock;
	}

//	public void reverseRobotLock(){
//		this.setRobotLock(!this.isRobotLocked());
//	}
	
	public boolean isRobotLocked() {
		return robotLock;
	}

	public void setRobotLock(boolean lock) {
		this.robotLock = lock;
	}

	@Override
	public void run(){
		long time = System.currentTimeMillis();
		long odoUpdatePeriod=10;
		long odoDuration, lsrDuration;
		long lsrUpdatePeriod=200;
		long lastOdoUpdate=time;
		long lastLsrUpdate=time;
		try {
			while(!isRobotClosing()){
				time = System.currentTimeMillis();
				odoDuration = time-lastOdoUpdate;
				lsrDuration = time-lastLsrUpdate;
				synchronized (this){
					updateTarget(path);
					if(!this.isMotorLocked() && odoDuration>=odoUpdatePeriod){
						lastOdoUpdate=time;
						Pose p = updateMotionModel2(this, this.ut, odoDuration / 1000.0);
						if(GridTools.boundaryCheck((int)Math.round(p.X), (int)Math.round(p.Y), grid)){
//							System.out.println("valid pose");
							this.X = p.X;
							this.Y = p.Y;
							this.H = p.H;
						}else{
//							System.out.println("invalid pose: " + p + ", "
//									+ "Occupancy: " + 
//									(grid.map_array((int)Math.round(p.X), (int)Math.round(p.Y))
//									==Grid.GRID_OCCUPIED?"Occupied":"Empty"));
							
						}
					}
					//update sensor data 
					if(this.grid!=null && lsrDuration>=lsrUpdatePeriod){
						lastLsrUpdate=time;
						updateSensorWithGaussianNoise();
					}
					Thread.sleep(10);
				}
			
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			try {
				this.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public List<Pose> getPath() {
		return path;
	}

	public void setPath(Pose... poses){
		targetID = 1;
		for(Pose p : poses){
			this.path.add(p);
		}
	}
	
	public void setPath(List<Pose> path) {
//		this.path = path;
		targetID = 1;
		this.path.addAll(path);
	}

	
	private void updateTarget(List<Pose> path) {
		if(!path.isEmpty() && targetID > 0){
			//TODO adding last pose when new target shown
			if(ifArrive((Pose)this, path.get(targetID-1), lastTarget)){//if arrive the target, update target
//				System.out.println("robot is tracking");
				lastTarget = path.get(targetID-1).clone();
				if(targetID < path.size()  ){//if there is next target in the path, do update new target
					//update robot pose to current target
					this.setPose(path.get(targetID-1));
					//update to next target
					this.targetID++;
					//update VelocityModel and go forward to the new target
					updateVelocityModel((Pose)this, path.get(targetID-1));
				}else if(targetID >= path.size()){//finished then lock robot
					//update robot pose to current target
					this.stop();//TODO 
					this.setPose(path.get(targetID-1));
					targetID = -1;//no target anymore.
					this.setMotorLock(false);
				}
				
					
				
			}else//keep moving
				;
		}
		else if(targetID == -1){//task is over, waiting for restart
//			System.out.println("robot is waiting");
		}else{
//			System.out.println("robot target is " + targetID);
		}
	}
	
	/**
	 * 
	 * @param t duration in seconds
	 */
	public static Pose updateMotionModel2(Pose p, VelocityModel ut, double t){
		Pose pos = p.clone();
		double w = ut.getAngular_velocity();
		if(Math.abs(w)<=0.0001){
			//updateMotionModel(double t)
			pos.X = pos.X +  ( ut.getVelocity() * t * Math.cos( Math.toRadians(pos.H) ) ) ;
			pos.Y = pos.Y +  ( ut.getVelocity() * t * Math.sin( Math.toRadians(pos.H) ) ) ;
			pos.H = Transformer.checkHeadRange((ut.getAngular_velocity() * t) + pos.H);
		}else{
			double r = ut.getVelocity()/Math.toRadians(w);
			
			pos.X = pos.X + r * ( 0 
						- Math.sin(Math.toRadians(pos.H)) 
						+ Math.sin(Math.toRadians(pos.H + w * t )) 
					);
			pos.Y = pos.Y + r * ( 0
						+ Math.cos(Math.toRadians(pos.H))
						- Math.cos(Math.toRadians(pos.H + w * t )) 
					);
			pos.H = Transformer.checkHeadRange( pos.H + w * t);
		}
		return pos;
	}
	
	private void updateSensorWithGaussianNoise() throws Exception{
		long stamp = System.currentTimeMillis();
		SimpleEntry<List<Float>, List<Point>> entry = GridTools.getLaserDist(this.grid, this.laser, this.X, this.Y, this.H, false);
		if(entry.getKey()!=null)
		{
			while(isRobotLocked()){
				Thread.sleep(0, 1);
			}
			this.setRobotLock(true);
			this.measurements= entry.getKey();
			this.measurement_true_points= entry.getValue();
			if(this.scanData == null) {
				Time time = new Time(stamp);
				this.scanData = new LaserScanData(entry.getKey(), this.laser, time, new PoseWithTimestamp(this, time));
			}
			else {
				this.scanData.timeStamp.setTime(stamp);
				this.scanData.groundTruthPose.stamp.setTime(stamp);
				this.scanData.groundTruthPose.X = this.X;
				this.scanData.groundTruthPose.Y = this.Y;
				this.scanData.groundTruthPose.H = this.H;
				this.scanData.beamranges = entry.getKey();
			}
			this.setSensorUpdated(true);
			this.setRobotLock(false);
		}
		else{//when the robot is at occupied position
			;//TODO assign all of them to zero
			;//TODO send out stop request
		}
	}
	
	private void assignData(Time stamp, List<Float> m, List<Point> mpts, PoseWithTimestamp pose) throws Exception{
		//while(isRobotLocked()){
		//	Thread.sleep(0, 1);
		//}
		//this.setRobotLock(true);
		//this.measurements= m;
		//this.measurement_true_points= mpts;
		//this.scanData = new LaserScanData(m, laser, stamp, pose);
		//this.setSensorUpdated(true);
		//this.setRobotLock(false);
	}

	private void setSensorUpdated(boolean b) {
		sensorUpdated = b;
	}
	
	public boolean isSensorUpdated(){
		return sensorUpdated;
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

	public void turnLeft() {
		this.ut.setAngular_velocity(0-standardAngularVelocity);
		this.ut.setVelocity(0);
	}

	public void turnRight() {
		this.ut.setAngular_velocity(standardAngularVelocity);
		this.ut.setVelocity(0);
	}

	public void goStraight() {
		this.ut.setAngular_velocity(0);
		this.ut.setVelocity(standardVelocity);
	}

	private boolean ifArrive(Pose current, Pose goal, Pose lastTarget) {
		//if the signs of curent-goal are different from the signs of lastTarget-goal
		Pose lg = goal.minus(lastTarget);
		Pose cg = goal.minus(current);
		if((lg.X<0 && cg.X>0) || (lg.X>0 && cg.X<0)){
			return true;
		}
		if((lg.Y<0 && cg.Y>0) || (lg.Y>0 && cg.Y<0)){
			return true;	
		}
		//distance error
		if(Pose.compareToDistance(current, goal)>Pose.ERROR)
			return false;
		//degree error
		if(Math.abs( Pose.compareToHead(current, goal) )>Pose.ERROR)
			return false;
		return true;
	}

	public RobotState(){
		this(null);
	}
	
	
	/**
	 * @param grid where the sensor data from.
	 * @param path the navigation path must be continuity.
	 */
	public RobotState(List<Pose> path){		
		//Path
		if(path!=null){
//			this.path.addAll(path);
			setPath(path);
		}
		//current Target
		targetID = 0;
	}
	
	public void setupSimulationRobot(Grid grid) throws Exception{
		this.laser.setupSensor(grid.laser);
		this.grid = grid;
		//initial state
		setInitState(this);
		setupFrame(grid.visualization);
	}
	
	@Override
	public void setupFrame(boolean visualization){
		//setup robot controller
		if(visualization){
			controller = new RobotController();
			controller.setupRobot(this);
		}
	}
	
	@Override
	public void setFrameLocation(int x, int y){
		controller.setLocation(x, y);
	}
	
	public void setInitState(RobotState robot){
		setInitModel(robot.ut);
		setInitPose(robot);
		this.lastTarget = new Pose(initRobot);
	}
	
	private void setInitPose(Pose p) {
		this.initRobot = new Pose(p);
	}
	
	private void setInitModel(VelocityModel m){
		this.initModel = new VelocityModel(m);
	}
	
	public void robotStartOver(){
		if(initRobot!=null){
			while(isRobotLocked()){
				try {
					Thread.sleep(0, 1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			this.setRobotLock(true);
			this.setPose(initRobot);
			this.lastTarget = new Pose(initRobot);
			this.setVelocityModel(initModel);
			this.targetID = this.path.isEmpty()?0:1;
			this.setRobotLock(false);
		}
	}

	static public VelocityModel ZEROU = new VelocityModel(0.0,0.0); 

	public VelocityModel getUt() {
		if(motorLock)
			return ZEROU;
		else
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

	public void setX(double X) {
		this.X = X;
	}
	
	public void setY(double Y) {
		this.Y = Y;
	}
	
	public void setHead(double Head) {
		this.H = Head;
	}

	public LaserScanData getLaserScan(){
		while(isRobotLocked()){
			try {
				Thread.sleep(0, 1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.setRobotLock(true);
		this.setSensorUpdated(false);
		this.setRobotLock(false);
		return this.scanData;
	}
	
	public List<Float> getMeasurements() {
		return measurements;
	}
	
	public List<Point> getMeasurement_points() {
		return this.measurement_true_points;
	}

	@Override
	public void close() throws IOException {
		if (!isRobotClosing()) {
			System.out.println("closing " + this.getClass().getName());
			this.closing = true;
			if (controller != null){
				controller.close();
				controller.dispose();
				controller = null;
			}
		}
	}
	
	public void setPose(Pose pose) {
		this.X = pose.X;
		this.Y = pose.Y;
		this.H = pose.H;
	}

	public void stop() {
		this.ut.set(0.0, 0.0);
	}
	

	public void setVelocityModel(VelocityModel u) {
		this.ut.setVelocity(u.getVelocity());
		this.ut.setAngular_velocity(u.getAngular_velocity());
		
	}
	
}
