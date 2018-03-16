package util.pf.sensor;


public abstract class Sensor {
	private final SensorType sensortype;
	public Sensor(SensorType sensortype){
		this.sensortype = sensortype;
	}
	
	public void setupSensor(Sensor sensor) throws Exception{//for sub-classes
		if(this.getSensortype()==null)
			throw new Exception("this is null");
		if(sensor.getSensortype()==null)
			throw new Exception("sensor is null");
		if(this.getSensortype()!=sensor.getSensortype())
			throw new Exception("cannot setup the sensor with differenty type.");
	}
	
	public SensorType getSensortype() {
		System.out.println("Sensor");
		if(this.sensortype ==null)
			try{throw new Exception("This cannot be null.");} catch (Exception e){e.printStackTrace();}
		return sensortype;
	}

	public static enum SensorType{
		ODOMETRY, LASER
	}
	
}
