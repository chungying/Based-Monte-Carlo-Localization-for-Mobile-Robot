package mcl;

import java.io.IOException;
import java.util.List;

import samcl.SAMCL;
import util.metrics.Particle;
import util.metrics.Transformer;
import util.robot.RobotState;

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
	}

	@Override
	public void Caculating_SER(float weight, float[] Zt, List<Particle> SER_set, List<Particle> global_set)
			throws IOException {
		//Do nothing in MCL
	}

	@Override
	public long updateParticle( List<Particle> src) throws Exception {
		long trasmission = System.currentTimeMillis();
		for(Particle p : src){
			p.setMeasurements(
					this.grid.getMeasurementsOnTime(p.getX(), p.getY(), Transformer.th2Z(p.getTh(), this.orientation))
					);
		}
		return System.currentTimeMillis()-trasmission;
	}

	@Override
	public long batchWeight(RobotState robot, List<Particle> src, float[] robotMeasurements)
			throws Exception {
		long weightTime = System.currentTimeMillis();
		for(Particle p : src){
			this.WeightParticle(p, robotMeasurements);
		}
		return System.currentTimeMillis()-weightTime;
	}

	public MCL(boolean cloud, int orientation, String mapFilename,
			float deltaEnergy, int nt, float xI, float aLPHA,
			int tournamentPresure) throws IOException {
		super(cloud, orientation, mapFilename, deltaEnergy, nt, xI, aLPHA,
				tournamentPresure);
	}

}
