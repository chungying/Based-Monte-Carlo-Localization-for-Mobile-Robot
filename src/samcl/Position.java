package samcl;

import java.awt.Point;
import java.util.Arrays;

public class Position {
		public int sensor_number;
		public Point[] measurement_points;
		public float[] circle_measurements;
		public float[] energy;

		/**
	 * @param z
	 * @return <pre>
	 * if z >= 0
	 * return the range of the measurements of this orientation
	 * else
	 * return all of the measurements
	 * </pre>
	 */
	public float[] getMeasurements(int z) {
		if (z >= 0) {
			float[] measurements = new float[this.sensor_number];
			int bias = (this.sensor_number - 1) / 2;
			int index;
			for (int i = 0; i < this.sensor_number; i++) {
				index = ((z - bias + i + this.circle_measurements.length) % this.circle_measurements.length);
				measurements[i] = this.circle_measurements[index];
			}
			return measurements;
		} else {
			return this.circle_measurements;
		}
	}

	/**
	 * Deprecated
	 * @param z
	 * @return <pre>
	 * return the energy of the orientation
	 * </pre>
	 */
	public float getEnergy(int z) {
		return energy[z];
	}

	/**
	 * @param z
	 * @return <pre>
	 * if z >= 0
	 * return the points of the measurements of this orientation
	 * else
	 * return all of the points of the measurements
	 */
	public Point[] getMeasurement_points(int z) {
		if (z >= 0) {
			Point[] measurements = new Point[this.sensor_number];
			int bias = (this.sensor_number - 1) / 2;
			int index;
			for (int i = 0; i < this.sensor_number; i++) {
				index = ((z - bias + i + this.circle_measurements.length) % this.circle_measurements.length);
				measurements[i] = this.measurement_points[index];
			}
			return measurements;
		} else {
			Point[] measurements = this.measurement_points;
			return measurements;
		}
	}
	
	private void setup(){
		this.sensor_number = (this.circle_measurements.length / 2) + 1;
		this.energy = new float[this.circle_measurements.length];
		float[] zt = null;
		for (int i = 0; i < this.circle_measurements.length; i++) {
			zt = this.getMeasurements(i);
			this.energy[i] = 0.0f;
			for (int j = 0; j < zt.length; j++) {
				this.energy[i] = this.energy[i] + zt[j];
			}
			this.energy[i] = this.energy[i] / ((float)zt.length);
		}
	}

	/**
	 * @param float[] measurements
	 * 
	 *        <pre>
	 * assign the measurements 
	 * and then compute the energy of all orientation
	 * </pre>
	 */
	public Position(float[] measurements) {
		super();
		this.circle_measurements = measurements;
		this.setup();
	}

	/**
	 * @param measurements
	 *            all of the measurements around the position
	 * @param measurement_points
	 *            all of the points of the measurements
	 * 
	 *            <pre>
	 * assign the measurements and all of the points of the measurements
	 * and then compute the energy of all orientation
	 * </pre>
	 */
	public Position(float[] measurements, Point[] measurement_points) {
		super();
		this.circle_measurements = measurements;
		this.measurement_points = measurement_points;
		this.setup();
	}

	/**
	 * @param sensor_number
	 */
	public Position(int orientation) {
		super();
		this.sensor_number = orientation / 2 + 1;
		this.circle_measurements = new float[orientation];
	}

	/**
	 * 
	 */
	public Position() {
		super();
	}

	@Override
	public String toString() {
		return "position [circle_measurements="
				+ Arrays.toString(circle_measurements) + ", energy="
				+ Arrays.toString(energy) + "]";
	}
}
