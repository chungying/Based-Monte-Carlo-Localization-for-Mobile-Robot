package util.pf.sensor.odom;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FloatConverter;

import util.pf.sensor.Sensor;

public class OdometricSensor extends Sensor{
	
	@Parameter(names = {"-ocov","--odomcovariance"}, 
			description = "covariance of odometric sensor", 
			required = false, converter = FloatConverter.class, arity = 1)
	public float covariance=0.001f;//for simulation, default value is very accurate
	
	@Parameter(names = {"-ost","--odomscantime"}, 
			description = "the time between two sequensive data", 
			required = false, converter = FloatConverter.class, arity = 1)
	public float scanTime=10;//update frequency, unit in ms
	
	public OdometricSensor() {
		super(Sensor.SensorType.ODOMETRY);
	}

	@Override
	public void setupSensor(Sensor sensor) throws Exception {
		super.setupSensor(sensor);
		
	}

}
