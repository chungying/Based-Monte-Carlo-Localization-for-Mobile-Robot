/**
 * 
 */
package samcl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JFrame;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import util.grid.Grid;
import util.gui.Panel;
import util.gui.Tools;
import util.metrics.Distribution;
import util.metrics.Particle;
import util.metrics.Transformer;
import util.robot.Pose;
import util.robot.RobotState;
import util.robot.VelocityModel;
import util.sensor.MCLLaserSensor;
import util.sensor.MCLLaserSensor.MCLLaserSensorData;
import util.sensor.MCLLaserSensor.ModelType;
import com.beust.jcommander.Parameter;

/**
 * @author w514
 *part1:Sampling total particles
 *part2:Determining the size of the global sample set and the local sample set
 *part3:Resampling local samples
 *part4:Drawing global samples
 *part5:Combining two particle sets
 */
public class SAMCL implements Closeable{
	static class MyEntry<K, V> implements Map.Entry<K, V>{
		private final K key;
		private V value;
		
		MyEntry(K key, V value){
			this.key = key;
			this.value = value;
		}
		
		@Override
		public K getKey() {
			return this.key;
		}

		@Override
		public V getValue() {
			return this.value;
		}

		@Override
		public V setValue(V value) {
			V old = this.value;
			this.value = value;
			return old;
		}
		
	}
	
	/**
	 * @param orientation angular resolution
	 * @param mapFilename 
	 * @param deltaEnergy dor defining the SER
	 * @param nt population 
	 * @param xI threshold 
	 * @param aLPHA rate of population
	 * @param tournamentPresure competetive strength
	 * @throws IOException 
	 */
	public SAMCL(int orientation,
			String mapFilename, 
			float deltaEnergy,
			int nt,
			float xI, 
			float aLPHA, 
			int tournamentPresure) throws IOException {
		//directive
		super();
		this.orientation = orientation;
		this.mapFilename = mapFilename;
		this.deltaEnergy = deltaEnergy;
		this.Nt = nt;
		this.XI = xI;
		this.ALPHA = aLPHA;
		this.tournamentPresure = tournamentPresure;
		//remains
	}
	
	/**
	 * @param orientation angular resolution
	 * @param mapFilename 
	 * @param deltaEnergy dor defining the SER
	 * @param nt population 
	 * @param xI threshold 
	 * @param aLPHA rate of population
	 * @param tournamentPresure competetive strength
	 * @throws IOException 
	 */
	public SAMCL(boolean cloud, 
			int orientation,
			String mapFilename, 
			float deltaEnergy,
			int nt,
			float xI, 
			float aLPHA, 
			int tournamentPresure) throws IOException {
		//directive
		super();
		this.orientation = orientation;
		this.mapFilename = mapFilename;
		this.deltaEnergy = deltaEnergy;
		this.Nt = nt;
		this.XI = xI;
		this.ALPHA = aLPHA;
		this.tournamentPresure = tournamentPresure;
		
		this.onCloud = cloud;
		
	}
	
	public SAMCL() throws IOException{
		
	}
	
	public void setup() throws Exception{
		if(this.deltaEnergyStr!=null)
			this.deltaEnergy = Float.parseFloat(deltaEnergyStr);
		if(this.alpha!=null)
			this.ALPHA = Float.parseFloat(this.alpha);
		if(this.xiStr!=null)
			this.XI = Float.parseFloat(xiStr);

		this.sensorNumber = this.orientation/2 + 1;
		//unused
		//this.sensorDeltaDegree = this.orientationDeltaDegree;
		//this.orientationDeltaDegree = 360/this.orientation;
		this.al = Distribution.al.clone();
		this.grid= new Grid(this.orientation, this.sensorNumber, this.mapFilename);
		
		if(this.onCloud){
			System.out.println("cloud setup");
			this.cloudSetup();
		}else{
			System.out.println("local setup");
			this.localSetup();
		}

	}
	
	private void localSetup() throws IOException{
		grid.readmap(max_distance);
		/**
		 *  the initialization of SAMCL 
		 */
		this.width = this.grid.width;
		this.height = this.grid.height;
		//TODO add a condition to choose if start mouse function or not
		//precomputed_grid.start_mouse(precomputed_grid);
	}
	
	private void cloudSetup() throws Exception{
		Logger.getRootLogger().setLevel(Level.WARN);
		Configuration conf = HBaseConfiguration.create();
		grid.setupTable(conf);
		grid.readmap(this.mapFilename, conf);
		this.table = this.grid.getTable(this.tableName);
		/**
		 *  the initialization of SAMCL 
		 */
		this.width = this.grid.width;
		this.height = this.grid.height;
		//TODO add a condition to choose if start mouse function or not
		//precomputed_grid.start_mouse(precomputed_grid);
		
		this.customizedSetup(conf);
	}
	public void customizedSetup(Configuration conf) throws Exception{ 
	}

	private boolean terminating = false;
	public void setTerminating(boolean terminate) {
		this.terminating = terminate;
	}
	private void setTerminated(boolean terminated) {
		this.terminated = terminated;
	}
	public boolean isTerminating() {
		return this.terminating ;
	}

	private boolean terminated = false;
	public boolean hasTerminated() {
		return this.terminated ;
	}

	private boolean closing = false;
	public void setClosing(boolean isClosing){
		this.closing = isClosing;
	}
	public boolean isClosing(){
		return this.closing;
	}

	private boolean closed = false;
	public boolean hasClosed(){
		return this.closed;
	}
	
	@Override
	public void close() throws IOException {
		if(!isClosing())
			setClosing(true);
		if(!isTerminating())
			setTerminating(true);
		while(!this.hasTerminated())
			;
		if(this.onCloud){
			this.table.close();
			this.grid.closeTable();
		}
		
		this.customizedClose();
		
		this.closed = true;
	}
	protected void customizedClose() {

		
	}

	@Parameter(names = "--visualization", help = false)
	public boolean visualization;

	@Parameter(names = "--help", help = true)
	public boolean help;

	public HTable table = null;

	//check the parameters 
	@Parameter(names = {"--showser"}, description = "if show the SER or not, default is false", required = false)
	public boolean ifShowSER = false;

	@Parameter(names = {"--showparticles"}, description = "if show the particles or not, default is false", required = false)
	public boolean ifShowParticles = false;

	@Parameter(names = {"--showmeasurements"}, description = "if show the measurements or not, default is false", required = false)
	public boolean ifShowSensors = false;

	@Parameter(names = {"-sr","--safeRange"}, description = "the range of edge which wouldn't be used in process, must be greater than 1, default is 10 pixel.", required = false)
	public int safe_edge = 10;

	@Parameter(names = {"-c","--converge"}, description = "start up/stop debug mode, default is to start up", required = false, arity = 1)
	private boolean convergeFlag = false;

	@Parameter(names = "--ignore", description = "robot and mcls ignore network letancy, default is false to consider it.", required = false, arity = 1)
	public boolean ignore = false;
	
	@Parameter(names = {"-D","--debug"}, description = "start up/stop debug mode, default is to start up", required = false, arity = 1)
	public boolean mode = true;
	
	@Parameter(names = "--period", description = "the period of an executed time.", required = false)
	private int period = 0;

	//check the parameters 
	@Parameter(names = {"-cl","--cloud"}, description = "if there on the cloud is or not, default is false", required = false)
	public boolean onCloud = false;
	
	@Parameter(names = {"-o","--orientation"}, description = "the number of orientation of a cell, default is 18.", required = false)
	public int orientation = 18;
	
	//for Pre_caching()
	@Parameter(names = {"-i","--image"}, description = "the image of map, default is \"file:///home/eeuser/map1024.jpeg\"", required = false)
	public String mapFilename = "file:///home/eeuser/map1024.jpeg";
	
	//for Caculating_SER()
	@Parameter(names = {"-d","--delta"}, description = "the delta of SER, default is 0.01", required = false)
	public String deltaEnergyStr = null;
	public float deltaEnergy = (float)0.01;
	
	//for Determining_size()
	@Parameter(names = {"-x","--xi"}, description = "the sensitive coefficient, default is 0.1", required = false)
	public String xiStr = null;
	public float XI = (float)0.1;
	
	@Parameter(names = {"-a","--alpha"}, description = "the ratio of population(global:local), default is 0.6", required = false)
	public String alpha = null;
	public float ALPHA = (float)0.6;
	
	@Parameter(names = {"-n","--number"}, description = "the number of total population, default is 100 particles.", required = false)
	public int Nt = 100;

	@Parameter(names = {"-t","--tableName"}, description = "the name of HBase table, default is \"map.512.4.split\"", required = false)
	public String tableName = "map.512.4.split";
	
	@Parameter(names = {"-p","--presure"}, description = "the tournament presure, default is 10 particles.", required = false)
	private int tournamentPresure = 10;
	
	@Parameter(names = {"-max_dist"}, description = "the max distance the sensor can measure in the unit of pixel, default is the diagonal length of input map image.", required = false)
	public int max_distance = -1;
	private int sensorNumber;
	//unused
	//private double orientationDeltaDegree;
	//private double sensorDeltaDegree;
	private int width;
	private int height;
	//for Pre_caching()
	public Grid grid;
	//for Sample_total_particles()
	
	//for Determining_size()
	protected int Nl;
	protected int Ng;
	public double[] al;
	
	//for sensor model of particle filter
	public MCLLaserSensor sensor = null;
	
	@Parameter(names = {"-sm","--sensor_model"}, description = "selection of sensor model of particle filter, default model is importance factor.", required = false)
	public int sensormodel_selection = 1;

	/**
	 * input:Map(M)
	 * output:3-Dimentional grid(G3D),energy grid(GE)
	 * needed function:laser ranger, 
	 * properties:Sensor number,
	 * @throws MalformedURLException 
	 */
	public void preCaching(/*Map.jpg*/) {
		//start to computing all of the grid of 3-dimension
		System.out.println("computing...");
		long startTime = System.currentTimeMillis();
		grid.pre_compute();
		long endTime = System.currentTimeMillis() - startTime;
		System.out.println("estimated_time:"+endTime+"ms");
	}
	
	/**
	 * run SAMCL
	 * @throws Throwable 
	 */
	public void run(RobotState robot, JFrame samcl_window) throws Throwable{
		boolean mode = false;
		this.setTerminating(false);
		this.setTerminated(false);
		
		System.out.println("press enter to continue.");
		System.in.read();
		System.out.println("start!");

		List<Particle> local_set = new ArrayList<Particle>();
		List<Particle> global_set = new ArrayList<Particle>();
		//in order to be thread-safe, use CopyOnWriteArrayList.
		List<Particle> current_set = new CopyOnWriteArrayList<Particle>();
		List<Particle> last_set = new CopyOnWriteArrayList<Particle>();
		List<Particle> SER_set = new CopyOnWriteArrayList<Particle>();
		Particle bestParticle = null;
		
		//Drawing the image
		BufferedImage samcl_image = new BufferedImage(this.grid.width,this.grid.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D grap = samcl_image.createGraphics();
		grap.drawImage(this.grid.map_image, null, 0, 0);
		Panel image_panel = new Panel(samcl_image);
		samcl_window.add(image_panel);
		samcl_window.setVisible(this.visualization);
		
		//initial motion model for particle filter
		Pose previousPose = new Pose((Pose)robot);
		Pose currentPose = null;
		VelocityModel u = new VelocityModel();
		
		//initial sensor model for particle filter
		if(this.sensormodel_selection==1){
			this.sensor = new MCLLaserSensor(ModelType.BEAM_MODEL);
		}else if(this.sensormodel_selection==2){
			this.sensor = new MCLLaserSensor(ModelType.LOSS_FUNCTION);
		}else
			this.sensor = new MCLLaserSensor(ModelType.BEAM_MODEL);
		this.sensor.setupSensor();
		
		//Initial Particles 
		this.globalSampling(last_set, robot);
		
		System.out.print(
						"counter\t"	+
						"time\t"+
						"duration\t"+
						"weight time\t"+
						"SER time\t"+
						"transmission time\t"+
						"best X\t"+
						"best Y\t"+
						"best H\t"+
						"best W\t"+
						"robot X\t"+
						"robot Y\t"+
						"robot H\t"+
						"\n");
		
		int counter = 0;
		long time = 0, duration = 0, startTime = System.currentTimeMillis();
		while(!this.isTerminating()){
			
			if(this.convergeFlag){
				this.converge(last_set, robot);
			}
			
			time = System.currentTimeMillis();
			counter = counter +1;
			
			//Setp 1: Prediction
			Transformer.debugMode(mode, "(1) Sampling\t");
			currentPose = new Pose(robot);
			u.setModel(robot.getUt());
			current_set.clear();
			long sampleTime = this.predictionParticles(last_set, current_set, u, currentPose, previousPose, duration);
			previousPose = currentPose;
			
			
			//Step 2: Weighting
			Transformer.debugMode(mode, "(2) Weighting\t");
			long transmission1, weightTime, transmission2;
			//TODO Zt has to be changed into LaserData
			float[] Zt = null;	
			//optimality is changed into maximizing.
			long[] times = this.weightAssignment(current_set, Zt, ignore, robot);
			transmission1 = times[0];
			weightTime = times[1];
			transmission2 = times[2];
			//Step 3: Determining size
			Transformer.debugMode(mode, "(3) Determining size\t");
			long determiningTime = System.currentTimeMillis();
			//optimality is changed into maximizing.
			//bestParticle = Transformer.minParticle(current_set); 
			bestParticle = Transformer.maxParticle(current_set); 
			this.determiningSize(bestParticle);
			determiningTime = System.currentTimeMillis() - determiningTime;
			//Step 3-1: Calculating SER
			Transformer.debugMode(mode, "(3-1) Calculating SER\t");
			long serTime = System.currentTimeMillis();
			//optimality is changed into maximizing. done!
			this.caculatingSER(current_set, bestParticle.getWeight(), Zt, SER_set, global_set);
			serTime = System.currentTimeMillis() - serTime;
			
			//Step 4: Local resampling
			Transformer.debugMode(mode, "(4) Local resampling\t");
			long localResamplingTime = System.currentTimeMillis();
			//optimality is changed into maximizing. done!
			this.localResampling(current_set, local_set, bestParticle);
			localResamplingTime = System.currentTimeMillis() - localResamplingTime;
//			System.out.println("\tlocal set size : \t" + local_set.size());
			
			//Step 5: Combimining
			Transformer.debugMode(mode, "(5) Combimining\n");
			long combiminingTime = System.currentTimeMillis();
			last_set.clear();
			last_set.addAll(this.combiningSets(local_set, global_set));
			local_set.clear();
			global_set.clear();
			combiminingTime = System.currentTimeMillis() - combiminingTime;
			
			//TODO Averagin Particle
			long averageTime = System.currentTimeMillis();
//			Particle averagePose = this.averagePose(current_set);
			averageTime = System.currentTimeMillis() - averageTime;
			//show out the information
			/**
			 * best particle
			 * average position
			 * robot position
			 * time
			 * is succeeded?
			 * */
			//log()
			
			
			
			
			this.delay(this.period);
			
			//draw image 
			long drawingTime = System.currentTimeMillis();
			if(this.visualization)
				this.drawing(grap, samcl_window, robot, bestParticle, current_set, SER_set);
			drawingTime = System.currentTimeMillis() - drawingTime;
			
			//update image
			image_panel.repaint();
			if(this.ignore)
				duration = System.currentTimeMillis() - time - transmission1;
			else
				duration = System.currentTimeMillis() - time;
			

			Transformer.debugMode(false,
					"Best position          :"+bestParticle.toString()+"\n",
					"Robot position         : \t" + robot+"\n",
					"Sensitive              : \t" + this.XI+"\n",
					"RPC counter            : \t" + this.grid.RPCcount+"\n",
					"Sampling Time	        : \t" + sampleTime + "\tms"+"\n",
					"Weighting Time	        : \t" + weightTime + "\tms"+"\n",
					"Determing Size Time    : \t" + determiningTime + "\tms"+"\n",
					"Caculating SER Time    : \t" + serTime + "\tms"+"\n",
					"Local Resampling Time  : \t" + localResamplingTime + "\tms"+"\n",
					"Combining Time	        : \t" + combiminingTime + "\tms"+"\n",
					"Averaging Time	        : \t" + averageTime + "\tms"+"\n",
					"Drawing Time	        : \t" + drawingTime + "\tms"+"\n",
					"Default Delay Time     : \t" + this.period + "\tms"+"\n",
					"Alpha argument         : \t" + Arrays.toString(this.al) + "\n",
					
					"*************************\n"
					);
			
			this.grid.RPCcount = 0;
			
			
			if(this.mode){
				System.out.print(
						"counter\t"	+
						"time\t"+
						"duration\t"+
						"weight time\t"+
						"SER time\t"+
						"transmission time\t"+
						"best X\t"+
						"best Y\t"+
						"best H\t"+
						"best W\t"+
						"robot X\t"+
						"robot Y\t"+
						"robot H\t"+
						"\n");
			}
			System.out.format("%5d\t",counter);
			System.out.format("%5d\t",time-startTime);
			System.out.format("%5d\t",duration);
			System.out.format("%5d\t",weightTime);
			System.out.format("%5d\t",duration - weightTime - transmission1 - transmission2);
			System.out.format("%5d\t",serTime);
			System.out.format("%5d\t",transmission1);
			System.out.format("%5d\t",transmission2);
			System.out.format("%.5f\t",bestParticle.getDX());
			System.out.format("%.5f\t",bestParticle.getDY());
			System.out.format("%.5f\t",bestParticle.getTh());
			System.out.format("%.5f\t",bestParticle.getWeight());
			System.out.format("%.5f\t",robot.X);
			System.out.format("%.5f\t",robot.Y);
			System.out.format("%.5f\t",robot.H);
			System.out.println();
			
//			Transformer.log(
//					"counter:", counter,
//					"time", time,
//					"duration:", duration,
//					"batch weight time: ", weightTime,
//					"SER duration", serTime,
//					bestParticle, 
//					robot, 
//					averagePose);
		
		}
		this.setTerminated(true);
	}
	
	public void globalSampling(List<Particle> set, RobotState robot){
		Random rand = new Random();
		for (int i = 0; i < this.Nt; i++) {
			Particle p = new Particle(
				safe_edge+rand.nextInt(this.width-(2*safe_edge)), 
				safe_edge+rand.nextInt(this.height-(2*safe_edge)), 
				Transformer.Z2Th(rand.nextInt(this.orientation), orientation));
			while (
					!p.underSafeEdge(this.width, this.height, this.safe_edge) ||
					this.grid.map_array(p.getX(), p.getY()) == Grid.GRID_OCCUPIED) {
				p.setX(safe_edge+rand.nextInt(this.width-(2*safe_edge)));
				p.setY(safe_edge+rand.nextInt(this.height-(2*safe_edge)));
				p.setTh(Transformer.Z2Th(rand.nextInt(this.orientation), orientation));
				
				if(this.sensor.getModeltype().equals(ModelType.DEFAULT))
					p.setWeight(1/this.Nt);
				if(this.sensor.getModeltype().equals(ModelType.BEAM_MODEL))
					p.setWeight(1/this.Nt);
				else if(this.sensor.getModeltype().equals(ModelType.LOSS_FUNCTION))
					p.setWeight(-Float.MAX_VALUE);
			}
			set.add(p);
		}
	}
	
	public void converge(List<Particle> current, RobotState robot){
		current.clear();
		for(int i = 0;i< this.Nt;i++){
			current.add(new Particle(robot.X, robot.Y, robot.H));
		}
		this.convergeFlag = false;
	}
	
	@SuppressWarnings("unused")
	private Particle averagePose(List<Particle> src_set) {
		double xSum = 0;
		double ySum = 0;
		double zSum = 0;
		for(Particle p : src_set){
			xSum = xSum + p.getDX();
			ySum = ySum + p.getDY();
			zSum = zSum + p.getTh();
		}
		return new Particle(xSum/src_set.size(), ySum/src_set.size(), zSum/src_set.size());
	}
	
	/**
	 * input:last particles set(Xt-1),motion control(ut),3-Dimentional grid(G3D)
	 * output:particles(xt),weight(wt)
	 * @param duration 
	 * @throws Exception 
	 */
	public long predictionParticles(
			List<Particle> src,
			List<Particle> dst,
			VelocityModel u, 
			Pose currentPose,
			Pose previousPose,
			long duration) throws Exception{
		long sampleTime = System.currentTimeMillis();

		Random random = new Random();
		if(!src.isEmpty()){
			first:
				for(Particle p : src){
					int i = 0;
					do{
						if(i>10)
							continue first;
						i++;
//						Distribution.MotionSampling(p, u, (double)duration/1000, random, this.al); 
						Distribution.OdemetryMotionSampling(p, currentPose, previousPose, duration, random, al);
					}while(!Distribution.boundaryCheck(p, this.grid));
					
					dst.add(p.clone());
					
					//this.pixel_sampling(p, 11, 7, random);
				}
		}
		else{
			System.out.println("*********the src_set is empty!!!!!!");
			throw new Exception("The set is empty!\n");
		}
		if(dst.size()==0)
			throw new Exception("there is no result!!!!");
		return System.currentTimeMillis() - sampleTime;
	}
	
	public long[] weightAssignment( List<Particle> src, float[] robotMeasurements, boolean ignore, RobotState robot) throws Exception{
		boolean previousState = robot.isLock1();
		long[] timers = new long[3];
		
		if(ignore)
			robot.lock1();
		
		timers[0] = this.raycastingUpdate(src);
		
		if(ignore)
			robot.setLock1(previousState);
		//update robot's sensor
		robotMeasurements = robot.getMeasurements();
		
		/*if(robotMeasurements == null)
			System.out.println("something wrong.");*/
		
		timers[1] = this.batchWeight(src, robotMeasurements);
		timers[2] = 0;
		return timers;
	}
	
	public long raycastingUpdate( List<Particle> src) throws Exception {
		long trasmission = System.currentTimeMillis();
		//get sensor data of all particles.
		if (this.onCloud) {
			//get measurements from cloud  and weight
			try {
				this.grid.getBatchFromCloud2(this.table, src);
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				for(Particle p: src){
					System.out.println(Transformer.xy2RowkeyString(p.getDX(), p.getDY()));
				}
				System.exit(1);
			}
		} else {
			//get measurements from local database and weight
			/*int count=0;*/
			for (Particle p : src) {
				/*if(*/
					this.grid.getMeasurementsAnyway( this.table, this.onCloud, p)
							/*==true)*/
					/*count++*/;
			}
			/*System.out.println(count+ " particles have measurements.");*/
		}
		return System.currentTimeMillis() - trasmission;
	}
	
	public long batchWeight(List<Particle> src, float[] robotMeasurements) throws Exception {
		long weightTime = System.currentTimeMillis();
		MCLLaserSensorData data = this.sensor.new MCLLaserSensorData(robotMeasurements);
		this.sensor.callableModelFunction.updateModel(src, data );
		Float total = this.sensor.callableModelFunction.call();
		
		/*System.out.println("total is " + total);*/
		/*if (robotMeasurements!=null) {
			float total = 0.0f;
			for(Particle p : src){
				this.weightAParticle(p, robotMeasurements);
				total += p.getWeight();
			}
			//normalization is needed by regular method of particle filter.
			if(total>0){
				for(Particle p : src){
					p.setWeight(p.getWeight()/total);
				}
			}
		}else{
			throw new Exception("robot didn't get the sensor data!!!");
		}*/
		
		return System.currentTimeMillis() - weightTime;
	}
	
	@SuppressWarnings("unused")
	@Deprecated
	private void weightAParticle(Particle p, float[] robotMeasurements) throws Exception{
		//if the position is occupied.		
		if( this.grid.map_array(p.getX(), p.getY())==Grid.GRID_EMPTY ) {
			//if the particle has got the measurements or would get measurements from Grid
			if(p.isIfmeasurements()){//FIXME
				//optimality is changed into maximizing. done!
				float w = 
				Transformer.WeightFloat_BeamModel(p.getMeasurements(), robotMeasurements);
				p.setWeight(w*p.getWeight());
			}else{
				throw new Exception("no measurement in the particle: " + p+". Raycasting might be failed.");
				/*//Cloud , Grid class------done
				float[] measurements = this.grid.getMeasurements(this.table, onCloud, p.getX(), p.getY(), p.getTh());
				p.setWeight(Transformer.WeightFloat(measurements, robotMeasurements));*/
			}
		}else{
			//if the position is occupied, then assign the worst weight.
			//optimality is changed into maxmizing so this value is the worst case. done!
			if(this.sensor.getModeltype().equals(ModelType.DEFAULT))
				p.setWeight(0);
			else if(this.sensor.getModeltype().equals(ModelType.BEAM_MODEL))
				p.setWeight(0);
			else if(this.sensor.getModeltype().equals(ModelType.LOSS_FUNCTION))
				p.setWeight(-Float.MAX_VALUE);
		}
	}
	
	/**
	 * parameters:sensitive coefficient(Threshold),the ratio of the global samples and local samples(Alpha)
	 * input:the maximum of weight(wmax),total number of particles(NT)
	 * output:the number of global samples(NG),the number of local samples(NL)
	 */
	public void determiningSize(Particle bestParticle){
		//Particle bestParticle = Transformer.minParticle(src);
		if (bestParticle.getWeight() < this.XI) {
			this.Nl = this.Nt;
		}	
		else {
			this.Nl = (int) (this.ALPHA * this.Nt);
		}
		this.Ng = this.Nt - this.Nl;
		
	}
		
	/**
	 * input:measurement(Zt),energy grid(GE)
	 * output:similar energy region(SER)
	 * @param current_set 
	 * @param best_weight 
	 * @throws IOException 
	 */
	public void caculatingSER(List<Particle> current_set, float best_weight, float[] Zt, List<Particle> SER_set, List<Particle> global_set) throws Exception{
		
		float minus_total_weight = 0f;
		float normalized_weight = 0;
		
		if(this.sensor.getModeltype().equals(ModelType.DEFAULT))
			normalized_weight = best_weight; 
		else if(this.sensor.getModeltype().equals(ModelType.BEAM_MODEL))
			normalized_weight = best_weight; 
		else if(this.sensor.getModeltype().equals(ModelType.LOSS_FUNCTION)){
			for(Particle p: current_set){
				if(p.getWeight()!=-Float.MAX_VALUE)
					minus_total_weight+=p.getWeight();
			}
			normalized_weight = 1 - best_weight/minus_total_weight;
		}
		
		//optimality is changed into maximizing. done!
		//If normalized best weight ranging from 0.0 to 1.0 is lower than a positive parameter XI
		//then SER is calculated.
		//if (best_weight > this.XI)
		if (normalized_weight<this.XI) {//if do calculate SER or not?/
			/*Get the reference energy*/
			float energy = Transformer.CalculateEnergy(Zt,this.grid.max_distance);
			float UpperBoundary = energy + this.deltaEnergy;
			float LowerBoundary = energy - this.deltaEnergy;
			if(LowerBoundary<0)
				LowerBoundary = 0.0f;
			if(UpperBoundary>1.0f)
				UpperBoundary = 1.0f;
			SER_set.clear();
			if (onCloud) {
				this.cloudCaculatingSER(SER_set, LowerBoundary, UpperBoundary);
			}else{
				this.localCaculatingSER(SER_set, LowerBoundary,  UpperBoundary);
			}
			//Step 3-2: Global resampling
//			System.out.println("(4)\tGlobal resampling\t");
			long globalResampleTime = System.currentTimeMillis();
			if (SER_set.size()>0) {
				global_set.clear();
				int randIdx;
				Random random = new Random();
				for (int i = 0; i < this.Ng; i++) {
					randIdx = random.nextInt(SER_set.size());
					//Particle particleC = src.get(rand).clone();
					Particle particle = new Particle(
							SER_set.get(randIdx).getDX(), 
							SER_set.get(randIdx).getDY(),
							SER_set.get(randIdx).getTh());
					global_set.add(particle);
					SER_set.remove(randIdx);
					//System.out.println("SER particle "+i +":"+particle.toString() );
				}
			}else{
				System.out.println("source set is empty");
			}
			globalResampleTime = System.currentTimeMillis() - globalResampleTime;
			
		}
	}
	
	private void cloudCaculatingSER(List<Particle> SER_set, float LowerBoundary, float UpperBoundary) throws IOException{
		SER_set.clear();
		//TODO return List<Particle>
		//System.out.println("start to scan in caculating SER on the cloud");
		this.grid.scan(this.table, SER_set, LowerBoundary, UpperBoundary);
	}

	private void localCaculatingSER(List<Particle> SER_set, float LowerBoundary, float UpperBoundary){
		SER_set.clear();
		for(int x = this.safe_edge ; x < this.width-this.safe_edge ; x++){
			for (int y = this.safe_edge; y < this.height-this.safe_edge; y++) {
				if (this.grid.map_array(x, y) == Grid.GRID_EMPTY) {
					for(int z = 0; z < this.orientation; z++){
						/**
						 * Define a position of the SER
						 * Add this (x,y,z) to the SER_set 
						 */
						float temp1 = this.grid.G[x][y].getEnergy(z);
						if( temp1 >= LowerBoundary &&
								temp1 <= UpperBoundary){
							SER_set.add(new Particle(x, y, Transformer.Z2Th(z, this.orientation)));
							//Particle parti = new Particle(x,y,r.nextInt(this.orientation));
							//SER_set.add(parti);
						}
					}
				}
			}
		}
	}

	/**
	 * input:the number of local samples(NL),particles(xt),weight(wt)
	 * output:XLt
	 */
	public void localResampling(List<Particle> src, List<Particle> dst, Particle bestParticle){
		dst.clear();
		dst.add(bestParticle);
		for (int i = dst.size(); i < this.Nl; i++) {
			//Roulette way
			//Tournament way
			//optimality is changed into maximizing. done!
			Particle particle = Transformer.tournament(tournamentPresure, src);
			dst.add(particle.clone());
			//System.out.println();
		}
		System.out.println(bestParticle);
	}
	
	

	/**
	 * input:XLt,XGt
	 * output:Xt
	 */
	private List<Particle> combiningSets(List<Particle> set1, List<Particle> set2){
		List<Particle> results = new ArrayList<Particle>(set1);
		results.addAll(set2);
		return results;
	}
	
	private void delay(int milliSecond) {
		try {
			if(milliSecond >0)
				Thread.sleep(milliSecond);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public void drawing(Graphics2D grap, JFrame window
			, RobotState robot, Particle bestParticle, List<Particle> particles, List<Particle> SER){
		//Graphics2D grap = samcl_image.createGraphics();
		grap.drawImage(this.grid.map_image, null, 0, 0);
		
		//SER
		if (SER.size() >= 1&&this.ifShowSER) {
			Tools.drawBatchPoint(grap, SER, 1, Color.PINK);
		}
		
		//Particles
		if(this.ifShowParticles){
			//Tools.drawBatchPoint(grap, particles, 2, Color.BLUE);
			for(Particle p: particles){
				Tools.drawRobot(grap, p.getX(), p.getY(), p.getTh(), 4, Color.BLUE);
			}
		}
		
		//Measurements
		if(this.ifShowSensors){
			//robot sensor hits
			for(Point p: robot.getMeasurement_points()){
				Tools.drawPoint(grap, p.x, p.y, -1.0, 4, Color.RED);
			}
		}
		
		//Robot
		Tools.drawRobot(grap, (int)Math.round(robot.X), (int)Math.round(robot.Y), robot.H, 10, Color.RED);
		
		
		
		//Best Particle
		Tools.drawRobot(grap, bestParticle.getX(), bestParticle.getY(), bestParticle.getTh(), 8, Color.GREEN);
		
	}
	public void forceConverge(){
		this.convergeFlag = true;
	}

}
