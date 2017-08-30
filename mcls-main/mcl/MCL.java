package mcl;

import java.io.IOException;
import java.util.List;

import samcl.SAMCL;
import util.metrics.Particle;

public class MCL extends SAMCL{
	
	@Override
	public void determiningSize(Particle bestParticle) {
		this.Nl = this.Nt;
		this.Ng = 0;
		//return Transformer.minParticle(src);
	}

	@Override
	public void caculatingSER(List<Particle> current_set, float best_weight, List<Float> Zt, List<Particle> SER_set, List<Particle> global_set)
			throws IOException {
		//Do nothing in MCL
	}

	public MCL(boolean cloud, 
			int orientation, //TODO should be replaced by laser
//			float deltaEnergy, //TODO No need for MCL
			int nt, 
//			float xI, //TODO No need for MCL
//			float aLPHA,//TODO No need for MCL
			int tournamentPresure) throws IOException {
		super(cloud, /*orientation,*/ 0/*deltaEnergy*/, nt, 0/*xI*/, 0/*aLPHA*/,
				tournamentPresure);
	}

}
