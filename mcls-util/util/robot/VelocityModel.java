package util.robot;

public class VelocityModel {
	
	public VelocityModel() {
		super();
	}
	public VelocityModel(VelocityModel m) {
		super();
		this.setVelocity(m.getVelocity());
		this.setAngular_velocity(m.getAngular_velocity());
	}
	public VelocityModel(double velocity, double angular_velocity) {
		this.velocity = velocity;
		this.angular_velocity = angular_velocity;
	}
	public double velocity;// pixel/s
	public double angular_velocity;// degree/s
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
	public void reset(double v, double w) {
		this.velocity = v;
		this.angular_velocity = w;
		
	}
	
	@Override
	public String toString(){
		return "Velocity Model["+ String.format("%.2f",this.velocity) + "\t" 
				+ String.format("%.2f",this.angular_velocity) + "\t]";
	}
}
