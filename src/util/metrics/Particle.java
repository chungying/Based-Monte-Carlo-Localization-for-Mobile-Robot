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
		//this.th = this.z * this.delta_degree;
	}

	public Particle(Particle particle) {
		// TODO Auto-generated constructor stub
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
	public Particle/*Object*/ clone() {
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


	
}
