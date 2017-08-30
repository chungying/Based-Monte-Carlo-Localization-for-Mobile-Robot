package util.measurementmodel;

import java.util.List;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import util.metrics.Particle;
import util.sensormodel.callbackfunc.BeamModel;
import util.sensormodel.callbackfunc.LogBeamModel;
import util.sensormodel.callbackfunc.LossFunction;
import util.sensormodel.callbackfunc.SensorModelCallback;

public class MCLLaserModel extends LaserModel{
	
	public MCLLaserModel(){}
	
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
	 * calculating importance factor of particles based on .
	 * @param laserData 
	 */
	public void calculateParticleWeight(List<Particle> set, /*LaserScanData data,*/ LaserData laserData) {
		try {
			this.callableModelFunction.call(set, laserData);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@Parameter(names = {"-sm","--sensormodel"}, 
			description = "selection of sensor model of particle filter, default model is beam model.", 
			required = false, arity = 1, converter = ModelTypeConverter.class)
	private ModelType modeltype = ModelType.DEFAULT;
	
	@Override
	public void setupSensor() throws Exception {
		super.setupSensor();
		this.setModeltype(this.modeltype);
	}
	
	public void setModeltype(ModelType modeltype) {
			this.modeltype = modeltype;
			setupCallableModel(modeltype);
	}
	
	public ModelType getModeltype() {
		return modeltype;
	}
	
	public SensorModelCallback<List<Particle>, LaserData, Float> callableModelFunction;
	private void setupCallableModel(ModelType model_type) {
		switch(modeltype){
		case BEAM_MODEL:
			this.modeltype = ModelType.BEAM_MODEL;
			this.callableModelFunction = new BeamModel();
			break;
			
		case LOSS_FUNCTION:
			this.modeltype = ModelType.LOSS_FUNCTION;
			this.callableModelFunction = new LossFunction();
			break;

		case LOG_BEAM_MODEL:
			this.modeltype = ModelType.LOG_BEAM_MODEL;
			this.callableModelFunction = new LogBeamModel();
		default:
			this.callableModelFunction = new BeamModel();
			break;
		}
	}
	


}
