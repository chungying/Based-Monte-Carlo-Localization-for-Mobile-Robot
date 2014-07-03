package file2hbase;

import java.io.IOException;
//import java.util.Vector;



import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.HTable;
//import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Reducer;

import samcl.*;
import util.metrics.Transformer;

public class Reducer2Hbase 
	extends Reducer<IntWritable, RectangleWritableComparable, ImmutableBytesWritable, Put>{
	private HTable Table = null;
	//private Vector<Put> Puts = null;
	private byte[] Family_Distance = null;
	private byte[] Family_Energy = null;
	private byte[] Family_X = null; 
	private byte[] Family_Y = null;
	
	public enum Counters {
		A,
		B,
		C,
		D,
		E,
		F,
		G,
		H,
		I,
		J,
		K,
		PUT
	}
	
	@Override
	protected void setup(Context context) 
			throws IOException, InterruptedException {
		
		Table = new HTable(context.getConfiguration(), context.getConfiguration().get("conf.table","map"));
		//Puts = new Vector<Put>();
		Family_Distance = Bytes.toBytes(context.getConfiguration().get("conf.family.distance","distance"));
		Family_Energy = Bytes.toBytes(context.getConfiguration().get("conf.family.energy","energy"));
		Family_X = Bytes.toBytes(context.getConfiguration().get("conf.family.laserpoint.x","laserpoint.x"));
		Family_Y = Bytes.toBytes(context.getConfiguration().get("conf.family.laserpoint.y","laserpoint.y"));
		
		
	}

	
	
	public void reduce(IntWritable key, Iterable<RectangleWritableComparable> values, Context context)
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
		List<Put> puts = new ArrayList<Put>();
		
		for (RectangleWritableComparable value: values) {
			try {
				context.getCounter(Counters.A).increment(1);
				Grid gridmap = new Grid(orientation, (orientation/2)+1, path);

				gridmap.readmap(path, context);

				gridmap.pre_compute(value.x.get(), value.y.get(), value.width.get(),
						value.height.get());
				context.getCounter(Counters.A).increment(1);
				//int rowkey;
				String row_str = new String();
				Random random = new Random();;
				for (int i = 0; i < value.width.get(); i++) {
					
					for (int j = 0; j < value.height.get(); j++) {
						
						//TODO transform to absolute type
						row_str = Transformer.xy2RowkeyString(i, j, random);
						//2014/7/2
						//row_str = "("+( 10000+j+value.y.get() )+","+( 10000+i+value.x.get() )+")";
						//rowkey = (j + value.y.get()) * (value.width.get()) + (i + value.x.get());
						
						context.getCounter(Counters.PUT).increment(1);
						
						
						
						//TODO	2014/05/04 test for energy as rowkey
						
						
						Put put = new Put(Bytes.toBytes(row_str));
						//TODO improve
						//put.setWriteToWAL(false);
						put.setDurability(Durability.SKIP_WAL);
						
						for (int k = 0; k < gridmap.orientation; k++) {
							
//							if("distance".equals((String)Bytes.toString(Family_Distance))){
//								context.getCounter(Counters.B).increment(1);
//							}
//							if(gridmap.orientation==4){
//								context.getCounter(Counters.C).increment(1);
//							}
							
							context.getCounter(Counters.D).increment(1);
							
							String measurements = String.valueOf(gridmap.G[i][j].circle_measurements[k]);
							//TODO
							put.add(Family_Distance,
									Bytes.toBytes(String.valueOf(k)),
									Bytes.toBytes(measurements));
							
							context.getCounter(Counters.E).increment(1);
							
							//TODO improve energy to 2 dimensional
							String energy = String.valueOf(gridmap.G[i][j].getEnergy(k));
							Put putEnergy = new Put(Bytes.toBytes(energy));
							putEnergy.add(Family_Energy, 
									Bytes.toBytes(row_str),
									Bytes.toBytes(String.valueOf(k)));//TODO add something useful
							//context.write(new ImmutableBytesWritable(Bytes.toBytes(energy)), putEnergy);
							puts.add(putEnergy);
							
							context.getCounter(Counters.F).increment(1);
							
							String laser_x = String.valueOf(gridmap.G[i][j].measurement_points[k].x);
							//TODO
							put.add(Family_X,
									Bytes.toBytes(String.valueOf(k)),
									Bytes.toBytes(laser_x));
					
							context.getCounter(Counters.G).increment(1);
							
							String laser_y = String.valueOf(gridmap.G[i][j].measurement_points[k].y);
							//TODO
							put.add(Family_Y,
									Bytes.toBytes(String.valueOf(k)),
									Bytes.toBytes(laser_y));
						}
						
						
						
						context.getCounter(Counters.H).increment(1);
						puts.add(put);
						
						//context.write(new ImmutableBytesWritable(Bytes.toBytes(rowkey)), put);
						context.getCounter(Counters.I).increment(1);
					}
				}
				System.out.println("last key: "+row_str.toString());
			} catch (Exception e) {
				context.getCounter(Counters.J).increment(1);
				e.printStackTrace();
			}
		}
		context.getCounter(Counters.K).increment(1);
		Table.put(puts);
		
	}

	@Override
	protected void cleanup(Context context)
			throws IOException, InterruptedException {
		//Table.flushCommits();
		super.cleanup(context);
	}
	

}
