package coprocessor.services;

import imclroe.IMCLROE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.client.coprocessor.Batch.Call;
import org.apache.hadoop.hbase.coprocessor.CoprocessorException;
import org.apache.hadoop.hbase.coprocessor.CoprocessorService;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;

import util.metrics.Particle;
import util.metrics.Transformer;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;
import com.google.protobuf.ServiceException;

import coprocessor.services.generated.*;
import coprocessor.services.generated.OewcProtos2.OewcService;
import coprocessor.services.generated.OewcProtos2.OewcRequest;
import coprocessor.services.generated.OewcProtos2.OewcResponse;
import coprocessor.services.generated.RpcProxyProtos.ProxyRequest;
import coprocessor.services.generated.RpcProxyProtos.ProxyResponse;

public class RpcProxyEndpoint extends RpcProxyProtos.RpcProxyService
implements Coprocessor, CoprocessorService{

	@Override
	public Service getService() {
		return this;
	}

	private RegionCoprocessorEnvironment env;
	private HTable table;
	@Override
	public void start(CoprocessorEnvironment arg0) throws IOException {
		if(env instanceof RegionCoprocessorEnvironment){
//			this.region = ((RegionCoprocessorEnvironment)env).getRegion();
			this.env = (RegionCoprocessorEnvironment)env;
		} else {
			throw new CoprocessorException("Must be loaded on a table region!");
		}	
		this.table = new HTable(this.env.getConfiguration(), this.env.getRegion().getTableDesc().getName());
	}

	@Override
	public void stop(CoprocessorEnvironment arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getCalculationResult(RpcController controller,
			OewcRequest request, RpcCallback<OewcResponse> done) {
		
		
		
		List<Long> times = new ArrayList<Long>();
		times.add(System.currentTimeMillis());
		//Batch.Call<OewcService,OewcResponse> b = new OewcCall( src, robotMeasurements, orientation);
		Call<OewcService, OewcResponse> b = 
				new Batch.Call<OewcService, OewcResponse>() {
			@Override
			public OewcResponse call(OewcService endpoint) throws IOException {
				//initialize the tow objects.(not important)
				BlockingRpcCallback<OewcResponse> done = new BlockingRpcCallback<OewcResponse>();
				RpcController controller = new ServerRpcController();
				//this part is our point to implement packaging the data(request) and calling the customized method(getRowCount).
				//TODO
				OewcRequest request = setupProxyRequest(); 
//				IMCLROE.setupRequest(src, robotMeasurements, orientation);
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
			results = table.coprocessorService(OewcService.class, "0000".getBytes(), "0000".getBytes(), b);
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
			durations.add((long) entry.getValue().getCount());
//			Transformer.debugMode(mode, "getCount:"+entry.getValue().getCount());
//			Transformer.debugMode(mode, entry.getValue().getStr());
//			for(OewcProtos.Particle op : entry.getValue().getParticlesList()){
//				result.add(IMCLROE.ParticleFromO(op, orientation));
//			}
			//TODO combine all of response into one!
			
		}
		times.add(System.currentTimeMillis());
		//change the result to src
		OewcResponse.Builder oewcResponseBuilder = OewcResponse.newBuilder();
		oewcResponseBuilder.setCount(value)
//		src.clear();
//		src.addAll(result);
		times.add(System.currentTimeMillis());
		System.out.print("longest weighting\t"+Collections.max(durations)+"\t");
//		Transformer.debugMode(mode, "-----------------------------");
		for(Long time: times){
//			Transformer.debugMode(mode, "\t"+counter + "\t:"+ time);
		}
		
	}

	
}
