package util.recorder;

import java.util.ArrayList;

import util.robot.Pose;

public class Record {
	public ArrayList<PoseWithCovariance> estimates = new ArrayList<PoseWithCovariance>();
	public ArrayList<PoseWithTimestamp> groundTruth = new ArrayList<PoseWithTimestamp>();
	public ArrayList<PoseWithTimestamp> odometricEstimate = new ArrayList<PoseWithTimestamp>();
//	public ArrayList<Boolean> converged = new ArrayList<Boolean>();
	//TODO if there is any other result should be recorded, add them here
	
	public static ArrayList<Record> allRecords = new ArrayList<Record>();
	public static void collect(Record r, PoseWithCovariance e, PoseWithTimestamp truth, PoseWithTimestamp odom){
		r.estimates.add(e);
		r.groundTruth.add(truth);
		r.odometricEstimate.add(odom);
	}
	
	public static void statistics(){
		System.out.println("has collected " + Record.allRecords.size() + " run(s) of data.");
		int i = 0 ;
		int acceptance = 0;
		ArrayList<Pose> averageErr = new ArrayList<Pose>();
		for(Record rec: Record.allRecords){
			i++;
			Pose xt_hat = rec.estimates.get(rec.estimates.size()-1), 
			xt = rec.groundTruth.get(rec.groundTruth.size()-1);
			Pose error = xt.minus(xt_hat);
			averageErr.add(error);
			System.out.println(i + " "
//					+ "estimate: " + rec.estimates.size() + " "
					+ "estimate: " + xt_hat + " "
//					+ "groundTruth: " + rec.groundTruth.size()
					+ "groundTruth: " + xt + " "
					+ "error: " + error
					);
			//TODO successful localization
			boolean succ = successfulLocalization(
					rec.estimates.get(rec.estimates.size()-1), 
					rec.groundTruth.get(rec.groundTruth.size()-1),
					10//pixels
					);
			if(succ)
				acceptance++;
			//TODO RMSE measure
			//TODO KLD measure
		}
		//TODO output statistics
		double sumX, sumY, sumCos, sumSin;
		for(Pose p: averageErr){
			
		}
		System.out.println("The successful rate is " + acceptance + " out of " + i + ", " 
		+ String.format("%.4f %%", (double)acceptance/i*100) + ".");
	}
	
	private static boolean successfulLocalization(Pose estimate, Pose groundTruth, double threshold){
		if(Math.abs(estimate.X - groundTruth.X)>threshold)
			return false;
		if(Math.abs(estimate.Y - groundTruth.Y)>threshold)
			return false;
		return true;
	}
}
