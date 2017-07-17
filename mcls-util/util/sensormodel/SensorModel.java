package util.sensormodel;

import java.util.List;
import java.util.concurrent.Callable;

import util.metrics.Particle;
import util.sensor.MCLLaserSensor;

public abstract class SensorModel implements Callable<Float>{
	
	//public RobotState robot;
	public List<Particle> set;
	public MCLLaserSensor.MCLLaserSensorData data;
	public boolean isupdated = false;
	
	public void updateModel(/*RobotState robot,*/ List<Particle> set, MCLLaserSensor.MCLLaserSensorData data){
		//this.robot = robot;
		this.set = set;
		this.data = data;
		this.isupdated = true;
	}
	
	

	
	
}
