package imclroe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;
import org.apache.hadoop.hbase.util.Pair;

import com.beust.jcommander.Parameter;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

import endpoint.services.generated.OewcProtos;
import endpoint.services.generated.OewcProtos2;
import endpoint.services.generated.OewcProtos.OewcRequest;
import endpoint.services.generated.RpcProxyProtos;
import samcl.SAMCL;
import util.Transformer;
import util.grid.Grid;
import util.pf.Particle;
import util.pf.sensor.laser.LaserModel.LaserModelData;
import util.robot.RobotState;

/**
 * @author wuser
 *
 */
public class IMCLROE extends SAMCL{
	
//	public IMCLROE() throws IOException {
//		super();
//	}
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
	
	@Parameter(names = {"-E","--endpoint"}, 
			description = "choose the endpoint type, oewc, oewc2, and proxy modes.", 
			arity = 1, required = false)
	public int endpoint = 2;
	
	@Override
	public List<Long> weightAssignment(
			List<Particle> src,
			RobotState robot,
			LaserModelData laserData,
			Grid grid
			)throws Exception {
		
		if (!grid.onCloud) {
			throw new Exception("Grid is not on cloud.");
		}
		// TODO 
//		robotMeasurements = robot.getMeasurements();
		//long[] timers = new long[3];
		List<Long> ts = null;
		//remove duplicated particle in X-Y domain
				Transformer.filterParticle(src);
				//choose endpoint
				if(this.endpoint==1){
					Transformer.debugMode(debugMode, "choose the proxy endpoint version.\n");
					//timers = this.proxyEndpoint(src, robotMeasurements);
					ts = this.proxyEndpoint(src, /*robotMeasurements*/laserData);
				}
				else if(this.endpoint==2){
					Transformer.debugMode(debugMode, "choose the oewc2 endpoint version.\n");
					//timers = this.oewc2Endpoint(src, robotMeasurements, 1000);
					ts = this.oewc2Endpoint(src, /*robotMeasurements*/laserData, 1000);
				}
				else{
					Transformer.debugMode(true, "there is inappropriate endpoint\n");
				}
		//return timers;
		return ts;		
		
	}

//	@Override
//	public long updateParticle( List<Particle> src) throws Exception {
//		// TODO Auto-generated method stub
//		return -1;
//	}

/*	@Override
	public long batchWeight(RobotState robot, List<Particle> src, float[] robotMeasurements)
			throws Exception {
		//remove duplicated particle in X-Y domain
		Transformer.filterParticle(src);
		//choose endpoint
		if(this.endpoint==1){
			Transformer.debugMode(mode, "choose the proxy endpoint version.\n");
			return this.proxyEndpoint(src, robotMeasurements);
		}
		else if(this.endpoint==2){
			Transformer.debugMode(mode, "choose the oewc2 endpoint version.\n");
			return this.oewc2Endpoint(src, robotMeasurements, 1000);
		}
		else{
			Transformer.debugMode(true, "there is inappropriate endpoint\n");
			return -1;
		}
	}*/

	private List<Long> proxyEndpoint(final List<Particle> src, final LaserModelData laserData) {
		// TODO proxy endpoint
		
		final List<Pair<Map<Long, String>, OewcProtos2.OewcResponse>> results=Collections.synchronizedList(
				new ArrayList<Pair<Map<Long, String>, OewcProtos2.OewcResponse>>());
		
		Batch.Call<RpcProxyProtos.RpcProxyService, Pair<Map<Long, String>,OewcProtos2.OewcResponse>> call = 
				new Batch.Call<RpcProxyProtos.RpcProxyService, Pair<Map<Long, String>,OewcProtos2.OewcResponse>>() {
			
			@Override
			public Pair<Map<Long, String>,OewcProtos2.OewcResponse> call(RpcProxyProtos.RpcProxyService endpoint) throws IOException {
				Map<Long, String> records = Collections.synchronizedMap(new TreeMap<Long, String>());
				records.put(System.currentTimeMillis(), "call");
				//initialize the tow objects.(not important)
				BlockingRpcCallback<OewcProtos2.OewcResponse> done = new BlockingRpcCallback<OewcProtos2.OewcResponse>();
				RpcController controller = new ServerRpcController();
				//this part is our point to implement packaging the data(request) and calling the customized method(getRowCount).
				
				//build src
				ArrayList<OewcProtos2.Particle> ps = new ArrayList<OewcProtos2.Particle>();
				for(Particle p : src){
					ps.add(
						OewcProtos2.Particle.newBuilder()
							.setX((float)p.getDX())
							.setY((float)p.getDY())
							.setZ(Transformer.th2Z(p.getTh(), sensor.getOrientation())).build()
					);
				}
//				List<Float> Zt = new ArrayList<Float>();
//				for(float f: robotMeasurements){
//					Zt.add(new Float(f));
//				}
				
				OewcProtos2.OewcRequest.Builder builder = OewcProtos2.OewcRequest.newBuilder();
				builder.addAllParticles(ps);
				builder.addAllMeasurements(laserData.data.beamranges);
				
				endpoint.getCalculationResult(controller, builder.build(), done);
				//get the results.
				records.put(System.currentTimeMillis(), "call end");
//				System.out.println("callable");
				return new Pair<Map<Long, String>,OewcProtos2.OewcResponse>(records, done.get());
			}
		};
		
		Batch.Callback<Pair<Map<Long, String>,OewcProtos2.OewcResponse>> callback = 
				new Batch.Callback<Pair<Map<Long, String>,OewcProtos2.OewcResponse>>() {
			 public void update(byte[] region, byte[] row, Pair<Map<Long, String>,OewcProtos2.OewcResponse> value) {
				 Transformer.debugMode(debugMode,"put result");
				 Transformer.debugMode(debugMode, "Thread call time: " + value.getFirst());
				 Transformer.debugMode(debugMode, value.getSecond().getStr());
				 results.add(new Pair<Map<Long, String>, OewcProtos2.OewcResponse>(value.getFirst(), value.getSecond()));
			 }
		};
		
		/**
		 * first:create Map<byte[], OewcResponse> results to store the results.
		 * second: execute the coprocessor with the arguments which are 
		 * RPC type(OewcService.class)
		 * start row key("0000".getBytes()) 
		 * stop row key("1000".getBytes())
		 * batch call object(b)
		 * */
		long durationAll = System.currentTimeMillis();
		try {
			int rand = (new Random()).nextInt(1000);
			table.coprocessorService(
					RpcProxyProtos.RpcProxyService.class, 
					String.format("%04d", rand).getBytes(), String.format("%04d",rand).getBytes(), 
					call,
					callback
					);
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		durationAll = System.currentTimeMillis() - durationAll;
		
		//setup weight and orientation to the particles(src)
		List<Integer> durationsOEWC = new ArrayList<Integer>();
		List<Integer> durationsReadingHDFS = new ArrayList<Integer>();
		List<Particle> result = new ArrayList<Particle>();
		for(Pair<Map<Long, String>, OewcProtos2.OewcResponse> entry:results){
			//show key
			Transformer.debugMode(debugMode,entry.getFirst());
			//show value
			Transformer.debugMode(debugMode,"W:",entry.getSecond().getCount());
			durationsOEWC.add(entry.getSecond().getCount());
			Transformer.debugMode(debugMode,"R:",(int)entry.getSecond().getWeight());
			durationsReadingHDFS.add((int)entry.getSecond().getWeight());
			for(OewcProtos2.Particle op : entry.getSecond().getParticlesList()){
				result.add(
						new Particle(
								op.getX(), 
								op.getY(), 
								// TODO change Z of OewcProtos2.OewcResponse into Integer. 
								Transformer.Z2Th((int)op.getZ(), this.sensor.getOrientation()), 
//								op.getW(),
								op.getW())//TODO getting double type
						);
			}
		}

		//change the result to src
		List<Long> ts = new ArrayList<Long>();
		long t1 = Collections.max(durationsOEWC);
		long t2 = Collections.max(durationsReadingHDFS);
		ts.add(durationAll-t1);
		ts.add(t2-t1);
		ts.add(t2);
		/*long[] timers = new long[3];
		timers[2] = Collections.max(durationsReadingHDFS);
		timers[1] = Collections.max(durationsOEWC);
		timers[0] = durationAll - timers[1];
		timers[1] -= timers[2];*/

//		System.out.print("\t");
		
		src.clear();
		src.addAll(result);
		
		//return timers;
		return ts;
	}

	private List<Long> oewc2Endpoint(final List<Particle> src, final LaserModelData laserData, int endkey) {
		// TODO oewc version 2 endpoint
		
		Batch.Call<OewcProtos2.Oewc2Service, Pair<Map<String, Long>,OewcProtos2.OewcResponse>> call = 
				new Batch.Call<OewcProtos2.Oewc2Service, Pair<Map<String, Long>,OewcProtos2.OewcResponse>>() {
			
			@Override
			public Pair<Map<String, Long>,OewcProtos2.OewcResponse> call(OewcProtos2.Oewc2Service endpoint) throws IOException {
				
				Map<String, Long> records = Collections.synchronizedMap(new TreeMap<String, Long>());
				
				records.put("call", System.currentTimeMillis());
				//initialize the tow objects.(not important)
				BlockingRpcCallback<OewcProtos2.OewcResponse> done = new BlockingRpcCallback<OewcProtos2.OewcResponse>();
				RpcController controller = new ServerRpcController();
				//this part is our point to implement packaging the data(request) and calling the customized method(getRowCount).
				
				//build src
				ArrayList<OewcProtos2.Particle> ps = new ArrayList<OewcProtos2.Particle>();
				for(Particle p : src){
					ps.add(
						OewcProtos2.Particle.newBuilder()
							.setX((float)p.getDX())
							.setY((float)p.getDY())
							.setZ(Transformer.th2Z(p.getTh(), sensor.getOrientation())).build()
					);
				}
//				List<Float> Zt = new ArrayList<Float>();
//				for(float f: data){
//					Zt.add(new Float(f));
//				}
				
				OewcProtos2.OewcRequest.Builder builder = OewcProtos2.OewcRequest.newBuilder();
				builder.addAllParticles(ps);
				builder.addAllMeasurements(laserData.data.beamranges);

				//get the results.
				endpoint.getOewc2Result(controller, builder.build(), done);
								
				records.put("call end", System.currentTimeMillis());

				return new Pair<Map<String, Long>,OewcProtos2.OewcResponse>(records, done.get());
			}	
		};

		final List<Pair<Map<String, Long>, OewcProtos2.OewcResponse>> results=Collections.synchronizedList(
				new ArrayList<Pair<Map<String, Long>, OewcProtos2.OewcResponse>>());
		
		
		Batch.Callback<Pair<Map<String, Long>,OewcProtos2.OewcResponse>> callback = 
				new Batch.Callback<Pair<Map<String, Long>,OewcProtos2.OewcResponse>>() {
			 public void update(byte[] region, byte[] row, Pair<Map<String, Long>,OewcProtos2.OewcResponse> value) {
				 Transformer.debugMode(debugMode,"put result");
				 Transformer.debugMode(debugMode, "Thread call time: " + value.getFirst());
				 Transformer.debugMode(debugMode, value.getSecond().getStr());
				 results.add(new Pair<Map<String, Long>, OewcProtos2.OewcResponse>(value.getFirst(), value.getSecond()));
			 }
		};
		
		/**
		 * first:create Map<byte[], OewcResponse> results to store the results.
		 * second: execute the coprocessor with the arguments which are 
		 * RPC type(OewcService.class)
		 * start row key("0000".getBytes()) 
		 * stop row key("1000".getBytes())
		 * batch call object(b)
		 * */
		long durationAll= System.currentTimeMillis();
		try {
			table.coprocessorService(
					OewcProtos2.Oewc2Service.class, 
					"0000".getBytes(), "1000".getBytes(), 
					call,
					callback
					);
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		durationAll = System.currentTimeMillis() - durationAll;
		
		//setup weight and orientation to the particles(src)
		List<Integer> durationsSending = new ArrayList<Integer>();
		List<Integer> durationsOEWC = new ArrayList<Integer>();
		List<Integer> durationsReadingHDFS = new ArrayList<Integer>();
		List<Particle> result = new ArrayList<Particle>();
		List<Integer> particlesNo = new ArrayList<Integer>();
		Transformer.debugMode(debugMode,"result no.:"+ results.size());
		if(results.size()<2)
			try {
				System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		for(Pair<Map<String, Long>,OewcProtos2.OewcResponse> entry:results){
			//show key
			durationsSending.add((int)(entry.getFirst().get("call end") - entry.getFirst().get("call")));
			//show value
			durationsOEWC.add(entry.getSecond().getCount());//OEWC time and reading HDFS time
			durationsReadingHDFS.add((int)entry.getSecond().getWeight());//reading HDFS time
			particlesNo.add(entry.getSecond().getParticlesCount());
			for(OewcProtos2.Particle op : entry.getSecond().getParticlesList()){
				result.add(
						new Particle(
								op.getX(), 
								op.getY(), 
								// TODO change Z of OewcProtos2.OewcResponse into Integer. 
								Transformer.Z2Th((int)op.getZ(), sensor.getOrientation()), 
//								op.getW(),
								op.getW())// TODO getting double type
						);
			}
		}
		
		List<Long> ts = new ArrayList<Long>(3);
		long t1 = Collections.max(durationsOEWC);
		long t2 = Collections.max(durationsReadingHDFS);
		ts.add(durationAll-t1);
		ts.add(t2-t1);
		ts.add(t2);
		/*long[] timers = new long[3];
		timers[2] = Collections.max(durationsReadingHDFS);
		timers[1] = Collections.max(durationsOEWC);
		timers[0] = durationAll - timers[1];
		timers[1] -= timers[2];*/

//		for(Integer i : particlesNo){
//			System.out.print(i+",");
//			
//		}
//		System.out.print("\t");
		
		//change the result to src
		src.clear();
		src.addAll(result);
		//return timers;
		return ts;
	}

	/*@Deprecated
	private long oewcEndpoint(final List<Particle> src, final float[] robotMeasurements){
		// TODO oewc endpoint version 1
		final Map<Long, String> records = Collections.synchronizedMap(new TreeMap<Long, String>());
		final Map<byte[], OewcResponse> results=Collections.synchronizedMap(
				new TreeMap<byte[], 
				OewcResponse>(Bytes.BYTES_COMPARATOR));
		
		Batch.Call<OewcService, OewcResponse> call = 
				new Batch.Call<OewcProtos.OewcService, OewcProtos.OewcResponse>() {
			@Override
			public OewcResponse call(OewcService endpoint) throws IOException {
				records.put(System.currentTimeMillis(), "call");
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
		
		Batch.Callback<OewcResponse> callback = 
				new Batch.Callback<OewcResponse>() {
			 public void update(byte[] region, byte[] row, OewcResponse value) {
				 records.put(System.currentTimeMillis(), "callback");
				 results.put(region, value);
			 }
		};		

		
		 * first:create Map<byte[], OewcResponse> results to store the results.
		 * second: execute the coprocessor with the arguments which are 
		 * RPC type(OewcService.class)
		 * start row key("0000".getBytes()) 
		 * stop row key("1000".getBytes())
		 * batch call object(b)
		 * 

		try {
			table.coprocessorService(
					OewcService.class, 
					"0000".getBytes(), "1000".getBytes(), 
					call,
					callback);
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//setup weight and orientatin to the particles(src)
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

		//change the result to src
		src.clear();
		src.addAll(result);

		Transformer.debugMode(true, records);
		
		return Collections.max(durations);
	}*/
	
	
	public static Particle ParticleFromO(
			endpoint.services.generated.OewcProtos.Particle op, int orientation) {
		Particle p = new Particle(op.getX(), op.getY(), Transformer.Z2Th(op.getZ(), orientation));
//		p.setWeight(op.getW());
		p.setWeightForNomalization(op.getW());
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


	
	
	/**
	 * 
	 */
	/*
	ExecutorService pool = null;
	@Override
	protected void customizedClose() {
		this.pool.shutdownNow();
	}
	
	List<Pair<byte[],byte[]>> regionKeys= new ArrayList<Pair<byte[],byte[]>>();
	List<HTable> tables = null;
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
	*/
	
	/**
	 * 
	 * @author wuser
	 *
	 */
	/*
	@Deprecated
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
	*/
	
	
	
	/**
	 * 
	 * @param src
	 * @param robotMeasurements
	 * @throws Throwable
	 */
	/*
	@Deprecated
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
	*/
	
	/*
	List<OewcCallable> threads = null;
	*/
	
	/**
	 * 
	 * @param mode
	 * @param orientation
	 * @param src
	 * @param robotMeasurements
	 * @param table
	 * @param keys
	 */
	/*
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
	*/
	
}
