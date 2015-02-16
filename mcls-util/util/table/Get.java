package util.table;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import util.metrics.Transformer;

public class Get {

	public static CommandLine parseArgs(String[] args){
		Options ops = new Options();
		
		ops.addOption("t", "table", true, "table where to access");
		ops.addOption("x", "X", true, "the X coordination");
		ops.addOption("y", "Y", true, "the Y coordination");
		
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(ops, args);
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage() + "\n");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( Get.class.getName() + " ", ops, true);
			System.exit(-1);
		}
		
		return cmd;
		
	}
	
	public static void main(String[] args) throws IOException {
		String[] test = {
				"-t","test6.18.split",
				"-x","44",
				"-y","190",
				""
		};
		
		args = test;
		
		CommandLine cmd = parseArgs(args);
		
		String tableName = cmd.getOptionValue("t");
		String X = cmd.getOptionValue("x");
		String Y = cmd.getOptionValue("y");
		
		
		int x = Integer.parseInt(X);
		int y = Integer.parseInt(Y);
		String rowKeyStr = Transformer.xy2RowkeyString(x, y);
		System.out.println("turn to RowKey:" + rowKeyStr);
		
		int x2 = Integer.parseInt(X);
		int y2 = Integer.parseInt(Y);
		String rowKeyStr2 = Transformer.xy2RowkeyString(x2+1, y2+1);
		System.out.println("turn to RowKey:" + rowKeyStr2);
		
		System.out.println("Start to get table");
		HTable table = null;
		try{
			Configuration conf = HBaseConfiguration.create();
			table = new HTable(conf , tableName);
			
			org.apache.hadoop.hbase.client.Get get = new org.apache.hadoop.hbase.client.Get(Bytes.toBytes(rowKeyStr));
			Result result = table.get(get);
			for(Entry<byte[], NavigableMap<byte[], byte[]>>  entry1: result.getNoVersionMap().entrySet()){
				for(Entry<byte[], byte[]> entry2: entry1.getValue().entrySet()){
					System.out.println("(\t"+
							Bytes.toString(result.getRow()) + ",\t" + 
							Bytes.toString(entry1.getKey()) + ",\t" +
							Bytes.toString(entry2.getKey()) + "\t)=> " +
							Bytes.toString(entry2.getValue())
							);
				}
			}
			System.out.println(result.toString());
			
			
			try {
				//System.out.println("constant:"+conf.getInt(HConstants.HBASE_REGIONSERVER_LEASE_PERIOD_KEY, -1));
				System.out.println("start sleep");
				Thread.sleep(65000);
				System.out.println("stop sleep");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("check htable if disconnect");
			
			org.apache.hadoop.hbase.client.Get get2 = new org.apache.hadoop.hbase.client.Get(Bytes.toBytes(rowKeyStr2));
			Result result2 = table.get(get2);
			
			for(Entry<byte[], NavigableMap<byte[], byte[]>>  entry1: result2.getNoVersionMap().entrySet()){
				for(Entry<byte[], byte[]> entry2: entry1.getValue().entrySet()){
					System.out.println("(\t"+
							Bytes.toString(result2.getRow()) + ",\t" + 
							Bytes.toString(entry1.getKey()) + ",\t" +
							Bytes.toString(entry2.getKey()) + "\t)=> " +
							Bytes.toString(entry2.getValue())
							);
				}
			}
			System.out.println(result2.toString());
			
		} finally{
			System.out.println("get operation timeout:"+table.getOperationTimeout());
			table.close();
		}
		
	}

}
