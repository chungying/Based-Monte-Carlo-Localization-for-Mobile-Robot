package util.grid;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.AbstractMap.SimpleEntry;

import util.Distribution;
import util.Transformer;
import util.pf.Particle;
import util.pf.sensor.laser.LaserSensor;

public class GridTools {
	

	/**
	 * This main is showing how's the uniform distribution among the clusters, there are 
 	 * 1, 2, 4, 8, and 16 nodes handling the grid map.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException{
		int orientation = 4;
		LaserSensor laserConfig = new LaserSensor();
		laserConfig.angle_min = -90;
		laserConfig.angle_max = 90;
		laserConfig.angle_resolution = Math.round(360/orientation);
		laserConfig.range_min = 0f;
		laserConfig.range_max = -1;
		@SuppressWarnings("resource")
		Grid grid = new Grid(/*4,4,-1,*/"file:///home/wuser/backup/jpg/sim_map.jpg"/*,null*/);
		grid.readMapImageFromLocal();
		
		int clusterNodesNumbers[] = {1,2,4,8,16};
		for (int number : clusterNodesNumbers) {
			List<Integer> list = new ArrayList<Integer>();
			for (int i = 0; i <= number; i++) {
				list.add(i * (1000 / number));
			}
			Map<Integer, Integer> map = new TreeMap<Integer, Integer>();
			for (int i = 0; i < number; i++) {
				map.put(i, 0);
			}
			Random rand = new Random();
			for (int x = 0; x < grid.width; x++) {
				for (int y = 0; y < grid.height; y++) {
					if (grid.map_array(x, y) == Grid.GRID_EMPTY) {
						rand.setSeed(Long.parseLong(Transformer.xy2String(
								x, y)));

						for (int i = 0; i < number; i++) {
							int r = rand.nextInt(1000);
							if (r >= list.get(i) && r < list.get(i + 1)) {
								map.put(i, map.get(i) + 1);
							}
						}
					}
				}

			}
			System.out.println(map);
		}
	}
	
	/**
	 * 
	 * @param X
	 * @param Y
	 * @param grid
	 * @return true if the position is valid, otherwise false 
	 */
	public static boolean boundaryCheck(int X, int Y, Grid grid){
		//check boundary
		if(X>grid.width || X<0 || Y>grid.height || Y<0){
			return false;
		}
		//check if it is occupied
		if(grid.map_array(X, Y)==Grid.GRID_OCCUPIED){
			return false;
		}
		return true;
	}

	public static boolean boundaryCheck(Particle p, Grid grid){
		return boundaryCheck(p.getX(), p.getY(), grid);
	}
	
	static public SimpleEntry<List<Float>, List<Point>> getLaserDist(
			Grid grid, LaserSensor laser, double X, double Y, double H){
		return getLaserDist(grid, laser, X, Y, H, false);
	}
	
	static public SimpleEntry<List<Float>, List<Point>> getLaserDist(
			Grid grid, LaserSensor laser, double X, double Y, double H, boolean noiseFlag){
		if(!GridTools.boundaryCheck((int)Math.round(X), (int)Math.round(Y), grid))
			return new SimpleEntry<List<Float>, List<Point>>(null, null);
		List<Float> measurements = new ArrayList<Float>();
		List<Point>  measurementPoints = new ArrayList<Point>();
		int checkX, checkY;
		int step;
		double bearing = H+(double)laser.angle_min;
		double max_bearing = bearing+(double)(laser.angle_max-laser.angle_min);
		for ( ;bearing <= max_bearing ;	bearing+=(double)laser.angle_resolution) {
			step = 0;
			checkX = (int)Math.round(X);
			checkY = (int)Math.round(Y);
			//TODO !Distribution.boundaryCheck(res, this.grid)
			
			while (grid.map_array(checkX, checkY) == Grid.GRID_EMPTY) {
				step++;//begin from 1 unit length.
				checkX = (int) Math.round((X + step
						* Math.cos(bearing * Math.PI / 180)));
				checkY = (int) Math.round((Y + step
						* Math.sin(bearing * Math.PI / 180)));
				//check max_dist
				if(step>=laser.range_max)
					break;
			}
			
			double dist = Math.sqrt(((checkX - X) * (checkX - X))
					+ ((checkY - Y) * (checkY - Y)));
			Point hitPoint = new Point(checkX, checkY);
			if(noiseFlag){
				dist = dist + Distribution.sample_normal_distribution(laser.variance);
				hitPoint.setLocation(
						(int) Math.round((X + dist * Math.cos(bearing * Math.PI / 180))), 
						(int) Math.round((Y + dist * Math.sin(bearing * Math.PI / 180))));
			}
			
			//Unit of measurements has to be changed into pixel. done!
			measurements.add((float) dist);

			measurementPoints.add(hitPoint);
		}
		return new SimpleEntry<List<Float>, List<Point>>(measurements, measurementPoints);
		
	}
	
}
