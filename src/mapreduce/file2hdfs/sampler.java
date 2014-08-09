package mapreduce.file2hdfs;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

import samcl.Grid;
import util.gui.Panel;
import util.metrics.Transformer;

public class Sampler {
	private static int distribution = 10;
	private static int samples = 1000;
	
	private static int statisticsRange = 100;
	private static int imageHeight = 300;
	private static int bandWidth = 3;
	
	private static int orientation  = 72;
	private static int sensorNumber = 19;
	private static String imagePath = "file:///Users/ihsumlee/Jolly/jpg/map.jpg";
	
	
	public static void main(String[] args) throws IOException{
		for(int i = 0 ; i < 10; i ++){
			long time = System.currentTimeMillis();
			sampler(imagePath, distribution, samples, orientation, orientation/2+1);
			time = System.currentTimeMillis() - time;
			System.out.println("time: "+ time +" ms");
		}
	}

	public static List<Float> sampler(String imagePath, int distribution, int samples, 
			int orientation, int sensorNumber, int statisticsRange, int imageHeight, int bandWidth
			) throws IOException{

		int columnWidth = Math.round(samples/distribution);
		NavigableMap<Float, Integer> map = new TreeMap<Float, Integer>();
		int[] list = new int[statisticsRange];
		List<Float> splitKeys = new ArrayList<Float>();
		
		sampling(map, new Grid(orientation, sensorNumber, imagePath), samples);
		
		statistic(splitKeys, list, map, columnWidth, statisticsRange);
		
		for(Float key: splitKeys){
			System.out.println(key);
		}
		
		drawImage(list, statisticsRange, imageHeight, bandWidth);
		
		return splitKeys;
	}
	
	public static List<Float> sampler(String imagePath, int distribution, int samples) throws IOException {
		return sampler(imagePath, distribution, samples, orientation, sensorNumber, statisticsRange, imageHeight, bandWidth);
	}
	
	public static List<Float> sampler(String imagePath, int distribution, int samples, int orientation, int sensorNumber) throws IOException {
		return sampler(imagePath, distribution, samples, orientation, sensorNumber, statisticsRange, imageHeight, bandWidth);
	}
	
	public static void statistic(List<Float> splitKeys, int[] list, NavigableMap<Float,Integer> map, int columnWidth, int statisticsRange){
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
				splitKeys.add(splitKey);
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
		BufferedImage image = new BufferedImage(statisticsRange*bandWidth, imageHeight, BufferedImage.TYPE_INT_BGR);
		Graphics2D graph = (Graphics2D) image.getGraphics();
		
		int maxHeight = 0;
		for(int value:list){
			if(value > maxHeight)
				maxHeight = value;
		}
		
		for(int j = 0; j < list.length; j++){
			int h = (int) Math.round( ( (double)list[j] / (double)maxHeight ) * imageHeight);
			int y = imageHeight-h;
			int x = j*bandWidth;
			graph.drawRect(x, y, bandWidth, h);
		}
		
		JFrame frame = new JFrame("distergram");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Panel panel = new Panel(image);
		frame.add(panel);
		frame.setSize(statisticsRange*bandWidth, imageHeight);
		frame.setVisible(true);
	}
	
	private static void sampling(NavigableMap<Float, Integer> map, Grid grid, int samples) throws IOException {
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
