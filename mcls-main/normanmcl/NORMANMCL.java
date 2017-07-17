package normanmcl;

import java.io.IOException;
import java.util.List;

import samcl.SAMCL;
import util.metrics.Particle;
import util.metrics.Transformer;
import util.robot.RobotState;

public class NORMANMCL extends SAMCL{
	

	@Override
	public void determiningSize(Particle bestParticle) {
		this.Nl = this.Nt;
		this.Ng = 0;
		//return Transformer.minParticle(src);
	}

	@Override
	public void caculatingSER(List<Particle> current_set, float weight, float[] Zt, List<Particle> SER_set, List<Particle> global_set)
			throws IOException {
//		// TODO Auto-generated method stub
//		super.Caculating_SER(weight, Zt, SER_set);
	}

	public NORMANMCL(boolean cloud, int orientation, String mapFilename,
			float deltaEnergy, int nt, float xI, float aLPHA,
			int tournamentPresure) throws IOException {
		super(cloud, orientation, mapFilename, deltaEnergy, nt, xI, aLPHA,
				tournamentPresure);
		// TODO Auto-generated constructor stub
	}

}
