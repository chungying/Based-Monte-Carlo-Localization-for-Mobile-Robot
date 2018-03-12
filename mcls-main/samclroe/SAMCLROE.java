package samclroe;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import samcl.SAMCL;
import util.Transformer;
import util.grid.Grid;
import util.oewc.Oewc;
import util.pf.Particle;
import util.pf.sensor.laser.LaserModel.LaserModelData;

public class SAMCLROE extends SAMCL{

	

	@Override
	public long batchWeight( List<Particle> src, LaserModelData laserData, Grid grid)
			throws Exception {
		
		long weightTime = System.currentTimeMillis();
		Transformer.filterParticle(src);
		//remove duplicated particles in X-Y domain
		for(Particle p : src){
				if (grid.map_array(p.getX(), p.getY()) == Grid.GRID_EMPTY) {
					Entry<Integer, Float> entry = Oewc.singleParticleModified(
							laserData.data.beamranges/*robotMeasurements*/, 
							p.getMeasurements());
					p.setTh(Transformer.Z2Th(entry.getKey(), this.sensor.getOrientation()));
//					p.setWeight(entry.getValue());
					p.setWeightForNomalization(entry.getValue());
				} else {
					//if the position is occupied, then assign the worst weight.
//					p.setWeight(1);
					p.setWeightForNomalization(1);
				}
		}
		return System.currentTimeMillis() - weightTime;
	}

	public SAMCLROE()
			throws IOException {
		super();
	}

}
