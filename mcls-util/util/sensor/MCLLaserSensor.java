package util.sensor;

import java.sql.Time;
import java.util.List;
import util.sensormodel.BasicSensorModel;
import util.sensormodel.BeamModel;
import util.sensormodel.LogBeamModel;
import util.sensormodel.LossFunction;
import util.metrics.Particle;
import util.robot.Pose;

public class MCLLaserSensor extends MCLSensor{
	
	public MCLLaserSensor(ModelType mt){
		this.modeltype = mt;
		setupSensor();
	}
	
	public static enum ModelType{
		BEAM_MODEL, LOSS_FUNCTION, LOG_BEAM_MODEL, DEFAULT
	}

	/**
	 * calculating importance factor of particles based on .
	 */
	@Override
	public void updateSensor(List<Particle> set, MCLSensorData data) {
		
		try {
			this.callableModelFunction.updateModel( set, (MCLLaserSensorData) data);
			this.callableModelFunction.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private ModelType modeltype = ModelType.DEFAULT;

	@Override
	public void setupSensor() {
		//TODO finish this setup function
		this.setModeltype(this.modeltype);
	}
	
	public void setModeltype(ModelType modeltype) {
			this.modeltype = modeltype;
			setupCallableModel(modeltype);
	}
	
	public ModelType getModeltype() {
		return modeltype;
	}
	
	public BasicSensorModel callableModelFunction;
	private void setupCallableModel(ModelType model_type) {
		switch(modeltype){
		case BEAM_MODEL:
			this.modeltype = ModelType.BEAM_MODEL;
			this.callableModelFunction = new BeamModel();//new BeamModel
			break;
			
		case LOSS_FUNCTION:
			this.modeltype = ModelType.LOSS_FUNCTION;
			this.callableModelFunction = new LossFunction();//new LossFunction
			break;

		case LOG_BEAM_MODEL:
			this.modeltype = ModelType.LOG_BEAM_MODEL;
			this.callableModelFunction = new LogBeamModel();
		default:
			this.callableModelFunction = new BeamModel();//new BeaModel
			break;
		}
	}
	
	public class MCLLaserSensorData extends MCLSensor.MCLSensorData{
		
		public MCLLaserSensorData() {
			this.setBeamRange(null);
			this.setBeamBearing(null);
		}
		public Pose truePose;
		public MCLLaserSensorData(float[] rm, MCLSensor sensor, Time time, Pose truePose) {
			this();
			this.beamranges = rm;
			this.sensor = sensor;
			this.timeStamp = time;
			this.truePose = truePose;
		}
		public MCLLaserSensorData(float[] rm, MCLSensor sensor, Time time) {
			this(rm, sensor, time, null);
		}
		

		//TODO modifying following getters and setters.
		public int getBeamCount() {
			return beamcount;
		}
		public void setBeamCount(int beamcount) {
			this.beamcount = beamcount;
		}
		public float[] getBeamBearing() {
			return beambearing;
		}
		public void setBeamBearing(float[] beambearing) {
			this.beambearing = beambearing;
		}
		public float[] getBeamRange() {
			return beamranges;
		}
		public void setBeamRange(float[] beamrange) {
			this.beamranges = beamrange;
		}
		private float[] beamranges;
		private float[] beambearing;
		private int beamcount;
		public float laser_max=40;
		public float sigma_hit = 4; //unit in pixels
		public float lambda_short = 0.1f;
		public float[] z_paras = {0.95f, 0.1f, 0.05f, 0.05f};
	}

}
