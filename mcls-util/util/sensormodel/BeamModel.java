package util.sensormodel;

import util.metrics.Particle;
import util.metrics.Transformer;

public class BeamModel extends BasicSensorModel{
	
	@Override
	public Float call() throws Exception {
		if(this.isupdated == false){
			return null;
		}
		
		Float total = 0.0f;
		for(Particle p : this.set){
			
			if(p.isIfmeasurements()){//FIXME
				//optimality is changed into maximizing. done!
				float[] a = p.getMeasurements();
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

		this.isupdated = false;
		return total;
	}
}
