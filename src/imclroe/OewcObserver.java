package imclroe;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.HRegion;
import org.apache.hadoop.hbase.util.Bytes;

import util.oewc.Oewc;

/**
 * @author w514
 *
 */
public class OewcObserver extends BaseRegionObserver{
	
	HRegion region = null;

	@Override
	public void start(CoprocessorEnvironment e) throws IOException {
		
	}


	@Override
	public void stop(CoprocessorEnvironment e) throws IOException {
		// TODO Auto-generated method stub
		super.stop(e);
	}


	@Override
	public void preGetOp(ObserverContext<RegionCoprocessorEnvironment> arg0,
			Get arg1, List<Cell> arg2) throws IOException {
//		if(isOewc(arg1)){
//			Zt = drawZtFromGet(arg1);
//			Circles = getFromRegion(arg1);
//			Entry<I,F> = Oewc.singleParticle(Zt,Circles);
//			result = createCell(arg1,Entry<Integer, Float> entry);
//			arg2.add(result);
//			bypass;
//		}
	}
	
	static public boolean isOewc(Get get){
		String rowkeyStr = Bytes.toString(get.getRow());
		return rowkeyStr.contains("oewc");
	}
	
	static public List<Float> drawZtFromGet(Get get){
		Map<byte[], NavigableSet<byte[]> > map= get.getFamilyMap();
		/*for(Entry<byte[], NavigableSet<byte[]>> entry: map.entrySet()){
			System.out.println("Family: "+Bytes.toString( entry.getKey() ) );
			System.out.println("Qualifier");
			for(byte[] bs: entry.getValue()){
				System.out.println(Bytes.toString(bs));
			}
		}*/
		
		
		return null;
	}
	
	public List<Float> getFromRegion(Get get){
		return null;
	}
	
	/**
	 * @param get
	 * @param entry
	 * @return Cell = [xxxx:XY, "fam", Z, W]
	 */
	public Cell createCell(Get get, Entry<Integer,Float> entry){
		Cell cell = CellUtil.createCell(
				get.getRow(),
				"fam".getBytes(),
				String.valueOf(entry.getKey()).getBytes(), 
				System.currentTimeMillis(), 
				KeyValue.Type.valueOf("Oewc").getCode(), 
				Bytes.toBytes(entry.getValue())
				);
		return cell;
	}
	

	//for test
	public static void main(String[] args) {
		Get get = new Get("0573:0010000100:Oewc".getBytes());
		if(isOewc(get)){
			System.out.println("there is oewc.");
			System.out.println(Bytes.toString(get.getRow()));
		}else{
			System.out.println("there is no oewc.");
			System.out.println(Bytes.toString(get.getRow()));
		}
		
		get.addColumn("fam1".getBytes(), "q1".getBytes());
		get.addColumn("fam1".getBytes(), "q2".getBytes());
		get.addColumn("fam1".getBytes(), "q3".getBytes());
		get.addColumn("fam2".getBytes(), "q4".getBytes());
		get.addColumn("fam2".getBytes(), "q5".getBytes());
		get.addColumn("fam3".getBytes(), "q6".getBytes());
		drawZtFromGet(get);

	}

}
