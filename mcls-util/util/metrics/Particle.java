package util.metrics;

public class Particle implements Cloneable{
	/**
	 * constructor
	*/
	private double x;
	private double y;
	private double th;
	
	/**
	 * be initialized outside
	*/
	private float weight;
	
	/**
	 * assgned from outside
	 */
	private boolean ifmeasurements = false;
	private float[] measurements;

	/**
	 * 
	 */
	public Particle(double x, double y, double th) {
		super();
		this.x = x;
		this.y = y;
		this.th = th;
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 */
	public Particle(Particle particle) {
		this(particle.getX(), particle.getY(), particle.getTh());
	}

//	public int getX() {
//		return (int)Math.round(x);
//	}

	public void setX(double x) {
		this.x = x;
	}

	public int getY() {
		return (int)Math.round(y);
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getTh() {
		return this.th;
	}

	public void setTh(double th) {
		this.th = Transformer.checkHeadRange(th);
	}
		
	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		return "Particle [\t" + 
						String.format("%05f", x) + "\t" + 
						String.format("%05f", y) + "\t" + 
						String.format("%05f", th) + "\t]"+
						"weight:\t"+String.format("%05f", weight);
	}

	@Override
	public Particle clone() {
		try {
			Particle p = (Particle)super.clone();
			return p;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			throw new InternalError();
		}
	}

	public boolean underSafeEdge(int width, int height, int safeEdge){
		return Particle.underSafeEdge(x, y, width, height, safeEdge);
	}
	
	static public boolean underSafeEdge(double x, double y, int width, int height, int safeEdge){
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
