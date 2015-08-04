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
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

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
		if(args.length == 0)
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
			Logger.getRootLogger().setLevel(Level.WARN);
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
			
			/*
			try {
				//System.out.println("constant:"+conf.getInt(HConstants.HBASE_REGIONSERVER_LEASE_PERIOD_KEY, -1));
				System.out.println("start sleep");
				Thread.sleep(600);
				System.out.println("stop sleep");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("check htable if disconnect");
			
			String startkey = "0.8", endkey = "0.9";
			org.apache.hadoop.hbase.client.Scan scan = new org.apache.hadoop.hbase.client.Scan(Bytes.toBytes(startkey),Bytes.toBytes(endkey));
			float chance = 0.1f;
			scan.setFilter(new RandomRowFilter(chance));
			ResultScanner scanner = table.getScanner(scan);
			int count = 0;
			for(Result result2 = scanner.next(); result2 != null; result2 = scanner.next()){
				count = count + result2.size();
				System.out.println(result2.toString());
			}
			System.out.println("count:"+ count);*/
			/*
			for(Entry<byte[], NavigableMap<byte[], byte[]>>  entry1: result2.getNoVersionMap().entrySet()){
				for(Entry<byte[], byte[]> entry2: entry1.getValue().entrySet()){
					System.out.println("(\t"+
							Bytes.toString(result2.getRow()) + ",\t" + 
							Bytes.toString(entry1.getKey()) + ",\t" +
							Bytes.toString(entry2.getKey()) + "\t)=> " +
							Bytes.toString(entry2.getValue())
							);
				}
			}*/
			
			
		} finally{
			System.out.println("get operation timeout:"+table.getOperationTimeout());
			table.close();
		}
		
	}

}
