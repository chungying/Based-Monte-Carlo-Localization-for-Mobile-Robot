package file2hbase;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import samcl.Grid;
import util.metrics.Transformer;
import file2hbase.input.RectangleSplit;

public class Image2Mapper extends Mapper< Text, RectangleSplit, ImmutableBytesWritable, Put>{
	private enum Counters {
		MAP,
		PUT
	}
	private byte[] Family_Distance = null;
	private byte[] Family_Energy = null;
	private byte[] Family_X = null; 
	private byte[] Family_Y = null;
	
	@Override
	protected void setup(Context context)
			throws IOException, InterruptedException {
		Family_Distance = Bytes.toBytes(context.getConfiguration().get("conf.family.distance","distance"));
		Family_Energy = Bytes.toBytes(context.getConfiguration().get("conf.family.energy","energy"));
		Family_X = Bytes.toBytes(context.getConfiguration().get("conf.family.laserpoint.x","laserpoint.x"));
		Family_Y = Bytes.toBytes(context.getConfiguration().get("conf.family.laserpoint.y","laserpoint.y"));
	}
	
	@Override
	protected void map(Text key, RectangleSplit value, Context context)
			throws IOException, InterruptedException {
		context.getCounter(Counters.MAP).increment(1);
		System.out.println("server name: " + java.net.InetAddress.getLocalHost().getHostName());
		System.out.println(key.toString());
		
		String orientationStr = context.getConfiguration().get("conf.orientation");
		int orientation;
		if(orientationStr!=null)
			orientation = Integer.parseInt(orientationStr);
		else{
			orientation = 4;
			System.out.println("conf.orientation is empty, setup orientation as default value 4.");
		}
		
		String pathStr = value.getPath().toString();
		int x = value.getRectangle().x.get();
		int y = value.getRectangle().y.get();
		int recWidth = value.getRectangle().width.get();
		int recHeight = value.getRectangle().height.get();
		
		try{
			Grid gridmap = new Grid(orientation, (orientation/2)+1, pathStr);
			gridmap.readmap(pathStr, context.getConfiguration());
			long time = System.currentTimeMillis();
			gridmap.pre_compute( x, y, recWidth, recHeight);
			time = System.currentTimeMillis() - time;
			System.out.println("the time of pre-compute is " + time +" ms.");
			//TODOdone send out the PUT and IMMUTABLEBYTESWRITABLE
			String rowkeyStr = "";
			Random random = new Random();
			for(int i = 0; i < x; i++){
				for(int j = 0; j < y; j++){
					rowkeyStr = Transformer.xy2RowkeyString(i, j, random);
					
					for(int k = 0; j < orientation; k++){
						context.getCounter(Counters.PUT).increment(1);
						String measurements = String.valueOf(gridmap.G[i][j].circle_measurements[k]);
						Put putDistance = new Put(Bytes.toBytes(rowkeyStr));
						putDistance.setDurability(Durability.SKIP_WAL);
						putDistance.add(Family_Distance,
								Bytes.toBytes(String.valueOf(k)),
								Bytes.toBytes(measurements));
						context.write(new ImmutableBytesWritable(Bytes.toBytes(rowkeyStr)),
								putDistance);
						
						String energy = String.valueOf(gridmap.G[i][j].getEnergy(k));
						Put putEnergy = new Put(Bytes.toBytes(energy));
						putEnergy.setDurability(Durability.SKIP_WAL);
						putEnergy.add(Family_Energy, 
								Bytes.toBytes(rowkeyStr),
								Bytes.toBytes(String.valueOf(k)));
						context.write(new ImmutableBytesWritable(Bytes.toBytes(energy)),
								putEnergy);
					}
				}
			}
		}catch(Exception e){
			System.out.println(e);	
		}
	}
	
	
	
}
