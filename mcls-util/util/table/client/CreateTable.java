package util.table.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HBaseAdmin;

import com.beust.jcommander.Parameter;

public class CreateTable extends Base{
	@Parameter(names = {"-i","--input"}, 
			description = "the path of image, default is \"hdfs:///user/eeuser/jpg/sim_map.jpg\"", 
			required = true, arity = 1)
	public String tableName = "hdfs:///user/eeuser/jpg/sim_map.jpg";

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		System.out.println("start the programe");
		Configuration conf = HBaseConfiguration.create();
		HBaseAdmin admin = new HBaseAdmin(conf);
		admin.deleteTable("test6.18.split");
		
		
		/*List<Cell> cells = new ArrayList<Cell>();
		Cell cell3 = CellUtil.createCell(
				"r2".getBytes(), 
				"f1".getBytes(), 
				"q3".getBytes(), 
				System.currentTimeMillis(), 
				KeyValue.Type.Put.getCode(), 
				"v3".getBytes());
		cells.add(cell3);
		Cell cell1 = CellUtil.createCell(
				"r1".getBytes(), 
				"f1".getBytes(), 
				"q1".getBytes(), 
				System.currentTimeMillis(), 
				KeyValue.Type.Put.getCode(), 
				"v1".getBytes());
		cells.add(cell1);
		Cell cell2 = CellUtil.createCell(
				"r1".getBytes(), 
				"f1".getBytes(), 
				"q2".getBytes(), 
				System.currentTimeMillis(), 
				KeyValue.Type.Put.getCode(), 
				"v2".getBytes());
		cells.add(cell2);
		Cell cell4 = CellUtil.createCell(
				"r1".getBytes(), 
				"f1".getBytes(), 
				"q4".getBytes(), 
				System.currentTimeMillis(), 
				KeyValue.Type.Put.getCode(), 
				"v4".getBytes());
		cells.add(cell4);


		Result result = Result.create(cells);
		System.out.println(Bytes.toString(result.getRow()));
		Map<byte[],byte[]> map = result.getFamilyMap("f1".getBytes());
		for(Entry<byte[], byte[]> e: map.entrySet()){
			System.out.println(Bytes.toString(e.getKey())+","+Bytes.toString(e.getValue()));
		}
		System.out.println();*/

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
}
