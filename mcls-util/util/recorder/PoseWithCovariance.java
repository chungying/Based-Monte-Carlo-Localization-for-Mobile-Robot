package util.recorder;

import java.sql.Time;

public class PoseWithCovariance extends PoseWithTimestamp{
	public PoseWithCovariance(Time t) {
		super(t);
	}

	public double[][] cov = new double[3][3];
	
}
