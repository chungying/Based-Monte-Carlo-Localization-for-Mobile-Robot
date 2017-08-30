package util.sensormodel.callbackfunc;

import java.util.List;

import util.measurementmodel.LaserModel.LaserData;
import util.metrics.Particle;
import util.metrics.Transformer;

public class BeamModel
extends SensorModelCallback<List<Particle>, LaserData, Float>{

	@Override
	public Float call(List<Particle> set, LaserData data) {
		Float total = 0.0f;
		for(Particle p : set){
			
			if(p.isIfmeasurements()){//FIXME
				//optimality is changed into maximizing. done!
				List<Float> a = p.getMeasurements();
				//float w = Transformer.weight_BeamModel_Gauss(a,b);
				float w = Transformer.weight_BeamModel(a, data);
				p.setWeight(w);
/*				if(w==0){
					System.out.println(Arrays.toString(a));
					System.out.println(Arrays.toString(b));
					w = Transformer.WeightFloat_BeamModel(a,b);
					System.out.println("it's impossible. ");
				}*/
			}else{
				//assigning the worst weight.
				p.setWeight(0);
				/*count++;*/
			}
			total += p.getWeight();
		}
		//normalization is needed by regular method of particle filter.
		if(total>0){
			for(Particle p : set){
				p.setWeight(p.getWeight()/total);
			}
		}

		return total;
	}

}
