package util.pf.sensor;


public abstract class Sensor {
	private final SensorType sensortype;
	public Sensor(SensorType sensortype){
		this.sensortype = sensortype;
	}
	
	public void setupSensor(Sensor sensor) throws Exception{
		if(this.getSensortype()!=sensor.getSensortype())
			throw new Exception("cannot setup the sensor with differenty type.");
	}
	
	public SensorType getSensortype() {
		return sensortype;
	}

	public static enum SensorType{
		ODOMETRY, LASER
	}
	
}
