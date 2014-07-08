package robot;

import java.util.ArrayList;
import java.util.List;

public class PathPlan {
	public static final double standardAngularVelocity = 15;// degree/second
	public static final double standardVelocity = 10;// pixel/second
	private Pose[] path = null;
	private int currentPose;
	public boolean nextPose(RobotState robot){
		if(path[currentPose+1].eauals(robot.getPose())){
			robot.stop();
			robot.setPose(path[currentPose]);
			currentPose++;
			this.setVelocityModel(robot, path[currentPose+1]);
			return true;
		}
		else{
			return false;
		}
	}
	
	private void setVelocityModel(RobotState robot, Pose dst){
		if(!robot.getPose().equlsHead(dst)){
			if((robot.getPose().H+360) < (dst.H+360))
				robot.setWt(PathPlan.standardAngularVelocity);
			else
				robot.setWt(0-PathPlan.standardAngularVelocity);
		}
		else if(!robot.getPose().equalsPose(dst)){
			robot.setVt(PathPlan.standardVelocity);
		}else{
			//do nothing
		}
		
	}
	
	public void setPath(RobotState robot, Pose[] path){
		List<Pose> newPath = new ArrayList<Pose>();
		
		for(int i = 0 ; i < path.length ; i++){
			
		}
	}
	
}
