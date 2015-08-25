package endpoint.services;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.client.coprocessor.Batch.Call;
import org.apache.hadoop.hbase.coprocessor.CoprocessorException;
import org.apache.hadoop.hbase.coprocessor.CoprocessorService;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Threads;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;
import com.google.protobuf.ServiceException;

import endpoint.services.generated.*;
import endpoint.services.generated.OewcProtos2.OewcRequest;
import endpoint.services.generated.OewcProtos2.OewcResponse;
import endpoint.services.generated.OewcProtos2.Oewc2Service;

public class RpcProxyEndpoint extends RpcProxyProtos.RpcProxyService
implements Coprocessor, CoprocessorService{

	@Override
	public Service getService() {
		return this;
	}

	private RegionCoprocessorEnvironment env;
	private HConnection connection;
	private HTable table;
	@Override
	public void start(CoprocessorEnvironment arg0) throws IOException {
		if(arg0 instanceof RegionCoprocessorEnvironment){
//			this.region = ((RegionCoprocessorEnvironment)env).getRegion();
			this.env = (RegionCoprocessorEnvironment)arg0;
		} else {
			throw new CoprocessorException("Must be loaded on a table region!");
		}	
		//avoid accessing hbase:meta and hbase:namespace
		byte[] thisTable = this.env.getRegion().getTableDesc().getTableName().getName();
		if(	thisTable=="hbase:meta".getBytes() || thisTable=="hbase:namespace".getBytes()){
			return ;
		}
		
		//create HTableConnection
		this.connection = HConnectionManager.createConnection(this.env.getConfiguration());
		
		ThreadPoolExecutor pool = new ThreadPoolExecutor(32,  Integer.MAX_VALUE, 60l, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), Threads.newDaemonThreadFactory("htable"));
		pool.allowCoreThreadTimeOut(true);
		
		this.table = (HTable) this.connection.getTable( this.env.getRegion().getTableDesc().getName(), pool);
//		this.table = new HTable(this.env.getConfiguration(), this.env.getRegion().getTableDesc().getName());
	}

	@Override
	public void stop(CoprocessorEnvironment arg0) throws IOException {
		// TODO Auto-generated method stub
		this.table.close();
		this.connection.close();
	}

	
	private class ProxyBatchCall implements Batch.Call<Oewc2Service, OewcResponse> {

		private OewcRequest request = null;
		ProxyBatchCall( OewcRequest request){
			this.request = request;
		}
		
		@Override
		public OewcResponse call(Oewc2Service endpoint) throws IOException {
			//initialize the tow objects.(not important)
			BlockingRpcCallback<OewcResponse> done = new BlockingRpcCallback<OewcResponse>();
			RpcController controller = new ServerRpcController();
			//this part is our point to implement packaging the data(request) and calling the customized method(getRowCount).
			//TODO passing through whole request directly.
			OewcRequest request = OewcRequest.newBuilder(this.request).build(); 
//			IMCLROE.setupRequest(src, robotMeasurements, orientation);
			endpoint.getOewc2Result(controller, request, done);
			//get the results.
			return done.get();
		}
		
	}
	
	@Override
	public void getCalculationResult(RpcController controller,
			OewcRequest request, RpcCallback<OewcResponse> done) {
		
		
		Call<Oewc2Service, OewcResponse> b = new ProxyBatchCall(request);
/*				new Batch.Call<OewcService, OewcResponse>() {
			@Override
			public OewcResponse call(OewcService endpoint) throws IOException {
				//initialize the tow objects.(not important)
				BlockingRpcCallback<OewcResponse> done = new BlockingRpcCallback<OewcResponse>();
				RpcController controller = new ServerRpcController();
				//this part is our point to implement packaging the data(request) and calling the customized method(getRowCount).
				//TODO passing through whole request directly.
				OewcRequest request = OewcRequest.newBuilder(request).build(); 
//				IMCLROE.setupRequest(src, robotMeasurements, orientation);
				endpoint.getRowCount(controller, request, done);
				//get the results.
				return done.get();
			}	
		};*/
		
		
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
			results = table.coprocessorService(Oewc2Service.class, "0000".getBytes(), "1000".getBytes(), b);
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		//package results into proxy response
		OewcResponse.Builder oewcResponseBuilder = OewcResponse.newBuilder();
		int maxCountOfResults = 0;
		String strOfResults = "";
		float maxWeightOfResults = 0;
		for(Entry<byte[], OewcResponse> entry:results.entrySet()){
			
			//parse entry's value
			//the Particles list can be appended directly.
			oewcResponseBuilder.addAllParticles(entry.getValue().getParticlesList());
			if(entry.getValue().getCount()>maxCountOfResults)
				maxCountOfResults = entry.getValue().getCount();
			if(entry.getValue().getWeight()>maxWeightOfResults)
				maxWeightOfResults = entry.getValue().getWeight();
			strOfResults = strOfResults	+ 
					"{" + Bytes.toString(entry.getKey())+", " + 
					entry.getValue().getStr() + "}\t";
		}
		
		strOfResults = strOfResults
				+"End of Proxy Endpoint\t";
		oewcResponseBuilder.setStr(strOfResults);
		oewcResponseBuilder.setCount(maxCountOfResults);//OEWC time and reading HDFS time
		oewcResponseBuilder.setWeight(maxWeightOfResults);//reading HDFS time
		
		//response is done!!!!!
		done.run(oewcResponseBuilder.build());		
	}

	
}
