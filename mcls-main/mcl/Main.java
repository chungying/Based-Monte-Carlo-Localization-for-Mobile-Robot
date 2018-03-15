package mcl;

import com.beust.jcommander.JCommander;

import util.runner.MCLRunner;

public class Main {
	
	public static void main(String[] args) throws Exception{
		if(args.length==0){
		        //for debug mode
			String[] targs = {""
					/*Map and Laser information*/
					,"-i", "file:///Users/Jolly/workspace/dataset/intel_lab/intel-map.png"
					,"-lares","6"
					,"-lamin","-90"
					,"-lamax","90"
					,"-lrmax","500"
					,"--sensormodel", "BEAM_MODEL"//"LOG_BEAM_MODEL"
					,"--sigmahit", "10"

					/*Robot information*/
					,"-rx","120"
					,"-ry","120"
					,"-rh","0"
					,"--linearvelocity", "0"

					/*MCL settings*/
					//,"--className", "mcl.MCL"
					//,"--className", "demcmcl.DEMCMCL", "--demcgenepool", "RESAMPLED_SET"
					,"--className", "imclroe.IMCLROE", "-cl", "true"
					,"--numberofparticles","1000"
					//,"--converge","true"//forcing initial convergence of particles.

					/*Experiment settings*/
					,"--runTimes", "0"
					,"--forwardDist", "250"

					/*Visualization part*/
					,"--visualization", "true"
					//,"--showparticles", "true"
					//,"--showmeasurements", "true"
					};
			System.out.println("please enter arguments");
			String[] targs2 = {"--help"};

			//args = targs;
			args = targs2;
		}
		
		MCLRunner runner = new MCLRunner();
		JCommander j = new JCommander();
		j.setAcceptUnknownOptions(true);
		j.addObject(runner);
		j.parse(args);
		runner.run(args);
	}
	
}
