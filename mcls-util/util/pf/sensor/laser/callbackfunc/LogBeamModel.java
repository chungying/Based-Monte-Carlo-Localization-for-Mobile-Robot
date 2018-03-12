package util.pf.sensor.laser.callbackfunc;

import java.util.List;

import util.Transformer;
import util.pf.Particle;
import util.pf.sensor.laser.LaserModel.LaserModelData;

public class LogBeamModel 
extends SensorModelCallback<List<Particle>, LaserModelData, Double>{

	@Override
	public Double call(List<Particle> set, LaserModelData data) {
		for(Particle p : set){
			if(p.isIfmeasurements()){
				double w = Transformer.weightLogBeamModel(p.getMeasurements(), data);
				p.setWeightForNomalization(w);//TODO
			}else{
				//assigning the worst weight.
//				p.setWeight(-Float.MAX_VALUE);
				p.setWeightForNomalization(-Double.MAX_VALUE);
			}
		}

		return .0;
	}

}
