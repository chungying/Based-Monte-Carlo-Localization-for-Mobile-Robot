package coprocessor.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.coprocessor.CoprocessorException;
import org.apache.hadoop.hbase.coprocessor.CoprocessorService;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;

import coprocessor.services.generated.*;
import coprocessor.services.generated.OewcProtos.OewcRequest;
import coprocessor.services.generated.OewcProtos.OewcResponse;
import coprocessor.services.generated.OewcProtos.Particle;
import coprocessor.services.generated.OewcProtos.OewcResponse.Builder;

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
		OewcResponse.Builder responseBuilder = null;
		try{ 
			//initial and get the data from client
			responseBuilder = OewcResponse.newBuilder();
			List<OewcProtos.Particle> requsetParts = request.getParticlesList();
			List<OewcProtos.Particle> existParticles = new ArrayList<OewcProtos.Particle>();
			List<Float> Zt = request.getMeasurementsList();
			if(Zt.isEmpty()){
				throw new Exception("there is no Zt in request");
			}
			// get Measurements
			//Step 1: Filter the Particles
			if(exsitRow(requsetParts, existParticles)){
				//Step 2: Orientation Estimation and setup response
				orientationEstimate(responseBuilder, existParticles, Zt);
				
				//Step 3: Send back the response
				responseBuilder.setCount(1).setWeight(1.0f);
				responseBuilder.setStr("Particle number: "+String.valueOf(existParticles.size()));
			}else{
				
				responseBuilder.setCount(-1).setWeight(-1.0f).build();
				responseBuilder.setStr("no OEWC"+requsetParts.toString());
			}
			done.run(responseBuilder.build());
	    
		}catch(Exception e){
			responseBuilder.setCount(-1).setWeight(-1.0f).build();
			responseBuilder.setStr("failed:"+e.toString());
			done.run(responseBuilder.build());
			e.printStackTrace();
		}
	}
	
	private float weightCalculate(List<Float> Mt, List<Float> Zt) {
		float w = 0.0f;
		for(int i = 0; i < Zt.size(); i++){
			w = w + Math.abs(Mt.get(i)- Zt.get(i));
		}
		w = w / Zt.size();
		return w;
	}

	private void orientationEstimate(Builder responseBuilder, List<Particle> existParticles, List<Float> zt) throws IOException {
		Result result = null;
		byte[] family = Bytes.toBytes("distance");
		for(Particle p : existParticles){
			//get the round measurements to circleMeasure
			Get get = new Get(Bytes.toBytes(p.toRowKeyString()));
			get.addFamily(family);
			result = this.env.getRegion().get(get);
			NavigableMap<byte[], byte[]> circleMap = new TreeMap<byte[], byte[]>(new BytesValueComparator());
			//getFamilyMap returns a Map of the form: Map<qualifier,value>
			circleMap.putAll(result.getFamilyMap(family));
			List<Float> circleMeasurements = new ArrayList<Float>();
			for(byte[] B: circleMap.values()){
				circleMeasurements.add(
						Float.parseFloat(
								Bytes.toString(B)));
			}
			//OE of a particle
			float bestW = Float.MAX_VALUE;
			int bestZ = 0;
			for(int i = 0 ; i< circleMeasurements.size() ; i++){
				//get the measurement of this orientation
				List<Float> mt = getMeasurements(i,circleMeasurements);
				//weight calculating
				float w = weightCalculate(mt, zt);
				//compare the weight
				if(w<bestW){
					bestW=w;
					bestZ=i;
				}
			}
			//output the best weight
			OewcProtos.Particle.Builder outputP = OewcProtos.Particle.newBuilder().
			setX(p.getX()).setY(p.getY()).setZ(bestZ).setW(bestW);
			responseBuilder.addParticles(outputP);
		}
		
	}
	
	public List<Float> getMeasurements(int z, List<Float> circleMeasurements){
		List<Float> measurements = new ArrayList<Float>();
		int sensor_number = (circleMeasurements.size()/2)+1;
		int bias = (sensor_number - 1) / 2;
		int index;
		for (int i = 0; i < sensor_number; i++) {
			index = ( (z - bias + i + circleMeasurements.size() ) % circleMeasurements.size() );
			measurements.add(circleMeasurements.get(index));
		}
		return measurements;
		
	}
	
	class BytesValueComparator implements Comparator<byte[]> {
		@Override
		public int compare(byte[] o1, byte[] o2) {
			if(o1==o2){
				return 0;
			}
			Double d1 = Double.parseDouble(Bytes.toString(o1));
			Double d2 = Double.parseDouble(Bytes.toString(o2));
			return d1.compareTo(d2);
		}
	}

	private boolean exsitRow(List<Particle> src, List<Particle> dst) {
		dst.clear();
		
		byte[] startKey = this.env.getRegion().getStartKey();
		byte[] endKey = this.env.getRegion().getEndKey();
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

}
