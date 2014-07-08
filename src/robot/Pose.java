package robot;

import util.metrics.Transformer;

public class Pose {
	public double X;
	public double Y;
	public double H;
	
	public Pose(double x, double y, double h) {
		super();
		X = x;
		Y = y;
		H = h;
	}

	public boolean equalsPose(Pose p) {
		double a = Math.sqrt((p.X - this.X)*( p.X - this.X) + (p.Y - this.Y)*( p.Y - this.Y ));
		if(a<0.5)
			return true;
		else
			return false;
	}
	
	public boolean equlsHead(Pose p){
		double d = Math.sqrt((p.H-this.H+360)*(p.H-this.H+360));
		if(Math.abs(d-360)<0.5)
			return true;
		else
			return false;
	}
	
	public boolean eauals(Pose p){
		if(!this.equalsPose(p))
			return false;
		else if(!this.equlsHead(p))
			return false;
		else
			return true;
	}
	
	public static double compareToOrientation(Pose src, Pose dst){
		return (Math.toDegrees((Math.atan2(dst.Y-src.Y, dst.X-src.X)))+360)%360;
	}
	
	public static double compareToDistance(Pose src, Pose dst){
		return Math.sqrt( (src.X-dst.X)*(src.X-dst.X)+(src.Y-dst.Y)*(src.Y-dst.Y) );
	}
	
	public static double compareToHead(Pose src, Pose dst){
		double a = Transformer.checkHeadRange(src.H);
		System.out.print("a:"+a+ " ");
		double b = Transformer.checkHeadRange(dst.H);
		System.out.print("b:"+b);
		
		
		if(Math.abs(b-a) <= 180 ) 
			return Transformer.checkHeadRange(b-a);//positive
		else
			return (b-a-360)%360;//negative
	}
	
	public static void main(String[] args){
		System.out.println("=>"+Pose.compareToHead(	new Pose(0,0,1), 
													new Pose(0,0,359)));
		System.out.println("=>"+Pose.compareToHead( new Pose(0,0,1), 
				  							  		new Pose(0,0,270)));
		System.out.println("=>"+Pose.compareToHead( new Pose(0,0,1), 
				  							  		new Pose(0,0,180)));
		System.out.println("=>"+Pose.compareToHead( new Pose(0,0,1), 
				  							  		new Pose(0,0,90)));
		
		System.out.println("=>"+Pose.compareToHead(	new Pose(0,0,359), 
													new Pose(0,0,1)));
		System.out.println("=>"+Pose.compareToHead( new Pose(0,0,270), 
			  										new Pose(0,0,1)));
		System.out.println("=>"+Pose.compareToHead( new Pose(0,0,180), 
			  										new Pose(0,0,1)));
		System.out.println("=>"+Pose.compareToHead( new Pose(0,0,90), 
			  										new Pose(0,0,1)));
		
	}
	
}
