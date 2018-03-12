package util.table.client;

import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.Comparator;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Threads;

public class WarmUp extends Base{

	public static class Bytes2IntegerComparator implements Comparator<byte[]>{

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
	
	@SuppressWarnings({ "resource", "unused" })
	public static void main(String[] args){
		String tableName = "hbase:meta";
		try {
			HBaseAdmin admin = new HBaseAdmin( HBaseConfiguration.create());
			boolean isDisabled = admin.isTableDisabled(tableName);
			System.out.println(tableName + " is disabled:"+ isDisabled);
			boolean isAvailable = admin.isTableAvailable(tableName);
			System.out.println(tableName + " is available:"+ isAvailable);
			
			//create HTableConnection
			System.out.println("Create connection...");
			HConnection connection = HConnectionManager.createConnection(HBaseConfiguration.create());
			System.out.println("Create thread pool...");
			ThreadPoolExecutor pool = new ThreadPoolExecutor(16,  Integer.MAX_VALUE, 60l, TimeUnit.SECONDS,
					new SynchronousQueue<Runnable>(), Threads.newDaemonThreadFactory("htable"));
			pool.allowCoreThreadTimeOut(true);
			System.out.println("Create table...");
			HTable table = (HTable) connection.getTable( tableName, pool);
			System.out.println("Done!");
		} catch (MasterNotRunningException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ZooKeeperConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//comparator of String by Integer type.
		/*NavigableMap<byte[], String> map = new TreeMap<byte[], String>(new Bytes2IntegerComparator());
		for(int i = 0; i < 100 ; i++){
			map.put(Bytes.toBytes(String.valueOf(i)), String.valueOf(i));
			
		}
		for(Entry<byte[], String> e: map.entrySet()){
			System.out.println(e.getValue());
		}*/
	}
	
	@Override
	public void run() {
		try {
			this.setup();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
		
		try {
			this.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

}
