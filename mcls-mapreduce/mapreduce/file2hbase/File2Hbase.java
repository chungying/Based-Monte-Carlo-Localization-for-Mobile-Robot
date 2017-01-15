package mapreduce.file2hbase;

// cc ImportFromFile MapReduce job that reads from a file and writes into a table.
import mapreduce.input.WholeFileInputFormat;
import mapreduce.partitioner.CustomePartitioner;
import mapreduce.type.RectangleWritableComparable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.io.IntWritable;
//import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
//import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

// vv ImportFromFile
public class File2Hbase {
	// ^^ ImportFromFile
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(File2Hbase.class);

	public static final String NAME = "ImportFromFile";

	public static CommandLine parseArgs(String[] args) throws ParseException {

		Options options = new Options();

		Option o = new Option("t", "table", true,
				"table to import into (must exist)");
		o.setArgName("table-name");
		o.setRequired(true);
		options.addOption(o);

		 o = new Option("m", "mappers", true,
		 "the number of mappers");
		 o.setArgName("mappers");
		 o.setRequired(true);
		 options.addOption(o);
		
		 o = new Option("o", "orientation", true,
		 "orientation");
		 o.setArgName("orientation");
		 o.setRequired(true);
		 options.addOption(o);

		o = new Option("i", "input", true, "the directory or file to read from");
		o.setArgName("path-in-HDFS");
		o.setRequired(true);
		options.addOption(o);

		options.addOption("d", "debug", false, "switch on DEBUG log level");

		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage() + "\n");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(NAME + " ", options, true);
			System.exit(-1);
		}

		if (cmd.hasOption("d")) {
			Logger log = Logger.getLogger("mapreduce");
			log.setLevel(Level.DEBUG);
		}

		return cmd;
	}


	public static void main(String[] args) throws Exception {

		Configuration conf = HBaseConfiguration.create();

		// ImportFromFile-7-Args Give the command line arguments to the
		// generic parser first to handle "-Dxyz" properties.
		String[] otherArgs = new GenericOptionsParser(conf, args)
				.getRemainingArgs();
		CommandLine cmd = parseArgs(otherArgs);
		// ^^ ImportFromFile
		// check debug flag and other options
		if (cmd.hasOption("d"))
			conf.set("conf.debug", "true");
		// get details
		// vv ImportFromFile

		String table = cmd.getOptionValue("t");
		String input = cmd.getOptionValue("i");
		String reducers = cmd.getOptionValue("m");
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
		
		job.setReducerClass(Reducer2Hbase.class);
		
		//job.setOutputFormatClass(NullOutputFormat.class);
		job.setOutputFormatClass(TableOutputFormat.class);
		job.getConfiguration().set(TableOutputFormat.OUTPUT_TABLE, table);
		job.setOutputKeyClass(ImmutableBytesWritable.class);
		job.setOutputValueClass(Put.class);
		//job.getConfiguration().set(TableOutputFormat.OUTPUT_TABLE, table);
		
		System.out.println("mapreduce.job.reduces = "+
				job.getConfiguration().get("mapreduce.job.reduces", "null"));
		
		/*System.out.println("mapreduce.job.ubertask.enable = "+
				job.getConfiguration().get("mapreduce.job.ubertask.enable", "null"));
			
		job.getConfiguration().set("mapreduce.job.ubertask.enable", "true");
		
		System.out.println("mapreduce.job.ubertask.enable = "+
				job.getConfiguration().get("mapreduce.job.ubertask.enable", "null"));
		
		System.out.println("mapreduce.job.ubertask.maxmaps = "+
				job.getConfiguration().get("mapreduce.job.ubertask.maxmaps", "null"));
		
		System.out.println("mapreduce.job.ubertask.maxreduces = "+
				job.getConfiguration().get("mapreduce.job.ubertask.maxreduces", "null"));
		
		job.getConfiguration().set("mapreduce.job.ubertask.maxreduces", "9");
		
		System.out.println("mapreduce.job.ubertask.maxreduces = "+
				job.getConfiguration().get("mapreduce.job.ubertask.maxreduces", "null"));*/
				
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}

// ^^ ImportFromFile
