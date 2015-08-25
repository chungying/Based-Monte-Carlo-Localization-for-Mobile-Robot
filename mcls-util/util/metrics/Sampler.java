package util.metrics;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

import util.grid.Grid;
import util.gui.Panel;
import util.metrics.Transformer;


public class Sampler {
	private static int distribution = 30;
	private static int samples = 1000;
	
	private static int statisticsRange = 100;
	private static int imageHeight = 300;
	private static int bandWidth = 3;
	
	private static int orientation  = 360;
	private static int sensorNumber = 181;
	private static String imagePath = "file:///home/wuser/backup/jpg/bigmap.jpg";
	
	public static void main(String[] args) throws Exception{
//		if(args.length<4){
//			System.exit(-1);
//		}
//		String imagePath = args[0];
//		int distribution = Integer.parseInt(args[1]);
//		int samples = Integer.parseInt(args[2]);
//		int orientation = Integer.parseInt(args[3]);
		

		long time = System.currentTimeMillis();
		sampler(imagePath, distribution, samples, orientation, orientation/2+1);
		time = System.currentTimeMillis() - time;
		System.out.println("time: "+ time +" ms");
		
	}

	public static Map<Float, Integer> sampler(String imagePath, int distribution, int samples, 
			int orientation, int sensorNumber, int statisticsRange, int imageHeight, int bandWidth
			) throws Exception{

		int columnWidth = Math.round(samples/distribution);
		NavigableMap<Float, Integer> map = new TreeMap<Float, Integer>();
		int[] list = new int[statisticsRange];
		Map<Float, Integer> splitKeysMap = new TreeMap<Float, Integer>();
//		List<Float> splitKeys = new ArrayList<Float>();
		
		sampling(map, new Grid(orientation, sensorNumber, imagePath), samples);
		
		//statistic(splitKeysMap, list, map, columnWidth, statisticsRange);
		statistic(splitKeysMap, list, map, columnWidth, statisticsRange);
		
		for(Entry<Float, Integer> entry: splitKeysMap.entrySet()){
			System.out.print(entry.getKey()+":");
			System.out.println(entry.getValue());
		}
		
		drawImage(list, statisticsRange, imageHeight, bandWidth);
		
		return splitKeysMap;
	}
	
	public static Map<Float, Integer> sampler(String imagePath, int distribution, int samples) throws Exception {
		return sampler(imagePath, distribution, samples, orientation, sensorNumber, statisticsRange, imageHeight, bandWidth);
	}
	
	public static Map<Float, Integer> sampler(String imagePath, int distribution, int samples, int orientation, int sensorNumber) throws Exception {
		return sampler(imagePath, distribution, samples, orientation, sensorNumber, statisticsRange, imageHeight, bandWidth);
	}
	
	public static void statistic(Map<Float, Integer> splitKeysMap/*, List<Float> splitKeys*/, int[] list, NavigableMap<Float,Integer> map, int columnWidth, int statisticsRange){
		int i  = 1;
		Float lastKey = 0.0f;
		Integer lastValue = 0;
		for(Entry<Float,Integer> e: map.entrySet()){
			Float currentKey = e.getKey();
			Integer currentValue = e.getValue() + lastValue;
			
			if(lastValue<(i*columnWidth) && currentValue>=(i*columnWidth)){
				Float deltaKey = currentKey - lastKey;
				Integer deltaValue = currentValue - lastValue;
				Integer diff = i*columnWidth - lastValue;
				Float splitKey = lastKey + (diff/deltaValue) * deltaKey;
				splitKeysMap.put(splitKey, currentValue);
//				splitKeys.add(splitKey);
				i++;
			}
			
			lastKey = currentKey;
			lastValue = currentValue;
			/*****/
			int value = Math.round(e.getKey()*(statisticsRange-1));
			list[value] = list[value]+e.getValue();
		}
	}
	
	public static void drawImage(int[] list, int statisticsRange, int imageHeight, int bandWidth){
		//initialize new image
		BufferedImage image = new BufferedImage(statisticsRange*bandWidth, imageHeight, BufferedImage.TYPE_INT_BGR);
		Graphics2D graph = (Graphics2D) image.getGraphics();
		//find the max height value
		int maxHeight = 0;
		for(int value:list){
			if(value > maxHeight)
				maxHeight = value;
		}
		//draw each histogram
		for(int j = 0; j < list.length; j++){
			int h = (int) Math.round( ( (double)list[j] / (double)maxHeight ) * imageHeight);
			int y = imageHeight-h;
			int x = j*bandWidth;
			graph.drawRect(x, y, bandWidth, h);
		}
		//update frame
		JFrame frame = new JFrame("distergram");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Panel panel = new Panel(image);
		frame.add(panel);
		frame.setSize(statisticsRange*bandWidth, imageHeight);
		frame.setVisible(true);
	}
	
	private static void sampling(NavigableMap<Float, Integer> map, Grid grid, int samples) throws Exception {
		grid.readmap();
		int width = grid.width;
		int height = grid.height;
		Random random = new Random();
		for(int i = 0; i < samples; i++){
			int x = random.nextInt(width);
			int y = random.nextInt(height);
			while(grid.map_array(x, y)==Grid.GRID_OCCUPIED){
				x = random.nextInt(width);
				y = random.nextInt(height);
			}
			int z = random.nextInt(grid.orientation);
			List<Float> circle = grid.getLaserDist(x, y).getKey();
			float[] measurements = Transformer.drawMeasurements(circle.toArray(new Float[circle.size()]), z);
			float energy = Transformer.CalculateEnergy(measurements);
			
			addOne(map,energy);
		}		
	}
	
	public static void addOne(NavigableMap<Float, Integer> map, Float key){
		if(map.get(key)!=null)
			map.put(key, (map.get(key)+1));
		else
			map.put(key, 1);
	}
	
}
