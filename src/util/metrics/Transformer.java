package util.metrics;

public class Transformer {
	
	static public String XY2String(int X, int Y){
		String str = "("+String.valueOf(10000 + Y)+","+String.valueOf(10000 + X)+")";
		return str;
	}
	
	static public int th2Z(double head, double orientation_delta_degree){
		return (int) Math.round( head/orientation_delta_degree );
	}
	
	static public double checkHeadRange(double h){
		return (h+720)%360;
	}
	
}
