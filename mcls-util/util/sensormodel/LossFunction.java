package util.sensormodel;

import util.metrics.Particle;
import util.metrics.Transformer;

public class LossFunction extends SensorModel{

	@Override
	public Float call() throws Exception {
		if(this.isupdated == false){
			return null;
		}

		for(Particle p : this.set){
			if(p.isIfmeasurements()){//FIXME
				//optimality is changed into maximizing. done!
				float w = Transformer.weightFloatLoss(p.getMeasurements(), data.getBeamRange());
				p.setWeight( 
						-1*w
					);
			}else{
				//assigning the worst weight.
				p.setWeight(-Float.MAX_VALUE);
			}
		}

		this.isupdated = false;
		return 0f;
	}
	
}
