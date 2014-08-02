package file2hbase.input;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

public class RectangleRecordReader extends RecordReader< Text, RectangleSplit>{
	
	private Text key;
	private RectangleSplit value;

	@Override
	public void initialize(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException {
		this.value  = (RectangleSplit) split;
		this.key = new Text(this.value.getRectangle().toString());
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		return false;
	}

	@Override
	public void close() throws IOException {
		
	}

	@Override
	public Text getCurrentKey() throws IOException, InterruptedException {
		return this.key;
	}

	@Override
	public RectangleSplit getCurrentValue() throws IOException,
			InterruptedException {
		return this.value;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return 1.0f;
	}

}
