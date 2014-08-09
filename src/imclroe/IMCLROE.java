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

import coprocessor.services.generated.OewcProtos;
import coprocessor.services.generated.OewcProtos.OewcRequest;
import coprocessor.services.generated.OewcProtos.OewcResponse;
import coprocessor.services.generated.OewcProtos.OewcService;
import samcl.SAMCL;
import util.metrics.Particle;
import util.metrics.Transformer;

public class IMCLROE extends SAMCL{
	/*
	public static void main(String[] args) throws Throwable{
		//for debug mode
		if(args.length==0){
			String[] targs = {"-cl"
					,"-t","map.512.4.split"
					//,"-i","file:///Users/ihsumlee/Jolly/jpg/white.jpg"
					,"-i","file:///home/w514/jpg/map.jpg"
					,"-o","4"
					,"-rl","true"
					,"-rx","100"
					,"-ry","100"
					,"-p","10"
					};
			args = targs;
		}
		//TODO finish the main function,1:add robot controller
		final IMCLROE imclroe = new IMCLROE(false, 18, "file:///home/w514/jpg/test6.jpg", 0.001f, 100, 0.01f, 0.3f, 10);
		JCommander jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(imclroe);
		jc.parse(args);
		
		imclroe.setup();
		if(!imclroe.onCloud)	imclroe.Pre_caching();
		
		RobotState robot = new RobotState(19,19, 0, imclroe.grid, null, null); 
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(robot);
		jc.parse(args);
		robot.setInitModel(robot.getUt());
		robot.setInitPose(robot.getPose());
		Thread t = new Thread(robot);
		t.start();
		
		RobotController robotController = new RobotController("robot controller", robot, imclroe);
		Window window = new Window("mcl image", imclroe,robot);
		
		for(int i = 0; i < 10; i ++){
			window.setTitle("mcl image:"+String.valueOf(i));
			imclroe.run(robot, window);
			robot.lock();
			robot.initRobot();
			robot.unlock();
		}
		
		imclroe.close();

	}
	*/
	@Override
	public void batchWeight(List<Particle> src, float[] robotMeasurements)
			throws Throwable {
		
		Batch.Call<OewcService,OewcResponse> b = new OewcCall( src, robotMeasurements, this.orientation);
		Map<byte[],OewcResponse> results = this.table.coprocessorService(OewcService.class, null, null, b);
		//setup weight and orientatin to the particles(src)
		Long sum = 0l;
		List<Particle> result = new ArrayList<Particle>();
		for(Entry<byte[], OewcResponse> entry:results.entrySet()){
			sum = sum + entry.getValue().getCount();
//			System.out.println(Bytes.toString(entry.getKey())+":"+entry.getValue().getStr());
			for(OewcProtos.Particle op : entry.getValue().getParticlesList()){
				result.add(IMCLROE.ParticleFromO(op, this.orientation));
			}
		}
		
		//change the result to src 
		src.clear();
		src.addAll(result);
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
