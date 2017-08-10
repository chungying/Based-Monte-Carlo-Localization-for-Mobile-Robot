package util.sensormodel;


import util.metrics.Particle;
import util.metrics.Transformer;

public class LogBeamModel extends BasicSensorModel{
	
	@Override
	public Float call() throws Exception {
		if(this.isupdated == false){
			return null;
		}

		for(Particle p : this.set){
			if(p.isIfmeasurements()){
				//optimality is changed into maximizing. done!
				float[] a = p.getMeasurements();
				float w = Transformer.weight_LogBeamModel(a, data);
				p.setWeight(w);
			}else{
				//assigning the worst weight.
				p.setWeight(-Float.MAX_VALUE);
			}
		}

		this.isupdated = false;
		return 0f;
	}
}

