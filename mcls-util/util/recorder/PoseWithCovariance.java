package util.recorder;

import java.sql.Time;

import util.robot.Pose;

public class PoseWithCovariance extends PoseWithTimestamp{
	public PoseWithCovariance(Time t) {
		super(t);
	}

	public double[][] cov = new double[3][3];

	
	public PoseWithCovariance clone() {
		// TODO Auto-generated method stub
		PoseWithCovariance clone = new PoseWithCovariance(this.stamp);
		clone.H = this.H;
		clone.X = this.X;
		clone.Y = this.Y;
		for( int i = 0 ; i < 3 ; i++)
			for (int j = 0 ; j < 3 ; j++)
				clone.cov[i][j] = this.cov[i][j];
		
		return clone;
	}
	
	
}
