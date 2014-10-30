package mapreduce.type;

import java.io.*;

import org.apache.hadoop.io.*;

public class RectangleWritableComparable implements WritableComparable<RectangleWritableComparable>{

	public enum RecordReaderCounters {
		tostring,
		equals,
		hashcode,
		comparato,
		write,
		readfields,
		set,
		constructor1,
		constructor2,
		constructor3
	 }
	//private Configuration conf;
	public IntWritable x;
	public IntWritable y;
	public IntWritable width;
	public IntWritable height;
	
	public RectangleWritableComparable(){
		this(new IntWritable(),new IntWritable(),new IntWritable(),new IntWritable());
		
	}
	
	public RectangleWritableComparable(IntWritable x, IntWritable y,
			IntWritable width, IntWritable height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	public RectangleWritableComparable(int x, int y,
			int width, int height) {
		this(new IntWritable(x), new IntWritable(y), new IntWritable(width), new IntWritable(height));
	}

	
	public int compareTo(RectangleWritableComparable rect) {
		int cmp = x.compareTo(rect.x);
		if(cmp != 0){
			return cmp;
		}
		cmp = y.compareTo(rect.y);
		if(cmp != 0){
			return cmp;
		}
		cmp = width.compareTo(rect.width);
		if(cmp != 0){
			return cmp;
		}
		return  height.compareTo(rect.height);
	}

	@Override
	public int hashCode() {
		int hc = x.hashCode();
		hc = hc*163 + y.hashCode();
		hc = hc*163 + width.hashCode();
		return hc*163 + height.hashCode(); 
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof RectangleWritableComparable){
			RectangleWritableComparable rect = (RectangleWritableComparable) obj;
			return x.equals(rect.x) && y.equals(rect.y) &&	width.equals(rect.width) &&	height.equals(rect.height);
		}
		return false;
	}

	@Override
	public String toString() {
		return "Rectangle area: ("+x+","+y+"),width: "+width+", height: "+height;
	}
	
	@Override
	public void readFields(DataInput in) throws IOException {
		x.readFields(in);
		y.readFields(in);
		width.readFields(in);
		height.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		x.write(out);
		y.write(out);
		width.write(out);
		height.write(out);
	}
	
	public static RectangleWritableComparable read(DataInput in) throws IOException{
		RectangleWritableComparable result = new RectangleWritableComparable();
		result.readFields(in);
		return result;
	}

}
