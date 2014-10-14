package imclroe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.protobuf.RpcController;

import coprocessor.services.generated.OewcProtos;
import coprocessor.services.generated.OewcProtos.OewcRequest;
import coprocessor.services.generated.OewcProtos.OewcResponse;
import coprocessor.services.generated.OewcProtos.OewcService;
import samcl.SAMCL;
import util.metrics.Particle;
import util.metrics.Transformer;

public class IMCLROE extends SAMCL{

	
	@Override
	public void batchWeight(List<Particle> src, float[] robotMeasurements)
			throws Throwable {
		List<Long> times = new ArrayList<Long>();
		times.add(System.currentTimeMillis());
		Batch.Call<OewcService,OewcResponse> b = new OewcCall( src, robotMeasurements, this.orientation);
		times.add(System.currentTimeMillis());
//		System.out.println("service.....");
//		System.out.println("src size:"+ src.size());
//		System.out.println(Arrays.toString(robotMeasurements));
//		System.out.println("orientation: "+this.orientation);
		Map<byte[],OewcResponse> results = this.table.coprocessorService(OewcService.class, "0000".getBytes(), "1000".getBytes(), b);
//		System.out.println("service done.");
		//setup weight and orientatin to the particles(src)
		times.add(System.currentTimeMillis());
		Long sum = 0l;
		List<Particle> result = new ArrayList<Particle>();
//		System.out.println("results....");
		for(Entry<byte[], OewcResponse> entry:results.entrySet()){
			sum = sum + entry.getValue().getCount();
			System.out.println(/*Bytes.toString(entry.getKey())+"\n"+*/entry.getValue().getStr());
			for(OewcProtos.Particle op : entry.getValue().getParticlesList()){
				result.add(IMCLROE.ParticleFromO(op, this.orientation));
			}
		}
		times.add(System.currentTimeMillis());
		//change the result to src 
//		System.out.println("store..");
		src.clear();
		src.addAll(result);
		times.add(System.currentTimeMillis());
		int counter = 0;
		System.out.println("-----------------------------");
		for(Long time: times){
			counter++;
			System.out.println("\t"+counter + "\t:"+ time);
		}
	}

	public IMCLROE(boolean cloud, int orientation, String mapFilename,
			float deltaEnergy, int nt, float xI, float aLPHA,
			int tournamentPresure) throws IOException {
		super(cloud, orientation, mapFilename, deltaEnergy, nt, xI, aLPHA,
				tournamentPresure);
	}
	
	public static Particle ParticleFromO(
			coprocessor.services.generated.OewcProtos.Particle op, int orientation) {
		Particle p = new Particle(op.getX(), op.getY(), Transformer.Z2Th(op.getZ(), orientation));
		p.setWeight(op.getW());
		return p;
	}

	public static OewcProtos.Particle particleFromS(Particle p, int orientation){
		OewcProtos.Particle.Builder builder = OewcProtos.Particle.newBuilder();
		builder.setX(p.getX()).setY(p.getY()).setZ(Transformer.th2Z(p.getTh(), orientation));
		return builder.build();
	}
	
	public static OewcRequest setupRequest(List<Particle> src, float[] measurements, int orientation){
		OewcRequest.Builder builder = OewcRequest.newBuilder();
		//build src
		ArrayList<OewcProtos.Particle> ps = new ArrayList<OewcProtos.Particle>();
		for(Particle p : src){
			ps.add(IMCLROE.particleFromS(p, orientation));
		}
		builder.addAllParticles(ps);
		List<Float> Zt = new ArrayList<Float>();
		for(float f: measurements){
			Zt.add(new Float(f));
		}
		builder.addAllMeasurements(Zt);
		return builder.build();
	}
	
	public class OewcCall implements Batch.Call<OewcService, OewcResponse>{
		
		List<Particle> particles = null;
		float[] robotMeasurements = null;
		int orientation;
		
		public OewcCall(List<Particle> particles, float[] robotMeasurements, int orientation){
			this.particles = particles;
			this.robotMeasurements = robotMeasurements;
			this.orientation = orientation;
		}
		
		@Override
		public OewcResponse call(OewcService endpoint) throws IOException {
			BlockingRpcCallback<OewcResponse> done = new BlockingRpcCallback<OewcResponse>();
			RpcController controller = new ServerRpcController();
			OewcRequest request = IMCLROE.setupRequest(this.particles, this.robotMeasurements, this.orientation);
			endpoint.getRowCount(controller, request, done);
			return done.get();
		}	
	}
	
}
