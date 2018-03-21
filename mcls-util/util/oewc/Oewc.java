package util.oewc;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map.Entry;

import util.Transformer;

public class Oewc {

	
	static public Entry<Integer, Float> singleParticleModified(List<Float> Zt, List<Float> circles) throws Exception{
		if(Zt.size() > circles.size())
			throw new Exception("cannot calculate OEWC!!!!!!!!!!!");
		float weightv1;
		//float weightv2;
		int bestZ = 0;
		float bestWeightv1 = 1;
		//float bestWeightv2 = 1;
		for(int z = 0; z < circles.size(); z++){
			//calculate the weight between Zt and the Sensor data with the orientation.
			weightv1 = 0;
			//weightv2 = 0;
			for(int i = 0 ; i < Zt.size() ; i++){
				/*single beam*/
				//version 1
				weightv1 += Math.abs(
						Zt.get(i)-
						circles.get( Transformer.local2global(i, z, circles.size()))
						);
				//version 2
				/*double hypz = Zt.get(i);
				double obsz = circles.get( Transformer.local2global(i, z, circles.size()));
				double prob = ( 1.0 / 20 * Math.sqrt(2.0 * Math.PI) 
						* Math.exp( (hypz-obsz)*(hypz-obsz) / ( -2*20*20 ) ) );
				weightv2 += Math.log(prob);*/
			}
			weightv1 = weightv1/Zt.size();
			//if the weight is better, keep it.
			if(z == 0 || bestWeightv1 > weightv1 ){//version 1
			//if(z == 0 || weight > bestWeight){//version 2
				bestWeightv1 = weightv1;
				//bestWeightv2 = weightv2;
				bestZ = z;
			}
		}
		return new AbstractMap.SimpleEntry<Integer, Float>(bestZ, bestWeightv1);
		//return new AbstractMap.SimpleEntry<Integer, Float>(bestZ, bestWeightv2);
	}
		
	@Deprecated
	static public Entry<Integer, Float> singleParticle(float[] Zt, float[] circles){
		float weight = 1;
		int bestZ = 0;
		float bestWeight = 1;
		for(int z = 0; z < circles.length; z++){
			//calculate the weight between Zt and the Sensor data with the orientation.
			weight = Transformer.weight_LossFunction(Zt, Transformer.drawMeasurements(circles, z));
			//if the weight is better, keep it.
			if(bestWeight > weight || z == 0){
				bestWeight = weight;
				bestZ = z;
			}
		}
		
		return new AbstractMap.SimpleEntry<Integer, Float>(bestZ, bestWeight);
	}
	
	@Deprecated
	static public Entry<Integer, Float> singleParticle(List<Float> Zt, List<Float> circles){
		float weight = 1;
		int bestZ = 0;
		float bestWeight = 1;
		for(int z = 0; z < circles.size(); z++){
			//calculate the weight between Zt and the Sensor data with the orientation.
			weight = Transformer.weight_LossFunction(Zt, Transformer.drawMeasurements(circles, z));
			//if the weight is better, keep it.
			if(bestWeight > weight || z == 0){
				bestWeight = weight;
				bestZ = z;
			}
		}
		return new AbstractMap.SimpleEntry<Integer, Float>(bestZ, bestWeight);
	}
	
}
