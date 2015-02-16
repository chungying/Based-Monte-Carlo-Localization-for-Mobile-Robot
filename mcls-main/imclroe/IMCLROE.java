package imclroe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;

import com.beust.jcommander.Parameter;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

import coprocessor.services.generated.OewcProtos;
import coprocessor.services.generated.OewcProtos.OewcRequest;
import coprocessor.services.generated.OewcProtos.OewcResponse;
import coprocessor.services.generated.OewcProtos.OewcService;
import samcl.SAMCL;
import util.metrics.Particle;
import util.metrics.Transformer;

public class IMCLROE extends SAMCL{
	
	//for test
	@SuppressWarnings({ })
	public static void main(String[] args){
		List<Integer> list1 = new ArrayList<Integer>();
		for(int i = 0 ; i <10; i++)
			list1.add(i);
		List<Integer> list2 = new ArrayList<Integer>();
		for(int i= list1.size()-1;i>=0;i--){
			list2.add(list1.get(i));
			list1.remove(i);
		}
		System.out.println("list1:"+list1);
		System.out.println("list2:"+list2);
		/*
		String i = HConstants.EMPTY_START_ROW.toString();
		String j = HConstants.EMPTY_END_ROW.toString();
		byte[] s = HConstants.EMPTY_START_ROW;
		byte[] t = HConstants.EMPTY_END_ROW;
		System.out.println("i length="+s.length);
		System.out.println("j length="+t.length);
		System.out.println(Bytes.compareTo(HConstants.EMPTY_START_ROW, HConstants.EMPTY_END_ROW)==0?i+"=="+j:i+"!="+j);
		System.out.println(Bytes.compareTo(HConstants.EMPTY_START_ROW, t)>0?i+">"+j:i+"<="+j);
		*/
		/*
		List<Integer> src = new ArrayList<Integer>();
		for(int i = 0 ; i < 90;i++){
			src.add(i);
		}
		
		Map<Integer, List<Integer>> classfy = new TreeMap<Integer, List<Integer>>();
		for(int i = 0; i < 10 ; i++){
			classfy.put(i, new ArrayList<Integer>());
		}
		
		for(Integer i:src){
			List<Integer> list = classfy.get(i%10);
			list.add(i);
		}
		
		System.out.println(classfy.toString());*/
		
		/*String str = "1:2:3:4:5:";
		String[] strs = str.split(":");
		System.out.println(str);
		for(String s: strs){
			System.out.println(s);
		}
		System.out.println("null pointer");
		String strnull = null;
		try{
			strnull.split(":");
		}catch(Exception e){
			System.out.println(e.toString());
			System.out.println(e.getMessage());
		}*/
	}
	
	@Parameter(names = {"-E","--endpoint"}, description = "start up/stop debug mode, default is to start up", required = false, arity = 1)
	public boolean endpoint = true;
	
	@Override
	public void batchWeight(List<Particle> src, float[] robotMeasurements)
			throws Throwable {
		if(this.endpoint){
			Transformer.debugMode(mode, "choose the endpoint.");
			this.oewcEndpoint(src, robotMeasurements);
		}
		else{
			Transformer.debugMode(mode, "choose the endpoint version 2.");
			this.oewcEndpoint2(src, robotMeasurements);
		}
	}
	
	@Deprecated
	private void oewcObserver(List<Particle> src, float[] robotMeasurements) throws IOException {
		//create List<Get> gets from List<Particle src
		Transformer.debugMode(this.mode, "get into the OewcObserver.\n");
		List<Get> gets = createGetList(src, robotMeasurements);
		//deal with the results
		Transformer.debugMode(this.mode, "a list of gets from table.\n");
		updateParticles(src, this.table.get(gets));
		Transformer.debugMode(this.mode, "OewcObserver end\n");
		
	}
	
	private void updateParticles(List<Particle> src, Result[] results) throws FileNotFoundException {
		try {
			if (src.size() == results.length) {
				Transformer.debugMode(mode, "start to parse the results.","size="+src.size(),"\n");
				for (int i = 0; i < src.size(); i++) {
					if(results[i].isEmpty()){
						Transformer.debugMode(mode, "OewcObserver Failed:no results.\n");
						throw new Exception("there is no result from OewcObserver. Rowkey:\n"+Bytes.toString(results[i].getRow()));
					}
					//parse the results[i]
					//rowkey type = HASH:XXXXXYYYYY:oewc:ORIENTATION:WEIGHTING
					String str = Bytes.toString(results[i].getRow());
					String[] strs = str.split(":");
					if (strs[2].contains("oewc")) {
						int best = Integer.parseInt(strs[3]);
						float weight = Float.parseFloat(strs[4]);
						//assign the best orientation and
						src.get(i).setWeight(weight);
						src.get(i).setTh(
								Transformer.Z2Th(best, this.orientation));
					}
					
					byte[] value = results[i].getValue("oewc".getBytes(), "oewc".getBytes());
					Transformer.debugMode(mode, "time:");
					for(int j = 0 ; j < value.length ; j+=8){
						Transformer.debugMode(mode, Bytes.toLong(Arrays.copyOfRange(value, j, j+8)));
					}
					Transformer.debugMode(mode, "\n");
				}
			}else{
				throw new Exception("size is not match.\n"
						+ "src.size()="+src.size()+",results.length="+results.length);
			}
				
		} catch (Exception e) {
			System.out.println(this.getClass().getName()+", update Particles");
			System.out.println(e.toString());
			System.out.println(e.getMessage());
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			System.out.println(sw.toString());
		}
	}

	private List<Get> createGetList(List<Particle> src,
			float[] robotMeasurements) {
		byte[] zt = Transformer.FA2BA(robotMeasurements);
		byte[] family = "distance".getBytes();
		byte[] qualifier = "data".getBytes();
		List<Get> gets = new ArrayList<Get>();
		Random random = new Random();
		for(Particle p : src){
			String str = Transformer.xy2RowkeyString(p.getX(), p.getY(), random) + ":oewc:";
			Get get = new Get(Bytes.add(str.getBytes(), zt));
			get.addColumn(family, qualifier);
			gets.add(get);
		}
		return gets;
	}

	private void oewcEndpoint(final List<Particle> src, final float[] robotMeasurements) 
			throws Throwable {
		List<Long> times = new ArrayList<Long>();
		times.add(System.currentTimeMillis());
		//Batch.Call<OewcService,OewcResponse> b = new OewcCall( src, robotMeasurements, orientation);
		Batch.Call<OewcService, OewcResponse> b = 
				new Batch.Call<OewcProtos.OewcService, OewcProtos.OewcResponse>() {
			@Override
			public OewcResponse call(OewcService endpoint) throws IOException {
				//initialize the tow objects.(not important)
				BlockingRpcCallback<OewcResponse> done = new BlockingRpcCallback<OewcResponse>();
				RpcController controller = new ServerRpcController();
				//this part is our point to implement packaging the data(request) and calling the customized method(getRowCount).
				OewcRequest request = IMCLROE.setupRequest(src, robotMeasurements, orientation);
				endpoint.getRowCount(controller, request, done);
				//get the results.
				return done.get();
			}	
		};
		times.add(System.currentTimeMillis());
		/*
		 * first:create Map<byte[], OewcResponse> results to store the results.
		 * second: execute the coprocessor with the arguments which are 
		 * RPC type(OewcService.class)
		 * start row key("0000".getBytes()) 
		 * stop row key("1000".getBytes())
		 * batch call object(b)
		 * */
		Map<byte[], OewcResponse> results=null;
		try {
			results = table.coprocessorService(OewcService.class, "0000".getBytes(), "1000".getBytes(), b);
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//setup weight and orientatin to the particles(src)
		times.add(System.currentTimeMillis());
		List<Long> durations = new ArrayList<Long>();
		List<Particle> result = new ArrayList<Particle>();
		for(Entry<byte[], OewcResponse> entry:results.entrySet()){
			durations.add(entry.getValue().getCount());
			Transformer.debugMode(mode, "getCount:"+entry.getValue().getCount());
			Transformer.debugMode(mode, entry.getValue().getStr());
			for(OewcProtos.Particle op : entry.getValue().getParticlesList()){
				result.add(IMCLROE.ParticleFromO(op, orientation));
			}
		}
		times.add(System.currentTimeMillis());
		//change the result to src
		src.clear();
		src.addAll(result);
		times.add(System.currentTimeMillis());
		int counter = 0;
		System.out.print("longest weighting\t"+Collections.max(durations)+"\t");
		Transformer.debugMode(mode, "-----------------------------");
		for(Long time: times){
			counter++;
			Transformer.debugMode(mode, "\t"+counter + "\t:"+ time);
		}
	}
	
	
	
	private void oewcEndpoint2(List<Particle> src, float[] robotMeasurements) 
			throws Throwable{
		//1.Filter
		Random random = new Random();
		for(OewcCallable thread:this.threads){
			List<Particle> ps = new ArrayList<Particle>();
			for(int i = src.size()-1 ; i>=0;i--){
				//TODO the conditions
				if(thread.isContains(Transformer.getHash(src.get(i), random))){
					ps.add(src.get(i));
					src.remove(i);
				}
			}
			thread.update(ps, robotMeasurements);
		}
		//2.Start the threads
		//TODO if step 1 is succeed, 1 will combine with 2.
		//ExecutorService threadPool = Executors.newFixedThreadPool(this.threads.size());
//		CompletionService<List<Particle>> pool = new ExecutorCompletionService<List<Particle>>(this.pool);
		List<Future<List<Particle>>> futures = new ArrayList<Future<List<Particle>>>(this.threads.size());
		for(int i = 0; i < this.threads.size();i++){
			futures.add(pool.submit(this.threads.get(i)));
		}
		//3.draw particles
		for(int i = 0;i<futures.size();i++){
//			src.addAll(pool.take().get());
			src.addAll(futures.get(i).get());
		}
		
	}
	
	List<OewcCallable> threads = null;
	List<HTable> tables = null;
	ExecutorService pool = null;
	List<Pair<byte[],byte[]>> regionKeys= new ArrayList<Pair<byte[],byte[]>>();
	@SuppressWarnings("deprecation")
	@Override
	public void customizedSetup(Configuration conf) throws Exception {
		if (!this.endpoint) {
			List<HRegionLocation> regions = this.table.getRegionsInRange(
					"0000".getBytes(), "1000".getBytes());
			this.threads = new ArrayList<OewcCallable>();
			this.tables = new ArrayList<HTable>();
			for (HRegionLocation r : regions) {
				if (Bytes.equals(r.getRegionInfo().getStartKey(),
						HConstants.EMPTY_START_ROW)
						&& Bytes.equals(r.getRegionInfo().getEndKey(),
								HConstants.EMPTY_END_ROW)) {
					System.out.println("the table has not yet been split." + r);
					throw new Exception("table didn't be split.");
				}
				regionKeys.add(new Pair<byte[], byte[]>(r.getRegionInfo()
						.getStartKey(), r.getRegionInfo().getEndKey()));
				this.tables.add(this.grid.getTable(tableName));
			}
			for (int i = 0; i < tables.size(); i++) {
				threads.add(new OewcCallable(this.tables.get(i),
						this.regionKeys.get(i)));
			}
			this.pool = HTable.getDefaultExecutor(conf);
		}
	}

	@Override
	protected void customizedClose() {
		this.pool.shutdownNow();
	}

	public static void oewcThread(
			boolean mode, 
			final int orientation ,
			final List<Particle> src, 
			final float[] robotMeasurements, 
			HTable table, 
			Pair<byte[],byte[]> keys){
		List<Long> times = new ArrayList<Long>();
		times.add(System.currentTimeMillis());
		//Batch.Call<OewcService,OewcResponse> b = new OewcCall( src, robotMeasurements, orientation);
		Batch.Call<OewcService, OewcResponse> b = 
				new Batch.Call<OewcProtos.OewcService, OewcProtos.OewcResponse>() {
			@Override
			public OewcResponse call(OewcService endpoint) throws IOException {
				BlockingRpcCallback<OewcResponse> done = new BlockingRpcCallback<OewcResponse>();
				RpcController controller = new ServerRpcController();
				OewcRequest request = IMCLROE.setupRequest(src, robotMeasurements, orientation);
				endpoint.getRowCount(controller, request, done);
				return done.get();
			}	
		};
		times.add(System.currentTimeMillis());
		Map<byte[], OewcResponse> results=null;
		try {
			results = table.coprocessorService(OewcService.class, keys.getFirst(), keys.getSecond(), b);
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//setup weight and orientatin to the particles(src)
		times.add(System.currentTimeMillis());
		Long sum = 0l;
		List<Particle> result = new ArrayList<Particle>();
		for(Entry<byte[], OewcResponse> entry:results.entrySet()){
			sum = sum + entry.getValue().getCount();
			Transformer.debugMode(mode, entry.getValue().getStr());
			for(OewcProtos.Particle op : entry.getValue().getParticlesList()){
				result.add(IMCLROE.ParticleFromO(op, orientation));
			}
		}
		times.add(System.currentTimeMillis());
		//change the result to src
		src.clear();
		src.addAll(result);
		times.add(System.currentTimeMillis());
		int counter = 0;
		Transformer.debugMode(mode, "-----------------------------");
		for(Long time: times){
			counter++;
			Transformer.debugMode(mode, "\t"+counter + "\t:"+ time);
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
	
	private class OewcCallable implements Callable<List<Particle>>{
		public HTable table = null;
		public Pair<byte[], byte[]> range = null;
		OewcCallable(HTable table, Pair<byte[],byte[]> range){
			this.table = table;
			this.range = range;
		}
		
		public boolean isContains(String hash) {
			if(Bytes.compareTo(hash.getBytes(),range.getFirst())<0)
				return false;
			if(Bytes.equals(range.getSecond(), HConstants.EMPTY_END_ROW))
				return true;
			if(Bytes.compareTo(hash.getBytes(),range.getSecond())<0)
				return true;
			return false;
		}

		private List<Particle> particles = null;
		private float[] robotMeasurements = null;
		public void update(List<Particle> particles, float[] robotMeasurements){
			this.particles = particles;
			this.robotMeasurements = robotMeasurements;
		}
		
		@Override
		public List<Particle> call() {
			oewcThread(
					IMCLROE.this.mode, 
					IMCLROE.this.orientation, 
					this.particles, 
					this.robotMeasurements,
					this.table,
					this.range);
			return this.particles;
		}	
	}
		
	static public class OewcCall implements Batch.Call<OewcService, OewcResponse>{
		
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
