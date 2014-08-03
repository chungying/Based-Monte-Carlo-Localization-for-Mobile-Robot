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

	@Override
	protected void reduce(ImmutableBytesWritable key, Iterable<Put> value,
			Context context)
			throws IOException, InterruptedException {
		for(Put put: value){
			for(Entry<byte[], List<KeyValue>> e:put.getFamilyMap().entrySet()){
				for(KeyValue kv: e.getValue()){
					context.write(key, kv);
				}
			}
		}
	}

}
