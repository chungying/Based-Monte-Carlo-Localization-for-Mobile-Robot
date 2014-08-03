package file2hbase.input;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;

import file2hbase.type.RectangleWritableComparable;

public class RectangleSplit extends InputSplit implements Writable{

	private Path filePath;
	private long length;
	private String[] hosts;
	
	private int imageWidth;
	private int imageHeight;
	private RectangleWritableComparable rectangle;
	
	public RectangleSplit(){
		
	}
	
	public RectangleSplit(Path filePath, long length, String[] hosts, 
			int imageWidth,	int imageHeight, RectangleWritableComparable rectangle) {
		this.filePath = filePath;
		this.length = length;
		this.hosts = hosts;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.rectangle = rectangle;
	}

	public Path getPath() {
		return this.filePath;
	}

	@Override
	public long getLength() {
		return this.length;
	}

	@Override
	public String[] getLocations() throws IOException {
		if(this.hosts == null)
			return new String[]{};
		return this.hosts;
	}

	public int getImageWidth() {
		return imageWidth;
	}

	public int getImageHeight() {
		return imageHeight;
	}

	public RectangleWritableComparable getRectangle() {
		return rectangle;
	}

	@Override
	public String toString() {
		return filePath + ":" + length + 
				", ImageWidth:"+ imageWidth + 
				",IimageHeight:"+ imageHeight + 
				", "+ rectangle;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.filePath = new Path(Text.readString(in));
		this.length = in.readLong();
		this.imageWidth = in.readInt();
		this.imageHeight = in.readInt();
		this.rectangle = RectangleWritableComparable.read(in);
		this.hosts = null;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		Text.writeString(out, this.filePath.toString());
		out.writeLong(this.length);
		out.writeInt(this.imageWidth);
		out.writeInt(this.imageHeight);
		this.rectangle.write(out);
	}

}
