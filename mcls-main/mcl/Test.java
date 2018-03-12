package mcl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import com.beust.jcommander.JCommander;

import util.grid.Grid;
import util.pf.Particle;
import util.pf.sensor.laser.LaserModel.LaserModelData;
import util.pf.sensor.laser.MCLLaserModel.ModelType;
import util.robot.Pose;
import util.robot.RobotState;
import util.robot.VelocityModel;

public class Test extends MCL{

	
	
	public static void main(String[] args) throws Throwable {
		//for debug mode
		if(args.length==0){
			String[] targs = {
					"-i"
					//,"file:///Users/Jolly/workspace/dataset/intel-map.png"//506m with 587 pixels
					,"file:///Users/Jolly/git/Cloud-based MCL/jpg/map_8590.jpg"
					,"--lrmax", "30"//10m is about 35 pixels
					,"-o","36"
//							,"-rl","true"
					,"-rx","60"
					,"-ry","60"
//							,"-rh","50"
//							,"-n","50"
//							,"-p","20"
					,"-D","false"
//							,"-c","true"
					,"--ignore", "true"
//							,"--showparticles"
					,"--period","500"
					,"--logfile"
					,"--visualization"
					,"--showmeasurements"
					,"--showparticles"
					,"--sensor_model", "1"
					};
			args = targs;
		}
		
		/**
		 * First step:
		 * to create the localization algorithm
		 * and setup the listener for SAMCL
		 */
		Test mcl = new Test(false,
				18, //orientation
				100, //total particle
				10);//competitive strength
		JCommander jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(mcl);
		jc.parse(args);
		if(mcl.help){
			jc.usage();
			System.exit(0);
		}
		
		Grid grid = new Grid();
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(grid);
		jc.parse(args);
		mcl.setupMCL(grid);
		
		/**
		 * Second step:
		 * to create a robot
		 * setup the listener of Robot
		 * */
		RobotState robot = new RobotState();
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(robot);
		jc.parse(args);
		robot.setupSimulationRobot(grid);
		Thread t = new Thread(robot);
		t.start();
		/**
		 * Third step:
		 * start to run mcl
		 */
		mcl.run(robot, grid);
		mcl.close();
		robot.close();
	}

	int shift = 25;
	int squaresize= 101;
	boolean ifshift = false;
	int headerWidth, headerHeight, robotIdx,rx,ry;
	@Override
	public void globalSampling(List<Particle> set, RobotState robot, Grid grid) {
		//initialization of particles at all positions.
		int totalParticle;
		int begx,endx,begy,endy;
		this.rx = (int) robot.X;
		this.ry = (int) robot.Y;
		this.shift = (int) robot.X-5;
		this.robotIdx = (int) ((robot.X/*RX*/-(shift-1)/*XL*/)*squaresize/*H*/+(robot.Y/*RY*/-(shift-1)/*YL*/));
		
		if(ifshift){
			begx = shift-1;
			endx = shift-1+squaresize;
			begy = shift-1;
			endy = shift-1+squaresize;
			totalParticle = squaresize*squaresize;
		}else{
			begx = 0;
			endx = grid.width;
			begy = 0;
			endy = grid.height;
			totalParticle = grid.height*grid.width;
		}
		this.headerWidth = endx-begx; 
		this.headerHeight= endy-begy;
		
		for(int x =  begx; x < endx; x++){
			for(int y = begy; y < endy; y++){
				Particle p = new Particle(x,y,robot.H);
				if(this.sensor.getModeltype().equals(ModelType.DEFAULT) ||
					this.sensor.getModeltype().equals(ModelType.BEAM_MODEL)){
//					p.setWeight(1/totalParticle);
					p.setWeightForNomalization(1/totalParticle);
				}
				else if(this.sensor.getModeltype().equals(ModelType.LOSS_FUNCTION)){
//					p.setWeight(-Float.MAX_VALUE);
					p.setWeightForNomalization(-Double.MAX_VALUE);
				}
				set.add(p);
			}
		}
		
		//TODO create a folder for writing records.
		timeLogFloder = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		File theDir = new File(timeLogFloder);

		// if the directory does not exist, create it
		if (!theDir.exists()) {
		    System.out.println("creating directory: " + theDir.getName());
		    boolean result = false;

		    try{
		        theDir.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se){
		        //handle it
		    }        
		    if(result) {    
		        System.out.println("DIR created");  
		    }
		}
	}

	@Override
	public long predictionParticles(List<Particle> src, List<Particle> dst, VelocityModel u, Pose currentPose,
			Pose previousPose, long duration) throws Exception {
		// do nothing to avoid changes of position of particles.
		dst.addAll(src);
		src.clear();
		return 0;
	}
	
	private List<Particle> records = null;
	int totalcount = 0;
	int diff_count = 0;
	@Override
	public void localResampling(List<Particle> src, List<Particle> dst,
			RobotState robot,
			LaserModelData laserData,
			Grid grid) {
		dst.addAll(src);
		this.records = dst;
		
		//convert records into text file for analysis in matlab.
		Particle rp = null;
		if(ifshift){
			rp = dst.get(this.robotIdx);
		}else{
			rp = dst.get(30*grid.height+30);
		}
		System.out.println("Robot " + rp);
		System.out.println(diff_count + " difference(s) out of " + totalcount + " runs.");
		totalcount++;

		if(totalcount<=5){
			writedown();
		}else if(this.ifShowSER){
			this.ifShowSER = false;
			writedown();
		} 
	}

	String timeLogFloder = "";
	int recordCount = 0;
	private void writedown() {
		recordCount++;
		BufferedWriter writer = null;
        try {
            //create a temporary file
            
            File logFile = new File(timeLogFloder+ "/" + recordCount + /*"_" + (this.ifshift?"shifted":"unshifted") +*/ ".txt");
            // This will output the full path where the file will be written to...
            System.out.println(logFile.getCanonicalPath());
            writer = new BufferedWriter(new FileWriter(logFile));
            
            //TODO write header information
            //width and height of this matrix, and robot pose.
            writer.write(this.header(this.headerWidth, this.headerHeight, this.rx, this.ry)+"\n");
            
            //write the whole particles.
            for(Particle p : records)
            	writer.write(p.getX() + " " + p.getY() + " " + p.getNomalizedWeight() + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
            }
            
        }
	}

	private String header(int w, int h, int robotx, int roboty){
		return	"width:" + w + " " + 
				"height:" + h + " "+
				"robotx:" + robotx + " " +
				"roboty:" + roboty + " ";
	}
	
	public Test(boolean cloud, int orientation, int nt,	int tournamentPresure) throws IOException {
		super(/*cloud ,orientation, nt, tournamentPresure*/);
	}

}
