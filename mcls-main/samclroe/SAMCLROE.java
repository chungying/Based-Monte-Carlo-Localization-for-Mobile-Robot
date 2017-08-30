package samclroe;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import samcl.SAMCL;
import util.grid.Grid;
import util.measurementmodel.LaserModel.LaserData;
import util.metrics.Particle;
import util.metrics.Transformer;
import util.oewc.Oewc;

public class SAMCLROE extends SAMCL{

	

	@Override
	public long batchWeight( List<Particle> src, LaserData laserData)
			throws Exception {
		
		long weightTime = System.currentTimeMillis();
		Transformer.filterParticle(src);
		//remove duplicated particles in X-Y domain
		for(Particle p : src){
				if (this.grid.map_array(p.getX(), p.getY()) == Grid.GRID_EMPTY) {
					Entry<Integer, Float> entry = Oewc.singleParticleModified(
							laserData.data.beamranges/*robotMeasurements*/, 
							p.getMeasurements());
					p.setTh(Transformer.Z2Th(entry.getKey(), this.sensor.getOrientation()));
					p.setWeight(entry.getValue());
				} else {
					//if the position is occupied, then assign the worst weight.
					p.setWeight(1);
				}
		}
		return System.currentTimeMillis() - weightTime;
	}

	public SAMCLROE(/*int orientation,*/ float deltaRnergy,
			int nt, float xI, float aLPHA, int tournamentPresure)
			throws IOException {
		super(/*orientation, */deltaRnergy, nt, xI, aLPHA,
				tournamentPresure);
	}

}
