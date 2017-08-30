package util.robot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.AbstractMap.SimpleEntry;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import samcl.SAMCL;
import util.grid.Grid;
import util.gui.Panel;
import util.gui.RobotController;
import util.gui.Tools;
import util.gui.VariablesController;
import util.gui.Window;
import util.metrics.Distribution;
import util.metrics.Particle;
import util.metrics.Transformer;
import util.sensor.LaserSensor;
import util.sensor.data.LaserScanData; 
public class RobotState extends Pose implements Runnable,Closeable{
	
	private boolean robotLock = false;
	@Parameter(names = {"-rl","--motorlock"}, description = "initial state of motor's lock", required = false, arity = 1)
	private boolean motorLock = false;
	private boolean sensorUpdated = false;
	private boolean closing = false;
	//current Target
	private int target = 0;
	
	private VelocityModel ut = new VelocityModel();
	private Pose initRobot = null;
	private List<Pose> path = null;
	private LaserScanData scanData = null;
	private List<Float> measurements = null;
	private List<Point> measurement_true_points = null;
	@ParametersDelegate
	public LaserSensor laser = new LaserSensor();
	private Grid grid = null;
	
	private Random rand = null;
	private VelocityModel initModel = null;
	private RobotController controller=null;
	public static final double standardAngularVelocity = 3;// degree/second
	public static final double standardVelocity = 0.01;// pixel/second
	
	@SuppressWarnings({ "unused" })
	public static void main(String[] args) throws Exception{
		//test for the sample motion model
		RobotState robot = new RobotState(100,100,0);
		SAMCL samcl = new SAMCL(
				(float) 0.005, //delta energy
				100, //total particle
				(float) 0.001, //threshold xi
				(float) 0.6, //rate of population
				10);//competitive strength
		JCommander jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(samcl);
		jc.parse(args);
		samcl.onCloud = false;
		samcl.setupGrid(robot.laser);
		//below is for test.
		Panel panel = new Panel(new BufferedImage(samcl.grid.width,samcl.grid.height, BufferedImage.TYPE_INT_ARGB));
		Window samcl_window = new Window("samcl image", samcl,robot);
		samcl_window.add(panel);
		samcl_window.setVisible(true);
		Graphics2D grap = panel.img.createGraphics();
		RobotController robotController = new RobotController("robot controller", robot,samcl);
		
		List<Particle> parts = new ArrayList<Particle>();
		
		for(int i = 0 ; i < 1000; i++){
			parts.add(new Particle(0, 0, 0));
		}
		double rx=0, ry=0,px=0, py=0;
		double rh=0.0;
		int i = 0;
		double[] al = Distribution.al.clone();
		System.out.println(Arrays.toString(al));
		VariablesController vc = new VariablesController(al);
		Random random = new Random();
		double time = 0.05;
		Particle nextRobot = new Particle(0,0,0);
		
		robot.setVt(100);
		robot.setWt(90);
		while(time<1){
			i++;
			Thread.sleep(20);
			grap.drawImage(samcl.grid.map_image, null, 0, 0);
			time = i*0.001;
			rx = robot.X;
			ry = robot.Y;
			rh = robot.H;
			for(Particle p : parts){
				p.setX(rx);
				p.setY(ry);
				p.setTh(rh);
				
				Distribution.MotionSampling(p, robot.getUt(), time, random, al);
				Tools.drawPoint(grap,  p.getX(), p.getY(), p.getTh(), 4, Color.BLUE);
			}
			
			Tools.drawRobot(grap,  (int)Math.round(robot.X),  (int)Math.round(robot.Y), robot.H, 20, Color.ORANGE);
			
			double r  =( robot.getVt()/Math.toRadians(robot.getWt()) );
			if(robot.getWt()!=0){
				nextRobot.setX((robot.X +  
						time*r *(+Math.sin( Math.toRadians( robot.H + robot.getWt()*time ) ) 
								-  Math.sin( Math.toRadians( robot.H ) ) 
								)));
				nextRobot.setY((robot.Y +  
						time*r *(+Math.cos( Math.toRadians( robot.H ) ) 
								-  Math.cos( Math.toRadians( robot.H + robot.getWt()*time ) )  
								)));
				nextRobot.setTh(Transformer.checkHeadRange( robot.H +
						robot.getWt()*time
						));
			}else{
				r=robot.getVt();
				nextRobot.setX(robot.X +  
						time*r *( Math.cos( Math.toRadians(robot.H) ) ));
				nextRobot.setY(robot.Y +  
						time*r *(  Math.sin( Math.toRadians(robot.H) ) ));
				nextRobot.setTh(Transformer.checkHeadRange( robot.H +
						robot.getWt()*time
						));
			}
			Tools.drawRobot(grap, 
					nextRobot.getX(), 
					nextRobot.getY(), 
					nextRobot.getTh()
					, 10, Color.RED);
			panel.repaint();
			
		}
	}

	

	public void reverseMotorLock(){
		this.setMotorLock(!this.isMotorLocked());
	}
	
	public boolean isMotorLocked() {
		return motorLock;
	}

	public void lockMotor() {
		this.setMotorLock(true);
	}
	
	public void unlockMotor() {
		this.setMotorLock(false);
	}
	
	public void setMotorLock(boolean lock) {
		this.motorLock = lock;
	}

	public void reverseRobotLock(){
		this.setRobotLock(!this.isRobotLocked());
	}
	
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
		while(!this.closing){
			time = System.currentTimeMillis();
			odoDuration = time-lastOdoUpdate;
			lsrDuration = time-lastLsrUpdate;
			try {
				synchronized (this){
						updateTarget(path);
						if(!this.isMotorLocked() && odoDuration>=odoUpdatePeriod){
							//System.out.println("Odometry Updated: "+odoDuration);
							lastOdoUpdate=time;
							//this.updateMotionModel(odoDuration / 1000.0);
							updateMotionModel2(this, this.ut, odoDuration / 1000.0);
						}
						//update sensor data 
						if(this.grid!=null && lsrDuration>=lsrUpdatePeriod){
							//System.out.println("Laser Updated: "+lsrDuration);
							lastLsrUpdate=time;
							try {
//								this.updateSensor();
								this.updateSensor2();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						Thread.sleep(10);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public List<Pose> getPath() {
		return path;
	}

	public void setPath(List<Pose> path) {
		this.path = path;
	}

	private void updateTarget(List<Pose> path) {
		
		
		if(!path.isEmpty()){//do update target
			
			if(ifArrive((Pose)this, path.get(target))){//if arrive the target, update target and VelocityModel
				this.stop();
				if(target < path.size() -1 ){//if it is not finished, do update target
					//update robot pose to current target
					this.setPose(path.get(target));
					//update target
					this.target++;
					//update VelocityModel
					updateVelocityModel((Pose)this, path.get(target));
				}else if(target == path.size() - 1){//finished then lock robot
					//update robot pose to current target
					this.setPose(path.get(target));
					this.setMotorLock(false);
				}
					
				
			}else//else do nothing to keep moving
				;
		}
		else//do nothing
			;
	}
	
	/**
	 * 
	 * @param t duration in seconds
	 */
	public static void updateMotionModel2(Pose pos, VelocityModel ut, double t){
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
	}

	private void updateSensor2() throws Exception{
		Pose pose = this.clone();
		SimpleEntry<List<Float>, List<Point>> entry = Grid.getLaserDist(pose, new Time(System.currentTimeMillis()), this.grid, this.laser);
		assignData(entry.getKey(), entry.getValue(), pose);
	}
	
	@SuppressWarnings("unused")
	private void updateSensor() throws Exception {
		Pose pose = this.clone();
		List<Float> m = this.grid.getMeasurementsAnyway(null , false, pose.X, pose.Y, pose.H);
		ArrayList<Point> mpts = new ArrayList<Point>();
		float tmp;
		assert(this.laser.range_max==grid.laser.range_max);
		for(int i = 0 ; i < m.size(); i++){
			tmp = m.get(i) + (float)Distribution.sample_normal_distribution(this.laser.variance, rand);
			if (tmp<0)
				m.set(i, 0f);
			else if(tmp>this.laser.range_max)
				m.set(i,this.laser.range_max);
			else
				m.set(i,tmp);
			
			//drawing hitting points on obstacles.
			//needing distance and orientation. 
			//The orientation is obtained from robot's heading and sensor index.
			int x = (int) Math.round(this.X+
					tmp * Math.cos( (this.H - 90.0 + i * this.grid.laser.angle_resolution) * Math.PI / 180)
					);
			int y = (int) Math.round(this.Y+
					tmp * Math.sin( (this.H - 90.0+ i * this.grid.laser.angle_resolution) * Math.PI / 180)
					);
			mpts.add(new Point(x,y));
		}
		assignData(m,mpts, pose);
	}
	
	private void assignData(List<Float> m, List<Point> mpts, Pose pose) throws Exception{
		while(isRobotLocked()){
			Thread.sleep(0, 1);
		}
		this.setRobotLock(true);
		//System.out.println("got robot lock in RS");
		this.measurements= m;
		this.measurement_true_points= mpts;
		this.scanData = new LaserScanData(m, laser, new Time(System.currentTimeMillis()), pose);
		this.setSensorUpdated(true);
		this.setRobotLock(false);
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

	private boolean ifArrive(Pose current, Pose goal) {
		//distance error
		if(Pose.compareToDistance(current, goal)>Pose.ERROR)
			return false;
		//degree error
		if(Math.abs( Pose.compareToHead(current, goal) )>Pose.ERROR)
			return false;
		return true;
	}


	/**
	 * @param x
	 * @param y
	 * @param head
	 * @throws IOException
	 *  Grid and HTable are null.
	 */
	public RobotState(int x, int y, double head) throws IOException {
		this(x, y, head, null);
	}
	
	
	/**
	 * @param x initial pose of robot.
	 * @param y initial pose of robot.
	 * @param head initial head of robot.
	 * @param grid where the sensor data from.
	 * @param path the navigation path must be continuity.
	 * @throws IOException 
	 */
	public RobotState(int x, int y, double head, List<Pose> path) throws IOException {
		this.X = x;
		this.Y = y;
		this.H = Transformer.checkHeadRange(head);
		
		//Path
		this.path = new ArrayList<Pose>();
		if(path!=null)
			this.path.addAll(path);
		//current Target
		target = 0;
	}
	
	public void setupSimulationRobot(Grid grid){
		this.grid = grid;
		this.rand = new Random();
		ut.setVelocity(0);
		ut.setAngular_velocity(0);
		//initial state
		setInitModel(this.ut);
		setInitPose(this);
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
			System.out.println("got robot lock");
			this.setPose(initRobot);
			this.setVelocityModel(initModel);
			this.target = 0;
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
		return scanData;
	}
	
	public List<Float> getMeasurements() {
		return measurements;
	}
	
	public List<Point> getMeasurement_points() {
		return this.measurement_true_points;
	}

	public RobotController getController() {
		return controller;
	}

	public void setController(RobotController controller) {
		this.controller = controller;
	}

	@Override
	public void close() throws IOException {
		this.closing = true;
		if(controller!=null)
			controller.close();
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
