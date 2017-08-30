package util.sensor.data;

import java.sql.Time;

import util.sensor.Sensor;

public class SensorData {
	public SensorData(Sensor sensor, Time timeStamp){
		this.sensor = sensor;
		this.timeStamp = timeStamp;
	}
	public Sensor sensor;
	public Time timeStamp;
}
