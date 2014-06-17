package robot;

public class RobotState {
	public int x;
	public int y;
	public double head;
	//public Degree th;
	public int Vt;
	public double Wt;
	
	/**
	 * @param x
	 * @param y
	 * @param head
	 * @param vt
	 * @param wt
	 */
	public RobotState(int x, int y, double head) {
		super();
		System.out.println("initial robot");
		this.x = x;
		this.y = y;
		this.head = head;
		Vt = 0;
		Wt = 0;
	}
	
	public void Update(){
		this.x = this.x + (int) (Vt*Math.cos(Math.toRadians(head))) /*+ (int)(Math.round(Wt))*/;
		this.y = this.y + (int) (Vt*Math.sin(Math.toRadians(head))) /*+ (int)(Math.round(Wt))*/;
		this.head = (this.head + this.Wt+720)%360;	
		System.out.println(this.toString());
	}

	public int getVt() {
		return Vt;
	}

	public void setVt(int vt) {
		Vt = vt;
	}

	/**
	 * @return the angular velocity in degree/times 
	 */
	public double getWt() {
		return Wt;
	}

	/**
	 * @param wt in degree/times
	 */
	public void setWt(double wt) {
		Wt = wt;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return 1;
	}
	
	public double getHead() {
		return head;
	}

	@Override
	public String toString() {
		return "RobotState (" + x + ", " + y + ", " + head + "), ["
				+ Vt + ", " + Wt + "]";
	}
	
	
	
}
