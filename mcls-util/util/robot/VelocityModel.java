package util.robot;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.DoubleConverter;

public class VelocityModel implements Cloneable{
	
	@Parameter(names = {"--linearvelocity"}, 
			description = "initial linear velocity of the simulated robot", 
			required = false, converter = DoubleConverter.class, arity = 1)
	public double velocity;// pixel/s
	
	@Parameter(names = {"--angularvelocity"}, 
			description = "initial angular velocity of the simulated robot", 
			required = false, converter = DoubleConverter.class, arity = 1)
	public double angular_velocity;// degree/s
	
	@Override
	public VelocityModel clone() throws CloneNotSupportedException {
		VelocityModel newObj = new VelocityModel(this);
		return newObj;
	}
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
	
	public double getVelocity() {
		return velocity;
	}
	synchronized public void setVelocity(double velocity) {
		this.velocity = velocity;
	}
	public double getAngular_velocity() {
		return angular_velocity;
	}
	synchronized public void setAngular_velocity(double angular_velocity) {
		this.angular_velocity = angular_velocity;
	}
	synchronized public void set(double v, double w) {
		this.velocity = v;
		this.angular_velocity = w;	
	}
	
	public void setModel(VelocityModel u){
		this.set(u.getVelocity(), u.getAngular_velocity());
	}
	
	@Override
	public String toString(){
		return "Velocity Model["+ String.format("%.2f",this.velocity) + "\t" 
				+ String.format("%.2f",this.angular_velocity) + "\t]";
	}
}
