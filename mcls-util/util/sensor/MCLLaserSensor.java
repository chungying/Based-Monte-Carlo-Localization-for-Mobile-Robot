package util.sensor;

import java.util.List;
import util.sensormodel.SensorModel;
import util.sensormodel.BeamModel;
import util.sensormodel.LossFunction;
import util.metrics.Particle;

public class MCLLaserSensor extends MCLSensor{
	
	public MCLLaserSensor(ModelType mt){
		this.modeltype = mt;
		setupSensor();
	}
	
	public static enum ModelType{
		BEAM_MODEL, LOSS_FUNCTION, DEFAULT
	}

	@Override
	public void setupSensor() {
		//TODO finish this setup function
		setupSensor(this.modeltype);
	}

	public void setupSensor(ModelType mt) {
		this.setModeltype(mt);
	}

	private ModelType modeltype = ModelType.DEFAULT;
	
	public ModelType getModeltype() {
		return modeltype;
	}

	public void setModeltype(ModelType modeltype) {
		this.modeltype = modeltype;
		//TODO release previous callable functions.
		setupCallableModel(modeltype);
	}

	public SensorModel callableModelFunction;
	private void setupCallableModel(ModelType model_type) {
		// TODO Auto-generated method stub
		switch(modeltype){
		case BEAM_MODEL:
			this.modeltype = ModelType.BEAM_MODEL;
			//TODO setupCallableModel(this.modeltype);
			this.callableModelFunction = new BeamModel();//new BeamModel
			break;
			
		case LOSS_FUNCTION:
			this.modeltype = ModelType.LOSS_FUNCTION;
			//TODO setupCallableModel(this.modeltype);
			this.callableModelFunction = new LossFunction();//new LossFunction
			break;

		default:
			//TODO setupCallableModel(ModelType.BEAM_MODEL);
			this.callableModelFunction = new BeamModel();//new BeaModel
			break;
		}
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public class MCLLaserSensorData extends MCLSensor.MCLSensorData{
		
		public MCLLaserSensorData() {
			this.setBeamRange(null);
			this.setBeamBearing(null);
		}
		
		public MCLLaserSensorData(float[] rm) {
			this();
			this.beamrange = rm;
		}

		//TODO modifying following getters and setters.
		public float getMaxDist() {
			return maxdist;
		}
		public void setMaxDist(float maxdist) {
			this.maxdist = maxdist;
		}
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
			return beamrange;
		}
		public void setBeamRange(float[] beamrange) {
			this.beamrange = beamrange;
		}
		private float[] beamrange;
		private float[] beambearing;
		private int beamcount;
		private float maxdist;
	}

}
