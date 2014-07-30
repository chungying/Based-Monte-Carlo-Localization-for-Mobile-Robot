package file2hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

public class Reducer2Hfile 
extends Reducer<IntWritable, RectangleWritableComparable, ImmutableBytesWritable, Put>{
	private HTable Table = null;
	private byte[] Family_Distance = null;
	private byte[] Family_Energy = null;
	private byte[] Family_X = null; 
	private byte[] Family_Y = null;
	@Override
	protected void setup(
			Reducer<IntWritable, RectangleWritableComparable, ImmutableBytesWritable, Put>.Context context)
			throws IOException, InterruptedException {

		Table = new HTable(context.getConfiguration(), context.getConfiguration().get("conf.table","map"));
		Family_Distance = Bytes.toBytes(context.getConfiguration().get("conf.family.distance","distance"));
		Family_Energy = Bytes.toBytes(context.getConfiguration().get("conf.family.energy","energy"));
		Family_X = Bytes.toBytes(context.getConfiguration().get("conf.family.laserpoint.x","laserpoint.x"));
		Family_Y = Bytes.toBytes(context.getConfiguration().get("conf.family.laserpoint.y","laserpoint.y"));
		
	}
	@Override
	protected void reduce(
			IntWritable key,
			Iterable<RectangleWritableComparable> values,
			Reducer<IntWritable, RectangleWritableComparable, ImmutableBytesWritable, Put>.Context context)
			throws IOException, InterruptedException {
		System.out.println("server name: " + java.net.InetAddress.getLocalHost().getHostName());
		System.out.println("key = "+key.get());
		try {
			System.out.println("part= "+context.getPartitionerClass().toString());
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		System.out.println("reucde number = "+context.getNumReduceTasks());
		
		String path = context.getConfiguration().get("conf.input", "/user/eeuser/map1024.jpeg");
		int orientation = context.getConfiguration().getInt("conf.orientation",4);
		//
		List<Put> puts = new ArrayList<Put>();
		
		
	}
	

}
