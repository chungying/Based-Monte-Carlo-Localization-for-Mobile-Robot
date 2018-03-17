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
		this.stamp = t;
	}
	
	public Time getTimeStampe(){
		return stamp;
	}
}
