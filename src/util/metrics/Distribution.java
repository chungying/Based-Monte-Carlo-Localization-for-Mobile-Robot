package util.metrics;

import java.util.Random;

import robot.VelocityModel;

public class Distribution {
	public static double sample_normal_distribution(double b){
		double upper = Math.sqrt(b);
		//double lower = 0-upper;
		
		double rand;
		double result = 0.0;
		
		for (int i = 0; i < 12; i++) {
			rand = (Math.random() * upper *2) - upper;
			result = result + rand;
		}
		
		return result/2;
	}
	
	public static int random(int min, int max){
		Random rand = new Random();
		int result = min + rand.nextInt(max - min + 1); 
		return result;
	}
	
	public static double[] al = {
		0.0001,0.0001,
		0.1,0.1,
		0.001,0.001
		};
	/**
	 * @param p ready to use motion sampling
	 * @param orientation 
	 * @param u the robot's velocity model
	 * @param deltaT miliseconds
	 */
	public static void Motion_sampling(Particle p, int orientation, VelocityModel u, double deltaT){
		
		
		double Vcup = u.velocity + 
				Distribution.sample_normal_distribution(al[0]*u.velocity*u.velocity + al[1]*u.angular_velocity*u.angular_velocity);
		double Wcup = u.angular_velocity + 
				Distribution.sample_normal_distribution(al[2]*u.velocity*u.velocity + al[3]*u.angular_velocity*u.angular_velocity);
		double Rcup =  Distribution.sample_normal_distribution(al[4]*u.velocity*u.velocity + al[5]*u.angular_velocity*u.angular_velocity);
		
		if(Wcup==0.0){
			Wcup = 4.9e-324;
		}
		if(Vcup==0.0){
			Vcup = 4.9e-324;
		}
		
		double temp = p.getX();
		double noise =  ( Vcup/Wcup ) *(
						 Math.sin( Math.toRadians( p.getTh() + Wcup*deltaT ) ) 
						-  Math.sin( Math.toRadians( p.getTh() ) ) 
						);
		if(noise==0.0){
			noise = Distribution.sample_normal_distribution(1);
		}
		p.setX((int)Math.round(temp + noise));
		
		temp = p.getY();
		noise =  ( Vcup/Wcup ) *( 
						 Math.cos( Math.toRadians( p.getTh() ) ) 
						-  Math.cos( Math.toRadians( p.getTh() + Wcup*deltaT ) )  
						);
		if(noise==0.0){
			noise = Distribution.sample_normal_distribution(1);
		}
		p.setY((int)Math.round(temp + noise));
		
		temp = p.getTh();
		noise = Wcup*deltaT + Rcup*deltaT;
		p.setZ(Transformer.th2Z(temp + noise, orientation, 360/orientation));
	}
	
}
