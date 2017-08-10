package util.robot;

import java.sql.Time;

public class Scan {
	public Time stamp;
	public float[] ranges;
	public double angle_min; 		//start angle of the scan [rad]
	public double angle_max;     	//end angle of the scan [rad]
	public double angle_increment;	// angular distance between measurements [rad]
	public float range_min;		//minimum range value [m]
	public float range_max;		//maximum range value [m]
	
	public Scan(float[] ranges, float range_min, float range_max, double angle_min, double angle_max, double angle_increment){
		
	}
}
