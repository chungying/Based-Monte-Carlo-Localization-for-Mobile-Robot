package mapreduce.partitioner;

import mapreduce.type.RectangleWritableComparable;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Partitioner;


public class CustomePartitioner extends Partitioner<IntWritable, RectangleWritableComparable> {

	@Override
	public int getPartition(IntWritable key, RectangleWritableComparable value,
			int reducernumber) {
		System.out.println("key's value    = "+key.get());
		System.out.println("reducer number = "+ reducernumber);
		
		return key.get()%reducernumber;
	}

}
