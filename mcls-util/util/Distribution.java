package util;

import java.util.Random;

public class Distribution {
	public static Random seed = new Random();
	
	public static double sample_normal_distribution(double variance){
		return sample_normal_distribution(variance, seed);
	}
	
	public static double sample_normal_distribution(double variance, Random random){
		double rand;
		double sum = 0.0;
		
		for (int i = 0; i < 12; i++) {
			//rand = (random.nextDouble() * upper *2) - upper;
			rand = (random.nextDouble() *2) - 1;//rand(-1,1)
			sum +=rand;
		}
		
		return variance*sum/6;
	}
	

}
