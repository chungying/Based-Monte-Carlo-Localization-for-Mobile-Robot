package mcl;

import java.io.IOException;
import java.util.List;

import samcl.SAMCL;
import util.metrics.Particle;
import util.metrics.Transformer;

public class MCL extends SAMCL{
	
	@Override
	public Particle Determining_size(List<Particle> src) {
		this.Nl = this.Nt;
		this.Ng = 0;
		return Transformer.minParticle(src);
	}

	@Override
	public void Global_drawing(List<Particle> src, List<Particle> dst) {
		//Do nothing in MCL
		//super.Global_drawing(src, dst);
	}

	@Override
	public void Caculating_SER(float weight, float[] Zt, List<Particle> SER_set, List<Particle> global_set)
			throws IOException {
//		// TODO Auto-generated method stub
//		super.Caculating_SER(weight, Zt, SER_set);
	}

	@Override
	public void batchWeight(List<Particle> src, float[] robotMeasurements)
			throws Exception {
		for(Particle p : src){
			float[] m = this.grid.getMeasurementsOnTime(p.getX(), p.getY(), Transformer.th2Z(p.getTh(), this.orientation));
			p.setMeasurements(m);
			this.WeightParticle(p, robotMeasurements);
		}
	}

	public MCL(boolean cloud, int orientation, String mapFilename,
			float deltaEnergy, int nt, float xI, float aLPHA,
			int tournamentPresure) throws IOException {
		super(cloud, orientation, mapFilename, deltaEnergy, nt, xI, aLPHA,
				tournamentPresure);
	}

}
