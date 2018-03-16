package util.pf.sensor.odom.callbackfunc;

import com.beust.jcommander.Parameter;

import util.Distribution;
import util.Transformer;
import util.pf.Particle;
import util.pf.sensor.Sensor;
import util.pf.sensor.odom.OdometricSensor;
import util.robot.Pose;

public class MCLMotionModel extends OdometricSensor{

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
	private static double[] al = {
			5,5,
			5,5,
			5,5
		};

	@Parameter(names = {"--odomAlpha1"}, description = "alpha parameter for odometric model", required = false, arity = 1)
	public double alpha1 = 5;
	
	@Parameter(names = {"--odomAlpha2"}, description = "alpha parameter for odometric model", required = false, arity = 1)
	public double alpha2 = 5;
	
	@Parameter(names = {"--odomAlpha3"}, description = "alpha parameter for odometric model", required = false, arity = 1)
	public double alpha3 = 5;
	
	@Parameter(names = {"--odomAlpha4"}, description = "alpha parameter for odometric model", required = false, arity = 1)
	public double alpha4 = 5;
	
	@Parameter(names = {"--odomAlpha5"}, description = "alpha parameter for odometric model", required = false, arity = 1)
	public double alpha5 = 5;
	
	@Parameter(names = {"--odomAlpha6"}, description = "alpha parameter for odometric model", required = false, arity = 1)
	public double alpha6 = 5;
	
	protected ModelType modelType = ModelType.DEFAULT;
	@Override
	public void setupSensor(Sensor sensor) throws Exception {
		super.setupSensor(sensor);
		if(MCLMotionModel.class.isAssignableFrom(sensor.getClass())){
			MCLMotionModel temp = ((MCLMotionModel)sensor);
			this.modelType = temp.getModelType();
			//this.alphas = temp.alphas;
			System.arraycopy( temp.alphas, 0, this.alphas, 0, temp.alphas.length );
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
	
	public String alphasToString(){
		//return Arrays.toString(this.alphas);
		return "[" + alpha1 + ", " + alpha2 + ", " + alpha3 + ", " + alpha4 + ", " + alpha5 + ", " + alpha6 + "]";
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
	 * @param model
	 */
	public static Particle OdemetryMotionSampling(final Particle p, final Pose curP, final Pose preP, double deltaT, MCLMotionModel model) {
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
		
		double v1 = Distribution.sample_normal_distribution(model.alpha1 * rot1  + model.alpha2 * trans/*, random*/);
		double v2 = Distribution.sample_normal_distribution(model.alpha3 * trans + model.alpha4 * (rot1 + rot2)/*, random*/);
		double v3 = Distribution.sample_normal_distribution(model.alpha5 * rot2  + model.alpha6 * trans/*, random*/);
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
