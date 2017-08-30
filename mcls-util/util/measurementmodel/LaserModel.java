package util.measurementmodel;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FloatConverter;

import util.sensor.LaserSensor;
import util.sensor.data.LaserScanData;

public class LaserModel extends LaserSensor{
	
	public LaserModel(){
	}
	
	@Parameter(names = {"--sigmahit"}, 
			description = "SIGMAHIT parameter of sensor beam model of particle filter, default value is 4.", 
			required = false, arity = 1, converter = FloatConverter.class)
	public float sigma_hit = 4; //unit in pixels
	@Parameter(names = {"--lambdashort"}, 
			description = "LAMDA parameter of sensor beam model of particle filter, default value is 0.1.", 
			required = false, arity = 1, converter = FloatConverter.class)
	public float lambda_short = 0.1f;
	@Parameter(names = {"--zhit"}, 
			description = "HIT parameter of sensor beam model of particle filter, default value is 0.95.", 
			required = false, arity = 1, converter = FloatConverter.class)
	public float z_hit  = 0.95f;
	@Parameter(names = {"--zshort"}, 
			description = "SHORT parameter of sensor beam model of particle filter, default value is 0.1.", 
			required = false, arity = 1, converter = FloatConverter.class)
	public float z_short= 0.1f;
	@Parameter(names = {"--zmax"}, 
			description = "MAX parameter of sensor beam model of particle filter, default value is 0.05.", 
			required = false, arity = 1, converter = FloatConverter.class)
	public float z_max  = 0.05f;
	@Parameter(names = {"--zrand"}, 
			description = "RAND parameter of sensor beam model of particle filter, default value is 0.05.", 
			required = false, arity = 1, converter = FloatConverter.class)
	public float z_rand = 0.05f;
	
	@Override
	public void setupSensor() throws Exception {
		super.setupSensor();
		//public float sigma_hit = 4; //unit in pixels
		if(this.sigma_hit==0)
			throw new Exception("laser variance cannot be zero.");
		if(lambda_short <= 0)
			throw new Exception("lamda_short cannot be eaqual to or smaller than zero.");
		
		float sum = z_hit+z_short+z_max+z_rand;
		z_hit /=sum;
		z_short /=sum;
		z_max /=sum;
		z_rand /=sum;
	}
	
	public static class LaserData{
		
		public LaserData(LaserScanData data, LaserModel sensor) {
			this.sensor = sensor;
			this.data = data;
		}
		
		public LaserScanData data;
		public LaserModel sensor;
	}
}
