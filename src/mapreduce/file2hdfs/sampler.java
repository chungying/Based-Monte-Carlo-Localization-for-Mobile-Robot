package mapreduce.file2hdfs;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.SequenceFile;

public class sampler {
	public static void main(String[] args) throws IOException{
		
		NavigableMap<Integer, Integer> map = createMap(1000);
		
		
		List<Float> list = new ArrayList<Float>();
		
		for(Float f: list){
			
			int value = Math.round(f*1000);
			addOne(map, value);
		}
		
		
		
		
	}
	
	public static boolean checkOut(float value){
		if(value<0.0f && value>1.0f)
			return false;
		return true;
	}
	
	public static void addOne(NavigableMap<Integer, Integer> map, int key){
		map.put(key, (map.get(key)+1));
	}
	
	public static NavigableMap<Integer, Integer> createMap(int range){
		NavigableMap<Integer, Integer> map = new TreeMap<Integer, Integer>();
		for(int i = 0 ; i < range; i++){
			map.put(i, 0);
		}
		return map;
	}
}
