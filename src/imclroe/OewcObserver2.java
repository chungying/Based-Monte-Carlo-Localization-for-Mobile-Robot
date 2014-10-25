package imclroe;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;

import util.metrics.Transformer;
import util.oewc.Oewc;

public class OewcObserver2 extends BaseRegionObserver{
	
	private HConnection connection = null;
	private byte[] tableName = null;

	@Override
	public void start(CoprocessorEnvironment e) throws IOException {
		// initialize the table pool
		this.connection = HConnectionManager.createConnection(e.getConfiguration());
		this.tableName = ((RegionCoprocessorEnvironment)e).getRegion().getTableDesc().getTableName().getName();
	}

	@Override
	public void stop(CoprocessorEnvironment e) throws IOException {
		// release the table pool
		if(this.connection!=null)
			this.connection.close();
	}

	@Override
	public void preGetOp(ObserverContext<RegionCoprocessorEnvironment> arg0,
			Get arg1, List<Cell> arg2) throws IOException {
		if(OewcObserver.isOewc(arg1)){//whether execute oewc or not.
			try{
				//get the measurements from rowkey
				List<Float> Zt = OewcObserver.drawZtFromGet(arg1.getRow());
				//get the simulations from region
				List<Float> Circles = getFromConnection(arg0, arg1, this.connection, this.tableName);
				//start up OEWC
				Entry<Integer, Float> oewc = Oewc.singleParticle(Zt, Circles);
				//add it into the return
				arg2.add(OewcObserver.createCell(arg1, oewc));
			}catch (Exception e) {
				//create cell of error and add it into arg2.
				arg2.add(OewcObserver.exception2Cell(e));
			}finally{
				//skip all further processing
				arg0.bypass();
			}
		}
	}

	private List<Float> getFromConnection(
			ObserverContext<RegionCoprocessorEnvironment> e, 
			Get get, HConnection connection, byte[] tableName) throws Exception{
		List<Float> output = null;
		try {
			Entry<byte[], NavigableSet<byte[]>> entry = 
					get.getFamilyMap().entrySet().iterator().next();
			byte[] family = entry.getKey();
			byte[] qualifier = entry.getValue().iterator().next();
			Get g = new Get(Arrays.copyOfRange(get.getRow(), 0, 15));
			g.addColumn(family, qualifier);
			HTable table = (HTable) connection.getTable(tableName);
			Result result = table.get(g);
			table.close();
			output = Transformer.BA2FA(0,
					result.getColumnLatestCell(family, qualifier).getValueArray());
		} catch (Exception e1) {
			throw e1;
		}
		return output;
	}

}
