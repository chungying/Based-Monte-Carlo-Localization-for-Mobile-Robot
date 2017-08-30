package util.sensormodel.callbackfunc;

import java.util.List;

import util.measurementmodel.LaserModel.LaserData;
import util.metrics.Particle;
import util.metrics.Transformer;

public class LossFunction extends SensorModelCallback<List<Particle>, LaserData, Float>{

	@Override
	public Float call(List<Particle> set, LaserData data) {
		for(Particle p : set){
			if(p.isIfmeasurements()){//FIXME
				//optimality is changed into maximizing. done!
				float w = Transformer.weight_LossFunction(p.getMeasurements(), data.data.beamranges);
				p.setWeight( 
						-1*w
					);
			}else{
				//assigning the worst weight.
				p.setWeight(-Float.MAX_VALUE);
			}
		}

		return 0f;
	}

}
