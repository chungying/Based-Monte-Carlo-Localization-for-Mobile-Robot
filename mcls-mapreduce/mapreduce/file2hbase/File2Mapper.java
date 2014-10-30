package mapreduce.file2hbase;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;

import javax.imageio.ImageIO;

import mapreduce.type.RectangleWritableComparable;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Mapper;

// ^^ ImportFromFile
/**
 * Implements the <code>Mapper</code> that takes the lines from the input
 * and outputs <code>Put</code> instances.
 */
// vv ImportFromFile

public class File2Mapper extends Mapper<NullWritable, BytesWritable, IntWritable, RectangleWritableComparable> { // co ImportFromFile-2-Mapper Define the mapper class, extending the provided Hadoop class.
	
	public enum Counters { 
		LINES, 
		FAULT,
		IN,
		IMAGE,
		reducer,
		getwidth,
		getheight,
		FOR,
		SUCCEED
	}

	

	@Override
	public void map(NullWritable key, BytesWritable value, Context context) 
	throws IOException {
//		try {
			
			//System.out.println("mapreduce.job.ubertask.maxreduces = "+
				//context.getConfiguration().get("mapreduce.job.ubertask.maxreduces", "null"));
			
			String filestr = context.getConfiguration().get("conf.input","/user/w514/map.jpg");
			FileSystem fs = FileSystem.get(URI.create(filestr), context.getConfiguration());
			FSDataInputStream in = fs.open(new Path(filestr));
			BufferedImage image = ImageIO.read(in);
			
			
			//context.getCounter(Counters.IN).increment(1);
			//byte[] contents = value.copyBytes();
			//InputStream in = new ByteArrayInputStream(contents);
			//context.getCounter(Counters.IMAGE).increment(1);
			//BufferedImage image = ImageIO.read(in);
			
			int reducenumber = context.getNumReduceTasks();
			//int reducenumber = context.getConfiguration().getInt("conf.reduce.number", 3);
			//context.getCounter(Counters.reducer).increment(1);
			
			
//			FileSystem fs = FileSystem.get(URI.create(filename), conf);
//			FSDataInputStream inputstream = fs.open(new Path(filename));
//			map_image = ImageIO.read(inputstream);
			
			int W = image.getWidth();
			//context.getCounter(Counters.getwidth).increment(1);
			int X = 0;
			
			//context.getCounter(Counters.getheight).increment(1);
			int H = image.getHeight()/reducenumber+1;
			int y;
			
			
			
			
			//context.getCounter(Counters.FOR).increment(1);
			for (int i = 0; i < reducenumber; i++) {
				y = i*H;
				RectangleWritableComparable rect = new RectangleWritableComparable( X, y, W, H);
				try {
					context.write(new IntWritable(i), rect);
					//context.getCounter(Counters.SUCCEED).increment(1);
				} catch (InterruptedException e) {
					//context.getCounter(Counters.FAULT).increment(1);
					e.printStackTrace();
				}
			}

			context.getCounter(Counters.SUCCEED).increment(1);
//		} 
//		catch (Exception e) {
//			e.printStackTrace();
//			context.getCounter(Counters.FAULT).increment(1);
//		}
	}
}