package samcl;

import java.awt.Point;
import java.io.Closeable;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Threads;

import util.Distribution;
import util.Transformer;
import util.grid.Grid;
import util.grid.GridTools;
import util.gui.FrameOwner;
import util.gui.VariablesController;
import util.pf.Particle;
import util.pf.sensor.data.LaserScanData;
import util.pf.sensor.laser.MCLLaserModel;
import util.pf.sensor.laser.LaserModel.LaserModelData;
import util.pf.sensor.laser.MCLLaserModel.ModelType;
import util.pf.sensor.odom.callbackfunc.MCLMotionModel;
import util.recorder.PoseWithCovariance;
import util.recorder.PoseWithTimestamp;
import util.recorder.Record;
import util.robot.Pose;
import util.robot.RobotState;
import util.robot.VelocityModel;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.converters.FloatConverter;

/**
 * @author jolly
 *part1:Sampling total particles
 *part2:Determining the size of the global sample set and the local sample set
 *part3:Resampling local samples
 *part4:Drawing global samples
 *part5:Combining two particle sets
 */
public class SAMCL extends MclBase implements Closeable, FrameOwner{	

	//variables not parameters
	private boolean initMcl = false;
	private boolean terminated = false;
	private boolean terminating = false;
	private boolean closing = false;
	private boolean closed = false;
	public HTable table = null;//for cloud computation
	protected int Nl;//for Determining_size()
	protected int Ng;//for Determining_size()
	private VariablesController vc = null;//for visualization

	//caching the initial state
	protected MclBase initialState = null;
	protected MCLLaserModel initialLaser = new MCLLaserModel();
	protected MCLMotionModel initialOdom = new MCLMotionModel();

	@ParametersDelegate
	protected MCLLaserModel sensor = new MCLLaserModel();//for sensor model of MCL
	
	@ParametersDelegate
	public MCLMotionModel odomModel = new MCLMotionModel();//for odometry model of MCL

	private void setInitState() {
		//TODO
		initMcl = true;
		initialState = new MclBase();
		initialState.setupMclBase(this);
		try{
			initialLaser.setupSensor(sensor);
			initialOdom.setupSensor(odomModel);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		
	}

	public void startOver(){
		if (initMcl) {
			this.setupMclBase(initialState);
			try{
				this.sensor.setupSensor(initialLaser);
				this.odomModel.setupSensor(initialOdom);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void setupMCL(Grid grid) throws Exception{
		this.sensor.setupSensor(grid.laser);
		if(this.table == null)
			this.table = grid.getTable(this.tableName);
		//Setting up a window for variable control
		this.setupFrame(grid.visualization);
		this.setInitState();
	}

	public void setTerminating(boolean terminate) {
		this.terminating = terminate;
	}
	private void setTerminated(boolean terminated) {
		this.terminated = terminated;
	}
	private boolean isTerminating() {
		return this.terminating ;
	}
	public boolean hasTerminated() {
		return this.terminated ;
	}

	public void setClosing(boolean isClosing){
		this.closing = isClosing;
	}
	public boolean isClosing(){
		return this.closing;
	}

	public boolean hasClosed(){
		return this.closed;
	}
	
	@Override
	public void close(){
		if (!closed) {
			System.out.println("closing " + this.getClass().getName());
			if (!isClosing())
				setClosing(true);
			if (!isTerminating())
				setTerminating(true);
			if (this.table != null) {
				try {
					this.table.close();
				} catch (Exception e) {
					e.printStackTrace();
				}finally{
					this.table = null;
				}
			}
			if (vc != null){
				this.vc.close();
				vc.dispose();
				vc = null;
			}
			this.customizedClose();
			this.closed = true;
		}
	}
	
	protected void customizedClose(){}

	/**
	 * 
	 * @param grid
	 */
	public void preCaching(Grid grid) {
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
	public void run(RobotState robot, Grid grid) throws Exception{
		if(this.isClosing())
			return;
		if(this.debugMode)
			System.out.print(
				"counter\t"	+
				"particleNo\t" +
				"timeOverAll\t"+
				"iteration\t"+
				"predictionTime\t"+
				"raycastingTime\t"+
				"weightTime\t"+
				"otherTransmissionTime\t"+
				"determiningTime\t"+
				"serTime\t"+
				"localResamplingTime\t"+
				"combiminingTime\t"+
				"best X\t"+
				"best Y\t"+
				"best H\t"+
				"robot X\t"+
				"robot Y\t"+
				"robot H\t"+
				"\n");
		
		//because this function might be restarted, it is necessary to define these status here.
		boolean debugMode = false;
		this.setTerminated(false);
		this.setTerminating(false);
		//when the function is restarted, applied old arguments

		ArrayList<Particle> local_set = new ArrayList<Particle>();
		ArrayList<Particle> global_set = new ArrayList<Particle>();
		ArrayList<Particle> current_set = new ArrayList<Particle>();
		ArrayList<Particle> last_set = new ArrayList<Particle>();
		ArrayList<Particle> SER_set = new ArrayList<Particle>();
		Particle bestParticle = null;//for calculating SER
		
		Record record = new Record();
		
		int counter = 0;
		long time = 0, iterationTime = 0, startTime = 0;
		
		//initial motion model for particle filter
		PoseWithTimestamp previousPose = new PoseWithTimestamp((Pose)robot, new Time(System.currentTimeMillis()));
		PoseWithTimestamp odometryPose = new PoseWithTimestamp((Pose)robot, new Time(System.currentTimeMillis()));
		Pose deltaP = new Pose();
		VelocityModel u = new VelocityModel();
		LaserModelData laserDataWithModel = null;
		LaserScanData laserData = null;
		
		laserData = robot.getLaserScan();
		while(laserData == null){
			Thread.sleep(0, 1);
			laserData = robot.getLaserScan();
			
		}
		laserDataWithModel = new LaserModelData(laserData, this.sensor);
		PoseWithCovariance estimateResult = new PoseWithCovariance(laserDataWithModel.data.timeStamp);
		
		//Initial Particles 
		this.globalSampling(current_set, robot, grid);
		//Initial Weighting
		this.weightAssignment(current_set, robot, laserDataWithModel, grid);
		last_set.addAll(current_set);
		estimateResult.stamp.setTime(laserDataWithModel.data.timeStamp.getTime());
		SAMCL.estimatePose(last_set, estimateResult);
		updateImagePanel(grid, robot, estimateResult, current_set, null);
		startTime = System.currentTimeMillis();
		while(!this.isTerminating() && (!robot.isAllTasksOver() && !robot.isRobotClosing())){
			if(this.convergeFlag){
				this.converge(last_set, robot);
			}
			time = System.currentTimeMillis();
			counter++;
			
			//update robot's sensor data
			//TODO adding Gaussian noise
			odometryPose.X = robot.X;
			odometryPose.Y = robot.Y;
			odometryPose.H = robot.H;
			odometryPose.stamp.setTime(System.currentTimeMillis());
			u.setModel(robot.getUt());
			//check if the delta of robot odometric data exceeds a threshold value.
			//if the delta value is larger than the threshold, robotMoved is true.
			Pose.deltaPose(odometryPose, previousPose, deltaP);
			boolean robotMoved = Math.abs(deltaP.X)>dThresh || 
					Math.abs(deltaP.Y)>dThresh ||
					Math.abs(deltaP.H)>aThresh;
			boolean newSensor = false;
			if(robotMoved) {
				//check if robot's sensor is updated, then newSensor set to true.
				if(robot.isSensorUpdated()){
					laserDataWithModel = new LaserModelData(robot.getLaserScan(), this.sensor);
					newSensor = true;
				}
			}
			
			if(!robotMoved){
				current_set.clear();
				current_set.addAll(last_set);
				//wait for a while then check update information again.
				Threads.sleep(1);
			}else if(robotMoved==true && newSensor==true){
				//if robotMoved is true and newSensor is true, update sensory model and make robotMoved and newSensor false.
				robotMoved = false; 
				newSensor = false;
				
				//TODO if updateMeasurement is true, update odometric model.
				//Setp 1: Prediction
				Transformer.debugMode(debugMode, "(1) Sampling\t");
				//, then predict the pose of hypotheses
				long predictionTime= this.predictionParticles(last_set, current_set, u, odometryPose, previousPose, grid, iterationTime);
				// and current odometry data became old data.
				previousPose.X = odometryPose.X;
				previousPose.Y = odometryPose.Y;
				previousPose.H = odometryPose.H;
				previousPose.stamp.setTime(odometryPose.stamp.getTime());
				
				//Step 2: Weighting
				Transformer.debugMode(debugMode, "(2) Weighting\t");
				long raycastingTime, weightTime, otherTransmisionTime;
				List<Long> times = this.weightAssignment(current_set, robot, laserDataWithModel, grid);
				raycastingTime = times.get(0);
				weightTime = times.get(1);
				otherTransmisionTime = times.get(2);
				
				//Step 3: Determining sizes of local set and global set
				Transformer.debugMode(debugMode, "(3) Determining size\t");
				long determiningTime = System.currentTimeMillis();
				//bestParticle = Transformer.minParticle(current_set); 
				bestParticle = Transformer.maxParticle(current_set); 
				this.determiningSize(bestParticle);
				determiningTime = System.currentTimeMillis() - determiningTime;
				//Step 3-1: Calculating SER
				Transformer.debugMode(debugMode, "(3-1) Calculating SER\t");
				long serTime = System.currentTimeMillis();
				this.caculatingSER(current_set, bestParticle, laserDataWithModel.data.beamranges, SER_set, global_set, grid);
				serTime = System.currentTimeMillis() - serTime;
			
				//Step 4: Local resampling
				Transformer.debugMode(debugMode, "(4) Local resampling\t");
				long localResamplingTime = System.currentTimeMillis();
				if((counter%resampleInterval)==0){
					this.localResampling(current_set, local_set, robot, laserDataWithModel, grid);
				}else{
					local_set.clear();
					local_set.addAll(current_set);
				}
				
				localResamplingTime = System.currentTimeMillis() - localResamplingTime;
			
				//Step 5: Combimining
				Transformer.debugMode(debugMode, "(5) Combimining\n");
				long combiminingTime = System.currentTimeMillis();
				last_set.clear();
				last_set.addAll(this.combiningSets(local_set, global_set));
				local_set.clear();
				global_set.clear();
				combiminingTime = System.currentTimeMillis() - combiminingTime;
				
				//Averaging Particle
				long averageTime = System.currentTimeMillis();
				estimateResult.stamp.setTime(laserDataWithModel.data.timeStamp.getTime());
				SAMCL.estimatePose(last_set, estimateResult);
				averageTime = System.currentTimeMillis() - averageTime;
				
				//TODO is this delay required?
				this.delay(this.period);
			
				//timer
				if(this.ignore)
					iterationTime = System.currentTimeMillis() - time - raycastingTime;
				else
					iterationTime = System.currentTimeMillis() - time;
			

				Transformer.debugMode(false,
						"Best position          :"+bestParticle.toString()+"\n",
						"Robot position         : \t" + robot+"\n",
						"Sensitive              : \t" + this.XI+"\n",
						"RPC counter            : \t" + grid.getRPCcount()+"\n",
						"Prediction Time        : \t" + predictionTime + "\tms"+"\n",
						"Weighting Time	        : \t" + weightTime + "\tms"+"\n",
						"Determing Size Time    : \t" + determiningTime + "\tms"+"\n",
						"Caculating SER Time    : \t" + serTime + "\tms"+"\n",
						"Local Resampling Time  : \t" + localResamplingTime + "\tms"+"\n",
						"Combining Time	        : \t" + combiminingTime + "\tms"+"\n",
						"Averaging Time	        : \t" + averageTime + "\tms"+"\n",
						"Default Delay Time     : \t" + this.period + "\tms"+"\n",
						"Alpha argument         : \t" + this.odomModel.alphasToString() + "\n",
						
						"*************************\n"
						);
			
			
			
				if(this.debugMode){
//					System.out.print(
//							"counter\t"	+
//							"particleNo\t" +
//							"timeOverAll\t"+
//							"iteration\t"+
//							"predictionTime\t"+
//							"raycastingTime\t"+
//							"weightTime\t"+
//							"otherTransmissionTime\t"+
//							"determiningTime\t"+
//							"serTime\t"+
//							"localResamplingTime\t"+
//							"combiminingTime\t"+
//							"best X\t"+
//							"best Y\t"+
//							"best H\t"+
//							"robot X\t"+
//							"robot Y\t"+
//							"robot H\t"+
//							"\n");
				
					System.out.format("%5d\t",counter);
					System.out.format("%5d\t",last_set.size());
					System.out.format("%5d\t",time-startTime);//Execution time from the beginning.
					System.out.format("%5d\t",iterationTime);//time for this iteration.
					System.out.format("%5d\t",predictionTime);//time for prediction.
					System.out.format("%5d\t",raycastingTime);//time for raycasting.
					System.out.format("%5d\t",weightTime);//time for weighting.
					System.out.format("%5d\t",otherTransmisionTime);//time for other transmission
					System.out.format("%5d\t",determiningTime);//time for finding the best particle
					System.out.format("%5d\t",serTime);//time for SER.
					System.out.format("%5d\t",localResamplingTime);//time for regular resampling.
					System.out.format("%5d\t",combiminingTime);//time for combining global and local particle sets.
					System.out.format("%.5f\t",estimateResult.X);
					System.out.format("%.5f\t",estimateResult.Y);
					System.out.format("%.5f\t",estimateResult.H);
					System.out.format("%.5f\t",robot.X);
					System.out.format("%.5f\t",robot.Y);
					System.out.format("%.5f\t",robot.H);
					System.out.println();
				}
				//collecting data
				Record.collect(record, estimateResult, laserDataWithModel.data.groundTruthPose, odometryPose);
			}
			//painting 
			updateImagePanel(grid, robot, estimateResult, current_set, SER_set);
			
		}
		this.setTerminated(true);
		Record.allRecords.add(record);
	}

	@Override
	public void setupFrame(boolean display) {
		if(display==false)
			return;
		if(vc == null){
			vc = new VariablesController();
			vc.setInstance(this);
		}
		vc.setVisible(true);
	}
	
	@Override
	public void setFrameLocation(int x, int y){
		vc.setLocation(x, y);
	}

	public void globalSampling(List<Particle> set, RobotState robot, Grid grid) throws Exception{
//		Random randSeed = new Random();
		for (int i = 0; i < this.Nt; i++) {
			Particle p = new Particle(
				safe_edge+Distribution.seed.nextInt(grid.width-(2*safe_edge)), 
				safe_edge+Distribution.seed.nextInt(grid.height-(2*safe_edge)), 
				Transformer.Z2Th(Distribution.seed.nextInt(this.sensor.getOrientation()), this.sensor.getOrientation()));
			while ( !p.underSafeEdge(grid.width, grid.height, this.safe_edge) ||
					grid.map_array(p.getX(), p.getY()) == Grid.GRID_OCCUPIED) {
				p.setX(safe_edge+Distribution.seed.nextInt(grid.width-(2*safe_edge)));
				p.setY(safe_edge+Distribution.seed.nextInt(grid.height-(2*safe_edge)));
				p.setTh(Transformer.Z2Th(Distribution.seed.nextInt(this.sensor.getOrientation()), this.sensor.getOrientation()));	
			}
			if( this.sensor.getModeltype().equals(ModelType.DEFAULT)||
				this.sensor.getModeltype().equals(ModelType.BEAM_MODEL)){
//				p.setWeight(1/this.Nt);
				p.setWeightForNomalization(1.0/this.Nt);
			}	
			else if(this.sensor.getModeltype().equals(ModelType.LOSS_FUNCTION)||
					this.sensor.getModeltype().equals(ModelType.LOG_BEAM_MODEL)){
//				p.setWeight(-Float.MAX_VALUE);
				p.setWeightForNomalization(-Double.MAX_VALUE);
			}
			if(grid.map_array[p.getX()][p.getY()] == Grid.GRID_OCCUPIED)
				throw new Exception("the uniformly spreaded pose is occupied.");
			set.add(p);
		}
	}
	
	public void converge(List<Particle> current, RobotState robot){
		current.clear();
		for(int i = 0;i< this.Nt;i++){
			current.add(new Particle(robot.X, robot.Y, robot.H, 1.0/Nt));
		}
		this.convergeFlag = false;
	}

	private static double[] sums = new double[4];
	private static double[][] covAccu = new double[2][2];
	private static double totalWeight;
	private static void estimatePose(List<Particle> particleSet, PoseWithCovariance result) {
		totalWeight = 0;
		sums[0] = 0.0;
		sums[1] = 0.0;
		sums[2] = 0.0;
		sums[3] = 0.0;
		covAccu[0][0] = 0.0;
		covAccu[0][1] = 0.0;
		covAccu[1][0] = 0.0;
		covAccu[1][1] = 0.0;
		for(Particle p : particleSet){
			//for estimating the particle set
			totalWeight+=p.getNomalizedWeight();
			sums[0] = sums[0] + p.getNomalizedWeight()*p.getStates()[0];
			sums[1] = sums[1] + p.getNomalizedWeight()*p.getStates()[1];
			sums[2] = sums[2] + p.getNomalizedWeight()*Math.cos(Math.toRadians(p.getStates()[2]));
			sums[3] = sums[3] + p.getNomalizedWeight()*Math.sin(Math.toRadians(p.getStates()[2]));
			for(int i = 0 ; i < 2 ; i++){
				for(int j = 0 ; j < 2 ; j++){
					covAccu[i][j] += p.getNomalizedWeight() * p.getStates()[i] * p.getStates()[j];
				}
			}
		
		}
		
		//mean values
		result.X = sums[0]/totalWeight;
		result.Y = sums[1]/totalWeight;
		result.H = Math.toDegrees(Math.atan2(sums[3],sums[2]));
		
		//covariance
		result.cov[0][0] = covAccu[0][0]/totalWeight - result.X * result.X;
		result.cov[0][1] = covAccu[0][1]/totalWeight - result.X * result.Y;
		result.cov[1][0] = covAccu[1][0]/totalWeight - result.Y * result.X;
		result.cov[1][1] = covAccu[1][1]/totalWeight - result.Y * result.Y;
		result.cov[2][2] = Math.toDegrees(-2 * Math.log(Math.sqrt(sums[2]*sums[2] + sums[3]*sums[3])));
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
			Grid grid,
			long duration) throws Exception{
		long sampleTime = System.currentTimeMillis();
		dst.clear();
//		odomModel.prediction();
		for(Particle p : src){
			Particle predictedParticle = MCLMotionModel.OdemetryMotionSampling(p, currentPose, previousPose, duration, odomModel);
			
			if ( grid.map_array(predictedParticle.getX(), predictedParticle.getY()) != Grid.GRID_OCCUPIED) {
				p.setX(predictedParticle.getDX());
				p.setY(predictedParticle.getDY());
				p.setTh(predictedParticle.getTh());
				dst.add(p);
			}
		}
		return System.currentTimeMillis() - sampleTime;
	}
	
	public List<Long> weightAssignment( List<Particle> src, 
			RobotState robot,
			LaserModelData laserData,
			Grid grid
			) throws Exception{
		boolean previousState = robot.isMotorLocked();
		//long[] timers = new long[3];
		List<Long> ts = new ArrayList<Long>(3);
		
		if(ignore)
			robot.setMotorLock(true);
		ts.add(this.raycastingUpdate(src, grid));
		//timers[0] = this.raycastingUpdate(src);
		if(ignore)
			robot.setMotorLock(previousState);
		
		if(laserData.data.beamranges.size()==0){
			System.out.println("something wrong.");
			throw new Exception("robot measurement is null.");
		}
		ts.add(this.batchWeight(src, laserData, grid));
		//timers[1] = this.batchWeight(src, robotMeasurements);
		ts.add(0l);
		//timers[2] = 0;
		//return timers;
		return ts;
	}
	
	public long raycastingUpdate( List<Particle> src, Grid grid) throws Exception {
		long trasmission = System.currentTimeMillis();
		//get sensor data of all particles.
		if (grid.onCloud) {
			//get measurements from cloud and assign the measurement to particles
			try {
				grid.getBatchFromCloud2(this.table, src);
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				for(Particle p: src){
					System.out.println(Transformer.xy2RowkeyString(p.getDX(), p.getDY()));
				}
				System.exit(1);
			}
		} else {
			//get measurements from local database.
			//If there is no pre-caching data, executing ray-casting immediately. 
//			boolean status = this.grid.assignMeasurementsAnyway(src);
			
			//calculate raycasting right away
			int count = 0;
			for(Particle p:src){
				SimpleEntry<List<Float>, List<Point>> e = GridTools.getLaserDist(grid, sensor, p.getDX(), p.getDY(), p.getTh());
				if(e == null)
					p.setMeasurements(null);
				else
					p.setMeasurements(e.getKey());
			}
		}
		return System.currentTimeMillis() - trasmission;
	}
	
	public long batchWeight(List<Particle> src, LaserModelData laserData, Grid grid) throws Exception {
		long weightTime = System.currentTimeMillis();
		this.sensor.calculateParticleWeight(src, laserData);
		return System.currentTimeMillis() - weightTime;
	}
	
	/**
	 * parameters:sensitive coefficient(Threshold),the ratio of the global samples and local samples(Alpha)
	 * input:the maximum of weight(wmax),total number of particles(NT)
	 * output:the number of global samples(NG),the number of local samples(NL)
	 */
	public void determiningSize(Particle bestParticle){
		//Particle bestParticle = Transformer.minParticle(src);
		if (/*bestParticle.getWeight() < this.XI ||*/ bestParticle.getNomalizedWeight()<this.XI) {
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
	 */
	public void caculatingSER(
			List<Particle> current_set, 
			Particle bestParticle, 
			List<Float> Zt, 
			List<Particle> SER_set, 
			List<Particle> global_set,
			Grid grid) throws Exception{
		
		
//		float normalized_weight = 0;
		double normalizedW = 0;
		if( this.sensor.getModeltype().equals(ModelType.DEFAULT) ||
			this.sensor.getModeltype().equals(ModelType.BEAM_MODEL)){
//			normalized_weight = bestParticle.getWeight(); 
			normalizedW = bestParticle.getNomalizedWeight();
		}
		else if(this.sensor.getModeltype().equals(ModelType.LOSS_FUNCTION)||
				this.sensor.getModeltype().equals(ModelType.LOG_BEAM_MODEL)){
			for(Particle p: current_set){
//				if(p.getWeight()!=-Float.MAX_VALUE)
//					normalized_weight+=p.getWeight();
				assert(p.getNomalizedWeight()!=-Double.MAX_VALUE);
				normalizedW+=p.getNomalizedWeight();
			}
//			normalized_weight = 1 - bestParticle.getWeight()/normalized_weight;
			normalizedW = 1 - bestParticle.getNomalizedWeight()/normalizedW;
		}
		
		//If normalized best weight ranging from 0.0 to 1.0 is lower than a positive parameter XI
		//then SER is calculated.
		//if (best_weight > this.XI)
		if (/*normalized_weight<this.XI ||*/ normalizedW < this.XI) {//if do calculate SER or not?/
			/*Get the reference energy*/
			float energy = Transformer.CalculateEnergy(Zt,this.sensor.range_max);
			float UpperBoundary = energy + this.deltaEnergy;
			float LowerBoundary = energy - this.deltaEnergy;
			if(LowerBoundary<0)
				LowerBoundary = 0.0f;
			if(UpperBoundary>1.0f)
				UpperBoundary = 1.0f;
			SER_set.clear();
			if (grid.onCloud) {
				this.cloudCaculatingSER(SER_set, LowerBoundary, UpperBoundary, grid);
			}else{
				this.localCaculatingSER(SER_set, LowerBoundary,  UpperBoundary, grid);
			}
			//Step 3-2: Global resampling
//			System.out.println("(4)\tGlobal resampling\t");
			long globalResampleTime = System.currentTimeMillis();
			if (SER_set.size()>0) {
				global_set.clear();
				int randIdx;
//				Random randSeed = new Random();
				for (int i = 0; i < this.Ng; i++) {
					randIdx = Distribution.seed.nextInt(SER_set.size());
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
	
	private void cloudCaculatingSER(List<Particle> SER_set, float LowerBoundary, float UpperBoundary, Grid grid){
		SER_set.clear();
		try {
			grid.scanFromHbase(this.table, SER_set, LowerBoundary, UpperBoundary);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void localCaculatingSER(List<Particle> SER_set, float LowerBoundary, float UpperBoundary, Grid grid){
		SER_set.clear();
		for(int x = this.safe_edge ; x < grid.width-this.safe_edge ; x++){
			for (int y = this.safe_edge; y < grid.height-this.safe_edge; y++) {
				if (grid.map_array(x, y) == Grid.GRID_EMPTY) {
					for(int z = 0; z < this.sensor.getOrientation(); z++){
						/**
						 * Define a position of the SER
						 * Add this (x,y,z) to the SER_set 
						 */
						float temp1 = grid.G[x][y].getEnergy(z);
						if( temp1 >= LowerBoundary &&
								temp1 <= UpperBoundary){
							SER_set.add(new Particle(x, y, Transformer.Z2Th(z, this.sensor.getOrientation())));
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
	public void localResampling(List<Particle> src, List<Particle> dst,
			RobotState robot,
			LaserModelData laserData,
			Grid grid){
		dst.clear();
		//System.out.println("tournament presure: " + tournamentPresure);

		for (int i = dst.size(); i < this.Nl; i++) {
			//Roulette way
			//Tournament way
			Particle particle = Transformer.tournament(tournamentPresure, src);
			dst.add(particle.clone());
			//System.out.println();
		}
	}

	/**
	 * input:XLt,XGt
	 * output:Xt
	 */
	private List<Particle> combiningSets(List<Particle> set1, List<Particle> set2){
		List<Particle> results = new ArrayList<Particle>(set1);
		results.addAll(set2);
		for(Particle p:results)
			p.setWeightForNomalization(1.0/results.size());
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
	
	private void updateImagePanel(
			Grid grid,
			RobotState robot, 
			PoseWithCovariance estimate, 
			List<Particle> particles,
			List<Particle> SER){
		if(!grid.visualization)
			return;
		//drawing
		grid.refresh();

		//SER
		if(SER!=null)
			grid.drawPoints(SER);
		
		//Particles
		if(this.ifShowParticles && particles.size()>0){
			for(Particle p: particles){
				grid.drawHypothesisRobot(p.getX(), p.getY(), p.getTh());
			}
		}
		
		if(robot!=null){
			grid.drawRobot(robot);
			if(this.ifShowSensors && robot.getMeasurement_points()!=null){
				for(Point p: robot.getMeasurement_points()){
					grid.drawLaserPoint(p.x, p.y);
				}	
			}
		}
		
		if(estimate!=null){
			grid.drawBestSolution((int)Math.round(estimate.X), (int)Math.round(estimate.Y), estimate.H);
			grid.drawConfidence((int)Math.round(estimate.X), (int)Math.round(estimate.Y), estimate.H, estimate.cov);
		}
		
			
		grid.repaint();
	}
	
	public void forceConverge(){
		this.convergeFlag = true;
	}

}
