package util.sensormodel;

import java.util.Arrays;

import com.kenai.jffi.Array;

import util.metrics.Particle;
import util.metrics.Transformer;

public class BeamModel extends SensorModel{
	
	@Override
	public Float call() throws Exception {
		if(this.isupdated == false){
			return null;
		}
		
		Float total = 0.0f;
		/*int count=0;*/
		for(Particle p : this.set){
			
			if(p.isIfmeasurements()){//FIXME
				//optimality is changed into maximizing. done!
				float[] a = p.getMeasurements();
				float[] b = data.getBeamRange();
				float w = Transformer.WeightFloat_BeamModel(a,b);
				p.setWeight( 
						w
					);
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
