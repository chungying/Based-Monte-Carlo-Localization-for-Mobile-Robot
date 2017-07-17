package util.metrics;

import java.util.Random;
import util.grid.Grid;
import util.robot.Pose;
import util.robot.VelocityModel;

public class Distribution {
	
	public static void main(String[] args){
		
		
		
		//test word convertor
		/*double x = 1235.00000012;
		System.out.println(x);
		System.out.println(Double.toString(x).substring(0, 5));
		System.out.println(String.format("%.0f", x));*/
		
/*		Map<Integer,Integer> map = new TreeMap<Integer, Integer>();
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
		window.setVisible(true);*/
		
	
		/*
		double x = 0, y = 0, z = 0;
		double v = 1, w = Double.MIN_VALUE, t = 1000;
		System.out.println("MIN_VALUE:"+w);
		double r = v/w;
		System.out.println("radius is:"+r);
		
		x = r * ( 0 
				+ Math.cos(Math.toRadians(z)) 
				- Math.cos( 
					Math.toRadians(	z+ w * t )
				) 
			);
		System.out.println("r	:"+r);
		System.out.println("x	:"+x);
		if(Double.isInfinite(r)){
				System.out.println("r is Infinity");
				r=Double.MAX_VALUE;
		}else
			System.out.println("r isn't Infinity");
		System.out.println("r	:"+r);
		x = r * ( 0 
				+ Math.cos(Math.toRadians(z)) 
				- Math.cos( 
					Math.toRadians(	z+ w * t )
				) 
			);
		System.out.println("x	:"+x);
		System.out.println("z+w*t				:"+(z+w*t));
		System.out.println("radians of z+w*t	:"+Math.toRadians(	z+ w * t ));
		System.out.println("PI/180				:"+(Math.PI/180));
		System.out.println("z+w*t				:"+(z+w*t)*(Math.PI/180));
		System.out.println("cos(z)				:"+Math.cos(Math.toRadians(z)));
		System.out.println("cos(doubleMIN)		:"+Math.cos(z+w*t));
		System.out.println("cos(z+w*t)			:"+Math.cos(Math.toRadians(	z+ w * t )));
		System.out.println("cos(w*t)			:"+Math.cos(w*t));
		*/
	}
	
	

	public static double sample_normal_distribution(double b){
		return sample_normal_distribution(b,new Random());
	}
	
	public static double sample_normal_distribution(double b, Random random){
		double upper = Math.sqrt(Math.abs(b));
		//double lower = 0-upper;
		
		double rand;
		double result = 0.0;
		
		for (int i = 0; i < 12; i++) {
			//rand = (random.nextDouble() * upper *2) - upper;
			rand = (random.nextDouble() *2) - 1;//rand(-1,1)
			result +=rand;
		}
		
		return b*result/6;
	}
	
	public static int random(int min, int max){
		Random rand = new Random();
		int result = min + rand.nextInt(max - min + 1); 
		return result;
	}
	
	public static double[] al = {
		20,20,
		20,20,
		20,20
		};
	
	public static void MotionSampling(Particle p, VelocityModel u, double deltaT){
		MotionSampling(p,  u, deltaT, new Random());
	}
	
	public static void MotionSampling(Particle p, VelocityModel u, double deltaT, Random random){
		MotionSampling(p,  u, deltaT, random, al);
	}
	
	
	/**
	 * 
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
				Distribution.sample_normal_distribution(al[0]*Math.abs(u.velocity)*Math.abs(u.velocity) + al[1]*Math.abs(Math.toRadians(u.angular_velocity))*Math.abs(Math.toRadians(u.angular_velocity)), random);
		double Wcup = Math.toRadians(u.angular_velocity) + 
				Distribution.sample_normal_distribution(al[2]*Math.abs(u.velocity)*Math.abs(u.velocity) + al[3]*Math.abs(Math.toRadians(u.angular_velocity))*Math.abs(Math.toRadians(u.angular_velocity)), random);
		double Rcup =  Distribution.sample_normal_distribution(al[4]*Math.abs(u.velocity)*Math.abs(u.velocity) + al[5]*Math.abs(Math.toRadians(u.angular_velocity))*Math.abs(Math.toRadians(u.angular_velocity)), random);
		
		//avoid arithmetical error, such as divide by ZERO.
		double noise = 0;
		if(Wcup==0.0)
			Wcup=Double.MIN_VALUE;
//		{
			//formula
			noise =  Vcup/Wcup *(
							+ Math.cos( Math.toRadians(p.getTh()) ) 
							- Math.cos( Math.toRadians(p.getTh()) + Wcup*deltaT ) 
							);
			//add some disturbances when u is static.
			if(noise==0.0){
				noise = Double.MIN_VALUE;
//				noise = Distribution.sample_normal_distribution(1);
			}
			p.setX(p.getDX() + noise*deltaT);
			
			//formula
			noise =  Vcup/Wcup *( 
							- Math.sin( Math.toRadians( p.getTh() ) ) 
							+ Math.sin( Math.toRadians( p.getTh() ) + Wcup*deltaT  )  
							);
			//add some disturbances when u is static.
			if(noise==0.0){
				noise = Double.MIN_VALUE;
//				noise = Distribution.sample_normal_distribution(1);
			}
			p.setY(p.getDY() + noise*deltaT);
			
			//formula
			noise = Wcup*deltaT + Rcup*deltaT;
			//add some disturbances when u is static.
			if(noise==0.0){
				noise = Double.MIN_VALUE;
//				noise = Distribution.sample_normal_distribution(5);
			}
			p.setTh(p.getTh() + Math.toDegrees(noise) );
//		}
	 /* else{
			//formula
			noise =  Vcup *( Math.cos( Math.toRadians( p.getTh() ) + Wcup*deltaT ) );
			//add some disturbances when u is static.
			if(noise==0.0){
//				noise = Distribution.sample_normal_distribution(1);
			}
			p.setX(p.getDX() + noise*deltaT);
			
			//formula
			noise =  Vcup *( Math.sin( Math.toRadians( p.getTh() ) + Wcup*deltaT ));
			//add some disturbances when u is static.
			if(noise==0.0){
//				noise = Distribution.sample_normal_distribution(1);
			}
			p.setY(p.getDY() + noise*deltaT);
			
			//formula
			noise = Wcup*deltaT + Rcup*deltaT;
			//add some disturbances when u is static.
			if(noise==0.0){
//				noise = Distribution.sample_normal_distribution(5);
			}
			p.setTh(p.getTh() + Math.toDegrees(noise) );
		}*/
	}

	/**
	 * 
	 * @param p
	 * @param curP
	 * @param preP
	 * @param deltaT
	 * @param random
	 * @param al
	 */
	public static void OdemetryMotionSampling(Particle p, Pose curP, Pose preP, double deltaT, Random random, double[] al) throws Exception{
//		double deltax = u.getVelocity()*Math.cos()
		double xbardelta = curP.X-preP.X;
		double ybardelta = curP.Y-preP.Y;
		if(xbardelta==0)
			xbardelta = Double.MIN_VALUE;
		if(ybardelta==0)
			ybardelta = Double.MIN_VALUE;
		double rot1;
		if (Math.sqrt((curP.X-preP.X)*(curP.X-preP.X)+(curP.Y-preP.Y)*(curP.Y-preP.Y))<0.001){
			rot1 = 0.0;
		}else
			rot1 = Pose.deltaTheta(Transformer.checkHeadRange(Math.toDegrees(Math.atan2(ybardelta,xbardelta))),preP.H);
		double rot2 = Pose.deltaTheta(curP.H, preP.H)-rot1;
		double trans= Math.sqrt(xbardelta*xbardelta+ybardelta*ybardelta);
		
		double rot1c = rot1 - sample_normal_distribution(al[0] * rot1 + al[1] * trans, random);
		double transc= trans- sample_normal_distribution(al[2] * trans + al[3] * (rot1 + rot2), random);
		double rot2c = rot2 - sample_normal_distribution(al[4] * rot2 + al[5] * trans, random);
		
		if(Double.isNaN(rot1c) || Double.isNaN(transc) || Double.isNaN(rot2c))
			throw new Exception("there is NaN!!!!!!!");
		
		p.setX(p.getDX() + transc * Math.cos( 
										Math.toRadians( p.getTh() + rot1c)
									));
		p.setY(p.getDY() + transc * Math.sin( 
				Math.toRadians( p.getTh() + rot1c)
			));
		p.setTh( p.getTh() + rot1c + rot2c );
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
