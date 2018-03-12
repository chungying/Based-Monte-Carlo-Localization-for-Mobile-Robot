package util.pf.sensor.data;

import java.sql.Time;
import java.util.List;

import util.pf.sensor.laser.LaserSensor;
import util.recorder.PoseWithTimestamp;

public class LaserScanData extends SensorData{
	
	public LaserScanData(List<Float> rm, LaserSensor sensor, Time time, PoseWithTimestamp truePose) {
		super(sensor, time);
		this.beamranges = rm;
		this.groundTruthPose = truePose;
		this.angle_min = sensor.angle_min;
		this.angle_max = sensor.angle_max;
		this.angle_increment = sensor.angle_resolution;
	}
	public LaserScanData(List<Float> rm, LaserSensor sensor, Time time) {
		this(rm, sensor, time, null);
	}
	public void setBeamRange(List<Float> beamrange) throws Exception{
		setBeamRange(beamrange, this.angle_min, this.angle_max, this.angle_increment);
	}
	public void setBeamRange(List<Float> beamrange, float amin, float amax, float ai) throws Exception {
		this.beamranges = beamrange;
		this.angle_min = amin;
		this.angle_max = amax;
		this.angle_increment = ai;
		if(Math.round((amax-amin)/ai)<beamrange.size()){
			throw new Exception("resolution does not match range count.");
		}
	}
	public int getBeamCount() {
		return beamranges.size();
	}
	public List<Float> beamranges = null;
	public float angle_min;
	public float angle_max;
	public float angle_increment;
	public PoseWithTimestamp groundTruthPose;//for simulation
}
