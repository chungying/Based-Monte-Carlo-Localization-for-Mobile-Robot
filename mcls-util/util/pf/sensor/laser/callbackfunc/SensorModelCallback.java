package util.pf.sensor.laser.callbackfunc;

public abstract class SensorModelCallback<IN1, IN2, OUT> {
	public abstract OUT call(IN1 in1, IN2 in2) throws Exception;
}
