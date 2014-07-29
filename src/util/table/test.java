package util.table;

import imclroe.IMCLROE;
import imclroe.IMCLROE.OewcCall;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;
import org.apache.hadoop.hbase.util.Bytes;

import util.metrics.Particle;
import util.metrics.Transformer;

import com.beust.jcommander.JCommander;
import com.google.protobuf.RpcController;

import coprocessor.services.OewcEndpoint;
import coprocessor.services.generated.OewcProtos;
import coprocessor.services.generated.OewcProtos.OewcRequest;
import coprocessor.services.generated.OewcProtos.OewcResponse;
import coprocessor.services.generated.OewcProtos.OewcService;

public class test extends Base{
	static public void main(String[] args){
		//for debug mode
		if(args.length==0){
			String[] targs = {
					"-t", "map.512.4.split"
					
					};
			args = targs;
		}
		
		JCommander jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.parse(args);
		test preWarm = new test();
		jc.addObject(preWarm);
		
		preWarm.run();
	}

	@Override
	public void run() {
		try {
			this.setup();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/**
		 * the implement body
		 */
		/***********************/
		//here
		/*String rowkey = Transformer.xy2RowkeyString(100, 100);
		System.out.println("rowkey:" + rowkey);
		String hash = Transformer.rowkeyString2Hash(rowkey);
		System.out.println("hash:" + hash);
		try {
		
			byte[][] bts = this.table.getEndKeys();
			for(byte[] bs: bts){
				System.out.println("bytes:"+bs.toString() + "length:"+bs.length);
				System.out.println(Bytes.compareTo(Bytes.toBytes(hash), bs));
				System.out.println("string:"+Bytes.toString(bs));
			}
			System.out.println("--------------------");
			byte[][] sbts = this.table.getStartKeys();
			for(byte[] bs: sbts){
				System.out.println("bytes:"+bs.toString() + "length:"+bs.length);
				System.out.println(Bytes.compareTo(Bytes.toBytes(hash), bs));
				System.out.println("string:"+Bytes.toString(bs));
			}
		} catch (IOException e1) {
		
			e1.printStackTrace();
		}*/
		List<Particle> src = new ArrayList<Particle>();
		src.add(new Particle(100,100,4));
		src.add(new Particle(99,100,0));
		float[] measurements = {
				0.8922767f
				,0.76107526f
				,0.9433762f
		};
		
		try {
			test.batchWeight(src, measurements, this.table);
		} catch (Throwable e1) {
			System.exit(-1);
			e1.printStackTrace();
		}
		
		for(Particle p: src){
			System.out.println(p.toString());
		}
		
		
		/***********************/
		try {
			this.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void batchWeight(List<Particle> src, float[] robotMeasurements, HTable table)
			throws Throwable {
		
		Batch.Call<OewcService,OewcResponse> b = new OewcCall(src,robotMeasurements);
		Map<byte[],OewcResponse> results = table.coprocessorService(OewcService.class, null, null, b);
		//setup weight and orientatin to the particles(src)
		Long sum = 0l;
		List<Particle> result = new ArrayList<Particle>();
		for(Entry<byte[], OewcResponse> entry:results.entrySet()){
			sum = sum + entry.getValue().getCount();
			System.out.println(Bytes.toString(entry.getKey())+":"+entry.getValue().getStr());
			for(OewcProtos.Particle op : entry.getValue().getParticlesList()){
				result.add(IMCLROE.ParticleFromO(op));
			}
		}
		
		//change the result to src 
		src.clear();
		src.addAll(result);
	}
	
	static public class OewcCall implements Batch.Call<OewcService, OewcResponse>{
		List<Particle> particles = null;
		float[] robotMeasurements = null;
		public OewcCall(List<Particle> particles, float[] robotMeasurements){
			this.particles = particles;
			this.robotMeasurements = robotMeasurements;
		}
		
		@Override
		public OewcResponse call(OewcService endpoint) throws IOException {
			// TODO IMCLROE
			BlockingRpcCallback<OewcResponse> done = new BlockingRpcCallback<OewcResponse>();
			RpcController controller = new ServerRpcController();
			OewcRequest request = IMCLROE.setupRequest(particles, robotMeasurements);
			//System.out.println(request.toString());
			endpoint.getRowCount(controller, request, done);
			return done.get();
		}

	
	}
	
}
