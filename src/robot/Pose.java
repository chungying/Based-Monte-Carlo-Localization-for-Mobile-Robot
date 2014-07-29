package robot;

import util.metrics.Transformer;

public class Pose {
	public static final double ERROR = 0.15;
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
	
	@Override
	public String toString() {
		return "Pose [\t" + X + "\t" + Y + "\t" + H + "\t]";
	}

	public static void main(String[] args){
		Transformer.log(
		Pose.compareToOrientation(	new Pose(0,0,0), new Pose(0,-1,359)),"\n",
		Pose.compareToOrientation( new Pose(0,0,90), new Pose(0,1,270)),"\n",
		Pose.compareToOrientation( new Pose(0,0,90), new Pose(1,0,180)),"\n",
		Pose.compareToOrientation( new Pose(0,0,90), new Pose(-1,0,90)),"\n",
		Pose.compareToOrientation(	new Pose(0,0,1), new Pose(1,1,1)),"\n",
		Pose.compareToOrientation( new Pose(0,0,271), new Pose(1,-1,1)),"\n",
		Pose.compareToOrientation( new Pose(0,0,181), new Pose(-1,1,1)),"\n",
		Pose.compareToOrientation( new Pose(0,0,91), new Pose(-1,-1,1)));
		
	}
}
