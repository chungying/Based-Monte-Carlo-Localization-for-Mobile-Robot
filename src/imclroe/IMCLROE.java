package imclroe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

import coprocessor.services.OewcEndpoint;
import coprocessor.services.generated.OewcProtos;
import coprocessor.services.generated.OewcProtos.OewcRequest;
import coprocessor.services.generated.OewcProtos.OewcResponse;
import samcl.SAMCL;
import util.metrics.Particle;

public class IMCLROE extends SAMCL{
	
	public static void main(String[] args){
		//TODO finish the main function
	}
	
	@Override
	public void batchWeight(List<Particle> src, float[] robotMeasurements)
			throws ServiceException, Throwable {
		
		Batch.Call<OewcEndpoint,OewcResponse> b = new OewcCall(src,robotMeasurements);
		Map<byte[],OewcResponse> results = this.table.coprocessorService(OewcEndpoint.class, null, null, b);
		//setup weight and orientatin to the particles(src)
		Long sum = 0l;
		List<Particle> result = new ArrayList<Particle>();
		for(Entry<byte[], OewcResponse> entry:results.entrySet()){
			sum = sum + entry.getValue().getCount();
			for(OewcProtos.Particle op : entry.getValue().getParticlesList()){
				result.add(IMCLROE.ParticleFromO(op));
			}
		}
		
		//change the result to src 
		src.clear();
		src.addAll(result);
	}

	public IMCLROE(boolean cloud, int orientation, String map_filename,
			float delta_energy, int nt, float xI, float aLPHA,
			int tournament_presure) throws IOException {
		super(cloud, orientation, map_filename, delta_energy, nt, xI, aLPHA,
				tournament_presure);
	}
	public static Particle ParticleFromO(
			coprocessor.services.generated.OewcProtos.Particle op) {
		Particle p = new Particle(op.getX(), op.getY(), op.getZ());
		p.setWeight(op.getW());
		return p;
	}

	public static OewcProtos.Particle particleFromS(Particle p){
		OewcProtos.Particle.Builder builder = OewcProtos.Particle.newBuilder();
		builder.setX(p.getX()).setY(p.getY()).setZ(p.getZ());
		return builder.build();
	}
	public static OewcRequest setupRequest(List<Particle> src, float[] measurements){
		OewcRequest.Builder builder = OewcRequest.newBuilder();
		ArrayList<OewcProtos.Particle> ps = new ArrayList<OewcProtos.Particle>();
		for(Particle p : src){
			ps.add(IMCLROE.particleFromS(p));
		}
		builder.addAllParticles(ps);
		return builder.build();
	}
	public class OewcCall implements Batch.Call<OewcEndpoint, OewcResponse>{
		List<Particle> particles = null;
		float[] robotMeasurements = null;
		public OewcCall(List<Particle> particles, float[] robotMeasurements){
			this.particles = particles;
			this.robotMeasurements = robotMeasurements;
		}
		
		@Override
		public OewcResponse call(OewcEndpoint endpoint) throws IOException {
			// TODO IMCLROE
			BlockingRpcCallback<OewcResponse> done = new BlockingRpcCallback<OewcResponse>();
			RpcController controller = new ServerRpcController();
			OewcRequest request = IMCLROE.setupRequest(particles, robotMeasurements);
			endpoint.getRowCount(controller, request, done);
			return done.get();
		}
		
	}

}
