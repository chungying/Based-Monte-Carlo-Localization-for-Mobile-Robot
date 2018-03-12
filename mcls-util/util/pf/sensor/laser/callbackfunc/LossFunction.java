package util.pf.sensor.laser.callbackfunc;

import java.util.List;

import util.Transformer;
import util.pf.Particle;
import util.pf.sensor.laser.LaserModel.LaserModelData;

public class LossFunction extends SensorModelCallback<List<Particle>, LaserModelData, Double>{

	@Override
	public Double call(List<Particle> set, LaserModelData data) {
		for(Particle p : set){
			if(p.isIfmeasurements()){
				double w = Transformer.weight_LossFunction(p.getMeasurements(), data.data.beamranges);
//				p.setWeight(-1*w);//TODO
				p.setWeightForNomalization(-1*w);
			}else{
				//assigning the worst weight.
//				p.setWeight(-Float.MAX_VALUE);
				p.setWeightForNomalization(-Double.MAX_VALUE);
			}
		}

		return .0;
	}

}
