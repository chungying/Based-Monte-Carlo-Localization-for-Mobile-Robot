package util.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import com.kenai.jffi.Array;

public class Transformer {
	
	public static List<Float> BA2FA(int offset, byte[] BA){
		List<Float> result = new ArrayList<Float>();
		for(int i = offset ; i < BA.length ; i+=4){
			result.add(
					Bytes.toFloat(
							Arrays.copyOfRange(BA, i, i+4)));
		}
		return result;
	}
	
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
		byte[] result = null;
		try {
			if(BA==null){
				System.out.println("BA is null!!");
				throw new NullPointerException();
			}
			result = Arrays.copyOfRange(BA, i*4, i*4+4);
		} catch (Exception e) {
			System.out.println("i = "+i+",BA = ");
			for(Float f: BA2FA(0,BA)){
				System.out.print(f+",");
			}
			e.printStackTrace();
			
		}
		return result;
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
		for(Object ob: obs){
			System.out.print(ob.toString()+"\t");
		}
		System.out.println();
	}
	
	static public int th2Z(double head, int orientation){
		return ((int) Math.round( head/(360/orientation) ) )% orientation;
	}
	
	
	static public double checkHeadRange(double h){
		return (h%360+360)%360;
	}
	
	public static int local2global(int localIndex, int particleHead, int orientation){
		return (
				particleHead + 
				localIndex + 
				Math.round( orientation * (360-90)/360 ) 
					)% orientation;
	}
	
	public static int global2local(int globalIndex, int particleHead, int orientation){
		return (((globalIndex-particleHead-Math.round(orientation*(360-90)/360))%360)+360)%360;
	}
	
	public static float[] drawMeasurements(Float[] circles, int z) {
		int sensor_number = (circles.length/2) +1;
		float[] measurements = new float[sensor_number];
		int bias = (sensor_number - 1) / 2;
		int index;
		for (int i = 0; i < sensor_number; i++) {
			index = ( (z - bias + i + circles.length) % circles.length );
			measurements[i] = circles[index];
		}
		return measurements;
	}

	public static float[] drawMeasurements(List<Float> circles, int z) {
		int sensor_number = (circles.size()/2) +1;
		float[] measurements = new float[sensor_number];
		int bias = (sensor_number - 1) / 2;
		int index;
		for (int i = 0; i < sensor_number; i++) {
			index = ( (z - bias + i + circles.size()) % circles.size() );
			measurements[i] = circles.get(index);
		}
		return measurements;
	}

	public static float[] drawMeasurements(float[] circles, int z) {		
		int sensor_number = (circles.length/2) +1;
		float[] measurements = new float[sensor_number];
		int bias = (sensor_number - 1) / 2;
		int index;
		for (int i = 0; i < sensor_number; i++) {
			index = ( (z - bias + i + circles.length) % circles.length );
			measurements[i] = circles[index];
		}
		return measurements;	
	}
	

	/*public float Caculate_energy(float[] Zt){//TODO static?
		float energy = 0;
		for (int i = 0; i < Zt.length; i++) {
			
			energy = energy + Zt[i];
		}
		energy = energy / ((float)Zt.length);
		return energy;
	}*/
	
	public static float CalculateEnergy(float[] measurements){
		float energy = 0.0f;
		for(float m: measurements){
			energy+=m;
		}
		energy = energy/measurements.length;
		return energy;
	}

	public static float WeightFloat(float[] a, float[] b){
		try {
			//check if length is equal
			if(a.length!=b.length)
				throw new Exception("The lengh of a is different from b."); 
			//start to calculate the weight, importance factor
			float weight = 0;
			for( int i = 0 ; i < a.length ; i++ ){

				if(a[i]>1.0f || b[i]>1.0f)
					throw new Exception("the value is not normalized.");
				//calculating
				weight = weight + Math.abs(b[i]-a[i]);
			}
			weight = weight / a.length;
			return weight;
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		// return the worst weight
		return 1;
	}
	
	public static float WeightFloat(List<Float> Mt, List<Float> Zt) {
		float w = 0.0f;
		for(int i = 0; i < Zt.size(); i++){
			w = w + Math.abs(Mt.get(i)- Zt.get(i));
		}
		w = w / Zt.size();
		return w;
	}
	
	public static float WeightFloat(List<Float> a, float[] b) {
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
		Particle tempp = minParticle(temp_set);		
		return tempp;
	}
	
	public static Particle maxParticle( List<Particle> particles ){
		Particle max_particle = particles.get(0);
		float max_weight = max_particle.getWeight();
		for (int i = 1; i < particles.size(); i++) {
			if (max_weight <= particles.get(i).getWeight()) {
				max_particle = particles.get(i);
				max_weight = max_particle.getWeight();
			}
		}
		return max_particle.clone();
	}
	
	public static Particle minParticle( List<Particle> srcSet ){
		Particle min_particle = srcSet.get(0);
		float min_weight = min_particle.getWeight();
		for (int i = 1; i < srcSet.size(); i++) {
			if (min_weight > srcSet.get(i).getWeight()) {
				min_particle = srcSet.get(i);
				min_weight = min_particle.getWeight();
			}
		}
		
		return min_particle.clone();
	}

	static public String xy2String(int x, int y){
		return String.format("%05d", x)+String.format("%05d", y);
	}
	
	static final String separator = ":";
	public static String xy2RowkeyString(long l, String str, Random random){
		random.setSeed(l);
		String rand = String.format("%04d", random.nextInt(1000));
		return rand+separator+str;
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
//		for(int i =0; i<=180;i++){
//			log("local="+i+"=>"+local2global(i,0,360));
//		}
		for(int i = 90 ; i<=180 ; i++){
			log("global="+i+"=>"+global2local(i,180,360));
		}
		
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
	
}