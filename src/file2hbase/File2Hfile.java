package file2hbase;

import org.apache.commons.cli.CommandLine;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat;
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat2;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class File2Hfile {
	
	public static void main(String[] args) throws Exception {

		Configuration conf = HBaseConfiguration.create();

		// ImportFromFile-7-Args Give the command line arguments to the
		// generic parser first to handle "-Dxyz" properties.
		String[] otherArgs = new GenericOptionsParser(conf, args)
				.getRemainingArgs();
		CommandLine cmd = File2Hbase.parseArgs(otherArgs);
		// ^^ ImportFromFile
		// check debug flag and other options
		if (cmd.hasOption("d"))
			conf.set("conf.debug", "true");
		// get details
		// vv ImportFromFile

		String table = cmd.getOptionValue("t");
		String input = cmd.getOptionValue("i");
		String reducers = cmd.getOptionValue("r");
		String orientation = cmd.getOptionValue("o");
		System.out.println(input);
		
		// ImportFromFile-8-JobDef Define the job with the required classes.
		@SuppressWarnings("deprecation")
		Job job = new Job(conf, "Import from file " + input + "through " + reducers + "reducer(s)" + " into table " + table);
		// ((JobConf)job.getConfiguration()).setJar("/home/w514/iff.jar");
		job.setJarByClass(File2Hbase.class);
		
		FileInputFormat.addInputPath(job, new Path(input));
		job.getConfiguration().set("conf.input", input);
		job.setInputFormatClass(WholeFileInputFormat.class);
		
		job.setMapperClass(File2Mapper.class);
		
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(RectangleWritableComparable.class);
		
		job.setPartitionerClass(CustomePartitioner.class);
		job.getConfiguration().set("conf.orientation", orientation);
		job.setNumReduceTasks(Integer.parseInt(reducers));
		
		job.getConfiguration().set("conf.table", table);
		job.getConfiguration().set("conf.family.distance", "distance");
		job.getConfiguration().set("conf.family.energy", "energy");
		job.getConfiguration().set("conf.family.laserpoint.x", "laserpoint.x");
		job.getConfiguration().set("conf.family.laserpoint.y", "laserpoint.y");
		
		job.setReducerClass(Reducer2Hfile.class);
		
		job.setOutputFormatClass(HFileOutputFormat2.class);
		FileOutputFormat.setOutputPath(job, new Path("/user/w514/hfileTableTest"));
		HTable hTable = new HTable(conf, table);
		HFileOutputFormat2.configureIncrementalLoad(job, hTable);
		
		
		System.out.println("mapreduce.job.reduces = "+
				job.getConfiguration().get("mapreduce.job.reduces", "null"));

		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
