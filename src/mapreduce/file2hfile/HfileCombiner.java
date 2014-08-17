package mapreduce.file2hfile;

import java.io.IOException;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.mapreduce.Reducer;


public class HfileCombiner 
extends Reducer<ImmutableBytesWritable, Put, ImmutableBytesWritable, Put>{
	private enum Counters {
		REDUCE,
		PUT,
		FAMILY,
		KEYVALUE,
	};
	@Override
	protected void reduce(ImmutableBytesWritable key, Iterable<Put> value,
			Context context)
			throws IOException, InterruptedException {
		context.getCounter(Counters.REDUCE).increment(1);
		for(Put put: value){
			context.getCounter(Counters.PUT).increment(1);
			context.write(key, put);
		}
	}

}
