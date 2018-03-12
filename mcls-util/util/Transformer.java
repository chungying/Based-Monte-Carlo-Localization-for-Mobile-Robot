package util;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import util.pf.Particle;
import util.pf.sensor.laser.LaserModel;
import util.pf.sensor.laser.LaserModel.LaserModelData;

public class Transformer {
	public static Random randomSeed = new Random();
	public static byte[] FA2BA(List<Float> FA){
		byte[] BA = new byte[0];
		for(float f: FA){
			BA = Bytes.add(BA, Bytes.toBytes(f));
		}
		return BA;
	}
	
	public static byte[] FA2BA(float[] FA){
		byte[] BA = new byte[0];
		for(float f: FA){
			BA = Bytes.add(BA, Bytes.toBytes(f));
		}
		return BA;
	}
	
	public static byte[] getBA(int i, byte[] BA) throws Exception{

		return Arrays.copyOfRange(BA, i*4, i*4+4);
	}
	
	public static List<Float> BA2FA(int offset, byte[] BA){
		List<Float> result = new ArrayList<Float>();
		for(int i = offset ; i < BA.length ; i+=4){
			result.add(
					Bytes.toFloat(
							Arrays.copyOfRange(BA, i, i+4)));
		}
		return result;
	}

	public static long result2Array(byte[] familyName, Result result, List<Float> FA) throws Exception{
		long timer = System.currentTimeMillis();
		if(!FA.isEmpty()){
			throw new Exception("the dst array is not empty. there are some bugs.");
		}

		byte[] BA = result.getValue(familyName, Bytes.toBytes("data"));

		FA.addAll( BA2FA( 0, BA));
//		for(int i = 0 ; i*4 < BA.length; i++){
//			FA.add(Bytes.toFloat(
//					Transformer.getBA(i, BA)));
//		}

		return System.currentTimeMillis() - timer;
			
		}

	static public Put createPut(byte[] row, byte[] family, byte[] qualifier, byte[] value){
		Put put = new Put(row);
		put.setDurability(Durability.SKIP_WAL);
		put.add(family, qualifier, value);
		return put;
	}
	
	static public void debugMode(boolean mode,Object... obs){
		if(mode){
			for(Object ob: obs){
				System.out.print(ob.toString());
			}
		}
	}
	
	static public void log(Object... obs){
		log(System.out, obs);
	}
	
	static public void log(PrintStream ps, Object... obs){

			for(Object ob: obs){
				ps.print(ob.toString()+"\t");
			}
			ps.println();
	}
	
	
	static public int th2Z(double head, int orientation){
		return ((int) Math.round( head/(360/orientation) ) )% orientation;
	}
	
	static public double checkAngleRange(double h){
		return h;
	}
	static public double checkHeadRange(double h){
		return (h%360+360)%360;
	}
	
	public static List<Float> drawMeasurements(Float[] circles, int z) {
		int sensor_number = (circles.length/2) +1;
		List<Float> measurements = new ArrayList<Float>();
		//int bias = (sensor_number - 1) / 2;
		int globalIndex;
		for (int sensorIndex = 0; sensorIndex < sensor_number; sensorIndex++) {
			globalIndex = local2global(sensorIndex, z, circles.length);
			//globalIndex = ( (z - bias + i + circles.length) % circles.length );
			measurements.add( circles[globalIndex]);
		}
		return measurements;
	}

	public static float[] drawMeasurements(List<Float> circles, int z) {
		int sensor_number = (circles.size()/2) +1;
		float[] measurements = new float[sensor_number];
		//int bias = (sensor_number - 1) / 2;
		int globalIndex;
		for (int sensorIndex = 0; sensorIndex < sensor_number; sensorIndex++) {
			globalIndex = local2global(sensorIndex, z, circles.size());
			//globalIndex = ( (z - bias + i + circles.size()) % circles.size() );
			measurements[sensorIndex] = circles.get(globalIndex);
		}
		return measurements;
	}

	public static float[] drawMeasurements(float[] circles, int z) {		
		int sensor_number = (circles.length/2) +1;
		float[] measurements = new float[sensor_number];
		//int bias = (sensor_number - 1) / 2;
		int globalIndex;
		for (int sensorIndex = 0; sensorIndex < sensor_number; sensorIndex++) {
			globalIndex = local2global(sensorIndex, z, circles.length);
			//globalIndex = ( (z - bias + i + circles.length) % circles.length );
			measurements[sensorIndex] = circles[globalIndex];
		}
		return measurements;	
	}
	
	public static int local2global(int sensorIndex, int particleHead, int orientation){
		return (
				particleHead + 
				sensorIndex + 
				Math.round( orientation * (360-90)/360 ) 
					)% orientation;
	}

	public static int global2local(int globalIndex, int particleHead, int orientation){
		return (((globalIndex-particleHead-Math.round(orientation*(360-90)/360))%360)+360)%360;
	}

	public static double weightLogBeamModel(List<Float> hypData, LaserModelData data){
		double logSum = 0f;
		List<Float> obsData = data.data.beamranges;
		if(hypData.size() != obsData.size()){
			System.out.println(hypData.size() +", " + obsData.size());
		}
		assert(hypData.size() == obsData.size());
		LaserModel sensor = data.sensor;
		for(int i=0;i< obsData.size();i++){
			 
			double prob =weightSingleBeam(obsData.get(i), hypData.get(i),
					sensor.sigma_hit,
					sensor.lambda_short,
					sensor.range_max,
					new float[]{sensor.z_hit,sensor.z_hit,sensor.z_hit,sensor.z_hit}
					);
			
			logSum += Math.log(prob);
		}
		return logSum;
	}
	
	public static double weightSingleBeam(float obsz, float hypz, float sig_hit, float lamda, float laser_max, float[] z_paras){
		double p = 0;
		//1 gaussian
//		NormalDistribution n = new NormalDistribution(hypz, sig_hit);
//		p += n.density(obsz) / ( n.cumulativeProbability(laser_max)-n.cumulativeProbability(0));
		p += z_paras[0]*( 1.0 / sig_hit * Math.sqrt(2.0 * Math.PI) 
				* Math.exp( (hypz-obsz)*(hypz-obsz) / ( -2*sig_hit*sig_hit ) ) );
		
		//2 short
		if (obsz<hypz)
			p += z_paras[1] * (lamda*Math.exp(-1*lamda*obsz));
		
		//3 maximum
		if (obsz>=laser_max)
			p += z_paras[2];
		
		//4 random failure
		p += z_paras[3] / laser_max;
		assert(p!=0);
		return p;
	}
	
	public static double[] weight_BeamModel(List<Float> hypData, LaserModelData data) {
		double prob = 0.0;
//		double prob = 1.0;
		double logWeight = 0.0;
		List<Float> obsData = data.data.beamranges;
		assert(hypData.size()==obsData.size());
		LaserModel sensor = data.sensor;
		for(int i=0;i< obsData.size();i++){
			double beamW = weightSingleBeam(obsData.get(i), hypData.get(i),
					sensor.sigma_hit,
					sensor.lambda_short,
					sensor.range_max,
					new float[]{sensor.z_hit,sensor.z_hit,sensor.z_hit,sensor.z_hit}
					);
			prob += beamW;
			logWeight += Math.log(beamW);
//			prob *= beamW;
		}
		return new double[]{ prob, logWeight};
	}
	
	/*@Deprecated these two functions are only called by SAMCL's old weighting function.
	 * The weighting function is deleted now.
	public static float weight_BeamModel(float[] obsData, float[]hypData){
		float sig_hit = 4;
		float lambda_short = 0.1f;
		float laser_max = 40;
		float[] z_paras = {0.95f, 0.1f, 0.05f, 0.05f};
		
		float prob = 1.0f;
		for(int i=0;i< obsData.length;i++){
			prob *= weight_SingleBeam(obsData[i], hypData[i],
					sig_hit,
					lambda_short,
					laser_max,
					z_paras
					);
		}
		return prob;
	}
	public static float weight_BeamModel_Gauss(float[] obsData, float[] hypData){
		
		float prob = 1.0f;
		float sig = 1;
		for(int i=0;i< obsData.length;i++){
			float z = obsData[i]-hypData[i];
			//pz = exp(-z^2/2sig^2)
			double pz = Math.exp(-(z*z)/(2*(sig*sig)));
			prob *=pz;
		}
		
		return prob;
	}*/
	
	//optimality changes
	public static float weight_LossFunction(float[] a, float[] b){
		
		//Flowing is the loss of sensor measurement.
		//check if length is equal
		int length = a.length, d=0;
		if(a.length!=b.length){
			//throw new Exception("The length of a array is different from b.");
			d = Math.abs(a.length-b.length);
		}
		
		//start to calculate the weight, importance factor
		float weight = 0;
		//TODO sensor variance shall be calibrated by users of the laser sensor.
		//default value is one.
		float sensor_variance = 1;
		for( int i = 0 ; i < length-d ; i++ ){
			//if(a[i]<0.0f || b[i]<0.0f)
			//	throw new Exception("the value is negative.");
			//loss function
			weight = weight + Math.abs(b[i]-a[i])/sensor_variance;
		}
		weight = weight / length;
		return weight;
	}

	public static double weight_LossFunction(List<Float> Mt, List<Float> Zt) {
		double w = 0.0f;
		for(int i = 0; i < Zt.size(); i++){
			w = w + Math.abs(Mt.get(i)- Zt.get(i));
		}
		w = w / Zt.size();
		return w;
	}
	
	public static float weight_LossFunction(List<Float> a, float[] b) {
		try {
			//check if length is equal
			if(a.size()!=b.length)
				throw new Exception("The lengh of a is different from b."); 
			//start to calculate the weight, importance factor
			float weight = 0;
			for( int i = 0 ; i < a.size() ; i++ ){

				if(a.get(i)>1.0f || b[i]>1.0f)
					throw new Exception("the value is not normalized.");
				//calculating
				weight = weight + Math.abs(b[i]-a.get(i));
			}
			weight = weight / a.size();
			return weight;
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		// return the worst weight
		return 1;
	}
	
	public static float CalculateEnergy(List<Float> measurements,float max_dist) /*throws Exception*/{

		if (measurements == null)
			//throw new Exception("CalculateEnergy: the array is null.");
			return -1;
		float energy = 0.0f;
		for(float m: measurements){
			//energy+=m;
			energy += (1- m/max_dist);
		}
		energy = energy/measurements.size();
		return energy;
	}
	
	/**
	 * Resampling from a cumulative probability table from ROS package, AMCL.
	 */
	public static void resamplingCPT(List<Particle> src, List<Particle> dst){
		dst.clear();
		double[] c = new double[src.size()+1];
		c[0]= .0;
		for(int i = 0; i < src.size() ; ++i){
			c[i+1] = c[i]+src.get(i).getNomalizedWeight();
		}
		//TODO 
		double w = 1.0/src.size();
		
		for(int i = 0 ; i < src.size() ; i++){
			int idx = 0;
			double rand = randomSeed.nextDouble();
			for(;idx<src.size();idx++){
				if(c[idx]<=rand && rand<=c[idx+1]){
					break;
				}
			}
			Particle p = src.get(idx).clone();
			p.setWeightForNomalization(w);
			dst.add(p);
		}
		
		
	}
	
	/**
	 * Low variance resampling from Probabilistic Robotics.
	 * @param src
	 * @param dst
	 */
	public static void resamplingLowVariance(List<Particle> src, List<Particle> dst){
		dst.clear();
		double rand = randomSeed.nextDouble()/src.size();
//		float sumWeight = 0;
		double sumW = 0;
		for(Particle p: src){
//			sumWeight+=p.getWeight();
			sumW+=p.getNomalizedWeight();
		}
//		assert(Math.abs(sumW-1.0)<0.1);//sum should be 1 
		if(Math.abs(sumW-1.0)>0.1)
			System.out.println("sum should be 1 and instead: " + sumW);
//		float cWeight = src.get(0).getWeight();
		double cW = src.get(0).getNomalizedWeight();
		int i = 0;
		for(int m = 1; m <= src.size();++m){
			double u = (m-1.0)/src.size();
			u += rand;
			while(  (/*u>cWeight||*/u>cW)
					&&i<src.size()-1){
				i++;
//				cWeight+=src.get(i).getWeight();
				cW+=src.get(i).getNomalizedWeight();
			}
			if(i>=src.size()-1)
				break;
			dst.add(src.get(i).clone());
		}
		while(i>=src.size()-1 && dst.size()<src.size()){
			dst.add(src.get(randomSeed.nextInt(src.size())).clone());
		}
	}

	/**
	 * 
	 * @param tournamentPresure	greater presure, less diversity
	 * @param srcSet		the group ready to be picked up
	 * @return		a picked particle at this time.
	 */
	public static Particle tournament(int tournamentPresure, List<Particle> srcSet) {
		List<Particle> temp_set = new ArrayList<Particle>();
		temp_set.clear();
		int random ;
		Random r = new Random();
		for(int j = 0;j<tournamentPresure;j++){
			random = r.nextInt(srcSet.size());
			temp_set.add(srcSet.get(random));
		}
		Particle tempp = maxParticle(temp_set);	
		return tempp;
	}
	
	public static Particle maxParticle( List<Particle> particles ){
		Particle max_particle = particles.get(0);
//		float max_weight = max_particle.getWeight();
		double maxW = max_particle.getNomalizedWeight();
		for (int i = 1; i < particles.size(); i++) {
			if (/*max_weight < particles.get(i).getWeight() ||*/
					maxW < particles.get(i).getNomalizedWeight()) {
				max_particle = particles.get(i);
//				max_weight = max_particle.getWeight();
				maxW = max_particle.getNomalizedWeight();
			}
		}
		return max_particle.clone();
	}
	
	public static Particle minParticle( List<Particle> srcSet ){
		Particle min_particle = srcSet.get(0);
//		float min_weight = min_particle.getWeight();
		double minW = min_particle.getNomalizedWeight();
		for (int i = 1; i < srcSet.size(); i++) {
			if (/*min_weight > srcSet.get(i).getWeight() ||*/
					minW>srcSet.get(i).getNomalizedWeight()
					) {
				min_particle = srcSet.get(i);
//				min_weight = min_particle.getWeight();
				minW = min_particle.getNomalizedWeight();
			}
		}
		
		return min_particle.clone();
	}

	static public String xy2String(int x, int y){
		return String.format("%05d", x)+String.format("%05d", y);
	}

	static public String xy2String(double x, double y){
		return String.format("%05d", Math.round(x))+String.format("%05d", Math.round(y));
	}

	static public String xy2String(float x, float y){
		return String.format("%05d", Math.round(x))+String.format("%05d", Math.round(y));
	}
	
	public static String getHash(Particle p, Random random){
		return getHash(Long.parseLong(xy2String(p.getX(),p.getY())), random);
	}
	
	public static void filterParticle(List<Particle> src){
		Random random = new Random();
		TreeMap<String, Particle> map = new TreeMap<String, Particle>();
		for(Particle p: src){
			map.put(xy2RowkeyString(p.getDX(),p.getDY(), random), p);
		}
		src.clear();
		src.addAll(map.values());
	}
	
	static final String separator = ":";
	public static String getHash(long l, Random random){
		random.setSeed(l);
		return String.format("%04d", random.nextInt(1000));
	}
	
	public static String xy2RowkeyString(long l, String str, Random random){
		String rand = getHash(l, random);
		return rand+separator+str;
	}
	
	public static String xy2RowkeyString( double X, double Y , Random random){
		return xy2RowkeyString((int)Math.round(X), (int)Math.round(Y), random);
	}
	
	public static String xy2RowkeyString( double X, double Y){
		return xy2RowkeyString(X, Y, new Random());
	}
	
	public static String xy2RowkeyString( float X, float Y , Random random){
		return xy2RowkeyString(Math.round(X), Math.round(Y), random);
	}
	
	public static String xy2RowkeyString( float X, float Y){
		return xy2RowkeyString(X, Y, new Random());
	}
	
	public static String xy2RowkeyString( int X, int Y , Random random){
		String str = xy2String(X,Y);
		return xy2RowkeyString(Long.parseLong(str), str, random);
	}
	
	public static String xy2RowkeyString( int X, int Y){
		return xy2RowkeyString(X, Y, new Random());
	}
	
	/**
	 * @param rowkey is the form of "....:0000000000" where "." is a character and "0" is a number.
	 * @param p is the Particle stored the X and Y from rowkey.
	 */
	public static void rowkeyString2xy(String rowkey, Particle p){
		String str = rowkey.replaceAll("...."+separator, "");
		p.setX(Integer.valueOf( str.substring(0, 5) ));
		p.setY(Integer.valueOf( str.substring(5,10) ));
	}
	
	public static String rowkeyString2Hash(String rowkey) {
		return rowkey.split(separator)[0];
	}

	public static int rowkeyString2X(String rowkey){
		return Integer.valueOf(rowkey.replaceAll("...."+separator, "").substring(0, 5));
	}
	
	public static int rowkeyString2Y(String rowkey){
		return Integer.valueOf(rowkey.replaceAll("...."+separator, "").substring(5, 10));
	}
	
	
	public static void main(String[] args) throws IOException{
		double t1, t2, t3;
		
		//checkHeadRange(double) test
		t1 = 359;
		t2 = 1; 
//		System.out.println(checkHeadRange(t2-t1));//-358, ans: 2
		System.out.println(((t2-t1)+180)%360-180);//1-359, 2
		System.out.println((checkHeadRange(t2-t1)+180)%360-180);//1-359, 2
		System.out.println((checkHeadRange(checkHeadRange(t2)-checkHeadRange(t1))+180)%360-180);//1-359, 2
		
		System.out.println();
		t3 = t1;
		t1 = t2;
		t2 = t3;
//		System.out.println(checkHeadRange(t2-t1));//358, ans: -2
		System.out.println(((t2-t1)+180)%360-180);//359-1, -2
		System.out.println((checkHeadRange(t2-t1)+180)%360-180);//359-1, -2
		System.out.println((checkHeadRange(checkHeadRange(t2)-checkHeadRange(t1))+180)%360-180);//359-1, -2
					
		
		
		//extends test
		/*String[] test = {
				"-rx","25"
				,"-ry","50"
				};
		PoseTest pt = new PoseTest();
		
		JCommander jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(pt);
		jc.parse(test);
		System.out.println(pt);
		System.out.println((Pose)pt.getPose());*/
		
		
		//string test
		/*double x = 12345.12345;
		System.out.println(String.format("%.4f",x));*/

		//System.out.println(String.format("%05d", Math.round(x)));
		
		
		/*List<Long> t1 = new ArrayList<Long>();
		for(int i = 0 ; i< 5; i++){
			t1.add(System.currentTimeMillis()+i);
		}
		List<Long> t2 = new ArrayList<Long>();
		for(int i =3;i<t1.size();i++){
			t2.add(t1.get(i));
		}
		System.out.println("before");
		System.out.println(Arrays.toString(t1.toArray()));
		System.out.println(Arrays.toString(t2.toArray()));
		for(int i = 0 ;i<t2.size();i++){
			Long l = t2.get(i);
			System.out.print(l);
			t2.get(i)
			System.out.println("+1="+l);
			
		}
		System.out.println("after");
		System.out.println(Arrays.toString(t1.toArray()));
		System.out.println(Arrays.toString(t2.toArray()));*/

		
		/*
		Random random = new Random();
		int x = 1156;
		int y = 5765;
		String str = xy2RowkeyString(x,y, random);
		String str2 = xy2String(x+1,y+1);
		System.out.println("str:"+str);
		System.out.println("str2:"+str2);
		Particle p = new Particle(0, 0, 0);
		System.out.println("1\t"+p.toString());
		rowkeyString2xy(str, p);
		System.out.println("str\t"+p.toString());
		rowkeyString2xy(str2, p);
		System.out.println("str2\t"+p.toString());
		System.out.println(" str x:" + rowkeyString2X(str));
		System.out.println(" str y:" + rowkeyString2Y(str));
		System.out.println(" str2 x:" + rowkeyString2X(str2));
		System.out.println(" str2 y:" + rowkeyString2Y(str2));
		System.out.println(str);
		System.out.println(str2);
		System.out.println(str.replaceAll("...."+separator, ""));
		System.out.println(str2.replaceAll("...."+separator, ""));
		*/
	}

	public static double Z2Th(int z, int orientation) {
		return checkHeadRange(z*360/orientation);
	}
	
	//TODO what's this?
	public static int Energy2Count(float energy, List<Float> curve){
		int i = 0;
		for(Float f: curve){
			if(f>energy)
				;
			i=i+1;
		}
		return 0;
	}
	
}
