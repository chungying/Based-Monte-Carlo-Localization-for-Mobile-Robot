package util.pf.sensor.laser;

import java.util.List;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import util.pf.Particle;
import util.pf.sensor.Sensor;
import util.pf.sensor.laser.callbackfunc.BeamModel;
import util.pf.sensor.laser.callbackfunc.LogBeamModel;
import util.pf.sensor.laser.callbackfunc.LossFunction;
import util.pf.sensor.laser.callbackfunc.SensorModelCallback;

public class MCLLaserModel extends LaserModel{
	
	@Parameter(names = {"-sm","--sensormodel"}, 
			description = "selection of sensor model of particle filter, default model is beam model.", 
			required = false, arity = 1, converter = ModelTypeConverter.class)
	private ModelType modelType = ModelType.DEFAULT;
	
	public static enum ModelType{
		BEAM_MODEL, LOSS_FUNCTION, LOG_BEAM_MODEL, DEFAULT;

		public static ModelType fromString(String value) {
			for(ModelType output:ModelType.values()){
				if(output.toString().equalsIgnoreCase(value)){
					return output;
				}
			}
			return ModelType.DEFAULT;
		}
	}
	
	public static class ModelTypeConverter implements IStringConverter<ModelType> {
		 
	    @Override
	    public ModelType convert(String value) {
	    	ModelType convertedValue = ModelType.fromString(value);
	 
	        if(convertedValue == null) {
	            throw new ParameterException("Value " + value + "can not be converted to OutputEnum. " +
	                    "Available values are: console, pdf, xls.");
	        }
	        return convertedValue;
	    }
	}

	/**
	 * 
	 * @param set
	 * @param laserData
	 * @return total weight by BeamModel.call(...), and zero by others.
	 * 
	 */
	public double calculateParticleWeight(List<Particle> set, final LaserModelData laserData) {
		try {
			return this.callableModelFunction.call(set, laserData);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;//if failed return 0.
	}
	
	@Override
	public void setupSensor(Sensor sensor) throws Exception {
		super.setupSensor(sensor);
		if(MCLLaserModel.class.isAssignableFrom(sensor.getClass())){
			this.modelType = ((MCLLaserModel)sensor).modelType;
		}
		setupCallableModel(modelType);
	}
	
	public ModelType getModeltype() {
		return modelType;
	}
	
	protected SensorModelCallback<List<Particle>, LaserModelData, Double> callableModelFunction = new BeamModel();;
	
	protected void setupCallableModel(ModelType modelType) {
		switch(modelType){
		case BEAM_MODEL:
			this.modelType = ModelType.BEAM_MODEL;
			this.callableModelFunction = new BeamModel();
			break;
			
		case LOSS_FUNCTION:
			this.modelType = ModelType.LOSS_FUNCTION;
			this.callableModelFunction = new LossFunction();
			break;

		case LOG_BEAM_MODEL:
			this.modelType = ModelType.LOG_BEAM_MODEL;
			this.callableModelFunction = new LogBeamModel();
			break;
		
		default:
				break;
		}
	}
	


}
