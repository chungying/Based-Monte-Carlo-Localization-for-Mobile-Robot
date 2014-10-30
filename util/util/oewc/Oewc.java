package util.oewc;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map.Entry;

import util.metrics.Transformer;

public class Oewc {
	
	static public Entry<Integer, Float> singleParticleModified(float[] Zt, float[] circles){
		float weight;
		int bestZ =0;
		float bestWeight = 1;
		for(int z = 0 ; z < circles.length; z++){
			//calculate the weight
			weight = 0;
			for(int i = 0 ; i < Zt.length;i++){
				weight += Math.abs(Zt[i]-circles[Transformer.local2global(i, z, circles.length)]); 
			}
			weight = weight/Zt.length;
			if(bestWeight>weight){
				bestWeight = weight;
				bestZ = z;
			}
		}
		
		return new AbstractMap.SimpleEntry<Integer, Float>(bestZ, bestWeight);
	}
		
	static public Entry<Integer, Float> singleParticle(float[] Zt, float[] circles){
		float weight = 1;
		int bestZ = 0;
		float bestWeight = 1;
//		particle.setWeight(1.0f);
		for(int z = 0; z < circles.length; z++){
			//calculate the weight between Zt and the Sensor data with the orientation.
			weight = Transformer.WeightFloat(Zt, Transformer.drawMeasurements(circles, z));
			//if the weight is better, keep it.
			if(bestWeight > weight){
				bestWeight = weight;
				bestZ = z;
//				particle.setWeight(weight);
//				particle.setTh(z);
			}
		}
		
		return new AbstractMap.SimpleEntry<Integer, Float>(bestZ, bestWeight);
	}
	
	static public Entry<Integer, Float> singleParticle(List<Float> Zt, List<Float> circles){
		float weight = 1;
		int bestZ = 0;
		float bestWeight = 1;
		for(int z = 0; z < circles.size(); z++){
			//calculate the weight between Zt and the Sensor data with the orientation.
			weight = Transformer.WeightFloat(Zt, Transformer.drawMeasurements(circles, z));
			//if the weight is better, keep it.
			if(bestWeight > weight){
				bestWeight = weight;
				bestZ = z;
			}
		}
		return new AbstractMap.SimpleEntry<Integer, Float>(bestZ, bestWeight);
	}
	
}
