package util.metrics;

public class Particle implements Cloneable{
	/**
	 * constructor
	*/
	private int x;
	private int y;
	private int z;
	public int orientation;
	public float delta_degree;
	//private float th;
	
	/**
	 * be initialized outside
	*/
	private float weight;
	/**
	 * assgned from outside
	 */
	private boolean ifmeasurements;
	private float[] measurements;

	/**
	 * 
	 */
	public Particle(int x, int y, int z) {
		super();
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 */
	public Particle(int x, int y, int z, int orientation2) {
		super();
		this.x = x;
		this.y = y;
		this.z = z;
		this.orientation = orientation2;
		this.delta_degree = 360/this.orientation;
	}

	public Particle(Particle particle) {
		this(particle.getX(), particle.getY(), particle.getZ(), particle.orientation);
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}
	
	public void setX(double x) {
		this.x = (int)Math.round(x);
	}
	
	public void setX(float x) {
		this.x = (int)Math.round(x);
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}
	
	public void setY(float y) {
		this.y = (int)Math.round(y);
	}
	
	public void setY(double y) {
		this.y = (int)Math.round(y);
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public float getTh() {
		return this.z * this.delta_degree;
	}
	
	public void setTh(float th) {
		this.z = Math.round((th%360)/this.delta_degree)%this.orientation;
	}
	
	public void setTh(double th) {
		this.setTh((float)th);
	}

	public int getOrientation() {
		return orientation;
	}

	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}

	public float getDelta_degree() {
		return delta_degree;
	}

	public void setDelta_degree(float delta_degree) {
		this.delta_degree = delta_degree;
	}
	
	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		return "Particle [x=" + x + ", y=" + y + ", z=" + z + "], weight: "+this.weight;
	}

	@Override
	public Particle clone() {
		// TODO Auto-generated method stub
		try {
			Particle p = (Particle)super.clone();
			return p;
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new InternalError();
		}
	}

	public boolean underSafeEdge(int width, int height, int safeEdge){
		return Particle.underSafeEdge(x, y, width, height, safeEdge);
	}
	
	static public boolean underSafeEdge(int x, int y, int width, int height, int safeEdge){
		//check out all of the input are positive
		if(x<0 || y<0 || width<0 || height<0 || safeEdge<0)
			return false;
		
		if(		x > safeEdge && 
				y > safeEdge &&
				x < (width-safeEdge) && 
				y < (height-safeEdge))
		{
			return true;
		}
		else 
		{
			return false;
		}
	}

	public float[] getMeasurements() {
		return measurements;
	}

	public void setMeasurements(float[] measurements2) {
		this.ifmeasurements = true;
		this.measurements = measurements2;
		
	}

	public boolean isIfmeasurements() {
		return ifmeasurements;
	}
	
	

	
}
