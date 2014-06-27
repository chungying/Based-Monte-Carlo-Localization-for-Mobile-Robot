/**
 * 
 */
package samcl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Random;
import java.util.Vector;

import javax.swing.JFrame;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.util.Bytes;

import robot.VelocityModel;
import robot.RobotState;
import util.gui.Panel;
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
public class SAMCL {
	public boolean isClosing;
	/**
	 * run SAMCL
	 * @throws IOException 
	 */
	public synchronized void run(RobotState robot, JFrame samcl_window) throws IOException{
		this.isClosing = false;
		//robot = new RobotState(32,41,0);
	
		System.out.println("press enter to continue.");
		System.in.read();
	
		
		//Drawing the image
		BufferedImage samcl_image = new BufferedImage(this.precomputed_grid.width,this.precomputed_grid.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D grap = samcl_image.createGraphics();
		//Cloud , Grid class------done
		grap.drawImage(this.precomputed_grid.map_image, null, 0, 0);
		grap.setColor(Color.RED);
				
		//Initial Particles and Painting
		this.last_set.clear();
		Particle p = null;
		for (int i = 0; i < this.Nt; i++) {
			p=this.global_sampling();
			this.last_set.addElement(p);
			grap.drawOval(p.getX()-2, p.getY()-2, 4, 4);
		}
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
				
		//get the robot's rangefinder
		//float[] Zt = this.precomputed_grid.getMeasurements( onCloud, robot.getX(), robot.getY(), Transformer.th2Z(robot.getHead(), orientation_delta_degree) );
		float[] Zt = robot.getMeasurements();
		
		int counter = 0;
		while(wl.isClosing!=true){
			counter++;
			System.out.println("Generation\t"+counter+"\t-------------------");
			//new map image
			grap.drawImage(this.precomputed_grid.map_image, null, 0, 0);
			
			//Cloud , Grid class-------done
			//include weighting
			System.out.println("(1)\tSampling and weighting\t");
			long sampleTime = System.currentTimeMillis();
			//System.out.println("last_set size:" + last_set.size());
			//System.out.println("ZT size:" + Zt.length);
			this.Prediction_total_particles(last_set, null, Zt);

			//use Threshold determine if or not going into this.---done
			System.out.println("(2)\tDetermining size\t");
			long determiningTime = System.currentTimeMillis();
			Particle max_p = this.Determining_size(last_set);
			long serTime = 0;
			if (max_p.getWeight()>this.XI) {
				//Cloud , Grid class
				System.out.println("\tCaculating SER\t");
				serTime = System.currentTimeMillis();
				this.Caculating_SER(Zt);
				System.out.println("\tSER set size = \t"+this.SER_set.size());
				//System.out.println("\tSER size: "+ this.SER_set.size());
				//Draw SER
				if (this.SER_set.size() >= 1) {
					//System.out.println("there are " + this.SER_set.size() + " positions");
					grap.setColor(Color.PINK);
					for (int i = 0; i < SER_set.size(); i++) {
						int x = SER_set.elementAt(i).getX();
						int y = SER_set.elementAt(i).getY();
						grap.drawOval(x, y, 1, 1);
					}
				} else {
					System.out.println("there are " + this.SER_set.size() + " positions");
				}
				//----------------------------------------------------------
			}
			//Best particle
			grap.setColor(Color.GREEN);
			grap.drawOval(max_p.getX()-4, max_p.getY()-4, 8, 8);
			grap.drawLine(max_p.getX(), max_p.getY(), 
					max_p.getX()+(int) Math.round(20*Math.cos(Math.toRadians(max_p.getZ()*this.orientation_delta_degree))), 
					max_p.getY()+(int) Math.round(20*Math.sin(Math.toRadians(max_p.getZ()*this.orientation_delta_degree))));
			
			System.out.println("(3)\tLocal resampling\t");
			long localResamplingTime = System.currentTimeMillis();
			
			this.Local_resampling(this.last_set, this.local_set);
			System.out.println("(4)\tGlobal resampling\t");
			long globalResampleTime = System.currentTimeMillis();
			
			this.Global_drawing(this.SER_set, this.global_set);
			System.out.println("\tlocal set size : \t" + this.local_set.size());
			System.out.println("\tglobal set size: \t" + this.global_set.size());
			System.out.println("(5)\tCombimining\t");
			long combiminingTime = System.currentTimeMillis();
			
			this.next_set = this.Combining_sets(this.local_set, this.global_set);
			System.out.println("\tnext set size: \t" + this.last_set.size());
			
			//Draw particles
//			grap.setColor(Color.GRAY);
//			for (int i = 0; i < next_set.size(); i++) {
//				int x = next_set.elementAt(i).getX();
//				int y = next_set.elementAt(i).getY();
//				int z = next_set.elementAt(i).getZ();
//				grap.drawOval(x-2, y-2, 4, 4);
//				grap.drawLine(x, y, 
//						x+(int) Math.round( 10*Math.cos( Math.toRadians( z*this.orientation_delta_degree ) ) ), 
//						y+(int) Math.round( 10*Math.sin( Math.toRadians( z*this.orientation_delta_degree ) ) ));
//			}
			
			//Draw Robot and show image
			grap.setColor(Color.RED);
			
			int rx = robot.getX();
			int ry = robot.getY();
			double rh = robot.getHead();
			grap.drawOval(rx-5, ry-5, 10, 10);
			grap.drawLine(rx, ry, 
					rx+(int)Math.round(20*Math.cos(Math.toRadians(rh))), 
					ry+(int)Math.round(20*Math.sin(Math.toRadians(rh))));
			
			//update image
			image_panel.repaint();
			
			
			//Zt = this.precomputed_grid.getMeasurements( onCloud, robot.getX(), robot.getY(), Transformer.th2Z(robot.getHead(), this.orientation_delta_degree) );
			Zt = robot.getMeasurements();
			this.last_set = this.next_set;
			
			long endTime = System.currentTimeMillis();
			System.out.println("Best position:"+max_p.toString());
			System.out.println("*************************");
			System.out.println("Sensitive           : \t" + this.XI);
			System.out.println("RPC counter         : \t"+this.precomputed_grid.RPCcount);
			this.precomputed_grid.RPCcount = 0;
			System.out.println("Sampling Time		: \t" + (determiningTime - sampleTime) + "\tms");
			System.out.println("Determing Size Time	: \t" + (localResamplingTime - determiningTime) + "\tms");
			if(serTime!=0)
				System.out.println("Caculating SER Time	: \t\t\t\t\t" + (localResamplingTime - serTime) + "\tms");
			System.out.println("Local Resampling Time	: \t" + (combiminingTime - localResamplingTime) + "\tms");
			System.out.println("Global Resampling Time	: \t" + (combiminingTime - globalResampleTime) + "\tms");
			System.out.println("Combining Time		: \t" + (endTime - combiminingTime) + "\tms");
			//robotz = (int) Math.round( robot.getHead()/this.orientation_delta_degree );
			this.SER_set.clear();
			//TODO from Robot
			//Cloud , Grid class----------done
			
			try {
				Thread.sleep(33);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
		this.last_set = new Vector<Particle>();
		this.temp_set = new Vector<Particle>();
		this.next_set = new Vector<Particle>();
		this.local_set = new Vector<Particle>();
		this.global_set = new Vector<Particle>();
		this.SER_set = new Vector<Particle>();
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
	
	private void cloudSetup() throws IOException{
		Configuration conf = HBaseConfiguration.create();
		precomputed_grid.setupTable(conf);
		precomputed_grid.readmap(this.map_filename, conf);
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
	
	//TODO
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
	public Vector<Particle> last_set;
	public Vector<Particle> temp_set;
	public Vector<Particle> next_set;
	public Vector<Particle> local_set;
	public Vector<Particle> global_set;
	public Vector<Particle> SER_set;
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
	public void Caculating_SER(float[] Zt) throws IOException{
		/*Get the reference energy*/
		float energy = this.Caculate_energy(Zt);
		
		float UpperBoundary = energy + this.delta_energy;
		float LowerBoundary = energy - this.delta_energy;
		if(LowerBoundary<0)
			LowerBoundary = 0.0f;
		if(UpperBoundary>1.0f)
			UpperBoundary = 1.0f;
		//System.out.println("Zt:"+Arrays.toString(Zt));
		//System.out.println("Robot's Energy: "+ energy);
		//System.out.println("upper boundary: "+ UpperBoundary);
		//System.out.println("lower boundary: "+ LowerBoundary);
		if (onCloud) {
			this.cloudCaculatingSER(LowerBoundary, UpperBoundary);
		}else{
			this.localCaculatingSER( LowerBoundary,  UpperBoundary);
		}
	}
	
	/**
	 * input:last particles set(Xt-1),motion control(ut),measurement(Zt),3-Dimentional grid(G3D)
	 * output:particles(xt),weight(wt)
	 */
	public void Prediction_total_particles(Vector<Particle> elder, VelocityModel u, float[] robotMeasurements){
		//System.out.println("*********into Sample_total_particles");
		try {
			if(!elder.isEmpty()){
				for(int i = 0; i < elder.size(); i++){
					//this.Motion_sampling(last_set.elementAt(i),u);
					//System.out.println("sampling.... i :	" + i);
					this.pixel_sampling(elder.elementAt(i), 11, 7);
				}
				
				this.batchWeight(elder, robotMeasurements);
				
			}
			else{
				//System.out.println("*********into else");
				throw new Exception("The set is empty!\n");
			}
		} catch (Exception e) {

			System.out.println(e.toString()+"\n there is no last set in elder set.");
		}		
	}
	
	/**
	 * parameters:sensitive coefficient(Threshold),the ratio of the global samples and local samples(Alpha)
	 * input:the maximum of weight(wmax),total number of particles(NT)
	 * output:the number of global samples(NG),the number of local samples(NL)
	 */
	public Particle Determining_size(Vector<Particle> set){
		Particle max_p = this.Min_particle(set);
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
	public void Local_resampling(Vector<Particle> sor_set, Vector<Particle> des_set){
		des_set.clear();
		for (int i = 0; i < this.Nl; i++) {
			//this.next_set.add(e);
			//Roulette way
			Particle particle = tournament(tournament_presure, sor_set);
			des_set.addElement(particle.clone());
			//System.out.println();
		
			//Tournament way
			
		}
	}
	
	

	/**
	 * input:the number of global samples(NG),similar energy region(SER)
	 * output:XGt
	 */
	public void Global_drawing(Vector<Particle> src_set, Vector<Particle> des_set){
		if (src_set.size()>0) {
			//
			des_set.clear();
			Particle particle;
			int rand;
			Random random = new Random();
			for (int i = 0; i < this.Ng; i++) {
				rand = random.nextInt(src_set.size());
				particle = new Particle(src_set.elementAt(rand).getX(), src_set
						.elementAt(rand).getY(),
						src_set.elementAt(rand).getZ(), this.orientation);
				des_set.addElement(particle);
				src_set.remove(rand);
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
	public Vector<Particle> Combining_sets(Vector<Particle> a, Vector<Particle> b){
		
		@SuppressWarnings("unchecked")
		Vector<Particle> result = (Vector<Particle>) a.clone() ;
		result.addAll(b);
		return result;
	}
	
	private void cloudCaculatingSER(float LowerBoundary, float UpperBoundary) throws IOException{
		this.SER_set.clear();
		//System.out.println("start to scan in caculating SER on the cloud");
		this.precomputed_grid.scan(this.SER_set, LowerBoundary, UpperBoundary);
	}
	
	private void localCaculatingSER(float LowerBoundary, float UpperBoundary){
		this.SER_set.clear();
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
							this.SER_set.addElement(parti);
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
	
	//TODO unfinished
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
	
	private void batchWeight(Vector<Particle> particles, float[] robotMeasurements) throws IOException {
		//get sensor data of all particles.
		if(this.onCloud){
			//get measurements from cloud 
			this.precomputed_grid.getBatchFromCloud(particles, Bytes.toBytes("distance"));
		}else{
			//get measurements from local database
			for(Particle p : particles){
				p.setMeasurements(this.precomputed_grid.getMeasurements(this.onCloud, p.getX(), p.getY(), p.getZ()));
			}
		}
	
		//calculate the weight of all particles.
		for(Particle p : particles){
			this.WeightParticle(p, robotMeasurements);
		}
	}
	
	private void WeightParticle(Particle p, float[] robotMeasurements) throws IOException{
		//if the position is occupied.
		if( this.precomputed_grid.map_array(p.getX(), p.getY())==Grid.GRID_EMPTY ) {
			//if the particle has got the measurements or would get measurements from Grid
			if(p.isIfmeasurements()){
				p.setWeight(Transformer.WeightFloat(p.getMeasurements(), robotMeasurements));
			}else{
				//Cloud , Grid class------done
				float[] measurements = this.precomputed_grid.getMeasurements(onCloud, p.getX(), p.getY(), p.getZ());
				p.setWeight(Transformer.WeightFloat(measurements, robotMeasurements));
			}
		}else{
			//if the position is occupied, then assign the worst weight.
			p.setWeight(1);
		}
	}
	
	/**
	 * 
	 * @param tournament_presure2	greater presure, less diversity
	 * @param particles		the group ready to be picked up
	 * @return		a picked particle at this time.
	 */
	private Particle tournament(int tournament_presure2, Vector<Particle> particles) {
		this.temp_set.clear();
		int random ;
		Random r = new Random();
		for(int j = 0;j<tournament_presure2;j++){
			random = r.nextInt(particles.size());
			//random = (int) (Math.round( Math.random() * ( this.Ng - 1 ) ) % this.Ng);
			this.temp_set.addElement(particles.get(random));
		}
		
		Particle tempp = this.Min_particle(temp_set); 
		//Particle tempp = this.Max_particle(temp_set); 
		
		return tempp;
	}
	

	@SuppressWarnings("unused")
	private Particle Max_particle( Vector<Particle> particles ){
		Particle max_particle = particles.elementAt(0);
		float max_weight = max_particle.getWeight();
		for (int i = 1; i < particles.size(); i++) {
			if (max_weight <= particles.elementAt(i).getWeight()) {
				max_particle = particles.elementAt(i);
				max_weight = max_particle.getWeight();
			}
		}
		return max_particle;
	}
	
	private Particle Min_particle( Vector<Particle> particles ){
		Particle min_particle = particles.elementAt(0);
		float min_weight = min_particle.getWeight();
		for (int i = 1; i < particles.size(); i++) {
			if (min_weight > particles.elementAt(i).getWeight()) {
				min_particle = particles.elementAt(i);
				min_weight = min_particle.getWeight();
			}
		}
		
		return min_particle;
	}
	
	public float Caculate_energy(float[] Zt){
		float energy = 0;
		for (int i = 0; i < Zt.length; i++) {
			
			energy = energy + Zt[i];
		}
		energy = energy / ((float)Zt.length);
		return energy;
	}

}
