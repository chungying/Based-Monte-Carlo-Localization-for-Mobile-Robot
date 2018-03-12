package util.pf;

import java.util.List;

import util.Transformer;

public class Particle implements Cloneable{
	/**
	 * constructor
	*/
	private double[] states = new double[3];
	
//	private double x;
//	private double y;
//	private double th;
	
	/**
	 * be initialized outside
	*/
	private double weightForNomalization;
	private double originalWeight;
	private double logWeight;
	/**
	 * assgned from outside
	 */
	private boolean ifmeasurements = false;
	private List<Float> measurements = null;

	/**
	 * 
	 * @param inputX
	 * @param inputY
	 * @param inputTh
	 */
	public Particle(double inputX, double inputY, double inputTh) {
		super();
//		this.x = inputX;
//		this.y = inputY;
//		this.th = inputTh;
		this.states[0] = inputX;
		this.states[1] = inputY;
		this.states[2] = inputTh;
//		assert(this.x == this.states[0]);
//		assert(this.y == this.states[1]);
//		assert(this.th == this.states[2]);
	}
	
	/**
	 * 
	 * @param inputX
	 * @param inputY
	 * @param inputTh
	 * @param weight
	 */
	public Particle(double inputX, double inputY, double inputTh, double w) {
		this(inputX,inputY,inputTh);
		this.weightForNomalization = w;
		this.originalWeight = w;
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 */
	public Particle(final Particle particle) {
		this(particle.getDX(), particle.getDY(), particle.getTh());
		this.weightForNomalization = particle.getNomalizedWeight();
		this.originalWeight = particle.getOriginalWeight();
		this.logWeight = particle.getLogW();
	}

	public int getX() {
//		int o1 = (int)Math.round(this.x);
//		int o2 = (int)Math.round(this.states[0]);
//		assert(o1==o2);
//		return o1;
//		return (int)Math.round(x);
		return (int)Math.round(this.states[0]);
	}

	public final double[] getStates(){
		return this.states;
	}
	
	public double getDX() {
//		assert(this.x==this.states[0]);
//		return this.x;
		return this.states[0];
	}
	
	public void setX(double inputX) {
//		this.x = inputX;
		this.states[0] = inputX;
//		assert(this.x == this.states[0]);
//		assert(this.y == this.states[1]);
//		assert(this.th == this.states[2]);
	}

	public int getY() {
//		int o1 = (int)Math.round(this.y);
//		int o2 = (int)Math.round(this.states[1]);
//		assert(o1 == o2);
//		return o1;
//		return (int)Math.round(y);
		return (int)Math.round(this.states[1]);
	}

	public double getDY() {
//		assert(this.y == this.states[1]);
//		return this.y;
		return this.states[1];
	}
	
	public void setY(double inputY) {
//		this.y = inputY;
		this.states[1] = inputY;
//		assert(this.x == this.states[0]);
//		assert(this.y == this.states[1]);
//		assert(this.th == this.states[2]);
	}

	public double getTh() {
//		assert(this.th == this.states[2]);
//		return this.th;
		return this.states[2];
	}

	public void setTh(double inputTh) {
//		this.th = Transformer.checkHeadRange(inputTh);
		this.states[2] = Transformer.checkHeadRange(inputTh);
//		assert(this.x == this.states[0]);
//		assert(this.y == this.states[1]);
//		assert(this.th == this.states[2]);
	}
		
	public void setLogW(double logW){
		this.logWeight = logW;
	}
	
	public double getLogW(){
		return this.logWeight;
	}
	
	public void setWeightForNomalization(double w){
		this.weightForNomalization = w;
	}
	public double getNomalizedWeight(){
		return this.weightForNomalization;
	}
	
	public void setOriginalWeight(double w){
		this.originalWeight = w;
	}
	
	public double getOriginalWeight(){
		return this.originalWeight;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Particle[ ");
		sb.append(String.format("%.5f ", this.states[0]));
		sb.append(String.format("%.5f ", this.states[1]));
		sb.append(String.format("%.5f ", this.states[2]));
//		sb.append(String.format("%.5f ", x));
//		sb.append(String.format("%.5f ", y));
//		sb.append(String.format("%.5f ", th));
		sb.append("] Weight: ");
//		sb.append(String.format("%.5f", weight));
		sb.append(String.format("%.5f", weightForNomalization));
		return sb.toString();
	}

	@Override
	public Particle clone() {
		try {
//			Particle p = (Particle)super.clone();
			Particle p = new Particle(this);
			assert(p.getDX()==this.states[0]);
//			assert(p.getDX()==this.x);
//			assert(this.x == this.states[0]);
//			assert(this.y == this.states[1]);
//			assert(this.th == this.states[2]);
			return p;
		} catch (Exception e) {
			e.printStackTrace();
			throw new InternalError();
		}
	}

	public boolean underSafeEdge(int width, int height, int safeEdge){
//		boolean o1 = Particle.underSafeEdge(x, y, width, height, safeEdge);
//		boolean o2 = Particle.underSafeEdge(this.states[0], this.states[1], width, height, safeEdge);
//		assert(o1==o2);
//		return o1;
//		return Particle.underSafeEdge(x, y, width, height, safeEdge);
		return Particle.underSafeEdge(this.states[0], this.states[1], width, height, safeEdge);
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

	public List<Float> getMeasurements() {
		return measurements;
	}

	public void setMeasurements(List<Float> m) {
		if(m==null)
			this.ifmeasurements = false;
		else
			this.ifmeasurements = true;
		
		this.measurements = m;
	}

	public boolean isIfmeasurements() {
		return ifmeasurements;
	}
	
}
