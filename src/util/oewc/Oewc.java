package util.oewc;

import util.metrics.Particle;
import util.metrics.Transformer;

public class Oewc {
		
	static public void singleParticle(float[] Zt, Particle particle, float[] circles){
		//TODO
		float weight = 1;
		particle.setWeight(1.0f);
		for(int z = 0; z < particle.orientation; z++){
			//calculate the weight between Zt and the Sensor data with the orientation.
			weight = Transformer.WeightFloat(Zt, Transformer.getMeasurements(circles, z));
			//if the weight is better, keep it.
			if(particle.getWeight()>weight){
				particle.setWeight(weight);
				particle.setZ(z);
			}
		}
	}
}
