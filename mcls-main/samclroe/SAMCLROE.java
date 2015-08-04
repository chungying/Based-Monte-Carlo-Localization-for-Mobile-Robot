package samclroe;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import samcl.SAMCL;
import util.grid.Grid;
import util.metrics.Particle;
import util.metrics.Transformer;
import util.oewc.Oewc;
import util.robot.RobotState;

public class SAMCLROE extends SAMCL{

	

	@Override
	public long batchWeight(RobotState robot, List<Particle> src, float[] robotMeasurements)
			throws Exception {
		//remove duplicated particles in X-Y domain
		Transformer.filterParticle(src);
		
		long weightTime = -1, timeSum = 0;
		boolean lock;
		for(Particle p : src){
//			try {
				if (this.grid.map_array(p.getX(), p.getY()) == Grid.GRID_EMPTY) {
					lock = robot.isLock();					
					robot.lock();
					float[] f = this.grid.getMeasurements(this.table, onCloud,
							p.getX(), p.getY(), -1);
					
					if(!lock){
						robot.unlock();
					}
					weightTime = System.nanoTime();
					Entry<Integer, Float> entry = Oewc.singleParticleModified(
							robotMeasurements, f);
					timeSum = timeSum + System.nanoTime() - weightTime;
					p.setTh(Transformer.Z2Th(entry.getKey(), this.orientation));
					p.setWeight(entry.getValue());
				} else {
					//if the position is occupied, then assign the worst weight.
					p.setWeight(1);
				}
//			} catch (Exception e) {
//				System.out.println(e+"\n"+p);
//			}
		}
		
		return (long)timeSum/1000000;//nano second to mili second
	}

	public SAMCLROE(int orientation, String mapFilename, float deltaRnergy,
			int nt, float xI, float aLPHA, int tournamentPresure)
			throws IOException {
		super(orientation, mapFilename, deltaRnergy, nt, xI, aLPHA,
				tournamentPresure);
	}

}
