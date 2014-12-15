package robot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.hbase.client.HTable;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.DoubleConverter;

import samcl.Grid;
import samcl.SAMCL;
import util.gui.Panel;
import util.gui.RobotController;
import util.gui.Tools;
import util.gui.VariablesController;
import util.gui.Window;
import util.metrics.Distribution;
import util.metrics.Particle;
import util.metrics.Transformer;

public class RobotState implements Runnable,Closeable{
	
	public static final double standardAngularVelocity = 15;// degree/second
	public static final double standardVelocity = 10;// pixel/second
	
	public static void main(String[] args) throws Exception{
		//test for the sample motion model
		RobotState robot = new RobotState(100,100,0);
		SAMCL samcl = new SAMCL(
				18, //orientation
				"file:///home/wuser/backup/jpg/map.jpg",//map image file
//				"hdfs:///user/eeuser/map1024.jpeg",
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
		samcl.setup();
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
		int rx=0, ry=0,px=0, py=0;
		double rh=0.0;
		int i = 0;
		double[] al = Distribution.al.clone();
		System.out.println(Arrays.toString(al));
		VariablesController vc = new VariablesController(al);
		Random random = new Random();
//		double time = System.currentTimeMillis()/1000;
		double time = 0.05;
		Particle nextRobot = new Particle(0,0,0);
		
		robot.setVt(100);
		robot.setWt(90);
//		robot.unlock();
		while(time<1){
			i++;
			Thread.sleep(20);
			grap.drawImage(samcl.grid.map_image, null, 0, 0);
			//System.out.println(robot.toString());
//			px = robot.getX();
//			py = robot.getY();
			//time = System.currentTimeMillis()/1000 - time;
			time = i*0.001;
			rx = robot.getX();
			ry = robot.getY();
			rh = robot.getHead();
			//System.out.println(robot);
			System.out.println("counter"+i);
			System.out.println("time="+time+"s");
			for(Particle p : parts){
				
				
				if(i<100000000){
					p.setX(rx);
					p.setY(ry);
					p.setTh(rh);
				}
				
				Distribution.MotionSampling(p, robot.getUt(), time, random, al);
				Tools.drawPoint(grap,  p.getX(), p.getY(), p.getTh(), 4, Color.BLUE);
//				System.out.println(p.toString());
			}
			
			Tools.drawRobot(grap,  robot.getX(),  robot.getY(), robot.getHead(), 20, Color.ORANGE);
			
			double r  =( robot.getVt()/Math.toRadians(robot.getWt()) );
			if(robot.getWt()!=0){
				nextRobot.setX((robot.getX() +  
						time*r *(+Math.sin( Math.toRadians( robot.getHead() + robot.getWt()*time ) ) 
								-  Math.sin( Math.toRadians( robot.getHead() ) ) 
								)));
				nextRobot.setY((robot.getY() +  
						time*r *(+Math.cos( Math.toRadians( robot.getHead() ) ) 
								-  Math.cos( Math.toRadians( robot.getHead() + robot.getWt()*time ) )  
								)));
				nextRobot.setTh(Transformer.checkHeadRange( robot.getHead() +
						robot.getWt()*time
						));
			}else{
				r=robot.getVt();
				nextRobot.setX(robot.getX() +  
						time*r *( Math.cos( Math.toRadians(robot.getHead()) ) ));
				nextRobot.setY(robot.getY() +  
						time*r *(  Math.sin( Math.toRadians(robot.getHead()) ) ));
				nextRobot.setTh(Transformer.checkHeadRange( robot.getHead() +
						robot.getWt()*time
						));
			}
			Tools.drawRobot(grap, 
					nextRobot.getX(), 
					nextRobot.getY(), 
					nextRobot.getTh()
					, 10, Color.RED);
//			this.x +  ( ut.getVelocity() * t * Math.cos( Math.toRadians(head) ) ) /*+ (int)(Math.round(Wt))*/;
//			this.y = this.y +  ( ut.getVelocity() * t * Math.sin( Math.toRadians(head) ) ) /*+ (int)(Math.round(Wt))*/;
//			this.head = Transformer.checkHeadRange((ut.getAngular_velocity() * t) + this.head);
			panel.repaint();
			System.out.println(robot.toString());
			System.out.println(nextRobot.toString());
			
		}
		
		
		//test the robot's update state
		/*List<Pose> path = new ArrayList<Pose>();
		path.add(new Pose(400,150,0));
		path.add(new Pose(400,150,90));
		path.add(new Pose(400,400,90));
		path.add(new Pose(400,400,180));
		path.add(new Pose(350,400,180));
		path.add(new Pose(350,400,90));
		path.add(new Pose(350,550,90));
		path.add(new Pose(350,550,180));
		path.add(new Pose(150,550,180));
		path.add(new Pose(150,550,270));
		path.add(new Pose(150,150,270));
		path.add(new Pose(150,150,0));
		//test
		RobotState robot = new RobotState(150,150,0,path);
		String[] sts = {
//				"-rx","400.0",
				"-rl","false"
		};
		new JCommander(robot,sts);
		robot.setInitPose(robot.getPose());
		//robot.setWt(1);
		//robot.setVt(1);
		Thread t = new Thread(robot);
		t.start();
		
		@SuppressWarnings("unused")
		RobotController lstn = new RobotController("robot controller", robot);
		
		
		try {
			System.out.println("sleep 2s .......");
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("set up velocity");
		robot.setVt(10);
		robot.setInitModel(robot.getModel());
		//robot.setWt(1);
		System.out.println("unlock robot");
		//robot.unlock();
		while(true){
			try {
				Thread.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println(robot.toString());
		}*/
	}

	
	private VelocityModel getModel() {
		return this.ut;
	}


	public void reverseLock(){
		this.setLock(!this.isLock());
	}
	
	public boolean isLock() {
		return lock;
	}

	public void lock() {
		this.setLock(true);
	}
	
	public void unlock() {
		this.setLock(false);
	}
	
	private void setLock(boolean lock) {
		this.lock = lock;
	}

	@Override
		public void run(){
			if(this.onCloud){
				try {
					this.table = this.grid.getTable(tableName);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			long time = 0;
			long duration = 0;
			
			while(true){
				//System.out.println("lock : " + this.isLock());
//				if (this.isLock()) {
					//System.out.println(this.toString());
					//System.out.println("target:" + target);
					//delay
					time = System.currentTimeMillis();
					try {
						if(!this.isLock()){
							updateTarget(path);
							this.update(duration / 1000.0);
						}
						
						Thread.sleep(10);
						//update sensor data 
						if(this.grid!=null){
							try {
								this.updateSensor();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					//next iteration
					duration = System.currentTimeMillis() - time;
//				}
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
				}else if(target == path.size() - 1){//finished then lock robot
					//update robot pose to current target
					this.setPose(path.get(target));
					this.setLock(false);
				}
					
				
			}else//else do nothing to keep moving
				;
		}
		else//do nothing
			;
	}

	public void update(double t) throws IOException{
		//update pose
		this.x = this.x +  ( ut.getVelocity() * t * Math.cos( Math.toRadians(head) ) ) /*+ (int)(Math.round(Wt))*/;
		this.y = this.y +  ( ut.getVelocity() * t * Math.sin( Math.toRadians(head) ) ) /*+ (int)(Math.round(Wt))*/;
		this.head = Transformer.checkHeadRange((ut.getAngular_velocity() * t) + this.head);
	}

	private void updateSensor() throws Exception {
		this.setMeasurements(this.grid.getMeasurementsAnyway(this.table , onCloud, this.x, this.y, this.head));
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

	@Override
	public String toString() {
		return "RobotState (" + String.format("% 3.3f", x) + "\t," + String.format("% 3.3f", y) + "\t," + String.format("% 3.3f", head) + "\t),\n"+
					"state : "+(this.isLock()? "  lock":"unlock")+"["+ ut.getVelocity() + "\t," + ut.getAngular_velocity() + "\t]\n"+
					Arrays.toString(measurements);
	}
	@Parameter(names = {"-cl","--cloud"}, description = "if be on the cloud, default is false", required = false)
	private boolean onCloud;
	@Parameter(names = {"-rl","--robotlock"}, description = "initialize robot's lock", required = false, arity = 1)
	private boolean lock = true;
	@Parameter(names = {"-rx","--robotx"}, description = "initialize robot's X-Axis", required = false, converter = DoubleConverter.class)
	private double x;
	@Parameter(names = {"-ry","--roboty"}, description = "initialize robot's Y-Axis", required = false, converter = DoubleConverter.class)
	private double y;
	@Parameter(names = {"-rh","--robothead"}, description = "initialize robot's Head", required = false, converter = DoubleConverter.class)
	private double head;
	@Parameter(names = {"-t","--table"}, description = "name of table", required = false)
	private String tableName;
	private VelocityModel ut = new VelocityModel();
	private List<Pose> path = null;
	//current Target
	private int target = 0;
	private float[] measurements;
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
		this(x, y, head, null, null, null);
	}
	
	/**
	 * @param x
	 * @param y
	 * @param head
	 * @param path
	 * @throws IOException 
	 */
	public RobotState(int x, int y, double head, List<Pose> path) throws IOException {
		this(x, y, head, null, null, path);
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
		this(x, y, head, grid, null, null);
	}
	
	/**
	 * @param x
	 * @param y
	 * @param head
	 * @param grid
	 * @param path
	 * @throws IOException
	 * HTable is null.
	 */
	public RobotState(int x, int y, double head, Grid grid, List<Pose> path) throws IOException {
		this(x, y, head, grid, null, path);
	}
	
	/**
	 * @param x
	 * @param y
	 * @param head
	 * @param grid
	 * @param tableName
	 * @throws IOException
	 * HTable is null.
	 */
	public RobotState(int x, int y, double head, Grid grid, String tableName) throws IOException {
		this(x, y, head, grid, tableName, null);
	}
	
	/**
	 * @param x initial pose of robot.
	 * @param y initial pose of robot.
	 * @param head initial head of robot.
	 * @param grid where the sensor data from.
	 * @param tableName 
	 * @param path the navigation path must be continuity.
	 * @throws IOException 
	 */
	public RobotState(int x, int y, double head, Grid grid, String tableName, List<Pose> path) throws IOException {
		super();
		System.out.println("initial robot");
		this.x = x;
		this.y = y;
		this.head = Transformer.checkHeadRange(head);
		ut.setVelocity(0);
		ut.setAngular_velocity(0);
		
		this.grid = grid;
		this.tableName = tableName;
		
		
		//Path
		this.path = new ArrayList<Pose>();
		if(path!=null)
			this.path.addAll(path);
		
		//current Target
		target = 0;
	}
	
	private Pose initRobot = null;
	private VelocityModel initModel = null;
	public void setInitPose(Pose p) {
		this.initRobot = p;
	}
	public void setInitModel(VelocityModel m){
		this.initModel = new VelocityModel(m);
	}
	
	public void initRobot(){
		this.setPose(initRobot);
		this.setVelocityModel(initModel);
		this.target = 0;
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
		if(this.onCloud) 
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
	
}
