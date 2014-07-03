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
import util.gui.WindowListen;
import util.metrics.Distribution;
import util.metrics.Particle;
import util.metrics.Transformer;

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
	
	public void Drawing(Graphics2D grap, JFrame window
			, RobotState robot, Particle bestParticle, List<Particle> particles, List<Particle> SER){
		//TODO IMAGE
		//initial Graphics2D
		//BufferedImage samcl_image = new BufferedImage(this.precomputed_grid.width,this.precomputed_grid.height, BufferedImage.TYPE_INT_ARGB);
		
		//Graphics2D grap = samcl_image.createGraphics();
		grap.drawImage(this.precomputed_grid.map_image, null, 0, 0);
		
		
		//Robot
		Tools.drawRobot(grap, robot.getX(), robot.getY(), robot.getHead(), 10, Color.RED);

		//Best Particle
		Tools.drawRobot(grap, bestParticle.getX(), bestParticle.getY(), bestParticle.getTh(), 8, Color.GREEN);

		//SER
		if (SER.size() >= 1) {
			Tools.drawBatchPoint(grap, SER, 1, Color.PINK);
		}
		
		
		
	}
	
	
	public boolean isClosing;
	/**
	 * run SAMCL
	 * @throws IOException 
	 */
	public void run(RobotState robot, JFrame samcl_window) throws IOException{

		List<Particle> local_set = new ArrayList<Particle>();
		List<Particle> global_set = new ArrayList<Particle>();
		List<Particle> last_set = new ArrayList<Particle>();
		//in order to be thread-safe, use CopyOnWriteArrayList.
		List<Particle> next_set = new CopyOnWriteArrayList<Particle>();
		List<Particle> SER_set = new CopyOnWriteArrayList<Particle>();
		
		
		this.isClosing = false;
		//robot = new RobotState(32,41,0);
	
		System.out.println("press enter to continue.");
		System.in.read();
		//TODO IMAGE
		//Drawing the image
		BufferedImage samcl_image = new BufferedImage(this.precomputed_grid.width,this.precomputed_grid.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D grap = samcl_image.createGraphics();
		grap.drawImage(this.precomputed_grid.map_image, null, 0, 0);
				
		//Initial Particles and Painting
		last_set.clear();
		Particle p = null;
		for (int i = 0; i < this.Nt; i++) {
			p=this.global_sampling();
			last_set.add(p);
			//TODOend IMAGE
			//abolish paint the initial particles 2014/6/27
			//grap.drawOval(p.getX()-2, p.getY()-2, 4, 4);
		}
		
		//TODO WINDOW
		Panel image_panel = new Panel(samcl_image);
		//Painting on the Frame.		
		//JFrame samcl_window = new JFrame("samcl image");
		//samcl_window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		samcl_window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		WindowListen wl = new WindowListen(samcl_window, this.isClosing);
		samcl_window.addWindowListener(wl);
		//Show window
		samcl_window.setSize(width, height);
		//samcl_window.add(new JLabel(new ImageIcon(samcl_image)));
		samcl_window.add(image_panel);
		samcl_window.setVisible(true);
				
		//get the robot's range finder
		//float[] Zt = this.precomputed_grid.getMeasurements( onCloud, robot.getX(), robot.getY(), Transformer.th2Z(robot.getHead(), orientation_delta_degree) );
		float[] Zt = robot.getMeasurements();
		
		int counter = 0;
		
		while(wl.isClosing!=true){
			counter++;
			System.out.println(this.getClass().getName()+"\tGeneration\t"+counter+"\t-------------------");
			//update robot's sensor
			//Zt = this.precomputed_grid.getMeasurements( onCloud, robot.getX(), robot.getY(), Transformer.th2Z(robot.getHead(), this.orientation_delta_degree) );
			Zt = robot.getMeasurements();
			
			//Setp 1: Sampling
			System.out.println("(1)\tSampling\t");
			long sampleTime = System.currentTimeMillis();
			this.Prediction_total_particles(last_set, null, Zt);
			sampleTime = System.currentTimeMillis() - sampleTime;
			
			//Step 1-2: Weighting
			System.out.println("(1-2)\tWeighting\t");
			long weightTime = System.currentTimeMillis();
			this.batchWeight(last_set, Zt);
			weightTime = System.currentTimeMillis() - weightTime;
			
			//Step 2: Determining size
			System.out.println("(2)\tDetermining size\t");
			long determiningTime = System.currentTimeMillis();
			Particle max_p = this.Determining_size(last_set);
			determiningTime = System.currentTimeMillis() - determiningTime;
			long serTime = System.currentTimeMillis();
			if (max_p.getWeight()>this.XI) {
				System.out.println("\tCaculating SER\t");
				this.Caculating_SER(Zt, SER_set);
				System.out.println("\tSER set size = \t"+SER_set.size());
				serTime = System.currentTimeMillis() -serTime;
			}else{
				serTime = System.currentTimeMillis() - serTime;
			}
			
			//Step 3-1: Local resampling
			System.out.println("(3)\tLocal resampling\t");
			long localResamplingTime = System.currentTimeMillis();
			this.Local_resampling(last_set, local_set);
			localResamplingTime = System.currentTimeMillis() - localResamplingTime;
			System.out.println("\tlocal set size : \t" + local_set.size());
			
			//Step 3-2: Global resampling
			System.out.println("(4)\tGlobal resampling\t");
			long globalResampleTime = System.currentTimeMillis();
			this.Global_drawing(SER_set, global_set);
			globalResampleTime = System.currentTimeMillis() - globalResampleTime;
			System.out.println("\tglobal set size: \t" + global_set.size());
			
			//Step 4: Combimining
			System.out.println("(5)\tCombimining\t");
			long combiminingTime = System.currentTimeMillis();
			next_set.clear();
			next_set.addAll(this.Combining_sets(local_set, global_set));
			combiminingTime = System.currentTimeMillis() - combiminingTime;
			System.out.println("\tnext set size: \t" + last_set.size());
			//this.last_set = this.next_set;
			last_set.clear();
			last_set.addAll(next_set);
			
			//show out the information
			System.out.println("Best position:"+max_p.toString());
			System.out.println("Sensitive           : \t" + this.XI);
			System.out.println("RPC counter         : \t"+this.precomputed_grid.RPCcount);
			this.precomputed_grid.RPCcount = 0;
			System.out.println("Sampling Time		: \t" + sampleTime + "\tms");
			System.out.println("Weighting Time		: \t" + weightTime + "\tms");
			System.out.println("Determing Size Time	: \t" + determiningTime + "\tms");
			System.out.println("Caculating SER Time	: \t\t\t\t\t" + serTime + "\tms");
			System.out.println("Local Resampling Time	: \t" + localResamplingTime + "\tms");
			System.out.println("Global Resampling Time	: \t" + globalResampleTime + "\tms");
			System.out.println("Combining Time		: \t" + combiminingTime + "\tms");
			System.out.println("*************************");
			//robotz = (int) Math.round( robot.getHead()/this.orientation_delta_degree );
			try {
				Thread.sleep(33);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			//draw image 
			this.Drawing(grap, samcl_window, robot, max_p, next_set, SER_set);
			
			//update image
			image_panel.repaint();
		}
	}
	
	public void setup() throws IOException{
		if(this.Delta_Energy!=null)
			this.delta_energy = Float.parseFloat(Delta_Energy);
		if(this.alpha!=null)
			this.ALPHA = Float.parseFloat(this.alpha);
		if(this.xi!=null)
			this.XI = Float.parseFloat(xi);
		
		this.orientation_delta_degree = 360/this.orientation;
		this.sensor_number = this.orientation/2 + 1;
		this.sensor_delta_degree = this.orientation_delta_degree;
		
		this.precomputed_grid = new Grid(this.orientation, this.sensor_number, this.map_filename);
		
		if(this.onCloud){
			this.cloudSetup();
		}else{
			this.localSetup();
		}
	}
	
	private void localSetup(){
		precomputed_grid.readmap();
		/**
		 *  the initialization of SAMCL 
		 */
		this.width = this.precomputed_grid.width;
		this.height = this.precomputed_grid.height;
		//TODO add a condition to choose if start mouse function or not
		//precomputed_grid.start_mouse(precomputed_grid);
	}
	
	protected HTable table = null;
	private String tableName = "map.512.4.split";
	private void cloudSetup() throws IOException{
		Configuration conf = HBaseConfiguration.create();
		precomputed_grid.setupTable(conf);
		precomputed_grid.readmap(this.map_filename, conf);
		this.table = this.precomputed_grid.getTable(this.tableName);
		/**
		 *  the initialization of SAMCL 
		 */
		this.width = this.precomputed_grid.width;
		this.height = this.precomputed_grid.height;
		//TODO add a condition to choose if start mouse function or not
		//precomputed_grid.start_mouse(precomputed_grid);
	}
	
	/**
	 * @param orientation angular resolution
	 * @param map_filename 
	 * @param delta_energy dor defining the SER
	 * @param nt population 
	 * @param xI threshold 
	 * @param aLPHA rate of population
	 * @param tournament_presure competetive strength
	 * @throws IOException 
	 */
	public SAMCL(int orientation,
			String map_filename, 
			float delta_energy,
			int nt,
			float xI, 
			float aLPHA, 
			int tournament_presure) throws IOException {
		//directive
		super();
		this.orientation = orientation;
		this.map_filename = map_filename;
		this.delta_energy = delta_energy;
		this.Nt = nt;
		this.XI = xI;
		this.ALPHA = aLPHA;
		this.tournament_presure = tournament_presure;
		//remains
	}
	
	/**
	 * @param orientation angular resolution
	 * @param map_filename 
	 * @param delta_energy dor defining the SER
	 * @param nt population 
	 * @param xI threshold 
	 * @param aLPHA rate of population
	 * @param tournament_presure competetive strength
	 * @throws IOException 
	 */
	public SAMCL(boolean cloud, 
			int orientation,
			String map_filename, 
			float delta_energy,
			int nt,
			float xI, 
			float aLPHA, 
			int tournament_presure) throws IOException {
		//directive
		super();
		this.orientation = orientation;
		this.map_filename = map_filename;
		this.delta_energy = delta_energy;
		this.Nt = nt;
		this.XI = xI;
		this.ALPHA = aLPHA;
		this.tournament_presure = tournament_presure;
		
		this.onCloud = true;
		
	}
	
	public SAMCL() throws IOException{
		
	}
	
	//check the parameters 
	//TODO 2014/7/3 should add "table name" argument
	@Parameter(names = {"-cl","--cloud"}, description = "if be on the cloud", required = false)
	public boolean onCloud = false;
	
	@Parameter(names = {"-o","--orientation"}, description = "the number of orientation of a cell", required = false)
	public int orientation = 18;
	
	//for Pre_caching()
	@Parameter(names = {"-i","--image"}, description = "the image of map", required = false)
	public String map_filename = "file:///home/eeuser/map1024.jpeg";
	
	//for Caculating_SER()
	@Parameter(names = {"-d","--delta"}, description = "the delta of SER", required = false)
	public String Delta_Energy = null;
	public float delta_energy = (float)0.01;
	
	//for Determining_size()
	@Parameter(names = {"-x","--xi"}, description = "the sensitive coefficient", required = false)
	public String xi = null;
	public float XI = (float)0.1;
	@Parameter(names = {"-a","--alpha"}, description = "the ratio of population", required = false)
	public String alpha = null;
	public float ALPHA = (float)0.6;
	@Parameter(names = {"-n","--number"}, description = "the number of total population", required = false)
	public int Nt = 100;
	
	@Parameter(names = {"-p","--presure"}, description = "the tournament presure", required = false)
	private int tournament_presure;
	
	private double orientation_delta_degree;
	private int sensor_number;
	@SuppressWarnings("unused")
	private double sensor_delta_degree;
	private int width;
	private int height;
	//for Pre_caching()
	public Grid precomputed_grid;
	//for Sample_total_particles()
	
	//for Determining_size()
	private int Nl;
	private int Ng;
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
		long start_time = System.currentTimeMillis();
		precomputed_grid.pre_compute();
		long estimated_time = System.currentTimeMillis() - start_time;
		System.out.println("estimated_time:"+estimated_time+"ms");
	}
	
//	/**
//	 * for cloud 
//	 * what's the type of map_filename ?
//	 * @param conf
//	 */
//	public void Pre_caching(Configuration conf) {
//		System.out.println("computing...");
//		long start_time = System.currentTimeMillis();
//		precomputed_grid.pre_compute();
//		long estimated_time = System.currentTimeMillis() - start_time;
//		System.out.println("estimated_time:"+estimated_time+"ms");
//	}
		
	/**
	 * input:measurement(Zt),energy grid(GE)
	 * output:similar energy region(SER)
	 * @throws IOException 
	 */
	public void Caculating_SER(float[] Zt, List<Particle> SER_set) throws IOException{
		/*Get the reference energy*/
		float energy = this.Caculate_energy(Zt);
		float UpperBoundary = energy + this.delta_energy;
		float LowerBoundary = energy - this.delta_energy;
		if(LowerBoundary<0)
			LowerBoundary = 0.0f;
		if(UpperBoundary>1.0f)
			UpperBoundary = 1.0f;
		SER_set.clear();
		//System.out.println("Zt:"+Arrays.toString(Zt));
		//System.out.println("Robot's Energy: "+ energy);
		//System.out.println("upper boundary: "+ UpperBoundary);
		//System.out.println("lower boundary: "+ LowerBoundary);
		if (onCloud) {
			this.cloudCaculatingSER(SER_set, LowerBoundary, UpperBoundary);
		}else{
			this.localCaculatingSER(SER_set, LowerBoundary,  UpperBoundary);
		}
	}
	
	/**
	 * input:last particles set(Xt-1),motion control(ut),measurement(Zt),3-Dimentional grid(G3D)
	 * output:particles(xt),weight(wt)
	 */
	public void Prediction_total_particles(List<Particle> src, VelocityModel u, float[] robotMeasurements){
		//System.out.println("*********into Sample_total_particles");
		try {
			if(!src.isEmpty()){
				for(int i = 0; i < src.size(); i++){
					//this.Motion_sampling(src.elementAt(i),u);
					//System.out.println("sampling.... i :	" + i);
					this.pixel_sampling(src.get(i), 11, 7);
				}
			}
			else{
				//System.out.println("*********into else");
				throw new Exception("The set is empty!\n");
			}
		} catch (Exception e) {

			System.out.println(e.toString()+"\n there is no last set in elder set.");
		}		
	}
	
	public void batchWeight(List<Particle> src, float[] robotMeasurements) throws IOException {
		//get sensor data of all particles.
		if(this.onCloud){
			//get measurements from cloud  and weight
			this.precomputed_grid.getBatchFromCloud(this.table, src);
			for(Particle p : src){
				this.WeightParticle(p, robotMeasurements);
			}
		}else{
			//get measurements from local database and weight
			for(Particle p : src){
				p.setMeasurements(this.precomputed_grid.getMeasurements(this.table, this.onCloud, p.getX(), p.getY(), p.getZ()));
				this.WeightParticle(p, robotMeasurements);
			}
		}
	}

	/**
	 * parameters:sensitive coefficient(Threshold),the ratio of the global samples and local samples(Alpha)
	 * input:the maximum of weight(wmax),total number of particles(NT)
	 * output:the number of global samples(NG),the number of local samples(NL)
	 */
	public Particle Determining_size(List<Particle> src){
		Particle max_p = Transformer.minParticle(src);
		if (max_p.getWeight() < this.XI) {
			this.Nl = this.Nt;
		}	
		else {
			this.Nl = (int) (this.ALPHA * this.Nt);
		}
		this.Ng = this.Nt - this.Nl;
		
		return max_p;
	}
	
	/**
	 * input:the number of local samples(NL),particles(xt),weight(wt)
	 * output:XLt
	 */
	public void Local_resampling(List<Particle> src, List<Particle> dst){
		dst.clear();
		for (int i = 0; i < this.Nl; i++) {
			//Roulette way
			//Tournament way
			Particle particle = Transformer.tournament(tournament_presure, src);
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
			//
			dst.clear();
			Particle particle;
			int rand;
			Random random = new Random();
			for (int i = 0; i < this.Ng; i++) {
				rand = random.nextInt(src.size());
				particle = new Particle(src.get(rand).getX(), src
						.get(rand).getY(),
						src.get(rand).getZ(), this.orientation);
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
		//System.out.println("start to scan in caculating SER on the cloud");
		this.precomputed_grid.scan(this.table, SER_set, LowerBoundary, UpperBoundary);
	}
	
	private void localCaculatingSER(List<Particle> SER_set, float LowerBoundary, float UpperBoundary){
		SER_set.clear();
		float temp1;
		Random r = new Random();
		for(int x = this.safe_edge ; x < this.width-this.safe_edge ; x++){
			for (int y = this.safe_edge; y < this.height-this.safe_edge; y++) {
				if (this.precomputed_grid.map_array(x, y) == Grid.GRID_EMPTY) {
					for(int z = 0; z < this.orientation; z++){
						
						temp1 = this.precomputed_grid.G[x][y].getEnergy(z);
						/**
						 * Define a position of the SER
						 * Add this (x,y,z) to the SER_set 
						 */
						if( temp1 >= LowerBoundary &&
								temp1 <= UpperBoundary){
							Particle parti = new Particle(x,y,r.nextInt(this.orientation));
							SER_set.add(parti);
						}
					}
				}
			}
		}
	}
	
	private int safe_edge = 10;
	
	private Particle global_sampling(){
		Random rand = new Random();
		Particle p = new Particle(
				safe_edge+rand.nextInt(this.width-(2*safe_edge)), 
				safe_edge+rand.nextInt(this.height-(2*safe_edge)), 
				rand.nextInt(this.orientation));
		while (
				!p.underSafeEdge(this.width, this.height, this.safe_edge) ||
				this.precomputed_grid.map_array(p.getX(), p.getY()) == Grid.GRID_OCCUPIED) {
			p = new Particle(
					safe_edge+rand.nextInt(this.width-(2*safe_edge)), 
					safe_edge+rand.nextInt(this.height-(2*safe_edge)), 
					rand.nextInt(this.orientation));
		}
		return p;
	}
	
	

	
	private void pixel_sampling(Particle p, int radius, int angular){
		
		
		Random random = new Random();
		int r ;
//		System.out.print("random intx\t");
		r = random.nextInt(radius)-( radius - 1 )/2;
//		System.out.print((int)r+"\n");
		int px = p.getX() + r;
//		System.out.print("random y\t");
		r = random.nextInt(radius)-( radius - 1 )/2;
//		System.out.print(r+"\n");
		int py = p.getY() + r;
	
		while(this.precomputed_grid.map_array(px, py) == Grid.GRID_OCCUPIED ){
			if(Particle.underSafeEdge(px, py, this.width, this.height, this.safe_edge) ==false){
				p = this.global_sampling();
			}
//			System.out.print("random x\t");
			r = random.nextInt(radius)-( radius - 1 )/2;
//			System.out.print(r+"\n");
			px = p.getX() + r;
//			System.out.print("random y\t");
			r = random.nextInt(radius)-( radius - 1 )/2;
//			System.out.print(r+"\n");
			py = p.getY() + r;
		}
		r = random.nextInt(angular)-( angular - 1 )/2;
		int pz = (p.getZ() + r + this.orientation) % this.orientation;
		
		p.setX(px);
		p.setY(py);
		p.setZ(pz);
	}
	
	//TODO motion sampling is unfinished
	@SuppressWarnings("unused")
	private void Motion_sampling(Particle p, VelocityModel u){
		double Vcup = u.velocity + 
				Distribution.sample_normal_distribution(u.velocity*u.velocity + u.angular_velocity*u.angular_velocity);
		double Wcup = u.angular_velocity + 
				Distribution.sample_normal_distribution(u.velocity*u.velocity + u.angular_velocity*u.angular_velocity);
		double Rcup =  Distribution.sample_normal_distribution(u.velocity*u.velocity + u.angular_velocity*u.angular_velocity);
		
		double temp = p.getX() 
				- ( Vcup/Wcup ) * ( Math.sin( Math.toRadians( p.getTh() ) ) )
				+ ( Vcup/Wcup ) * ( Math.sin( Math.toRadians( p.getTh() + u.angular_velocity ) ) );
		p.setX((int)Math.round(temp));
		
		temp = p.getX() 
				- ( Vcup/Wcup ) * ( Math.cos( Math.toRadians( p.getTh() ) ) )
				+ ( Vcup/Wcup ) * ( Math.cos( Math.toRadians( p.getTh() + u.angular_velocity ) ) );
		p.setY((int)Math.round(temp));
		
		temp = p.getTh() + Rcup;
		p.setTh(temp);
	}
	
	private void WeightParticle(Particle p, float[] robotMeasurements) throws IOException{
		//if the position is occupied.
		if( this.precomputed_grid.map_array(p.getX(), p.getY())==Grid.GRID_EMPTY ) {
			//if the particle has got the measurements or would get measurements from Grid
			if(p.isIfmeasurements()){
				p.setWeight(Transformer.WeightFloat(p.getMeasurements(), robotMeasurements));
			}else{
				//Cloud , Grid class------done
				float[] measurements = this.precomputed_grid.getMeasurements(this.table, onCloud, p.getX(), p.getY(), p.getZ());
				p.setWeight(Transformer.WeightFloat(measurements, robotMeasurements));
			}
		}else{
			//if the position is occupied, then assign the worst weight.
			p.setWeight(1);
		}
	}
	
	public float Caculate_energy(float[] Zt){
		float energy = 0;
		for (int i = 0; i < Zt.length; i++) {
			
			energy = energy + Zt[i];
		}
		energy = energy / ((float)Zt.length);
		return energy;
	}

	@Override
	public void close() throws IOException {
		if(this.table!=null)
			this.table.close();
	}

}
