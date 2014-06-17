package util.metrics;

import java.util.Random;

public class Distribution {
	public static double sample_normal_distribution(double b){
		double upper = Math.sqrt(b);
		//double lower = 0-upper;
		
		double rand;
		double result = 0.0;
		
		for (int i = 0; i < 12; i++) {
			rand = (Math.random() * upper*2) - upper;
			result = result + rand;
		}
		
		return result;
	}
	
	public static int random(int min, int max){
		Random rand = new Random();
		int result = min + rand.nextInt(max - min + 1); 
		return result;
	}
	
}
