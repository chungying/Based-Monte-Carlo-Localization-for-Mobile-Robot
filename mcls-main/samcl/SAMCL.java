/**
 * 
 */
package samcl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JFrame;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;

import robot.VelocityModel;
import robot.RobotState;
import util.gui.Panel;
import util.gui.Tools;
import util.metrics.Distribution;
import util.metrics.Particle;
import util.metrics.Transformer;

import com.beust.jcommander.Parameter;
import com.google.protobuf.ServiceException;

/**
 * @author w514
 *part1:Sampling total particles
 *part2:Determining the size of the global sample set and the local sample set
 *part3:Resampling local samples
 *part4:Drawing global samples
 *part5:Combining two particle sets
 */
public class SAMCL implements Closeable{
	
	private boolean terminated = false;
	public void setTerminated(boolean terminated) {
		this.terminated = terminated;
	}

	
	
	//	public boolean isClosing;
	/**
	 * run SAMCL
	 * @throws Throwable 
	 */
	public void run(RobotState robot, JFrame samcl_window) throws Throwable{
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
		
		//Drawing the image
		BufferedImage samcl_image = new BufferedImage(this.grid.width,this.grid.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D grap = samcl_image.createGraphics();
		grap.drawImage(this.grid.map_image, null, 0, 0);
		//TODO WINDOW
		Panel image_panel = new Panel(samcl_image);
		samcl_window.add(image_panel);
		samcl_window.setVisible(true);
		
		//Initial Particles and Painting
		for (int i = 0; i < this.Nt; i++) {
			last_set.add(this.global_sampling());
		}
		
		int counter = 0;
		long time = 0;
		long duration = 0;
		while(!this.isTerminated()){
			time = System.currentTimeMillis();
			counter = counter +1;
			
			//update robot's sensor
			float[] Zt = robot.getMeasurements();
			
			//Setp 1: Sampling
//			System.out.println("(1)\tSampling\t");
			Transformer.debugMode(mode, "(1)\tSampling\n");
			long sampleTime = System.currentTimeMillis();
			//TODO Particle
			current_set.clear();
			current_set.addAll(this.Prediction_total_particles(last_set, robot.getUt(), duration));
			sampleTime = System.currentTimeMillis() - sampleTime;
			
			//Step 2: Weighting
//			System.out.println("(2)\tWeighting\t");
			Transformer.debugMode(mode, "(2)\tWeighting\n");
			long weightTime = System.currentTimeMillis();
			//TODO Particle
			this.batchWeight(current_set, Zt);
			weightTime = System.currentTimeMillis() - weightTime;
			
			//Step 3: Determining size
//			System.out.println("(3)\tDetermining size\t");
			Transformer.debugMode(mode, "(3)\tDetermining size\n");
			long determiningTime = System.currentTimeMillis();
			Particle bestParticle = this.Determining_size(current_set);
			determiningTime = System.currentTimeMillis() - determiningTime;
			//Step 3-1: Calculating SER
			Transformer.debugMode(mode, "(3-1)\tCalculating SER\n");
			long serTime = System.currentTimeMillis();
			this.Caculating_SER(bestParticle.getWeight(), Zt, SER_set, global_set);
			serTime = System.currentTimeMillis() - serTime;
			
			//Step 4: Local resampling
//			System.out.println("(4)\tLocal resampling\t");
			Transformer.debugMode(mode, "(4)\tLocal resampling\n");
			long localResamplingTime = System.currentTimeMillis();
			this.Local_resampling(current_set, local_set, bestParticle);
			localResamplingTime = System.currentTimeMillis() - localResamplingTime;
//			System.out.println("\tlocal set size : \t" + local_set.size());
			
			//Step 5: Combimining
//			System.out.println("(5)\tCombimining\t");
			Transformer.debugMode(mode, "(5)\tCombimining\n");
			long combiminingTime = System.currentTimeMillis();
			last_set.clear();
			last_set.addAll(this.Combining_sets(local_set, global_set));
			combiminingTime = System.currentTimeMillis() - combiminingTime;
//			System.out.println("\tnext set size: \t" + last_set.size());
			
			//TODO Particle
			Particle averagePose = this.averagePose(current_set);
			//show out the information
			/**
			 * best particle
			 * average position
			 * robot position
			 * time
			 * is succeeded?
			 * */
			//log()
			
			//Transformer.log(this.isTerminated());
			Transformer.debugMode(mode,
					"Best position          :"+bestParticle.toString()+"\n",
					"Robot position         : \t"+robot.getPose().toString()+"\n",
					"Sensitive              : \t" + this.XI+"\n",
					"RPC counter            : \t" + this.grid.RPCcount+"\n",
					"Sampling Time	        : \t" + sampleTime + "\tms"+"\n",
					"Weighting Time	        : \t" + weightTime + "\tms"+"\n",
					"Determing Size Time    : \t" + determiningTime + "\tms"+"\n",
					"Caculating SER Time    : \t" + serTime + "\tms"+"\n",
					"Local Resampling Time  : \t" + localResamplingTime + "\tms"+"\n",
					"Combining Time	        : \t" + combiminingTime + "\tms"+"\n",
					"*************************\n"
					);
			
			this.grid.RPCcount = 0;
			this.delay(this.period);
			
			//draw image 
			//TODO Particle
			this.Drawing(grap, samcl_window, robot, bestParticle, current_set, SER_set);
			
			//update image
			image_panel.repaint();
			duration = System.currentTimeMillis() - time;
			Transformer.log(
					"counter:", counter,
					"time", time,
					"duration:", duration,
					"batch weight time: ", weightTime,
					"SER duration", serTime,
					bestParticle, 
					robot.getPose(), 
					averagePose);
			
//			Transformer.log(
//					"batch weight time: " + weightTime
//					);
		}
	}
	
	private boolean isTerminated() {
		return this.terminated ;
	}

	private Particle averagePose(List<Particle> src_set) {
		double xSum = 0;
		double ySum = 0;
		double zSum = 0;
		for(Particle p : src_set){
			xSum = xSum + p.getX();
			ySum = ySum + p.getY();
			zSum = zSum + p.getTh();
		}
		return new Particle(xSum/src_set.size(), ySum/src_set.size(), zSum/src_set.size());
	}

	private void delay(int milliSecond) {
		try {
			if(milliSecond >0)
				Thread.sleep(milliSecond);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void setup() throws Exception{
		if(this.deltaEnergyStr!=null)
			this.deltaEnergy = Float.parseFloat(deltaEnergyStr);
		if(this.alpha!=null)
			this.ALPHA = Float.parseFloat(this.alpha);
		if(this.xiStr!=null)
			this.XI = Float.parseFloat(xiStr);
		
		this.orientationDeltaDegree = 360/this.orientation;
		this.sensorNumber = this.orientation/2 + 1;
		this.sensorDeltaDegree = this.orientationDeltaDegree;
		
		this.grid= new Grid(this.orientation, this.sensorNumber, this.mapFilename);
		
		if(this.onCloud){
			System.out.println("cloud setup");
			this.cloudSetup();
		}else{
			System.out.println("local setup");
			this.localSetup();
		}
		this.customizedSetup();
	}
		public void customizedSetup() throws Exception{ 
	}
	
	private void localSetup(){
		grid.readmap();
		/**
		 *  the initialization of SAMCL 
		 */
		this.width = this.grid.width;
		this.height = this.grid.height;
		//TODO add a condition to choose if start mouse function or not
		//precomputed_grid.start_mouse(precomputed_grid);
	}
	
	public HTable table = null;
	private void cloudSetup() throws IOException{
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
	}
	
	//check the parameters 
	@Parameter(names = {"--showser"}, description = "if show the SER or not, default is false", required = false)
	public boolean ifShowSER = false;
	@Parameter(names = {"--showparticles"}, description = "if show the particles or not, default is false", required = false)
	public boolean ifShowParticles = false;
	@Parameter(names = {"--showmeasurements"}, description = "if show the measurements or not, default is false", required = false)
	public boolean ifShowSensors = false;
	
	public void Drawing(Graphics2D grap, JFrame window
				, RobotState robot, Particle bestParticle, List<Particle> particles, List<Particle> SER){
			//Graphics2D grap = samcl_image.createGraphics();
			grap.drawImage(this.grid.map_image, null, 0, 0);
			
			//Robot
			Tools.drawRobot(grap, robot.getX(), robot.getY(), robot.getHead(), 10, Color.RED);
	
			//Best Particle
			Tools.drawRobot(grap, bestParticle.getX(), bestParticle.getY(), bestParticle.getTh(), 8, Color.GREEN);
	
			//SER
			if (SER.size() >= 1&&this.ifShowSER) {
				Tools.drawBatchPoint(grap, SER, 1, Color.PINK);
			}
			
			//Particles
			if(this.ifShowParticles)
				Tools.drawBatchPoint(grap, particles, 2, Color.BLUE);;
			
			//Measurements
			if(this.ifShowSensors){
				;
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
	
	@Parameter(names = {"-D","--debug"}, description = "start up/stop debug mode, default is to start up", required = false, arity = 1)
	public boolean mode = true;
	
	@Parameter(names = "--period", description = "the period of an executed time.", required = false)
	private int period = 3;

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
	
	private double orientationDeltaDegree;
	private int sensorNumber;
	@SuppressWarnings("unused")
	private double sensorDeltaDegree;//TODO don't need
	private int width;
	private int height;
	//for Pre_caching()
	public Grid grid;
	//for Sample_total_particles()
	
	//for Determining_size()
	protected int Nl;
	protected int Ng;
	//for Local_resampling()
	
	
	
	
	
	/**
	 * input:Map(M)
	 * output:3-Dimentional grid(G3D),energy grid(GE)
	 * needed function:laser ranger, 
	 * properties:Sensor number,
	 * @throws MalformedURLException 
	 */
	public void Pre_caching(/*Map.jpg*/) {
		//start to computing all of the grid of 3-dimension
		System.out.println("computing...");
		long startTime = System.currentTimeMillis();
		grid.pre_compute();
		long endTime = System.currentTimeMillis() - startTime;
		System.out.println("estimated_time:"+endTime+"ms");
	}
		
	/**
	 * input:measurement(Zt),energy grid(GE)
	 * output:similar energy region(SER)
	 * @param weight 
	 * @throws IOException 
	 */
	public void Caculating_SER(float weight, float[] Zt, List<Particle> SER_set, List<Particle> global_set) throws IOException{
		if (weight>this.XI) {//if do calculate SER or not?
			/*Get the reference energy*/
			float energy = Transformer.CalculateEnergy(Zt);
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
			this.Global_drawing(SER_set, global_set);
			globalResampleTime = System.currentTimeMillis() - globalResampleTime;
//			System.out.println("\tglobal set size: \t" + global_set.size());
			
		}
	}
	
	/**
	 * input:last particles set(Xt-1),motion control(ut),3-Dimentional grid(G3D)
	 * output:particles(xt),weight(wt)
	 * @param duration 
	 * @throws Exception 
	 */
	public List<Particle> Prediction_total_particles(
			List<Particle> src, 
			VelocityModel u, 
			long duration) throws Exception{
		List<Particle> result = new ArrayList<Particle>();
		if(!src.isEmpty()){
			Random random = new Random();
			first:
				for(Particle p : src){
					int i = 0;
					do{
						if(i>10)
							continue first;
						i++;
						Distribution.Motion_sampling(p,this.orientation, u, duration/1000, random); 
					}while(!Distribution.boundaryCheck(p, this.grid));
					
					result.add(p.clone());
					
					//this.pixel_sampling(p, 11, 7, random);
				}
		}
		else{
			System.out.println("*********the src_set is empty!!!!!!");
//			throw new Exception("The set is empty!\n");
		}
		return result;
	}
	
	public void batchWeight(List<Particle> src, float[] robotMeasurements) throws IOException, ServiceException, Throwable {
		if (robotMeasurements!=null) {
			//get sensor data of all particles.
			if (this.onCloud) {
				//get measurements from cloud  and weight
				try {
					this.grid.getBatchFromCloud2(this.table, src);
				} catch (Exception e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
					for(Particle p: src){
						System.out.println(Transformer.xy2RowkeyString(p.getX(), p.getY()));
					}
					System.exit(1);
				}
				//this.grid.getBatchFromCloud2(this.table, src);
				for (Particle p : src) {
					this.WeightParticle(p, robotMeasurements);
				}
			} else {
				//get measurements from local database and weight
				for (Particle p : src) {
					p.setMeasurements(this.grid.getMeasurements(
							this.table, this.onCloud, p.getX(), p.getY(),
							p.getTh()));
					this.WeightParticle(p, robotMeasurements);
				}
			}
		}else{
			throw new Exception("robot didn't get the sensor data!!!");
		}
	}

	/**
	 * parameters:sensitive coefficient(Threshold),the ratio of the global samples and local samples(Alpha)
	 * input:the maximum of weight(wmax),total number of particles(NT)
	 * output:the number of global samples(NG),the number of local samples(NL)
	 */
	public Particle Determining_size(List<Particle> src){
		Particle bestParticle = Transformer.minParticle(src);
		if (bestParticle.getWeight() < this.XI) {
			this.Nl = this.Nt;
		}	
		else {
			this.Nl = (int) (this.ALPHA * this.Nt);
		}
		this.Ng = this.Nt - this.Nl;
		
		return bestParticle;
	}
	
	/**
	 * input:the number of local samples(NL),particles(xt),weight(wt)
	 * output:XLt
	 */
	public void Local_resampling(List<Particle> src, List<Particle> dst, Particle bestParticle){
		dst.clear();
		dst.add(bestParticle);
		for (int i = dst.size(); i < this.Nl; i++) {
			//Roulette way
			//Tournament way
			Particle particle = Transformer.tournament(tournamentPresure, src);
			dst.add(particle.clone());
			//System.out.println();
		}
	}
	
	

	/**
	 * input:the number of global samples(NG),similar energy region(SER)
	 * output:XGt
	 */
	public void Global_drawing(List<Particle> src, List<Particle> dst){
		if (src.size()>0) {
			dst.clear();
			int rand;
			Random random = new Random();
			for (int i = 0; i < this.Ng; i++) {
				rand = random.nextInt(src.size());
				//Particle particleC = src.get(rand).clone();
				Particle particle = new Particle(src.get(rand).getX(), src
						.get(rand).getY(),
						src.get(rand).getTh());
				dst.add(particle);
				src.remove(rand);
				//System.out.println("SER particle "+i +":"+particle.toString() );
			}
		}else{
			System.out.println("source set is empty");
		}
			
	}
	
	/**
	 * input:XLt,XGt
	 * output:Xt
	 */
	public List<Particle> Combining_sets(List<Particle> set1, List<Particle> set2){
		List<Particle> results = new ArrayList<Particle>(set1);
		results.addAll(set2);
		return results;
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
	
	@Parameter(names = {"-sr","--safeRange"}, description = "the range of edge which wouldn't be used in process, must be greater than 1, default is 10 pixel.", required = false)
	public int safe_edge = 10;
	private Particle global_sampling(){
		Random rand = new Random();
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
		}
		return p;
	}
	
	@SuppressWarnings("unused")
	private void pixel_sampling(
			Particle p, 
			int radius, 
			int angular, 
			Random random){//TODO static?

		int r ;
		r = random.nextInt(radius)-( radius - 1 )/2;
		int px = p.getX() + r;
		r = random.nextInt(radius)-( radius - 1 )/2;
		int py = p.getY() + r;
		
		while(this.grid.map_array(px, py) == Grid.GRID_OCCUPIED ){
			if(Particle.underSafeEdge(px, py, this.width, this.height, this.safe_edge) ==false){
				p = this.global_sampling();
			}
			r = random.nextInt(radius)-( radius - 1 )/2;
			px = p.getX() + r;
			r = random.nextInt(radius)-( radius - 1 )/2;
			py = p.getY() + r;
		}
		r = random.nextInt(angular)-( angular - 1 )/2;
		int pz = (/*p.getZ()*/Transformer.th2Z(p.getTh(), this.orientation)
				+ r + this.orientation) 
				% this.orientation;
		
		p.setX(px);
		p.setY(py);
		p.setTh(Transformer.Z2Th(pz, this.orientation));
	}
	
	public void WeightParticle(Particle p, float[] robotMeasurements) throws Exception{
		//if the position is occupied.
		if( this.grid.map_array(p.getX(), p.getY())==Grid.GRID_EMPTY ) {
			//if the particle has got the measurements or would get measurements from Grid
			if(p.isIfmeasurements()){//FIXME
				p.setWeight(Transformer.WeightFloat(p.getMeasurements(), robotMeasurements));
			}else{
				//Cloud , Grid class------done
				float[] measurements = this.grid.getMeasurements(this.table, onCloud, p.getX(), p.getY(), p.getTh());
				p.setWeight(Transformer.WeightFloat(measurements, robotMeasurements));
			}
		}else{
			//if the position is occupied, then assign the worst weight.
			p.setWeight(1);
		}
	}
	
	@Override
	public void close() throws IOException {
		if(this.onCloud){
			this.table.close();
			this.grid.closeTable();
		}
	}

}
