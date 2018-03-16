package util.pf.sensor.laser;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FloatConverter;

import util.pf.sensor.Sensor;

public class LaserSensor extends Sensor{
	
	@Parameter(names = {"-lv","--laservariance"}, 
			description = "variance of each laser beam", 
			required = false, converter = FloatConverter.class, arity = 1)
	public float variance=1f;//for simulation, default value is very accurate
	
	@Parameter(names = {"-lst","--laserscantime"}, 
			description = "the time between two scans", 
			required = false, converter = FloatConverter.class, arity = 1)
	public float scan_time=200;//update frequency, unit in ms
	
	@Parameter(names = {"-lamin","--laseranglemin"}, 
			description = "the bearing angle of the first beam in degree", 
			required = false, converter = FloatConverter.class, arity = 1)
	public float angle_min=-90;
	
	@Parameter(names = {"-lamax","--laseranglemax"}, 
			description = "the bearing angle of the last beam in degree", 
			required = false, converter = FloatConverter.class, arity = 1)
	public float angle_max=90;
	
	@Parameter(names = {"-lares","--laserangleresolution"}, 
			description = "the bearing resolution between two beams", 
			required = false, converter = FloatConverter.class, arity = 1)
	public float angle_resolution=3;
	
	@Parameter(names = {"-lrmin","--laserrangemin"}, 
			description = "minimum measurable distance", 
			required = false, converter = FloatConverter.class, arity = 1)
	public float range_min=0;//unit in pixels
	
	@Parameter(names = {"-lrmax","--laserrangemax"}, 
			description = "maximum measurable distance", 
			required = false, converter = FloatConverter.class, arity = 1)
	public float range_max=50;//unit in pixels
	public LaserSensor(){
		super(Sensor.SensorType.LASER);
	}
	
	public int getOrientation(){
		return Math.round(360/this.angle_resolution);
	}
	public int rangeCount(){
		return (int) Math.floor((this.angle_max-this.angle_min)/this.angle_resolution+1);
	}

	/**
	 * initialization
	 * @throws Exception 
	 */
	@Override
	public void setupSensor(Sensor sensor) throws Exception{
		super.setupSensor(sensor);
		if(LaserSensor.class.isAssignableFrom(sensor.getClass())) {
			LaserSensor srcSensor = (LaserSensor)sensor;
			if(srcSensor.scan_time<=0)
				throw new Exception("scan_time cannot be zero or negative.");
			if(srcSensor.angle_max-srcSensor.angle_min<=0)
				throw new Exception("maximum angle of the laser is smaller than minimum angle.");
			if(srcSensor.angle_resolution<=0)
				throw new Exception("the resolution is too small.");
			if(srcSensor.range_max<=srcSensor.range_min && srcSensor.range_max!=-1)
				throw new Exception("the maximum range of the laser is smaller than the minimum.");
			this.variance = srcSensor.variance;
			this.scan_time = srcSensor.scan_time;
			this.angle_min = srcSensor.angle_min;
			this.angle_max = srcSensor.angle_max;
			this.angle_resolution = srcSensor.angle_resolution;
			this.range_min = srcSensor.range_min;
			this.range_max = srcSensor.range_max;
		}
	}
	
}
