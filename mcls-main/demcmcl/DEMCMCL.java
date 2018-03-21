package demcmcl;

import java.util.ArrayList;
import java.util.List;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import mcl.MCL;
import util.Distribution;
import util.Transformer;
import util.grid.Grid;
import util.grid.GridTools;
import util.pf.Particle;
import util.pf.sensor.laser.LaserModel.LaserModelData;
import util.pf.sensor.laser.LaserSensor;
import util.robot.RobotState;
import util.runner.MCLRunner;

public class DEMCMCL extends MCL{
	
	public static void main(String[] args) throws Throwable{
		//for debug mode
		if(args.length==0){
			String[] targs = {			
//					"-i", "file:///Users/Jolly/workspace/dataset/simple/simmap.jpg"
					"-i", "file:///Users/Jolly/workspace/dataset/intel_lab/intel-map.png"
					,"-rx","120"
					,"-ry","120"
					,"-rh","0"
//					"-i", "file:///Users/Jolly/workspace/dataset/dataset_fr079/fr079_maps_gridmap.bmp"
//					,"-rx","170"
//					,"-ry","200"
//					,"-rh","90"
//					"-i", "/Users/Jolly/workspace/dataset/willowgarage/willowgarage2.pgm"
//					,"-rx","700"
//					,"-ry","900"
//					,"-rh","0"
					,"--numberofparticles","1000"
					,"-D","false"
					,"--showparticles", "true"
					,"--visualization", "true"
					,"--showmeasurements", "true"
					,"-lares","6"
					,"-lamin","-90"
					,"-lamax","90"
					,"-lrmax","500"
					,"--sigmahit", "5"
					,"--linearvelocity", "5"
					,"--sensormodel", "BEAM_MODEL"//"LOG_BEAM_MODEL"
					,"--className", "demcmcl.DEMCMCL"
//					,"--className", "demcmcl.MIXMCL"
					,"--forwardDist", "200"
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
	
	@Parameter(names = {"--demcita"}, description = "default is 1.5", required = false, arity = 1)
	protected double ita = 1.5;
	
	@Parameter(names = {"--demcnoise"}, description = "default is 1", required = false, arity = 1)
	protected double b = 1;
	
	public static enum GenePoolSource{
		RESAMPLED_SET, IRRESAMPLED_SET;

		public static GenePoolSource fromString(String value) {
			for(GenePoolSource output:GenePoolSource.values()){
				if(output.toString().equalsIgnoreCase(value)){
					return output;
				}
			}
			return GenePoolSource.RESAMPLED_SET;//this is default value.
		}
	}
	@Parameter(names = {"--demcgenepool"}, description = "the source of gene pool for DEMC, default is using resampled set. Options: RESAMPLED_SET and IRRESAMPLED_SET", required = false, arity =1)
	protected GenePoolSource genePool = GenePoolSource.RESAMPLED_SET;
	
	@Override
	public void localResampling(List<Particle> src, List<Particle> dst,
			RobotState robot,
			LaserModelData laserData,
			Grid grid,
			Particle bestParticle) {
		super.localResampling(src, dst, robot, laserData, grid, bestParticle);
		if(this.genePool == GenePoolSource.RESAMPLED_SET){
			DEMC(dst, dst, laserData, grid, this.ita, this.b);
		}else if(this.genePool == GenePoolSource.IRRESAMPLED_SET){
			DEMC(dst, src, laserData, grid, this.ita, this.b);
		}
		//reassign all particles with equal weigh
		for(Particle p: dst){
			p.setWeightForNomalization(1.0 / ((double)dst.size()));
		}
	}
	
	
	public static void DEMC(
			List<Particle> set, 
			final List<Particle> genePool, 
			final LaserModelData laserData, 
			final Grid grid,
			final double ita,
			final double b){
		//TODO record the acceptance rate
		@SuppressWarnings("unused")
		int acceptCount = 0, rejectCount = 0;
		double logR = 0, u = 0, logU = 0;
//		double R = 0, proposal = 0, current = 0, logC = 0, logP = 0;
		List<Particle> canSet = new ArrayList<Particle>();
		for(int idx = 0 ; idx < set.size() ; idx++){
			Particle parent = set.get(idx);
			//MH Jumps
			int[] r = select(genePool.size());
			Particle pr1 = genePool.get(r[0]), pr2 = genePool.get(r[1]);
			canSet.add(new Particle(
					variant(parent.getDX(), ita, pr1.getDX(), pr2.getDX(), b),
					variant(parent.getDY(), ita, pr1.getDY(), pr2.getDY(), b),
					variant(parent.getDX(), ita, pr1.getTh(), pr2.getTh(), b)
					));
		}
		
		for(int idx = 0 ; idx < canSet.size() ; idx++){
			Particle newChain = canSet.get(idx);
			Particle parent = set.get(idx);
			//evaluate
			List<Float> a = GridTools.getLaserDist(grid, laserData.sensor, newChain.getDX(), newChain.getDY(), newChain.getTh()).getKey();
			if(a==null){
				newChain.setLogW(Double.NEGATIVE_INFINITY);
				newChain.setOriginalWeight(0);
				newChain.setWeightForNomalization(0);
				continue;
			}
			double[] result = Transformer.weight_BeamModel(a, laserData);
			newChain.setWeightForNomalization(/*p.getNomalizedWeight() **/ result[0]);
			newChain.setOriginalWeight(result[0]);
			newChain.setLogW(result[1]);
			
			//accept or reject the new Markov chain
//			proposal = newChain.getOriginalWeight();
//			current = parent.getOriginalWeight();
//			logP = newChain.getLogW();
//			logC = parent.getLogW();
//			R = newChain.getOriginalWeight()/parent.getOriginalWeight();
			logR = newChain.getLogW() - parent.getLogW();
			u = Distribution.seed.nextDouble();
			logU = Math.log(u);
			if(logR > logU){
				//accept, swap newChain and parent, record parent for estimating.
				set.set(idx, newChain);
				acceptCount++;
			}else{
				//reject, do nothing 
				rejectCount++;
			}
		}
//		System.out.println("acceptance rate: " + acceptCount + "/" + set.size() );
//		System.out.println("proposal: " + proposal + ", logP: " + logP);
//		System.out.println("current : " + current + ", logC: " + logC);
//		System.out.println("ratio   : " + R + ", logR: " + logR);
//		System.out.println("uniform : " + u + ", logU: " + logU);
	}
	
	private static double variant(double xi, double ita, double xr1, double xr2, double b){
		return xi + ita * (xr1-xr2) + b * Distribution.seed.nextGaussian();
	}

	private static int[] select(int size){
		boolean isDuplicate = false;
		int[] indices = new int[2];
		for(int i = 0 ; i < indices.length;i++){
			do {
				isDuplicate = false;
				indices[i] = Distribution.seed.nextInt(size);
	
				for (int j = 0; j < i; j++) {
					if (indices[i] == indices[j]) {
						isDuplicate = true;
						break;
					}
				}
			} while (isDuplicate);
		}
		return indices;
	}
}
