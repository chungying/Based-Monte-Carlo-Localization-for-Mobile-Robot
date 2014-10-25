package imclroe;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Random;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.HRegion;
import org.apache.hadoop.hbase.util.Bytes;

import util.metrics.Transformer;
import util.oewc.Oewc;

/**
 * @author w514
 *
 */
public class OewcObserver extends BaseRegionObserver{
	public static String PASSWORD = "oewc"; 
	private HRegion region = null;
	@Override
	public void start(CoprocessorEnvironment e) throws IOException {
		this.region = ((RegionCoprocessorEnvironment)e).getRegion();
	}


	@Override
	public void preGetOp(ObserverContext<RegionCoprocessorEnvironment> arg0,
			Get arg1, List<Cell> arg2) throws IOException {
		if(isOewc(arg1)){//whether execute oewc or not.
			try{
				//get the measurements from rowkey
				List<Float> Zt = drawZtFromGet(arg1.getRow());
				//get the simulations from region
				List<Float> Circles = getFromRegion(arg0, arg1, this.region);
				//start up OEWC
				Entry<Integer, Float> oewc = Oewc.singleParticle(Zt, Circles);
				//add it into the return
				arg2.add(createCell(arg1, oewc));
			}catch (Exception e) {
				//create cell of error and add it into arg2.
				arg2.add(exception2Cell(e));
			}finally{
				//skip all further processing
				arg0.bypass();
			}
		}
	}

	
	public static boolean isOewc(Get get){
		String keyword = Bytes.toString(Arrays.copyOfRange(get.getRow(), 0, 21));
		System.out.println("keyword="+ keyword);
		return keyword.contains(PASSWORD);
	}
	
	static public List<Float> drawZtFromGet(byte[] BA) throws Exception{
		List<Float> result = null;
		try{
			result = Transformer.BA2FA(21, BA);
		}catch(Exception e){
			throw e;
		}
		return result;
	}
	
	public static List<Float> getFromRegion(ObserverContext<RegionCoprocessorEnvironment> e, 
			Get get, HRegion region) throws Exception{
		List<Float> output = null;
		try {
			Entry<byte[], NavigableSet<byte[]>> entry = 
					get.getFamilyMap().entrySet().iterator().next();
			byte[] family = entry.getKey();
			byte[] qualifier = entry.getValue().iterator().next();
			Get g = new Get(Arrays.copyOfRange(get.getRow(), 0, 15));
			g.addColumn(family, qualifier);
			Result result = region.get(g);
			output = Transformer.BA2FA(0,
					result.getColumnLatestCell(family, qualifier).getValueArray());
		} catch (Exception e1) {
			throw e1;
		}
		return output;
	}
	
	public static Cell exception2Cell(Exception e){
		//add exception message to cell and return it
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String str = e.toString()+"\nStack Trace:"+sw.toString();
		Cell error = CellUtil.createCell(
				str.getBytes(),
				"err".getBytes(),
				"err".getBytes(), 
				System.currentTimeMillis(), 
				KeyValue.Type.Put.getCode(),  
				"err".getBytes()
				);
		return error;
	}
	
	/**
	 * @param get
	 * @param entry
	 * @return Cell = [xxxx:XY, "fam", Z, W]
	 */
	public static Cell createCell(Get get, Entry<Integer,Float> entry) throws Exception{
		Cell cell = null;
		try {
			//rowkey type = HASH:XXXXXYYYYY:oewc:ORIENTATION:WEIGHTING
			byte[] rowkey = Arrays.copyOfRange(get.getRow(), 0, 21);
			String str = String.valueOf(entry.getKey()) +":"+ String.valueOf(entry.getValue());
			
			cell = CellUtil.createCell(
					Bytes.add(rowkey,str.getBytes()),
					"oewc".getBytes(),
					"oewc".getBytes(), 
					System.currentTimeMillis(), 
					KeyValue.Type.Put.getCode(),  
					Bytes.add(Bytes.toBytes(entry.getKey()),Bytes.toBytes(entry.getValue()))
					);
		} catch (Exception e) {
			throw e;
		}
		return cell;    
	}
	

	//for test
	public static void main(String[] args) throws Exception {
		//initialization
		List<Float> zt = new ArrayList<Float>();
		Random random = new Random();
		for(int i = 0 ; i < 360 ; i++){
			zt.add(random.nextFloat());
		}
		byte[] ztbs = Transformer.FA2BA(zt);
		
		String rowkeyStr = Transformer.xy2RowkeyString(random.nextInt(10000), random.nextInt(10000), random)+":oewc:";
		System.out.println("initialization");
		System.out.println(rowkeyStr);
		System.out.println("rowkey bytes length:" + rowkeyStr.getBytes().length);
		byte[] rowkey = Bytes.add(rowkeyStr.getBytes(), ztbs);
		Get get = new Get(rowkey);
		
		//start test
		System.out.println("start to test...");
		String str = Bytes.toString(get.getRow());
		String keyword = Bytes.toString(Arrays.copyOfRange(get.getRow(), 0, 21));
		System.out.println("keyword="+ keyword);
		List<Float> data = null;
		if(str.contains(keyword)){
			System.out.println("there is oewc.");
			System.out.println(keyword);
			data = drawZtFromGet(get.getRow());
			for(Float f: data){
				System.out.print(f+",");
			}
			System.out.println();
			for(Float f: zt){
				System.out.print(f+",");
			}
		}else{
			System.out.println("there is no oewc.");
			System.out.println(Bytes.toString(get.getRow()));
		}
		
//		get.addColumn("fam1".getBytes(), "q1".getBytes());
//		get.addColumn("fam1".getBytes(), "q2".getBytes());
//		get.addColumn("fam1".getBytes(), "q3".getBytes());
//		get.addColumn("fam2".getBytes(), "q4".getBytes());
//		get.addColumn("fam2".getBytes(), "q5".getBytes());
//		get.addColumn("fam3".getBytes(), "q6".getBytes());
		

	}

}
