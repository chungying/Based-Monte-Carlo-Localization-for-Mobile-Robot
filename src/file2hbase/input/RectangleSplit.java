package file2hbase.input;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;

public class RectangleSplit extends InputSplit implements Writable{

	private Path filePath;
	private long length;
	private String[] hosts;
	
	private int x;
	private int y;
	private int width;
	private int height;
	
	@Override
	public long getLength() throws IOException, InterruptedException {
		return this.length;
	}

	@Override
	public String[] getLocations() throws IOException, InterruptedException {
		if(this.hosts == null)
			return new String[]{};
		return this.hosts;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.filePath = new Path(Text.readString(in));
		this.length = in.readLong();
		this.x = in.readInt();
		this.y = in.readInt();
		this.width = in.readInt();
		this.height = in.readInt();
		this.hosts = null;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		Text.writeString(out, this.filePath.toString());
		out.writeLong(length);
		out.writeInt(this.x);
		out.writeInt(this.y);
		out.writeInt(this.width);
		out.writeInt(this.height);
	}

}
