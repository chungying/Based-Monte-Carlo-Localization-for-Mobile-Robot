package robot;

public class VelocityModel {
	
	public VelocityModel() {
		super();
	}
	public double velocity;
	public double angular_velocity;
	public double getVelocity() {
		return velocity;
	}
	public void setVelocity(double velocity) {
		this.velocity = velocity;
	}
	public double getAngular_velocity() {
		return angular_velocity;
	}
	public void setAngular_velocity(double angular_velocity) {
		this.angular_velocity = angular_velocity;
	}
}
