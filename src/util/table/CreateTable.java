package util.table;

import com.beust.jcommander.Parameter;

public class CreateTable extends Base{
	@Parameter(names = {"-i","--input"}, description = "the path of image, default is \"hdfs:///user/eeuser/jpg/sim_map.jpg\"", required = true)
	public String tableName = "hdfs:///user/eeuser/jpg/sim_map.jpg";

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
}
