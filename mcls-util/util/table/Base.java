package util.table;

import java.io.Closeable;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;

import com.beust.jcommander.Parameter;

abstract public class Base implements Closeable, Runnable{
	
	@Parameter(names = {"-t","--tableName"}, description = "the name of HBase table, default is \"map.512.4.split\"", required = true)
	public String tableName = "map.512.4.split";
	
	protected HTable table = null;
	protected Configuration conf = null;
	public HConnection connection = null;
	
	public void setup() throws IOException{
		System.out.println("getting the connection manager.....");
		conf = HBaseConfiguration.create();
		this.connection = HConnectionManager.createConnection(conf);
		
		if(this.tableName!=null){
			if(this.table==null){
				System.out.println("assigning table...");
				this.table = this.getTable(tableName);
			}
			else
				System.out.println("the table had assigned.");
		}else
			throw new NullPointerException("there is no table name. Should assign the table name");
	}
	
	public HTable getTable(String tableName) throws IOException{
		return (HTable) this.connection.getTable(tableName);
	}
	
	@Override
	public void close() throws IOException{
		if(this.table!=null)
			this.table.close();
		if(this.connection!=null)
			this.connection.close();
	}
	
	
}
