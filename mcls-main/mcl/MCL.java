package mcl;

import java.io.IOException;
import java.util.List;

import samcl.SAMCL;
import util.metrics.Particle;
import util.metrics.Transformer;
import util.robot.RobotState;

public class MCL extends SAMCL{
	
	@Override
	public void determiningSize(Particle bestParticle) {
		this.Nl = this.Nt;
		this.Ng = 0;
		//return Transformer.minParticle(src);
	}

	@Override
	public void caculatingSER(List<Particle> current_set, float best_weight, float[] Zt, List<Particle> SER_set, List<Particle> global_set)
			throws IOException {
		//Do nothing in MCL
	}

	public MCL(boolean cloud, int orientation, String mapFilename,
			float deltaEnergy, int nt, float xI, float aLPHA,
			int tournamentPresure) throws IOException {
		super(cloud, orientation, mapFilename, deltaEnergy, nt, xI, aLPHA,
				tournamentPresure);
	}

}
