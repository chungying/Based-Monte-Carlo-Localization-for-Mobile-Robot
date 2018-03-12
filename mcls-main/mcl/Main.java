package mcl;

import com.beust.jcommander.JCommander;

import util.runner.MCLRunner;

public class Main {
	
	public static void main(String[] args) throws Exception{
		//for debug mode
		if(args.length==0){
			String[] targs = {					
//					"-i", "file:///Users/Jolly/workspace/dataset/simple/simmap.jpg"
					"-i", "file:///Users/Jolly/workspace/dataset/intel_lab/intel-map.png"
					,"-rx","120"
					,"-ry","120"
					,"-rh","0"
//					,"-rx","84"
//					,"-ry","320"
//					,"-rh","90"
//					"-i", "file:///Users/Jolly/workspace/dataset/dataset_fr079/fr079_maps_gridmap.bmp"
//					,"-rx","170"
//					,"-ry","200"
//					,"-rh","90"
//					"-i", "/Users/Jolly/workspace/dataset/willowgarage/willowgarage2.pgm"
//					,"-rx","700"
//					,"-ry","900"
//					,"-rh","0"
					,"--numberofparticles","1000"
//					,"--converge","true"//forcing initial convergence of particles.
//					,"--showparticles", "true"
					,"--visualization", "true"
//					,"--showmeasurements", "true"
					,"-lares","6"
					,"-lamin","-90"
					,"-lamax","90"
					,"-lrmax","500"
					,"--sigmahit", "10"
					,"--linearvelocity", "0"
					,"--sensormodel", "BEAM_MODEL"//"LOG_BEAM_MODEL"
					,"--runTimes", "0"
//					,"--className", "mcl.MCL"
//					,"--className", "demcmcl.DEMCMCL", "--demcgenepool", "RESAMPLED_SET"
					,"--className", "demcmcl.MIXMCL"
					,"--forwardDist", "250"
					};
			args = targs;
		}
		
		MCLRunner runner = new MCLRunner();
		JCommander j = new JCommander();
		j.setAcceptUnknownOptions(true);
		j.addObject(runner);
		j.parse(args);
		runner.run(args);
	}
	
}
