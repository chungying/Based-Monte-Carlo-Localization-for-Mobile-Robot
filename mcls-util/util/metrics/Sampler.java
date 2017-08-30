package util.metrics;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

import util.grid.Grid;
import util.gui.Panel;
import util.metrics.Transformer;
import util.sensor.LaserSensor;


public class Sampler {
	@Parameter(names = {"-h", "--help"}, help = true)
	public boolean help;
	
	//sampling parameters
	@Parameter(names = "--splitNumber", description = "How many parts the energy grid map should be split, default is 4.", required = false, arity = 1)
	private int splitNumber = 4;
	@Parameter(names = "--sample", description = "the number of samples, default number is 1000.", required = false, arity = 1)
	private int samples = 1000;
	
	//visualization window parameters
	@Parameter(names = {"-vl","--visualization"}, description = "whether the visualization is performed, default is false", required = false, arity = 1)
	private boolean visualization = false;
	private int statisticsRange = 100;
	private int imageHeight = 300;
	private int bandWidth = 3;
	
	@ParametersDelegate
	private LaserSensor laser = new LaserSensor();
	
	//simulated sensor's parameters
	@Parameter(names = {"-o","--orientation"}, description = "the resolution of orientation, default number is 18.", required = false, arity = 1)
	private int orientation  = 18;
	//ray-casting map
	@Parameter(names = {"-i","--image"}, description = "the image of map, default is \"file:///Users/Jolly/git/Cloud-based MCL/jpg/simmap.jpg\"", required = false, arity = 1)
	private  String imagePath = "file:///Users/Jolly/git/Cloud-based MCL/jpg/simmap.jpg";
	
	public static void main(String[] args) throws Exception{
		
		Sampler sp = new Sampler();
		//JCommander parser
		JCommander jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(sp);
		jc.parse(args);
		if(sp.help){
			jc.usage();
			System.exit(0);
		}

		long time = System.currentTimeMillis();
		Map<Float, Integer> splitKeysMap = sp.sampler();
		time = System.currentTimeMillis() - time;
		if(sp.visualization){
			System.out.println("This programe is finished in "+ time +" ms.");
			System.out.println("The split keys are:");
		}
		
		System.out.print("\"{SPLITS =>[");
		for(Entry<Float, Integer> entry: splitKeysMap.entrySet()){
			System.out.print("'"+entry.getKey()+"', ");
		}
		for(int i = 1 ; i < sp.splitNumber-1;i++){
			System.out.printf("'"+"%04d"+"', ", i*1000/sp.splitNumber);
		}
		System.out.printf("'"+"%04d"+"'", (sp.splitNumber-1)*1000/sp.splitNumber);
		System.out.println("]}\"");
		
	}

	public Map<Float, Integer> sampler() throws Exception{

		int columnWidth = Math.round(samples/splitNumber);
		NavigableMap<Float, Integer> map = new TreeMap<Float, Integer>();
		int[] list = new int[statisticsRange];
		Map<Float, Integer> splitKeysMap = new TreeMap<Float, Integer>();
//		List<Float> splitKeys = new ArrayList<Float>();
		laser.angle_min = -90;
		laser.angle_max = 90;
		laser.angle_resolution = Math.round(360/orientation);
		laser.range_min = 0f;
		laser.range_max = -1;
		sampling(map, new Grid(/*orientation, orientation/2+1, -1,*/ imagePath, laser), samples);
		
		statistic(splitKeysMap, list, map, columnWidth, statisticsRange);
		
		if(this.visualization){
			drawImage(list, statisticsRange, imageHeight, bandWidth);
		}
		
		return splitKeysMap;
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
//		assert(grid.orientation==grid.laser.getOrientation());
		grid.readMapImageFromLocal();
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
			int z = random.nextInt(grid.laser.getOrientation());
			List<Float> circle = grid.getLaserDist(x, y).getKey();
			List<Float> measurements = Transformer.drawMeasurements(circle.toArray(new Float[circle.size()]), z);
//			assert(grid.max_distance==grid.laser.range_max);
			float energy = Transformer.CalculateEnergy(measurements, grid.laser.range_max);
			
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
