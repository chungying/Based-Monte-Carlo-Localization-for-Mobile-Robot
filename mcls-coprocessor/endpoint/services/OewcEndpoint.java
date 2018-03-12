package endpoint.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.coprocessor.CoprocessorException;
import org.apache.hadoop.hbase.coprocessor.CoprocessorService;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.HRegion;
import org.apache.hadoop.hbase.util.Bytes;

import util.Transformer;
import util.oewc.Oewc;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;

import endpoint.services.generated.*;
import endpoint.services.generated.OewcProtos.OewcRequest;
import endpoint.services.generated.OewcProtos.OewcResponse;
import endpoint.services.generated.OewcProtos.Particle;
import endpoint.services.generated.OewcProtos.OewcResponse.Builder;

public class OewcEndpoint extends OewcProtos.OewcService
implements Coprocessor, CoprocessorService{
	private RegionCoprocessorEnvironment env;
//	private HRegion region;
	//private List<OewcProtos.Particle> existParticles;

	@Override
	public Service getService() {
		return this;
	}

	@Override
	public void start(CoprocessorEnvironment env) throws IOException {
		if(env instanceof RegionCoprocessorEnvironment){
//			this.region = ((RegionCoprocessorEnvironment)env).getRegion();
			this.env = (RegionCoprocessorEnvironment)env;
		} else {
			throw new CoprocessorException("Must be loaded on a table region!");
		}		
	}

	@Override
	public void stop(CoprocessorEnvironment arg0) throws IOException {
		//there is nothing to do
		
	}

	@Override
	public void getRowCount(RpcController controller, OewcRequest request,
			RpcCallback<OewcResponse> done) {
		//initial and get the data from client
		String message = "start, ";
		OewcResponse.Builder responseBuilder = OewcResponse.newBuilder();
		long start = System.currentTimeMillis();
		long initial=0, step1=0, step2=0, step3=0, end=0;
		List<OewcProtos.Particle> requsetParts = request.getParticlesList();
		List<OewcProtos.Particle> existParticles = new ArrayList<OewcProtos.Particle>();
		try{ 
			List<Float> Zt = request.getMeasurementsList();
			if(Zt.isEmpty()){
				throw new Exception("there is no Zt in request");
			}
			initial = System.currentTimeMillis();
			// get Measurements
			//Step 1: Filter the Particles
			
			if(exsitRow(requsetParts, existParticles,
					this.env.getRegion().getStartKey(), this.env.getRegion().getEndKey())){
				step1 = System.currentTimeMillis();
				//Step 2: Orientation Estimation and setup response
				message = message+orientationEstimate( this.env.getRegion(), responseBuilder, existParticles, Zt);
				step2 = System.currentTimeMillis();
				//Step 3: Send back the response
				responseBuilder.setWeight(1.0f);
				message = message+"Particle number: "+String.valueOf(existParticles.size());
				step3 = System.currentTimeMillis();
			}else{
				step1 = System.currentTimeMillis();
				responseBuilder.setWeight(-1.0f).build();
				message = "no OEWC"+requsetParts.size();
			}
			end = System.currentTimeMillis();
	    
		}catch(Exception e){
			responseBuilder.setWeight(-1.0f).build();
			message = "failed:"+e.toString();
			
			e.printStackTrace();
		}finally{
			responseBuilder.setStr(
//					"start k:"+Bytes.toString(scan.getStartRow())+"\n"+
//					"stop k :"+Bytes.toString(scan.getStopRow())+"\n"+
//					"cells  :"+cells.size()+"\n"+
//					"rows   :"+rowMap.size()+"\n"+
//					"rowkey1:"+Bytes.toString(rowMap.keySet().iterator().next())+"\n"+
					"start  :"+String.valueOf(start)+"\n"+
					"initial:"+String.valueOf(initial)+"\n"+
					"step1  :"+String.valueOf(step1)+"\n"+
					"step2  :"+String.valueOf(step2)+"\n"+
					"step3  :"+String.valueOf(step3)+"\n"+
					"end    :"+String.valueOf(end)+"\n"+
					message);
			responseBuilder.setCount((end-start));
			done.run(responseBuilder.build());
		}
	}
	
	static public boolean exsitRow(List<Particle> src, List<Particle> dst, byte[] startKey, byte[] endKey) {
		dst.clear();
		
//		byte[] startKey = this.env.getRegion().getStartKey();
//		byte[] endKey = this.env.getRegion().getEndKey();
		if(startKey.length!=0 && endKey.length!=0){
			for(Particle p : src){
				String s = p.toRowKeyString();
				byte[] tmp = Bytes.toBytes(s);
				if(	Bytes.compareTo(tmp, startKey)>=0 &&
					Bytes.compareTo(tmp, endKey)<0){
					dst.add(p);
				}
			}
		}else if(startKey.length==0 && endKey.length!=0){
			for(Particle p : src){
				String s = p.toRowKeyString();
				byte[] tmp = Bytes.toBytes(s);
				if(Bytes.compareTo(tmp, endKey)<0){
					dst.add(p);
				}
			}
		}else if(startKey.length!=0 && endKey.length==0){
			for(Particle p : src){
				String s = p.toRowKeyString();
				byte[] tmp = Bytes.toBytes(s);
				if(Bytes.compareTo(tmp, startKey)>=0){
					dst.add(p);
				}
			}
		}else{
			for(Particle p : src){
				dst.add(p);
			}
		}
		
		if(dst.size()>0){
			return true;
		}else {
			return false;
		}
	}

	static byte[] family = Bytes.toBytes("distance");

	static public String orientationEstimate(
				HRegion region,
				Builder responseBuilder, 
				List<Particle> existParticles, 
				List<Float> zt) throws IOException {
			String message = "OEWC223 start, ";
			try{
			/**
			 * the bug of scanner need to be figureout
			 * */
				
				Result result = null;
				for(Particle p : existParticles){
					//get the round measurements to circleMeasure
	//				message = message +"1,";
					Get get = new Get(Bytes.toBytes(p.toRowKeyString()));
	//				message = message +"2,";
					get.addColumn(family, "data".getBytes());
	//				message = message +"3,";
					result = region.get(get);
//					result = this.env.getRegion().get(get);
	//				message = message +"4,";
					//List<Float> circleMeasurements = drawMeasurements(result);
					List<Float> circleMeasurements = new ArrayList<Float>();
					message = message + Transformer.result2Array(family, result, circleMeasurements);
	//				message = message +"5,";
					if(circleMeasurements.size()==0){
						throw new Exception("there is no circle measurements! ");
					}
					//OE of a particle
					Entry<Integer, Float> entry = Oewc.singleParticleModified(zt, circleMeasurements);
	//				message = message +"6,";
					//output the best weight
					OewcProtos.Particle.Builder outputP = OewcProtos.Particle.newBuilder().
					setX(p.getX()).setY(p.getY()).setZ(entry.getKey()).setW(entry.getValue());
	//				message = message +"7,";
					responseBuilder.addParticles(outputP);
				}
			}catch(Exception e){
				message = message+"OEWC failed, "+e.toString();
			}
			return message;
		}



}
