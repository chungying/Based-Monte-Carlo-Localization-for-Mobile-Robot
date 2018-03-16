package util.pf.sensor.laser;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FloatConverter;

import util.pf.sensor.Sensor;
import util.pf.sensor.data.LaserScanData;

/**
 * 
 * @author Jolly
 *
 */
public class LaserModel extends LaserSensor{
	
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
	public void setupSensor(Sensor sensor) throws Exception {
		System.out.println("LaserModel");
		super.setupSensor(sensor);
		if(LaserModel.class.isAssignableFrom(sensor.getClass())){
			LaserModel src = (LaserModel)sensor;
			this.sigma_hit = src.sigma_hit;
			this.lambda_short = src.lambda_short;
			this.z_hit   = src.z_hit  ;
			this.z_short = src.z_short;
			this.z_max   = src.z_max  ;
			this.z_rand  = src.z_rand ;
		}
		//public float sigma_hit = 4; //unit in pixels
		if(sigma_hit==0)
			throw new Exception("laser variance cannot be zero.");
		if(lambda_short <= 0)
			throw new Exception("lamda_short cannot be eaqual to or smaller than zero.");
		
		float sum = z_hit+z_short+z_max+z_rand;
		z_hit /=sum;
		z_short /=sum;
		z_max /=sum;
		z_rand /=sum;
	}
	
	/**
	 * 
	 * @author Jolly
	 *
	 */
	public static class LaserModelData{
		
		public LaserModelData(LaserScanData data, LaserModel sensor) {
			this.sensor = sensor;
			this.data = data;
		}
		
		public LaserScanData data;
		public LaserModel sensor;
	}
}
