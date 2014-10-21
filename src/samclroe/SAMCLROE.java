package samclroe;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import samcl.SAMCL;
import util.metrics.Particle;
import util.metrics.Transformer;
import util.oewc.Oewc;

public class SAMCLROE extends SAMCL{

	

	@Override
	public void batchWeight(List<Particle> src, float[] robotMeasurements)
			throws Exception {
		for(Particle p : src){
			Entry<Integer, Float> entry = Oewc.singleParticle(robotMeasurements, 
					this.grid.getMeasurements(this.table, onCloud, p.getX(), p.getY(), -1) , 
					this.orientation);
			p.setTh(Transformer.Z2Th(entry.getKey(), this.orientation));
			p.setWeight(entry.getValue());
		}
	}

	public SAMCLROE(int orientation, String mapFilename, float deltaRnergy,
			int nt, float xI, float aLPHA, int tournamentPresure)
			throws IOException {
		super(orientation, mapFilename, deltaRnergy, nt, xI, aLPHA,
				tournamentPresure);
	}

}
