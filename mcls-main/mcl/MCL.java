package mcl;

import java.io.IOException;
import java.util.List;

import com.beust.jcommander.Parameter;

import samcl.SAMCL;
import util.Transformer;
import util.grid.Grid;
import util.pf.Particle;
import util.pf.sensor.laser.LaserModel.LaserModelData;
import util.pf.sensor.laser.MCLLaserModel.ModelType;
import util.robot.RobotState;

public class MCL extends SAMCL{
	/**
	 * Augmented MCL parameters
	 */
	//TODO implement augmented MCL
	@SuppressWarnings("unused")
	private double wDiff;
	private double wFast;
	private double wSlow;
	@Parameter(names = {"--amcl"}, 
			description = "setup augmented mcl, default is false", 
			required = false, arity = 1)
	public boolean isAugmented = false;
	@Parameter(names = {"--alphaFast"}, 
			description = "alphaFast of Augmented MCL, default is 0.1.", 
			required = false, arity = 1)
	public double alphaFast = 0.1;
	@Parameter(names = {"--alphaSlow"}, 
			description = "alphaSlow of Augmented MCL, default is 0.001.", 
			required = false, arity = 1)
	public double alphaSlow = 0.001;
	
	@Override
	public long batchWeight(List<Particle> src, LaserModelData laserData, Grid grid) throws Exception {
		long weightTime = System.currentTimeMillis();
		double totalW = this.sensor.calculateParticleWeight(src, laserData);
		if(this.isAugmented){//TODO Augmented MCL updates wFast and wSlow while weighting
			double wAvg = totalW/src.size();
			
			if(this.wSlow == 0)
				this.wSlow = wAvg;
			else
				this.wSlow += this.alphaSlow*(wAvg - this.wSlow);
			
			if(this.wFast == 0)
				this.wFast = wAvg;
			else
				this.wFast += this.alphaFast*(wAvg - this.wFast);
		}
		return System.currentTimeMillis() - weightTime;
	}

	@Override
	public void globalSampling(List<Particle> set, RobotState robot, Grid grid) throws Exception {
		super.globalSampling(set, robot, grid);
	}

	@Override
	public void localResampling(List<Particle> src, List<Particle> dst,
			RobotState robot,
			LaserModelData laserData,
			Grid grid,
			Particle bestParticle) {
		dst.clear();
		if( this.sensor.getModeltype().equals(ModelType.DEFAULT)|| this.sensor.getModeltype().equals(ModelType.BEAM_MODEL)){
			Transformer.resamplingLowVariance(src,dst);
//			Transformer.resamplingCPT(src, dst);
//			super.localResampling(src, dst, bestParticle);
			for(Particle p:dst)
				p.setWeightForNomalization(1.0/dst.size());
		}else
			super.localResampling(src, dst, robot, laserData, grid, bestParticle);
	}

	@Override
	public void determiningSize(Particle bestParticle) {
		this.Nl = this.Nt;
		this.Ng = 0;
		//return Transformer.minParticle(src);
	}

	@Override
	public void caculatingSER(
			List<Particle> current_set, 
			Particle bestParticle, 
			List<Float> Zt, 
			List<Particle> SER_set, 
			List<Particle> global_set,
			Grid grid)
			throws IOException {
		//Do nothing in MCL
	}

	public MCL(){
		super();
	}

}
