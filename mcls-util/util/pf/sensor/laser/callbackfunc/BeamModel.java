package util.pf.sensor.laser.callbackfunc;

import java.util.List;

import util.Transformer;
import util.pf.Particle;
import util.pf.sensor.laser.LaserModel.LaserModelData;

public class BeamModel
extends SensorModelCallback<List<Particle>, LaserModelData, Double>{

	@Override
	public Double call(List<Particle> set, LaserModelData data) throws Exception {
		Double totalD = .0;
		int count1=0;
		for(Particle p : set){
			if(p.isIfmeasurements()){
				double[] result = Transformer.weight_BeamModel(p.getMeasurements(), data);
				p.setWeightForNomalization(p.getNomalizedWeight() * result[0]);
				p.setOriginalWeight(result[0]);
				p.setLogW(result[1]);
			}else{
				//assigning the worst weight.
				p.setWeightForNomalization(0);
				count1++;
			}
			totalD +=p.getNomalizedWeight();
			assert(totalD!=Double.POSITIVE_INFINITY);
		}
		assert(count1<set.size());//if count1 equals to the size, all of particles are in occupied area.
		
		//normalization is needed by regular method of particle filter.
		assert(totalD>0);
		double max = 0;
		double min = 1;
		//TODO implement augmented MCL
		@SuppressWarnings("unused")
		double wAvg = totalD/set.size();
//		if(wAvg < 0.01)
//			System.out.println("particle total weight is too small. " + totalD);
		//TODO totalD might be zero when all particles are located in invalid area.
		for(Particle p : set){
			double w = p.getNomalizedWeight()/totalD;
			assert(w>=0);
			p.setWeightForNomalization(w);
			if(max < w)
				max = w;
			if(min > w)
				min = w;
		}
//		System.out.println("max weight: " + max + ", min weight: " + min);
		return totalD;
	}

}
