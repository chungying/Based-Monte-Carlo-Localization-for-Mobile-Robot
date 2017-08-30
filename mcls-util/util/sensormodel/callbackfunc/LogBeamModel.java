package util.sensormodel.callbackfunc;

import java.util.List;

import util.measurementmodel.LaserModel.LaserData;
import util.metrics.Particle;
import util.metrics.Transformer;

public class LogBeamModel 
extends SensorModelCallback<List<Particle>, LaserData, Float>{

	@Override
	public Float call(List<Particle> set, LaserData data) {
		for(Particle p : set){
			if(p.isIfmeasurements()){
				//optimality is changed into maximizing. done!
				List<Float> a = p.getMeasurements();
				float w = Transformer.weight_LogBeamModel(a, data);
				p.setWeight(w);
			}else{
				//assigning the worst weight.
				p.setWeight(-Float.MAX_VALUE);
			}
		}

		return 0f;
	}

}
