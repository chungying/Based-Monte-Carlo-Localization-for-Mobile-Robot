package util.sensor;

import java.util.List;
import java.util.concurrent.Callable;

import samcl.SAMCL;
import util.metrics.Particle;

public abstract class MCLSensor {

	//data
	//sensor constructor
/*	public Sensor(){
		
	}*/
	
	//initialization
	public abstract void setupSensor();
	

	/***
	 * update information
	 */
	public abstract void updateSensor(List<Particle> set, MCLSensorData data);
	
	//field
	public boolean is_action;
	//get information
	//set information
	
	public class MCLSensorData{
		public MCLSensor sensor;
		public double timestamp;
	}
}
