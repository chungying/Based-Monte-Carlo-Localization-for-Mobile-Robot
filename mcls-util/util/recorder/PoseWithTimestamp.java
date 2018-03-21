package util.recorder;

import java.sql.Time;

import util.robot.Pose;

public class PoseWithTimestamp extends Pose{
	public Time stamp;
	
	public PoseWithTimestamp(Pose p, Time t){
		this(t);
		this.X = p.X;
		this.Y = p.Y;
		this.H = p.H;
	}
	
	public PoseWithTimestamp(Time t){
		this.stamp = new Time(t.getTime());
	}
	
	public Time getTimeStampe(){
		return stamp;
	}
	
	public PoseWithTimestamp clone() {
		PoseWithTimestamp clone = new PoseWithTimestamp(this.stamp);
		clone.X = this.X;
		clone.Y = this.Y;
		clone.H = this.H;
		return clone;
	}
}
