package getObserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.HRegion;
import org.apache.hadoop.hbase.regionserver.RegionScanner;

import util.Transformer;
import util.oewc.Oewc;

public class OewcObserver3 extends BaseRegionObserver{
	public static String PASSWORD = "oewc"; 
	private HRegion region = null;
	
	@Override
	public void start(CoprocessorEnvironment e) throws IOException {
		this.region = ((RegionCoprocessorEnvironment)e).getRegion();
	}

	@Override
	public void preGetOp(ObserverContext<RegionCoprocessorEnvironment> arg0,
			Get arg1, List<Cell> arg2) throws IOException {
		List<Long> times = new ArrayList<Long>();
		times.add(System.currentTimeMillis());
		if(OewcObserver.isOewc(arg1)){//whether execute oewc or not.
			try{
				//get the measurements from rowkey
				times.add(System.currentTimeMillis());
				List<Float> Zt = OewcObserver.drawZtFromGet(arg1.getRow());
				//get the simulations from region
				times.add(System.currentTimeMillis());
				List<Float> Circles = getFromRegionByScanner(arg0, arg1, this.region);
				//start up OEWC
				times.add(System.currentTimeMillis());
				Entry<Integer, Float> oewc = Oewc.singleParticleModified(Zt, Circles);
				//add it into the return
				times.add(System.currentTimeMillis());
				arg2.add(OewcObserver.createCell(arg1, oewc,times));
			}catch (Exception e) {
				//create cell of error and add it into arg2.
				arg2.add(OewcObserver.exception2Cell(e));
			}finally{
				//skip all further processing
				arg0.bypass();
			}
		}
	}

	private static List<Float> getFromRegionByScanner(
			ObserverContext<RegionCoprocessorEnvironment> e, Get get,
			HRegion region) throws Exception{
		List<Float> output = null;
		RegionScanner scanner = null;
		try {
			Entry<byte[], NavigableSet<byte[]>> entry = 
					get.getFamilyMap().entrySet().iterator().next();
			byte[] family = entry.getKey();
			byte[] qualifier = entry.getValue().iterator().next();
			Get g = new Get(Arrays.copyOfRange(get.getRow(), 0, 15));
			g.addColumn(family, qualifier);
			
			Scan scan = new Scan(g);
			scanner = region.getScanner(scan);
			List<Cell> getresults = new ArrayList<Cell>();
		    scanner.next(getresults);
			Result result = Result.create(getresults);
			
			output = Transformer.BA2FA(0,
					result.getColumnLatestCell(family, qualifier).getValueArray());
		} catch (Exception e1) {
			throw e1;
		}finally{
			if(scanner!=null)
				scanner.close();
		}
		return output;
	}
}
