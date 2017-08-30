package util.sensor;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FloatConverter;

public class LaserSensor extends Sensor{
	
	@Parameter(names = {"-lv","--laservariance"}, 
			description = "variance of each laser beam", 
			required = false, converter = FloatConverter.class, arity = 1)
	public float variance=0.001f;//for simulation, default value is very accurate
	
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
	 * Configuring this sensor from the configures of other LaserSensor object. 
	 * @param sensor is the reference of all configures.
	 * @throws Exception if there is any configure unreasonable, the exception object will warn you.
	 */
	public void setupSensor(LaserSensor sensor) throws Exception{
		this.variance = sensor.variance;
		this.scan_time = sensor.scan_time;
		this.angle_min = sensor.angle_min;
		this.angle_max = sensor.angle_max;
		this.angle_resolution = sensor.angle_resolution;
		this.range_min = sensor.range_min;
		this.range_max = sensor.range_max;
		//Check availability
		this.setupSensor();
	}

	/**
	 * initialization
	 * @throws Exception 
	 */
	@Override
	public void setupSensor() throws Exception{
		
		//check if all parameters are setup correctly.
		//public float scan_time;//update frequency, unit in ms
		if(this.scan_time<=0)
			throw new Exception("scan_time cannot be zero or negative.");
		//public float angle_min;//unit in degree
		//public float angle_max;//unit in degree
		//public float angle_resolution;//unit in degree
		if(this.angle_max-this.angle_min<=0)
			throw new Exception("maximum angle of the laser is smaller than minimum angle.");
		if(this.angle_resolution<=0)
			throw new Exception("the resolution is too small.");
		//public float range_min=0;//unit in pixels
		//public float range_max;//unit in pixels
		if(this.range_max<=this.range_min && this.range_max!=-1)
			throw new Exception("the maximum range of the laser is smaller than the minimum.");
	}
	
	//field
	//public boolean is_action;
	//get information
	//set information
	
	
}
