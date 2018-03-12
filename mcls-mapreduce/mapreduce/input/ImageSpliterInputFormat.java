package mapreduce.input;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import mapreduce.type.RectangleWritableComparable;

import util.imageprocess.PgmImage;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.util.StringUtils;

public class ImageSpliterInputFormat extends FileInputFormat<Text, RectangleSplit>{
	public static void main(String[] args){
		Path path = new Path("/home/w514/jpg/test6.jpg,/home/w514/jpg/map.jpg");
		System.out.println(path);
		System.out.println(path.getParent());
	}

//	 private static final Log LOG1 = LogFactory.getLog(ImageSpliterInputFormat.class);

	 public static final String MAP_NUMBER = 
			    "mapreduce.input.imagespliterinputformat.map.number";

	protected RectangleSplit makeSplit(Path filePath, long length, String[] hosts
			, int imageWidth, int imageHeight, RectangleWritableComparable rectangle) {
		return new RectangleSplit(filePath, length, hosts, imageWidth, imageHeight, rectangle);
	}

	@Override
	public List<InputSplit> getSplits(JobContext job) throws IOException {
		// generate splits
		List<InputSplit> splits = new ArrayList<InputSplit>();
		List<FileStatus> files = listStatus(job);
		System.out.println("go into the getSplits function!!!!!!");
		for (FileStatus file: files) {
			System.out.println("get the file:" + file.toString());
			Path path = file.getPath();
			long length = file.getLen();
			if (length != 0) {
				System.out.println("the file length:" + length);
				FileSystem fs = path.getFileSystem(job.getConfiguration());
				
				@SuppressWarnings("unused")
				BlockLocation[] blkLocations;
				if (file instanceof LocatedFileStatus) {
					blkLocations = ((LocatedFileStatus) file).getBlockLocations();
				} else {
					fs = path.getFileSystem(job.getConfiguration());
					blkLocations = fs.getFileBlockLocations(file, 0, length);
				}
                                BufferedImage image = null;
				if(path.toString().contains(".pgm") || path.toString().contains(".PGM"))
				{
					image = new PgmImage(path.toString()).img;
				}
				else
				{
					FSDataInputStream inputStream = fs.open(path);
					image = ImageIO.read(inputStream);
					inputStream.close();
				}
				int imageWidth = image.getWidth();
				int imageHeight = image.getHeight();
				//fs.close();
				
				int mapNumber = 1;
				String str = job.getConfiguration().get(MAP_NUMBER);
				if(str!=null){
					mapNumber = Integer.parseInt(str);
					System.out.println("setup the number of map tasks:" + str);
				}else
					System.out.println("didn't setup the number of map tasks, use default number 1.");
				
				int rectangleHeight = Math.round((float)imageHeight/mapNumber)+1;
				
				for(int i = 0; i < mapNumber; i++){
					int y = i*rectangleHeight;
					splits.add(makeSplit(path, length, new String[0]
						, imageWidth, imageHeight
						, new RectangleWritableComparable( 0, y, imageWidth, rectangleHeight)));
				}
			} else {
				System.out.println("the file is empty");
				//Create empty hosts array for zero length files
				splits.add(this.makeSplit(path, length, new String[0], 0, 0, new RectangleWritableComparable()));
			}
		}
		// Save the number of input files for metrics/loadgen
		job.getConfiguration().setLong(NUM_INPUT_FILES, files.size());
		System.out.println("Total # of splits: " + splits.size());
		return splits;
	}

	@Override
	public RecordReader<Text, RectangleSplit> createRecordReader(
			InputSplit split, TaskAttemptContext context) {
		return new RectangleRecordReader();
	}

	
	/**
	 * Sets the path as the input 
	 * for the map-reduce job.
	 * 
	 * @param job the job
	 * @param onlyPath only one path to be set as 
	 *        the input for the map-reduce job.
	 */
	public static void setInputPaths(Job job, 
			String onlyPath) throws IOException {
		setInputPaths(job, new Path(onlyPath));
	}
	
	/**
	 * Set the array of {@link Path}s as the list of inputs
	 * for the map-reduce job.
	 * 
	 * @param job The job to modify 
	 * @param inputPaths the {@link Path}s of the input directories/files 
	 * for the map-reduce job.
	 */ 
	public static void setInputPaths(Job job, Path inputPath) throws IOException {
		Configuration conf = job.getConfiguration();
		Path path = inputPath.getFileSystem(conf).makeQualified(inputPath);
		StringBuffer str = new StringBuffer(StringUtils.escapeString(path.toString()));
		conf.set(INPUT_DIR, str.toString());
	}
	
	/**
	 * Set the number of map tasks.
	 * 
	 * @param job The job to modify.
	 * @param mapNumber the number of map task.
	 */
	public static void setMapTaskNumber(Job job, int mapNumber){
		Configuration conf = job.getConfiguration();
		conf.setInt(MAP_NUMBER, mapNumber);
	}

}
