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
import org.apache.hadoop.hbase.util.Pair;

import util.Transformer;
import util.oewc.Oewc;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;

import endpoint.services.generated.OewcProtos2;
import endpoint.services.generated.OewcProtos2.OewcRequest;
import endpoint.services.generated.OewcProtos2.OewcResponse;
import endpoint.services.generated.OewcProtos2.Particle;
import endpoint.services.generated.OewcProtos2.OewcResponse.Builder;

public class OewcEndpoint2 extends OewcProtos2.Oewc2Service implements
		Coprocessor, CoprocessorService {

	@Override
	public Service getService() {
		return this;
	}

	private RegionCoprocessorEnvironment env;

	@Override
	public void start(CoprocessorEnvironment arg0) throws IOException {
		if (arg0 instanceof RegionCoprocessorEnvironment) {
			this.env = (RegionCoprocessorEnvironment) arg0;
		} else {
			throw new CoprocessorException("Must be loaded on a table region!");
		}
	}

	@Override
	public void stop(CoprocessorEnvironment arg0) throws IOException {
	}

	@Override
	public void getOewc2Result(RpcController controller, OewcRequest request,
			RpcCallback<OewcResponse> done) {
		long start = System.currentTimeMillis();
		this.env.getRegion().getTableDesc().getTableName().toString();

		// initial and get the data from client
		String message = "";
		OewcResponse.Builder responseBuilder = OewcResponse.newBuilder();
		
		long initial = -1, step1 = -1, step2 = -1, step3 = -1, end = -1;
		List<OewcProtos2.Particle> requsetParts = request.getParticlesList();
		List<OewcProtos2.Particle> existParticles = new ArrayList<OewcProtos2.Particle>();
		Pair<Long, Long> times = null;
		try {
			List<Float> Zt = request.getMeasurementsList();
			if (Zt.isEmpty()) {
				throw new Exception("there is no Zt in request");
			}
			initial = System.currentTimeMillis();
			// get Measurements
			// Step 1: Filter the Particles

			if (exsitRow(requsetParts, existParticles, this.env.getRegion()
					.getStartKey(), this.env.getRegion().getEndKey())) {
				step1 = System.currentTimeMillis();
				// Step 2: Orientation Estimation and setup response
				times = orientationEstimate(this.env.getRegion(),
								responseBuilder, existParticles, Zt);
				step2 = System.currentTimeMillis();
				// Step 3: Send back the response
				message = message + "Particle number is "
						+ String.valueOf(existParticles.size())+".\t";
				step3 = System.currentTimeMillis();
			} else {
				step1 = System.currentTimeMillis();
				message = "Particle number is" + requsetParts.size()+".\t";
			}
			end = System.currentTimeMillis();

		} catch (Exception e) {
			message = "FAILED:" + e.toString()+"\t";
		} finally {
			responseBuilder.setStr(
							"start  :" + String.valueOf(start)	+ "\t" + 
							"initial:" + String.valueOf(initial)+ "\t" + 
							"step1  :" + String.valueOf(step1)	+ "\t" + 
							"step2  :" + String.valueOf(step2)	+ "\t" + 
							"step3  :" + String.valueOf(step3) 	+ "\t" + 
							"end    :" + String.valueOf(end)	+ "\t" + 
							message + "\n");
			if(times!=null){
				responseBuilder.setWeight(times.getFirst().floatValue());//reading HDFS time 
				responseBuilder.setCount( times.getSecond().intValue());//OEWC time and reading HDFS time
			}
			done.run(responseBuilder.build());
		}
	}

	static public boolean exsitRow(List<Particle> src, List<Particle> dst,
			byte[] startKey, byte[] endKey) {
		dst.clear();

		// byte[] startKey = this.env.getRegion().getStartKey();
		// byte[] endKey = this.env.getRegion().getEndKey();
		if (startKey.length != 0 && endKey.length != 0) {
			for (Particle p : src) {
				// String s = p.toRowKeyString();
				String s = Transformer.xy2RowkeyString(p.getX(), p.getY());
				byte[] tmp = Bytes.toBytes(s);
				if (Bytes.compareTo(tmp, startKey) >= 0
						&& Bytes.compareTo(tmp, endKey) < 0) {
					dst.add(p);
				}
			}
		} else if (startKey.length == 0 && endKey.length != 0) {
			for (Particle p : src) {
				// String s = p.toRowKeyString();
				String s = Transformer.xy2RowkeyString(p.getX(), p.getY());
				byte[] tmp = Bytes.toBytes(s);
				if (Bytes.compareTo(tmp, endKey) < 0) {
					dst.add(p);
				}
			}
		} else if (startKey.length != 0 && endKey.length == 0) {
			for (Particle p : src) {
				// String s = p.toRowKeyString();
				String s = Transformer.xy2RowkeyString(p.getX(), p.getY());
				byte[] tmp = Bytes.toBytes(s);
				if (Bytes.compareTo(tmp, startKey) >= 0) {
					dst.add(p);
				}
			}
		} else {
			for (Particle p : src) {
				dst.add(p);
			}
		}

		if (dst.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	static byte[] family = Bytes.toBytes("distance");

	static public Pair<Long, Long> orientationEstimate(HRegion region,
			Builder responseBuilder, List<Particle> existParticles,
			List<Float> zt) throws Exception {
		long readTime = 0, time;
		long oewcTime = System.nanoTime();
		Result result = null;
		for (Particle p : existParticles) {
			
			time = System.nanoTime();
			// get the round measurements to circleMeasure
			Get get = new Get(Bytes.toBytes(Transformer.xy2RowkeyString(
					p.getX(), p.getY())));
			get.addColumn(family, "data".getBytes());
			result = region.get(get);
			List<Float> circleMeasurements = new ArrayList<Float>();
			Transformer.result2Array(family, result,
							circleMeasurements);
			readTime = readTime + System.nanoTime()-time;
			
			
			//Orientation Estimation and Weight Calculation
			// OE of a particle
			Entry<Integer, Float> entry = Oewc.singleParticleModified(zt, circleMeasurements);
			// output the best weight
			Particle.Builder outputP = Particle.newBuilder()
					.setX(p.getX())
					.setY(p.getY())
					.setZ(entry.getKey())
					.setW(entry.getValue());
			responseBuilder.addParticles(outputP);
			
			
		}
		oewcTime = System.nanoTime() - oewcTime;
		
		return new Pair<Long, Long>(
				Math.round(readTime/1000000.0), //reading HDFS time
				Math.round(oewcTime/1000000.0)//OEWC time and reading HDFS time
				);
	}

}
