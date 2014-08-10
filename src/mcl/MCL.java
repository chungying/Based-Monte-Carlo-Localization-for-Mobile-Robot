package mcl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles;
import org.apache.hadoop.util.ToolRunner;

import com.google.protobuf.ServiceException;

import samcl.SAMCL;
import util.metrics.Command;
import util.metrics.Particle;
import util.metrics.Transformer;

public class MCL extends SAMCL{
	
	public static void main(String[] args) throws IOException, InterruptedException{
		String timeStamp = "1407497724272";
		String path = "/user/"+System.getProperty("user.name")+"/hfiles/";
		String bulkLoad = "hbase org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles";
		String tableName = "map.512.4.split";
//		System.setProperty("user.name", "hbase");
//		System.out.println(Command.excuteCommand(
//				bulkLoad+" "
//				+path+tableName+"/"+timeStamp+" "
//				+tableName
//				));
		Configuration conf = HBaseConfiguration.create();
		String[] arg = {path+tableName+"/"+timeStamp, tableName};
		long time = System.currentTimeMillis();
		try {
			int exit = ToolRunner.run(new LoadIncrementalHFiles(conf), arg);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("time: "+ (System.currentTimeMillis()-time) + " ms");
		/*System.out.println(Command.excuteCommand(
				"sudo -u hdfs hadoop fs -ls "+
		"/user/w514/hfiles/map.512.4.split/1407497724272"));
		System.out.println(Command.excuteCommand(
				"sudo -u hdfs hadoop fs -chown -R hbase:hadoop "+
		"/user/w514/hfiles/map.512.4.split/1407497724272"));
		System.out.println(Command.excuteCommand(
				"sudo -u hdfs hadoop fs -ls "+
		"/user/w514/hfiles/map.512.4.split/1407497724272"));*/
	}
	
	@Override
	public Particle Determining_size(List<Particle> src) {
		this.Nl = this.Nt;
		this.Ng = 0;
		return Transformer.minParticle(src);
	}

	@Override
	public void Global_drawing(List<Particle> src, List<Particle> dst) {
		//Do nothing in MCL
		//super.Global_drawing(src, dst);
	}

	@Override
	public void Caculating_SER(float weight, float[] Zt, List<Particle> SER_set)
			throws IOException {
//		// TODO Auto-generated method stub
//		super.Caculating_SER(weight, Zt, SER_set);
	}

	@Override
	public void batchWeight(List<Particle> src, float[] robotMeasurements)
			throws IOException, ServiceException {
		for(Particle p : src){
			float[] m = this.grid.getMeasurementsOnTime(p.getX(), p.getY(), Transformer.th2Z(p.getTh(), this.orientation));
			p.setMeasurements(m);
			this.WeightParticle(p, robotMeasurements);
		}
	}

	public MCL(boolean cloud, int orientation, String mapFilename,
			float deltaEnergy, int nt, float xI, float aLPHA,
			int tournamentPresure) throws IOException {
		super(cloud, orientation, mapFilename, deltaEnergy, nt, xI, aLPHA,
				tournamentPresure);
	}

}
