package util.robot;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.DoubleConverter;

import util.metrics.Transformer;

public class Pose implements Cloneable{
	public Pose clone() throws CloneNotSupportedException{
		return (Pose) super.clone();
	}
	public static final double ERROR = 0.05;
	@Parameter(names = {"-rx","--robotx"}, description = "initialize robot's X-Axis", required = false, arity = 1, converter = DoubleConverter.class)
	public double X;
	@Parameter(names = {"-ry","--roboty"}, description = "initialize robot's Y-Axis", required = false, arity = 1, converter = DoubleConverter.class)
	public double Y;
	//unit:degree
	@Parameter(names = {"-rh","--robothead"}, description = "initialize robot's Head", required = false, arity = 1, converter = DoubleConverter.class)
	public double H;
	
	
	public Pose(){
		super();
	}
	
	public Pose(double x, double y, double h) {
		super();
		X = x;
		Y = y;
		H = h;
	}

	public Pose(Pose pose) {
		this(pose.X, pose.Y, pose.H);
	}

	public boolean equalsPose(Pose p) {
		double a = Math.sqrt((p.X - this.X)*( p.X - this.X) + (p.Y - this.Y)*( p.Y - this.Y ));
		if(a<0.5)
			return true;
		else
			return false;
	}
	
	public boolean equalsHead(Pose p){
		double d = Pose.compareToHead(this, p);
		if(Math.abs(d)<0.5)
			return true;
		else
			return false;
	}
	
	public boolean equal(Pose p){
		if(!this.equalsPose(p))
			return false;
		else if(!this.equalsHead(p))
			return false;
		else
			return true;
	}
	
	public static double compareToOrientation(Pose src, Pose dst){
		return Pose.compareToHead(src, new Pose(0, 0, Math.toDegrees( ( Math.atan2( dst.Y-src.Y, dst.X-src.X ) ) ) ) );
	}
	
	public static double compareToDistance(Pose src, Pose dst){
		return Math.sqrt( (src.X-dst.X)*(src.X-dst.X)+(src.Y-dst.Y)*(src.Y-dst.Y) );
	}
	
	public static double compareToHead(Pose src, Pose dst){
		//System.out.print(Transformer.checkHeadRange(src.H)+ "degree => "+Transformer.checkHeadRange(dst.H)+"degree = ");
		double d = Transformer.checkHeadRange(dst.H) - Transformer.checkHeadRange(src.H); 
		if(d>0){
			if(d>180)
				return d-360;
			else
				return d;
		}else{
			if(d<=-180)
				return d+360;
			else
				return d;
		}
	}
	
	public static double deltaTheta(double currentTh, double previousTh){
		return (Transformer.checkHeadRange(currentTh-previousTh)+180)%360-180;
	}
	
	@Override
	public String toString() {
		return String.format("%.4f %.4f %.4f", X, Y, H);
	}

}
