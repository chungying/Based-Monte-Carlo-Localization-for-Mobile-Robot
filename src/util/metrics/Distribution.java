package util.metrics;

import java.util.Random;

import robot.VelocityModel;
import samcl.Grid;

public class Distribution {
	public static double sample_normal_distribution(double b){
		return sample_normal_distribution(b,new Random());
	}
	
	public static double sample_normal_distribution(double b, Random random){
		double upper = Math.sqrt(b);
		//double lower = 0-upper;
		
		double rand;
		double result = 0.0;
		
		for (int i = 0; i < 12; i++) {
			rand = (random.nextDouble() * upper *2) - upper;
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
		0.01,0.01
		};
	
	public static void Motion_sampling(Particle p, int orientation, VelocityModel u, double deltaT){
		Motion_sampling(p, orientation, u, deltaT, new Random());
	}
	
	/**
	 * @param p ready to use motion sampling
	 * @param orientation 
	 * @param u the robot's velocity model
	 * @param deltaT miliseconds
	 */
	public static void Motion_sampling(Particle p, int orientation, VelocityModel u, double deltaT, Random random){
		
		//formula
		double Vcup = u.velocity + 
				Distribution.sample_normal_distribution(al[0]*u.velocity*u.velocity + al[1]*u.angular_velocity*u.angular_velocity, random);
		double Wcup = u.angular_velocity + 
				Distribution.sample_normal_distribution(al[2]*u.velocity*u.velocity + al[3]*u.angular_velocity*u.angular_velocity, random);
		double Rcup =  Distribution.sample_normal_distribution(al[4]*u.velocity*u.velocity + al[5]*u.angular_velocity*u.angular_velocity, random);
		
		//avoid arithmetical error, such as divide by ZERO.
		if(Wcup==0.0){
			Wcup = 4.9e-324;
		}
		if(Vcup==0.0){
			Vcup = 4.9e-324;
		}
		
		//formula
		double temp = p.getX();
		double noise =  ( Vcup/Wcup ) *(
						 Math.sin( Math.toRadians( p.getTh() + Wcup*deltaT ) ) 
						-  Math.sin( Math.toRadians( p.getTh() ) ) 
						);
		//add some disturbances when u is static.
		if(noise==0.0){
			noise = Distribution.sample_normal_distribution(1);
		}
		p.setX((int)Math.round(temp + noise));
		
		//formula
		temp = p.getY();
		noise =  ( Vcup/Wcup ) *( 
						 Math.cos( Math.toRadians( p.getTh() ) ) 
						-  Math.cos( Math.toRadians( p.getTh() + Wcup*deltaT ) )  
						);
		//add some disturbances when u is static.
		if(noise==0.0){
			noise = Distribution.sample_normal_distribution(1);
		}
		p.setY((int)Math.round(temp + noise));
		
		//formula
		temp = p.getTh();
		noise = Wcup*deltaT + Rcup*deltaT;
		//add some disturbances when u is static.
		if(noise==0.0){
			noise = Distribution.sample_normal_distribution(5);
		}
		p.setTh(temp+noise);
		
	}
	
	public static boolean boundaryCheck(Particle p, Grid grid){
		//check boundary
		if(p.getX()>grid.width || p.getX()<0 || p.getY()>grid.height || p.getY()<0){
			return false;
		}
		//check if it is occupied
		if(grid.map_array(p.getX(), p.getY())==Grid.GRID_OCCUPIED){
			return false;
		}
		return true;
	}
	
}
