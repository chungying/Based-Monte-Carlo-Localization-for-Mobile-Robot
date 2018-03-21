package samclroe;

import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import samcl.SAMCL;
import util.Transformer;
import util.grid.Grid;
import util.oewc.Oewc;
import util.pf.Particle;
import util.pf.sensor.laser.LaserModel.LaserModelData;
import util.pf.sensor.laser.MCLLaserModel.*;
import util.robot.RobotState;

public class SAMCLROE extends SAMCL{

	@Override
	public long raycastingUpdate( List<Particle> src, Grid grid) throws Exception {
		long trasmission = System.currentTimeMillis();
		//get sensor data of all particles.
		if (this.preCachingFlag) {
			//get measurements from local database.
			for(Particle p: src){
				boolean status = grid.map_array[p.getX()][p.getY()] == Grid.GRID_OCCUPIED;
				if(status){
					p.setMeasurements(null);
				}else{
					p.setMeasurements(grid.G[p.getX()][p.getY()].getMeasurements(-1));
				}
				
//				if( !p.isIfmeasurements()){
//					throw new Exception("cannot get measurements for this particle.");
//				}
			}
		}
		else {
			//TODO update raycasting right away
		}
		return System.currentTimeMillis() - trasmission;
	}
	
	Random random = new Random();
	@Override
	public long batchWeight( List<Particle> src, LaserModelData laserData, Grid grid)
			throws Exception {
		
		long weightTime = System.currentTimeMillis();
		Transformer.filterParticle(src);
		
		//remove duplicated particles in X-Y domain
		//this.updateImagePanel(grid, null, null, src, null);
		for(Particle p : src){
				if (grid.map_array(p.getX(), p.getY()) == Grid.GRID_EMPTY) {
					Entry<Integer, Float> entry = Oewc.singleParticleModified(
							laserData.data.beamranges, 
							p.getMeasurements());//Note that p.getMeasurements is circle measurements
					p.setTh(Transformer.Z2Th(entry.getKey(), this.sensor.getOrientation()));
					//p.setWeightForNomalization(entry.getValue());
					p.setOriginalWeight(entry.getValue());
				} else {
					//if the position is occupied, then assign the worst weight.
					//p.setWeightForNomalization(1);
					p.setOriginalWeight(Double.MAX_VALUE);
				}
		}
		
		
		return System.currentTimeMillis() - weightTime;
	}

	@Override
	public void localResampling(List<Particle> src, List<Particle> dst,
			RobotState robot,
			LaserModelData laserData,
			Grid grid,
			Particle bestParticle) {
		//Tournament
		dst.clear();
		dst.add(bestParticle);
		for (int i = dst.size(); i < this.Nl; i++) {
			//Roulette way
			//Tournament way
			Particle particle = Transformer.tournament(tournamentPresure, src);
			dst.add(particle.clone());
			//System.out.println();
		}
		//normalization for tournament
		int count = 0; 
		double minimumWeight = 1;//Double.MAX_VALUE
		for(Particle p: src){
			if(count == 0 || minimumWeight > p.getOriginalWeight()) {
				minimumWeight = p.getOriginalWeight();
			}
			//System.out.println("unnormalized : [" + (float)p.getDX() + ' ' + (float)p.getDY() + ' ' + p.getOriginalWeight()+']');
			count++;
		}
		//System.out.println("minimumWeight is " + minimumWeight);
		for(Particle p: src){
			p.setOriginalWeight(p.getOriginalWeight() - minimumWeight);
			//System.out.println("normalized : [" + (float)p.getDX() + ' ' + (float)p.getDY() + ' ' + p.getOriginalWeight()+']');
		}
	}

	public SAMCLROE() {
		super();
		System.out.println("setting sensor model type as " + ModelType.LOSS_FUNCTION);
		this.sensor.setupCallableModel(ModelType.LOSS_FUNCTION);
		System.out.println("the model type is " + this.sensor.getModeltype());
	}

}
