package util.metrics;

import util.robot.Pose;

public class PoseTest extends Pose{
	public String test = "lalala";
	public PoseTest() {
		super();
		// TODO Auto-generated constructor stub
	}
	public Pose getPose(){
		return (Pose) this;
	}
	
	
	public String toString(){
		return "Pose [\t" + X + "\t" + Y + "\t" + H + "\t]" + test;
	}
}
