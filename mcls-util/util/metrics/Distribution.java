package util.metrics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JPanel;

import util.grid.Grid;
import util.gui.Panel;
import util.gui.Tools;
import util.robot.VelocityModel;

public class Distribution {
	
	public static void main(String[] args){
		Map<Integer,Integer> map = new TreeMap<Integer, Integer>();
		int n = 1000000;
		int w = 1000;
		int h = 1000;
		for(int i = 0 ; i < n;i++){
			double d = sample_normal_distribution(1);
			int index = (int) (d*w/10);
			if(map.get(index)!=null){
				map.put(index,map.get(index)+1);
			}else{
				map.put(index,1);
			}
		}
		JFrame window = new JFrame("test");
		
		window.setSize(w,h);
		Panel panel = new Panel(new BufferedImage(w,h, BufferedImage.TYPE_INT_ARGB));
		Graphics2D grap = panel.img.createGraphics();
		for(Entry<Integer, Integer> e:map.entrySet()){
			System.out.println(e);
			Tools.drawPoint(grap, (int)Math.round(e.getKey()+w/2), (int)Math.round((double)e.getValue()/10000*(double)h), 0, 5, Color.BLACK);
		}
		panel.repaint();
		window.add(panel);
		window.setVisible(true);
		
		
	}
	
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
		100,100,
		100,100,
		10,10
		};
	
	public static void MotionSampling(Particle p, VelocityModel u, double deltaT){
		MotionSampling(p,  u, deltaT, new Random());
	}
	
	public static void MotionSampling(Particle p, VelocityModel u, double deltaT, Random random){
		MotionSampling(p,  u, deltaT, random, al);
	}
	
	
	/**
	 * @param p ready to use motion sampling
	 * @param u the robot's velocity model
	 * @param deltaT seconds
	 */
	public static void MotionSampling(Particle p, VelocityModel u, double deltaT, Random random, double[] al){
//		System.out.println("velocity model:"+u);
//		System.out.println("duration:"+deltaT);
//		System.out.println("al:"+al);
		//formula
		double Vcup = u.velocity + 
				Distribution.sample_normal_distribution(al[0]*Math.abs(u.velocity) + al[1]*Math.abs(u.angular_velocity), random);
		double Wcup = u.angular_velocity + 
				Distribution.sample_normal_distribution(al[2]*Math.abs(u.velocity) + al[3]*Math.abs(u.angular_velocity), random);
		double Rcup =  Distribution.sample_normal_distribution(al[4]*Math.abs(u.velocity) + al[5]*Math.abs(u.angular_velocity), random);
		
		//avoid arithmetical error, such as divide by ZERO.
		double noise = 0;
		if(u.angular_velocity!=0.0){
			//formula
			noise =  Vcup/Math.toRadians(Wcup) *(
							+ Math.sin( Math.toRadians( p.getTh() + Wcup*deltaT ) ) 
							- Math.sin( Math.toRadians( p.getTh() ) ) 
							);
			//add some disturbances when u is static.
			if(noise==0.0){
//				noise = Distribution.sample_normal_distribution(1);
			}
			p.setX(p.getX() + noise*deltaT);
			
			//formula
			noise =  Vcup/Math.toRadians(Wcup) *( 
							+ Math.cos( Math.toRadians( p.getTh() ) ) 
							- Math.cos( Math.toRadians( p.getTh() + Wcup*deltaT ) )  
							);
			//add some disturbances when u is static.
			if(noise==0.0){
//				noise = Distribution.sample_normal_distribution(1);
			}
			p.setY(p.getY() + noise*deltaT);
			
			//formula
			noise = Wcup*deltaT + Rcup*deltaT;
			//add some disturbances when u is static.
			if(noise==0.0){
//				noise = Distribution.sample_normal_distribution(5);
			}
			p.setTh(p.getTh()+noise);
		}else{
			//formula
			noise =  Vcup *( Math.cos( Math.toRadians( p.getTh() + Wcup*deltaT ) ) );
			//add some disturbances when u is static.
			if(noise==0.0){
//				noise = Distribution.sample_normal_distribution(1);
			}
			p.setX(p.getX() + noise*deltaT);
			
			//formula
			noise =  Vcup *( Math.sin( Math.toRadians( p.getTh() + Wcup*deltaT ) ));
			//add some disturbances when u is static.
			if(noise==0.0){
//				noise = Distribution.sample_normal_distribution(1);
			}
			p.setY(p.getY() + noise*deltaT);
			
			//formula
			noise = Wcup*deltaT + Rcup*deltaT;
			//add some disturbances when u is static.
			if(noise==0.0){
//				noise = Distribution.sample_normal_distribution(5);
			}
			p.setTh(p.getTh()+noise);
		}
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
