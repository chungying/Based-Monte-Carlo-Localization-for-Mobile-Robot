package file2hbase;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;


 class WholeFileImageRecordReader extends RecordReader<NullWritable, BytesWritable> {
//	 public enum RecordReaderCounters {
//		A,
//		B,
//		C,
//		D,
//		E,
//		F,
//		G,
//		H,
//		I,
//		J,
//		K,
//		L,
//		M,
//		N,
//		O,
//		P,
//		Q
//	 }
	 private Configuration conf;
	 private FileSplit filesplit;
	 private NullWritable key = NullWritable.get();
	 private boolean processed = false;
	 private BytesWritable value = new BytesWritable();
	// private TaskAttemptContext cont = null;
	
	@Override
	public void close() throws IOException {
		
	}

	@Override
	public NullWritable getCurrentKey() throws IOException,
			InterruptedException {
		return key;
	}

	@Override
	public BytesWritable getCurrentValue() throws IOException,
			InterruptedException {
		return value;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return processed ? 1.0f : 0.0f;
	}

	@Override
	public void initialize(InputSplit inputsplit, TaskAttemptContext context)
			throws IOException, InterruptedException {
		this.filesplit = (FileSplit) inputsplit;
		this.conf = context.getConfiguration();
		//cont = context;
		//cont.getCounter(RecordReaderCounters.A).increment(1);
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		FSDataInputStream in = null;
		//cont.getCounter(RecordReaderCounters.B).increment(1);
		if(!processed){
			//cont.getCounter(RecordReaderCounters.C).increment(1);
			byte[] contents = new byte[(int)filesplit.getLength()];
			try {
				//cont.getCounter(RecordReaderCounters.D).increment(1);
				Path filepath = filesplit.getPath();
				//cont.getCounter(RecordReaderCounters.E).increment(1);
				FileSystem fs = filepath.getFileSystem(conf);
				//cont.getCounter(RecordReaderCounters.F).increment(1);
				in = fs.open(filepath);
				//cont.getCounter(RecordReaderCounters.G).increment(1);
				BufferedImage img = ImageIO.read(in);
				//cont.getCounter(RecordReaderCounters.H).increment(1);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				//cont.getCounter(RecordReaderCounters.I).increment(1);
				ImageIO.write(img, "jpb", baos);
				//cont.getCounter(RecordReaderCounters.J).increment(1);
				baos.flush();
				//cont.getCounter(RecordReaderCounters.K).increment(1);
				contents = baos.toByteArray();
				//cont.getCounter(RecordReaderCounters.L).increment(1);
				
				
				this.value.set(contents, 0, contents.length);
				//cont.getCounter(RecordReaderCounters.M).increment(1);
			} catch (IOException e) {
				//cont.getCounter(RecordReaderCounters.N).increment(1);
				e.printStackTrace();
			} finally {
				//cont.getCounter(RecordReaderCounters.O).increment(1);
				IOUtils.closeStream(in);
			}
			//cont.getCounter(RecordReaderCounters.P).increment(1);
			processed = true;
			return true;
		}
		//cont.getCounter(RecordReaderCounters.Q).increment(1);
		return false;
	}

}
