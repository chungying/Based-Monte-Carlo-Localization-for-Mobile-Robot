package util.metrics;

public class Transformer {
	
	static public String XY2String(int X, int Y){
		String str = "("+String.valueOf(10000 + Y)+","+String.valueOf(10000 + X)+")";
		return str;
	}
	
	static public int th2Z(double head, int orientation, double orientation_delta_degree){
		return ((int) Math.round( head/orientation_delta_degree ) )% orientation;
	}
	
	static public double checkHeadRange(double h){
		return (h+720)%360;
	}

	public static float[] getMeasurements(float[] circles, int z) {
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
	
}
