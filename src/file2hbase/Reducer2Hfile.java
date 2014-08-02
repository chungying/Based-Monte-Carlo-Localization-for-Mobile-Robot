package file2hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

import samcl.Grid;
import util.metrics.Transformer;
import file2hbase.Reducer2Hbase.Counters;
import file2hbase.type.RectangleWritableComparable;

public class Reducer2Hfile 
extends Reducer<IntWritable, RectangleWritableComparable, ImmutableBytesWritable, KeyValue>{
	private HTable Table = null;
	private byte[] Family_Distance = null;
	private byte[] Family_Energy = null;
	private byte[] Family_X = null; 
	private byte[] Family_Y = null;
	@Override
	protected void setup(Context context)
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
			Context context)
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
		
		for (RectangleWritableComparable value: values) {
			try {
				KeyValue kv = new KeyValue(
						Bytes.toBytes(String.valueOf(value.x.get())), 
						Bytes.toBytes("distance"), 
						Bytes.toBytes(String.valueOf(value.y.get())), 
						System.currentTimeMillis(),
						Bytes.toBytes(String.valueOf(value.height.get())));
				ImmutableBytesWritable output = new ImmutableBytesWritable();
				output.set(Bytes.toBytes(String.valueOf(value.x.get())));
				context.write(output, kv);
//				Grid gridmap = new Grid(orientation, (orientation/2)+1, path);
//
//				gridmap.readmap(path, context);
//
//				gridmap.pre_compute(value.x.get(), value.y.get(), value.width.get(),
//						value.height.get());
//				//int rowkey;
//				String row_str = new String();
//				Random random = new Random();
//				int translateX = value.x.get();
//				int translateY = value.y.get();
//				for (int i = 0; i < value.width.get(); i++) {
//					
//					for (int j = 0; j < value.height.get(); j++) {
//						
//						// transform to absolute type
//						row_str = Transformer.xy2RowkeyString(i+translateX, j+translateY, random);
//						Put put = new Put(Bytes.toBytes(row_str));
//						
//						// have been improved to keep the better efficiency
//						//put.setWriteToWAL(false);
//						put.setDurability(Durability.SKIP_WAL);
//						
//						for (int k = 0; k < gridmap.orientation; k++) {							
//							String measurements = String.valueOf(gridmap.G[i][j].circle_measurements[k]);
//							
//							put.add(Family_Distance,
//									Bytes.toBytes(String.valueOf(k)),
//									Bytes.toBytes(measurements));
//														
//							String energy = String.valueOf(gridmap.G[i][j].getEnergy(k));
//							Put putEnergy = new Put(Bytes.toBytes(energy));
//							//putEnergy.setWriteToWAL(false);
//							put.setDurability(Durability.SKIP_WAL);
//							putEnergy.add(Family_Energy, 
//									Bytes.toBytes(row_str),
//									Bytes.toBytes(String.valueOf(k)));
//							
//							puts.add(putEnergy);
//							
//							//TODO separate the load
//							Table.put(putEnergy);
//							Table.put(put);
//						}
//						
//						puts.add(put);
//						
//					}
//				}
//				System.out.println("last key: "+row_str.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	

}
