package util.pf.sensor.odom.callbackfunc;

import util.Distribution;
import util.Transformer;
import util.pf.Particle;
import util.pf.sensor.Sensor;
import util.pf.sensor.odom.OdometricSensor;
import util.robot.Pose;

public class MCLMotionModel extends OdometricSensor{//TODO use this class to handle alpha parameters

	public static enum ModelType{
		ODOMETRY_MODEL, DEFAULT;
		public static ModelType fromString(String value) {
			for(ModelType output:ModelType.values()){
				if(output.toString().equalsIgnoreCase(value)){
					return output;
				}
			}
			return ModelType.DEFAULT;
		}
	}
	public double[] alphas = al.clone();
	public static double[] al = {
			5,5,
			5,5,
			5,5
		};
//	public static double[] al = {
//			25,25,
//			25,25,
//			25,25
//		};
	
	protected ModelType modelType = ModelType.DEFAULT;
	@Override
	public void setupSensor(Sensor sensor) throws Exception {
		super.setupSensor(sensor);
		if(MCLMotionModel.class.isAssignableFrom(sensor.getClass())){
			this.modelType = ((MCLMotionModel)sensor).getModelType();
		}
		this.setupCallbackFunction();
	}

	public ModelType getModelType() {
		return this.modelType;
	}

	public void setModelType(ModelType model){
		this.modelType = model;
		this.setupCallbackFunction();
	}
	protected Object callableModelFunction;
	protected void setupCallbackFunction() {
		switch(this.modelType){
		case ODOMETRY_MODEL:
			this.callableModelFunction = new Object();
			break;
		default:
			break;
		}
	}
	
	public void prediction(){
		//TODO
	}
	/**
	 * 
	 * @param p
	 * @param curP
	 * @param preP
	 * @param deltaT
	 * @param random
	 * @param al
	 */
	public static Particle OdemetryMotionSampling(final Particle p, final Pose curP, final Pose preP, double deltaT, /*Random random,*/ double[] al) /*throws Exception*/{
		double xbardelta = curP.X-preP.X;
		double ybardelta = curP.Y-preP.Y;
		if(xbardelta==0)
			xbardelta = Double.MIN_VALUE;
		if(ybardelta==0)
			ybardelta = Double.MIN_VALUE;
		double rot1;
		double trans = Math.sqrt(xbardelta*xbardelta+ybardelta*ybardelta);
		if (trans<=0.01){
			rot1 = 0.0;
		}else
			rot1 = Pose.deltaTheta(Transformer.checkHeadRange(Math.toDegrees(Math.atan2(ybardelta,xbardelta))),preP.H);
		double rot2 = Pose.deltaTheta(curP.H, preP.H)-rot1;
		
		double v1 = Distribution.sample_normal_distribution(al[0] * rot1 + al[1] * trans/*, random*/);
		double v2 = Distribution.sample_normal_distribution(al[2] * trans + al[3] * (rot1 + rot2)/*, random*/);
		double v3 = Distribution.sample_normal_distribution(al[4] * rot2 + al[5] * trans/*, random*/);
		double rot1c = rot1 - v1;
		double transc= trans - v2;
		double rot2c = rot2 - v3;
		
//		if(Double.isNaN(rot1c) || Double.isNaN(transc) || Double.isNaN(rot2c))
//			throw new Exception("there is NaN!!!!!!!");
		
		return new Particle(
				p.getDX() + transc * Math.cos(Math.toRadians( p.getTh() + rot1c)),
				p.getDY() + transc * Math.sin(Math.toRadians( p.getTh() + rot1c)),
				p.getTh() + rot1c + rot2c);
	}
	
	
}
