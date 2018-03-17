package util.oewc;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map.Entry;

import util.Transformer;

public class Oewc {

	
	static public Entry<Integer, Float> singleParticleModified(List<Float> Zt, List<Float> circles) throws Exception{
		if(Zt.size() > circles.size())
			throw new Exception("cannot calculate OEWC!!!!!!!!!!!");
		float weight;
		int bestZ = 0;
		float bestWeight = 1;
		for(int z = 0; z < circles.size(); z++){
			//calculate the weight between Zt and the Sensor data with the orientation.
			weight = 0;
			for(int i = 0 ; i < Zt.size() ; i++){
				weight = weight + Math.abs(
						Zt.get(i)-
						circles.get(
								Transformer.local2global(i, z, circles.size()))
						);
			}
			weight = weight/Zt.size();
			//if the weight is better, keep it.
			if(bestWeight > weight || z == 0){
				bestWeight = weight;
				bestZ = z;
			}
		}
		return new AbstractMap.SimpleEntry<Integer, Float>(bestZ, bestWeight);
	}
	
	static public Entry<Integer, Float> singleParticleModified(List<Float> Zt, float[] circles) throws Exception{
		if(Zt.size() > circles.length)
			throw new Exception("cannot calculate OEWC!!!!!!!!!!!");
		float weight;
		int bestZ =0;
		float bestWeight = 1;
		for(int z = 0 ; z < circles.length; z++){
			//calculate the weight
			weight = 0;
			for(int i = 0 ; i < Zt.size();i++){
				weight = weight + Math.abs(
						Zt.get(i)-
						circles[Transformer.local2global(i, z, circles.length)]); 
			}
			weight = weight/Zt.size();
			//if the weight is better, keep it.
			if(bestWeight>weight || z == 0){
				bestWeight = weight;
				bestZ = z;
			}
		}
		
		return new AbstractMap.SimpleEntry<Integer, Float>(bestZ, bestWeight);
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
