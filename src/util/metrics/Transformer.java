package util.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.hbase.util.Bytes;

public class Transformer {
	static public int th2Z(double head, int orientation, double orientation_delta_degree){
		return ((int) Math.round( head/orientation_delta_degree ) )% orientation;
	}
	
	static public double checkHeadRange(double h){
		return (h%360+360)%360;
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
	
	/**
	 * 
	 * @param tournament_presure2	greater presure, less diversity
	 * @param last_set2		the group ready to be picked up
	 * @return		a picked particle at this time.
	 */
	public static Particle tournament(int tournament_presure2, List<Particle> last_set2) {
		List<Particle> temp_set = new ArrayList<Particle>();
		temp_set.clear();
		int random ;
		Random r = new Random();
		for(int j = 0;j<tournament_presure2;j++){
			random = r.nextInt(last_set2.size());
			temp_set.add(last_set2.get(random));
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
		return max_particle;
	}
	
	public static Particle minParticle( List<Particle> last_set2 ){
		Particle min_particle = last_set2.get(0);
		float min_weight = min_particle.getWeight();
		for (int i = 1; i < last_set2.size(); i++) {
			if (min_weight > last_set2.get(i).getWeight()) {
				min_particle = last_set2.get(i);
				min_weight = min_particle.getWeight();
			}
		}
		
		return min_particle;
	}
	@Deprecated
	static public String XY2String(int X, int Y){
		String str = "("+String.valueOf(10000 + Y)+","+String.valueOf(10000 + X)+")";
		return str;
	}

	static public String xy2String(int X, int Y){
		return String.format("%05d", X)+String.format("%05d", Y);
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
	
	public static void main(String[] args) throws IOException{
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
		
//		System.out.println(str);
//		System.out.println(str2);
//		System.out.println(str.replaceAll("...."+separator, ""));
//		System.out.println(str2.replaceAll("...."+separator, ""));
		
	}
	
}
