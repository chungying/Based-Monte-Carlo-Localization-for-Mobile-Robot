package normanmcl;

import java.io.IOException;
import java.util.List;

import samcl.SAMCL;
import util.metrics.Particle;

public class NORMANMCL extends SAMCL{
	

	@Override
	public void determiningSize(Particle bestParticle) {
		this.Nl = this.Nt;
		this.Ng = 0;
		//return Transformer.minParticle(src);
	}

	@Override
	public void caculatingSER(List<Particle> current_set, float weight, List<Float> Zt, List<Particle> SER_set, List<Particle> global_set)
			throws IOException {
//		// TODO Auto-generated method stub
//		super.Caculating_SER(weight, Zt, SER_set);
	}

	public NORMANMCL(boolean cloud, /*int orientation,*/
			float deltaEnergy, int nt, float xI, float aLPHA,
			int tournamentPresure) throws IOException {
		super(cloud, /*orientation,*/ deltaEnergy, nt, xI, aLPHA,
				tournamentPresure);
		// TODO Auto-generated constructor stub
	}

}
