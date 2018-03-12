package normanmcl;

import java.io.IOException;
import java.util.List;

import util.grid.Grid;
import util.pf.Particle;
import samcl.SAMCL;

public class NORMANMCL extends SAMCL{
	

	@Override
	public void determiningSize(Particle bestParticle) {
		this.Nl = this.Nt;
		this.Ng = 0;
		//return Transformer.minParticle(src);
	}

	@Override
	public void caculatingSER(
			List<Particle> current_set, 
			Particle bestParticle, 
			List<Float> Zt, 
			List<Particle> SER_set,
			List<Particle> global_set,
			Grid grid)
			throws IOException {
//		// TODO Auto-generated method stub
//		super.Caculating_SER(weight, Zt, SER_set);
	}

	public NORMANMCL(boolean cloud, /*int orientation,*/
			float deltaEnergy, int nt, float xI, float aLPHA,
			int tournamentPresure) throws IOException {
		super();
		// TODO Auto-generated constructor stub
	}

}
