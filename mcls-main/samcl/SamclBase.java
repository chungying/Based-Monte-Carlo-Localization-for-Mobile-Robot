package samcl;

import java.io.IOException;
import java.util.List;

import com.google.protobuf.ServiceException;

import robot.VelocityModel;
import util.metrics.Particle;

public interface SamclBase {
	public void Caculating_SER(float weight, float[] Zt, List<Particle> SER_set, List<Particle> global_set) throws IOException;
	public List<Particle> Prediction_total_particles(
			List<Particle> src, 
			VelocityModel u, 
			long duration) throws Exception;
	public void batchWeight(List<Particle> src, float[] robotMeasurements) throws IOException, ServiceException, Throwable ;
	public Particle Determining_size(List<Particle> src);
	public void Local_resampling(List<Particle> src, List<Particle> dst, Particle bestParticle);
	public void Global_drawing(List<Particle> src, List<Particle> dst);
	public List<Particle> Combining_sets(List<Particle> set1, List<Particle> set2);
}
