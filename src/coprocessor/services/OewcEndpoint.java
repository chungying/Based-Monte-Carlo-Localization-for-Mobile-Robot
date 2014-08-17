package coprocessor.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.coprocessor.CoprocessorException;
import org.apache.hadoop.hbase.coprocessor.CoprocessorService;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.regionserver.RegionScanner;
import org.apache.hadoop.hbase.util.Bytes;

import util.metrics.Transformer;
import util.oewc.Oewc;

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
		//initial and get the data from client
		String message = "";
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
			
			if(exsitRow(requsetParts, existParticles)){
				step1 = System.currentTimeMillis();
				//Step 2: Orientation Estimation and setup response
				orientationEstimate(responseBuilder, existParticles, Zt);
				step2 = System.currentTimeMillis();
				//Step 3: Send back the response
				responseBuilder.setCount(1).setWeight(1.0f);
				message = "Particle number: "+String.valueOf(existParticles.size());
				step3 = System.currentTimeMillis();
			}else{
				step1 = System.currentTimeMillis();
				responseBuilder.setCount(-1).setWeight(-1.0f).build();
				message = "no OEWC"+requsetParts.size();
			}
			end = System.currentTimeMillis();
	    
		}catch(Exception e){
			responseBuilder.setCount(-1).setWeight(-1.0f).build();
			message = "failed:"+e.toString();
			
			e.printStackTrace();
		}finally{
			Scan scan = createScan(existParticles);
			scan.setCaching(1000);
			RegionScanner scanner = null;
			List<Cell> cells = new ArrayList<Cell>();
			/*try {
				scanner = this.env.getRegion().getScanner(scan);
				scanner.nextRaw(cells);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				try {
					scanner.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}*/
			
			Map<byte[], Integer> rowMap = new TreeMap<byte[], Integer>(Bytes.BYTES_COMPARATOR);
			for(Cell c: cells){
				if(rowMap.get(c.getRowArray()) != null){
					rowMap.put(c.getRowArray(), rowMap.get(c.getRowArray())+1);
				}else{
					rowMap.put(c.getRowArray(), 1);
				}
			}
			responseBuilder.setStr(
					"start k:"+Bytes.toString(scan.getStartRow())+"\n"+
					"stop k :"+Bytes.toString(scan.getStopRow())+"\n"+
					"cells  :"+cells.size()+"\n"+
					"rows   :"+rowMap.size()+"\n"+
					"rowkey1:"+Bytes.toString(rowMap.keySet().iterator().next())+"\n"+
					"start  :"+String.valueOf(start)+"\n"+
					"initial:"+String.valueOf(initial)+"\n"+
					"step1  :"+String.valueOf(step1)+"\n"+
					"step2  :"+String.valueOf(step2)+"\n"+
					"step3  :"+String.valueOf(step3)+"\n"+
					"end    :"+String.valueOf(end)+"\n"+
					message);
			done.run(responseBuilder.build());
		}
	}

	private Scan createScan(List<Particle> existParticles){
		List<Filter> filters = new ArrayList<Filter>();
		Random random = new Random();
		for(Particle p : existParticles){
			filters.add(
					new RowFilter(
							CompareFilter.CompareOp.EQUAL,
							new BinaryComparator(Bytes.toBytes(
									Transformer.xy2RowkeyString(p.getX(), p.getY(), random)))));
		}
		FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ONE, filters);
		Scan scan = new Scan(this.env.getRegion().getStartKey(), this.env.getRegion().getEndKey());
		scan.setFilter(filterList);
		return scan;
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
			Entry<Integer, Float> entry = Oewc.singleParticle(zt, circleMeasurements);
			//output the best weight
			OewcProtos.Particle.Builder outputP = OewcProtos.Particle.newBuilder().
			setX(p.getX()).setY(p.getY()).setZ(entry.getKey()).setW(entry.getValue());
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
