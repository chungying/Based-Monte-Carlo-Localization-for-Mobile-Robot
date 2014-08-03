package file2hbase;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.mapreduce.Reducer;


public class HfileReducer 
extends Reducer<ImmutableBytesWritable, Put, ImmutableBytesWritable, KeyValue>{
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
			for(Entry<byte[], List<KeyValue>> e:put.getFamilyMap().entrySet()){
				context.getCounter(Counters.FAMILY).increment(1);
				for(KeyValue kv: e.getValue()){
					context.getCounter(Counters.KEYVALUE).increment(1);
					context.write(key, kv);
				}
			}
		}
	}

}
