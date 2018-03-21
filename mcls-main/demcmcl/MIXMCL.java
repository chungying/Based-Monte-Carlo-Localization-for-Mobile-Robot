package demcmcl;

import java.util.List;
import util.grid.Grid;
import util.pf.Particle;
import util.pf.sensor.laser.LaserModel.LaserModelData;
import util.robot.RobotState;

public class MIXMCL extends DEMCMCL {
	
	@Override
	public void localResampling(List<Particle> src, List<Particle> dst,
			RobotState robot,
			LaserModelData laserData,
			Grid grid,
			Particle bestParticle) {
		DEMC(src, src, laserData, grid, ita, b);
		//reassign all particles with equal weigh
		double sumW = 0;
		for(Particle p:src){
			sumW+=p.getOriginalWeight();
		}
		for(Particle p:src){
			p.setWeightForNomalization(p.getOriginalWeight()/sumW);
		}
		super.localResampling(src, dst, robot, laserData, grid, bestParticle);
		for(Particle p: dst){
			p.setWeightForNomalization(1.0 / ((double)dst.size()));
		}
	}
}
