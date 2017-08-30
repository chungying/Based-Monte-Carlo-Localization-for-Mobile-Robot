package util.sensor;


public abstract class Sensor {
	private final SensorType sensortype;
	Sensor(SensorType sensortype){
		this.sensortype = sensortype;
	}
	
	public abstract void setupSensor() throws Exception;//for sub-classes
	
	public SensorType getSensortype() {
		return sensortype;
	}

	public static enum SensorType{
		ODOMETRY, LASER
	}
	
}
