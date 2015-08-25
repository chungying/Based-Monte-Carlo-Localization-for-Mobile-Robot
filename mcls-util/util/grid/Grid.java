/**
 * 
 */
package util.grid;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Threads;
import org.apache.hadoop.mapreduce.Reducer.Context;

import util.gui.MouseListener;
import util.metrics.Particle;
import util.metrics.Transformer;

/**
 * @author w514
 * 
 */
public class Grid extends MouseAdapter {
	public int RPCcount = 0;

	public enum Counters {
		READMAP, READ_SUCCEED, READ_FAILED
	}

	/**
	 * needed class
	 */
	public class position {
		public int sensor_number;
		public Point[] measurement_points;
		public float[] circle_measurements;
		public float[] energy;

		/**
		 * @param z
		 * @return <pre>
		 * if z >= 0
		 * return the range of the measurements of this orientation
		 * else
		 * return all of the measurements
		 * </pre>
		 */
		public float[] getMeasurements(int z) {
			if (z >= 0) {
				float[] measurements = new float[this.sensor_number];
				//int bias = (this.sensor_number - 1) / 2;
				int globalIndex;
				for (int i = 0; i < this.sensor_number; i++) {
					globalIndex = Transformer.local2global(i, z, this.circle_measurements.length);
					//globalIndex = ((z - bias + i + this.circle_measurements.length) % this.circle_measurements.length);
					measurements[i] = this.circle_measurements[globalIndex];
				}
				return measurements;
			} else {
				return this.circle_measurements;
			}
		}

		/**
		 * Deprecated
		 * 
		 * @param z
		 * @return <pre>
		 * return the energy of the orientation
		 * </pre>
		 */
		public float getEnergy(int z) {
			return energy[z];
		}

		/**
		 * @param z
		 * @return <pre>
		 * if z >= 0
		 * return the points of the measurements of this orientation
		 * else
		 * return all of the points of the measurements
		 */
		public Point[] getMeasurement_points(int z) {
			if (z >= 0) {
				Point[] measurements = new Point[this.sensor_number];
				//int bias = (this.sensor_number - 1) / 2;
				int globalIndex;
				for (int i = 0; i < this.sensor_number; i++) {
					globalIndex = Transformer.local2global(i, z, this.circle_measurements.length);
					//globalIndex = ((z - bias + i + this.circle_measurements.length) % this.circle_measurements.length);
					measurements[i] = this.measurement_points[globalIndex];
				}
				return measurements;
			} else {
				Point[] measurements = this.measurement_points;
				return measurements;
			}
		}

		private void setup() {
			this.sensor_number = (this.circle_measurements.length / 2) + 1;
			this.energy = new float[this.circle_measurements.length];
			float[] zt = null;
			for (int i = 0; i < this.circle_measurements.length; i++) {
				zt = this.getMeasurements(i);
				this.energy[i] = 0.0f;
				for (int j = 0; j < zt.length; j++) {
					this.energy[i] = this.energy[i] + zt[j];
				}
				this.energy[i] = this.energy[i] / ((float) zt.length);
			}
		}

		/**
		 * @param float[] measurements
		 * 
		 *        <pre>
		 * assign the measurements 
		 * and then compute the energy of all orientation
		 * </pre>
		 */
		public position(float[] measurements) {
			super();
			this.circle_measurements = measurements;
			this.setup();
		}

		/**
		 * @param measurements
		 *            all of the measurements around the position
		 * @param measurement_points
		 *            all of the points of the measurements
		 * 
		 *            <pre>
		 * assign the measurements and all of the points of the measurements
		 * and then compute the energy of all orientation
		 * </pre>
		 */
		public position(float[] measurements, Point[] measurement_points) {
			super();
			this.circle_measurements = measurements;
			this.measurement_points = measurement_points;
			this.setup();
		}

		/**
		 * @param sensor_number
		 */
		public position(int orientation) {
			super();
			this.sensor_number = orientation / 2 + 1;
			this.circle_measurements = new float[orientation];
		}

		/**
		 * 
		 */
		public position() {
			super();
		}

		@Override
		public String toString() {
			return "position [circle_measurements="
					+ Arrays.toString(circle_measurements) + ", energy="
					+ Arrays.toString(energy) + "]";
		}

	}

	/**
	 * properties
	 */
	public static boolean GRID_OCCUPIED = false;
	public static boolean GRID_EMPTY = true;
	private int safe_edge = 10;
	public int width;
	public int height;
	public int orientation;
	public double orientation_delta_degree;
	public int sensor_number;
	public double sensor_delta_degree;
	public float max_distance;
	private String map_filename;
	/**
	 * windows
	 */
	public JFrame show_window;
	public JFrame map_window;
	public JLabel show_window_label;
	public ImageIcon show_window_label_icon;
	public MouseAdapter mouse_adapter;

	public BufferedImage map_image;
	public BufferedImage showimage;
	public BufferedImage temp_image;
	public MouseListener window;
	public MouseMotionListener mouse = new MouseMotionListener() {

		@Override
		public void mouseDragged(MouseEvent e) {

		}

		@Override
		public void mouseMoved(MouseEvent e) {

		}

	};
	/**
	 * readmap()
	 */
	private boolean[][] map_array;
	/**
	 * 
	 */
	public position[][] G = null;

	/**
	 * 1 q`
	 * 
	 * @param Sensor_number
	 *            :the third dimension
	 * @param filename
	 *            :jpg,png...
	 * @throws MalformedURLException
	 */
	public Grid(int orientation, int Sensor_number, String filename) {
		// super();
		this.orientation = orientation;
		this.orientation_delta_degree = 360 / orientation;
		this.sensor_number = Sensor_number;
		this.sensor_delta_degree = 180 / (Sensor_number - 1);
		this.map_filename = filename;

	}

	public Grid() {
		// super();
		System.out.println("new Grid()");
	}
	

	private HConnection connection = null;
	private byte[] family = null;
	private byte[] energy = null;
	
	public HTable getTable(String tablename) throws IOException{
		if(tablename != null){
			System.out.println("get new HTable from Grid.");
			ThreadPoolExecutor pool = new ThreadPoolExecutor(3,  Integer.MAX_VALUE, 60l, TimeUnit.SECONDS,
					new SynchronousQueue<Runnable>(), Threads.newDaemonThreadFactory("htable"));
			pool.allowCoreThreadTimeOut(true);
			return (HTable)this.connection.getTable(tablename, pool );
//			return (HTable)this.connection.getTable(tablename);
		}
		else{
			System.out.println("tablename is null.Cannot get HTable from Grid.");
			return null;
		}
	}
	
	public void closeTable() throws IOException {
		connection.close();
	}

	public void setupTable(Configuration conf) throws IOException {
		System.out.println("set up table");
		this.family = Bytes.toBytes("distance");
		this.energy = Bytes.toBytes("energy");
		//setup connection to afford the getTable(tableName);
		connection = HConnectionManager.createConnection(conf);
		
	}

	public void scan(HTable table, List<Particle> ser_set, float lowerBoundary,
			float upperBoundary) throws IOException {
		ser_set.clear();
		//energy format
		// "0.0~1.0", "energy":"(X,Y)"  => "Z"
		String lower = String.valueOf(lowerBoundary);
		String upper = String.valueOf(upperBoundary);
		Scan scan = new Scan(Bytes.toBytes(lower), Bytes.toBytes(upper));
		scan.addFamily(this.energy);

		int caching = 1024;
		scan.setCaching(caching);

		Filter lowerFilter = new RowFilter(CompareFilter.CompareOp.GREATER,
				new BinaryComparator(Bytes.toBytes(String.valueOf(lower))));
		Filter upperFilter = new RowFilter(CompareFilter.CompareOp.LESS,
				new BinaryComparator(Bytes.toBytes(String.valueOf(upper))));
		FilterList fls = new FilterList();
		fls.addFilter(lowerFilter);
		fls.addFilter(upperFilter);
		scan.setFilter(fls);

		ResultScanner scanner = table.getScanner(scan);
		 
		for (Result[] results = scanner.next(caching); results.length != 0; results = scanner
				.next(caching)) {
			// for count RPC
			this.RPCcount++;
			for (Result result : results) {
				// List<Cell> Cells = result.listCells();
				//improved 0723
				for (Cell cell : result.rawCells()) {
					String XYstr = Bytes.toString(CellUtil.cloneQualifier(cell));
//					System.out.println("rowkey: "+XYstr);
					//convert Cell to Particle
					//improved 0723
					int x = Transformer.rowkeyString2X(XYstr);
					int y = Transformer.rowkeyString2Y(XYstr);
					int z = Integer.parseInt( Bytes.toString(CellUtil.cloneValue(cell) ) );
//					System.out.println("(x, y, z)=("+x+","+y+","+z+")");
					Particle p = new Particle(x, y, Transformer.Z2Th(z, this.orientation));
//					System.out.println(p.toString());
//					Transformer.rowkeyString2xy(XYstr, p);
//					System.out.println("second "+p.toString());
					if (p.underSafeEdge(width, height, safe_edge))
						ser_set.add(p);
					// System.out.println(p.toString());
					// System.out.println("----------------------------------------------------");
				}
			}
		}
	}

	public void getBatchFromCloud2(HTable table, List<Particle> src) throws Exception{
		// first step: setup List<Get>
		// HTable, Particles
		List<Get> gets = new ArrayList<Get>();
		byte[] fam = Bytes.toBytes("distance");
		for (Particle p : src) {//TODO check
			String str = Transformer.xy2RowkeyString(p.getDX(), p.getDY());
			Get get = new Get(Bytes.toBytes(str));
			get.addFamily(fam);
			gets.add(get);
		}
		
		// second: fetch from the Results to the List<Particles>
		// Particles(X, Y, Z), Results
		Result[] results = table.get(gets);
		if (results.length == src.size()) {
			for (int i = 0; i < results.length; i++) {
				Particle p = src.get(i);
				byte[] resultValue = results[i].getValue(
						fam,
						Bytes.toBytes("data")
						);
				float[] measurements = new float[this.sensor_number];
				if(resultValue!=null){
					for (int sensorIndex = 0; sensorIndex < this.sensor_number; sensorIndex++) {
						measurements[sensorIndex] = 
							Bytes.toFloat(
									//the beam value of byte array
									Transformer.getBA(
											//global index
											Transformer.local2global(
													sensorIndex, 
													Transformer.th2Z(p.getTh(), this.orientation), 
													this.orientation),
											resultValue)
							);
					}
				}else{
					throw new NullPointerException("bad particle"+src.get(i)+"\nrowkey:"+Bytes.toString(results[i].getRow()));
				}
				p.setMeasurements(measurements);
			}
		}else
			throw new IOException("the length is different.");
	}
	
	@Deprecated
	public void getBatchFromCloud(HTable table, List<Particle> src)
			throws IOException {
		// first step: setup List<Get>
		// HTable, Particles
		List<Get> gets = new ArrayList<Get>();
		byte[] fam = Bytes.toBytes("distance");
		for (Particle p : src) {
			String str = Transformer.xy2RowkeyString(p.getDX(), p.getDY());
			Get get = new Get(Bytes.toBytes(str));
			get.addFamily(fam);
			gets.add(get);
		}

		// second: fetch from the Results to the List<Particles>
		// Particles(X, Y, Z), Results
		Result[] results = table.get(gets);
		if (results.length == src.size()) {
			for (int i = 0; i < results.length; i++) {
				//convertResultToParticle(src.get(i), results[i], fam);
				Particle p = src.get(i);
				byte[] value = null;
				int globalIndex;
				//if (!results[i].isEmpty()) {
					float[] measurements = new float[this.sensor_number];
					//int bias = (this.sensor_number - 1) / 2;
					for (int sensorIndex = 0; sensorIndex < this.sensor_number; sensorIndex++) {
						globalIndex = 
								Transformer.local2global(
										sensorIndex, 
										Transformer.th2Z(p.getTh(), this.orientation), 
										this.orientation);
						/*globalIndex = (Transformer.th2Z(p.getTh(), this.orientation) 
								- bias + j + this.orientation) 
								% this.orientation;*/
						value = results[i].getValue(fam,Bytes.toBytes(String.valueOf(globalIndex)));
						//System.out.println(Bytes.toString(value));
						measurements[sensorIndex] = Float.valueOf(Bytes.toString(value));
					}
					p.setMeasurements(measurements);
				//}else
					//throw new IOException("There is no result!!");
			}
		}else
			throw new IOException("the length is different.");

	}

	private float[] getFromCloud2(HTable table, int X, int Y, int Z) throws Exception {
		this.RPCcount++;
		
		String rowkey = Transformer.xy2RowkeyString(X, Y);
		Get get = new Get(Bytes.toBytes(rowkey));
		get.addColumn(this.family, Bytes.toBytes("data"));
		Result result = table.get(get);
		byte[] BA = result.getValue(this.family, Bytes.toBytes("data"));
		if(Z>=0){
			float[] measurements = new float[this.sensor_number];
			//int bias = (this.sensor_number - 1) / 2;
			int globalIndex;
			for (int sensorIndex = 0; sensorIndex < this.sensor_number; sensorIndex++) {
				globalIndex = Transformer.local2global(sensorIndex, Z, this.orientation);
				//globalIndex = ((Z - bias + i + this.orientation) % this.orientation);
				measurements[sensorIndex] = Bytes.toFloat(Transformer.getBA(globalIndex, BA));
			}
			return measurements;
		}else{
			float[] measurements = new float[this.orientation];
			for (int i = 0; i < this.orientation; i++) {
				measurements[i] = Bytes.toFloat(Transformer.getBA(i, BA));
			}
			return measurements;
		}
	}

	@SuppressWarnings("unused")
	@Deprecated
	private float[] getFromCloud(HTable table, int X, int Y, int Z) throws IOException {
		// TODOdone count RPC times
		this.RPCcount++;

		String rowkey = Transformer.xy2RowkeyString(X, Y);
		//System.out.print("family:"+Bytes.toString(family)+"\t");
		//System.out.println("rowkey: " + rowkey);
		Get get = new Get(Bytes.toBytes(rowkey));
		get.addFamily(this.family);
		Result result = table.get(get);
		// List<Cell> cells = result.listCells();
		if (Z >= 0) {
			float[] measurements = new float[this.sensor_number];
			float temp = 0f;
			//int bias = (this.sensor_number - 1) / 2;
			int globalIndex;
			for (int sensorIndex = 0; sensorIndex < this.sensor_number; sensorIndex++) {
				globalIndex = Transformer.local2global(sensorIndex, Z, this.orientation);
				//globalIndex = ((Z - bias + i + this.orientation) % this.orientation);
				// System.out.println("index: " + index);
				temp = Float.parseFloat(Bytes.toString(result.getValue(
						this.family, Bytes.toBytes(String.valueOf(globalIndex)))));
				// System.out.println("temparary float = " + temp);
				measurements[sensorIndex] = temp;

			}
			return measurements;
		} else {
			float[] measurements = new float[this.orientation];
			for (int i = 0; i < this.orientation; i++) {
				measurements[i] = Bytes.toFloat(result.getValue(
						this.family/* Bytes.toBytes("distance") */,
						Bytes.toBytes(String.valueOf(i))));
			}
			return measurements;
		}
	}
	
	public float[] getMeasurementsAnyway(HTable table, boolean onCloud, double x, double y, double head)
			throws Exception{
		if(this.G!=null){
			return this.getMeasurements(table, onCloud, x, y, head);
		}else{
			return this.getMeasurementsOnTime(
					(int)Math.round(x), 
					(int)Math.round(y), 
					Transformer.th2Z(head, orientation));
		}
	}

	public float[] getMeasurementsOnTime(int x, int y, int z){
		List<Float> M = this.getLaserDist(x, y).getKey();
		return Transformer.drawMeasurements(M.toArray(new Float[M.size()]), z);
	}
	
	/**
	 * @param table if onCloud is true , read table from table.
	 * @param onCloud if onCloud is true , read table from table.
	 * @param x the pose where to read.
	 * @param y the pose where to read.
	 * @param head the orientation where to read.
	 * @return <pre>
	 * if onCloud is true , read table from table.
	 * <pre>
	 * @throws IOException
	 */
	public float[] getMeasurements(HTable table, boolean onCloud, int x, int y, double head)
			throws Exception {
		return this.getMeasurements(table, onCloud, x, y, Transformer.th2Z(head, this.orientation));

	}
	
	/**
	 * @param table if onCloud is true , read table from table.
	 * @param onCloud if onCloud is true , read table from table.
	 * @param x the pose where to read.
	 * @param y the pose where to read.
	 * @param head the orientation where to read.
	 * @return <pre>
	 * if onCloud is true , read table from table.
	 * <pre>
	 * @throws IOException
	 */
	public float[] getMeasurements(HTable table, boolean onCloud, double x, double y, double head)
			throws Exception {
		return this.getMeasurements(table, onCloud, (int)Math.round(x), (int)Math.round(y), Transformer.th2Z(head,
				this.orientation));
	}

	/**	 
	 * @param table if onCloud is true , read table from table.
	 * @param onCloud if onCloud is true , read table from table.
	 * @param x the pose where to read.
	 * @param y the pose where to read.
	 * @param z the orientation where to read.
	 * @return <pre>
	 * 
	 * if z >= 0
	 * return the range of the measurements of this orientation
	 * else
	 * return all of the measurements
	 * </pre>
	 */
	public float[] getMeasurements(HTable table, boolean oncloud, int x, int y, int z)
			throws Exception {
		if (oncloud) {
			return this.getFromCloud2(table, x, y, z);
		} 
		else// if( this.map_array(x,y)==Grid.GRID_EMPTY ) {
			return this.G[x][y].getMeasurements(z);
//		} else{
//			return null;
//		}
	}

	public void readmap() {
		try {
			this.map_image = ImageIO.read(new URL(this.map_filename));
			this.convert();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readmap(String filename) {
		this.map_filename = filename;
		this.readmap();
	}

	// for MapReduce, before calculate the data, task must read the map information from filesystem.
	public void readmap(String filename,
			@SuppressWarnings("rawtypes") Context context) {
		try {
			FileSystem fs = FileSystem.get(URI.create(filename),
					context.getConfiguration());
			FSDataInputStream inputstream = fs.open(new Path(filename));
			map_image = ImageIO.read(inputstream);
			this.convert();
			fs.close();
		} catch (IOException e) {
			context.getCounter(Counters.READ_FAILED).increment(1);
			e.printStackTrace();
		}

	}

	public void readmap(String filename, Configuration conf) {
		try {
			// context.getCounter(Counters.READMAP).increment(1);
			FileSystem fs = FileSystem.get(URI.create(filename), conf);
			FSDataInputStream inputstream = fs.open(new Path(filename));
			map_image = ImageIO.read(inputstream);
			// context.getCounter(Counters.READ_SUCCEED).increment(1);
			this.convert();
			fs.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void convert() {
		this.width = map_image.getWidth();
		this.height = map_image.getHeight();
		this.max_distance = (float) Point2D.Float.distance(0.0, 0.0,
				(double) this.width, (double) this.height);
		map_array = new boolean[this.width][this.height];
		Color black = new Color(0, 0, 0);
		Color pixel;
		for (int x = 0; x < map_image.getWidth(); x++) {
			for (int y = 0; y < map_image.getHeight(); y++) {
				pixel = new Color(map_image.getRGB(x, y));
				if (pixel.equals(black))
					map_array[x][y] = Grid.GRID_OCCUPIED;
				else
					map_array[x][y] = Grid.GRID_EMPTY;
			}
		}
	}
	private double totalProgress;
	private double currentProgress;
	/**
	 * 
	 */
	public void pre_compute() {
		this.totalProgress = this.width*this.height;
		int progress = 0;
		this.G = new position[this.width][this.height];
		for (int x = 0; x < this.width ; x++) {
			for (int y = 0; y < this.height ; y++) {
				this.currentProgress = x*y;
				if(Math.round(this.currentProgress/this.totalProgress)>progress){
					progress++;
					System.out.println("pre-caching progress"+progress+"%");
				}
				
				float[] temp = new float[this.orientation];
				Point[] measurement_points = new Point[this.orientation];
				if (x == 0) {
					for (int k = 0; k < measurement_points.length; k++) {
						measurement_points[k] = new Point(x, y);
					}
					this.G[x][y] = new position(temp, measurement_points);
				} else {
					

					this.getlaserdist(x, y, temp, measurement_points);
					this.G[x][y] = new position(temp, measurement_points);
				}
			}
		}
	}

	public void pre_compute(int x, int y, int width, int height) {
		try {
			this.G = new position[width][height];
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					float[] temp = new float[this.orientation];
					Point[] measurement_points = new Point[this.orientation];
					if (i + x == 0 || i + x >= this.width || j + y == 0
							|| j + y >= this.height) {//edge of the picture would not be calculated.
						for (int k = 0; k < measurement_points.length; k++) {
							measurement_points[k] = new Point(i + x, j + y);
						}
						this.G[i][j] = new position(temp, measurement_points);
						
					} else {
						this.getlaserdist(i + x, j + y, temp,
								measurement_points);
						this.G[i][j] = new position(temp, measurement_points);
					}
				}
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	@Deprecated
	public void getlaserdist(int x, int y, float[] measurements,
			Point[] measurement_points) {
		int checkX, checkY;
		int step;
		// double orientation_degree = this.orientation_delta_degree;
		for (int i = 0; i < this.orientation; i++) {
			step = 0;
			checkX = x;
			checkY = y;
			while (this.map_array(checkX, checkY) == Grid.GRID_EMPTY) {
				checkX = (int) Math.round((x + step
						* Math.cos((i * this.orientation_delta_degree)
								* Math.PI / 180)));
				checkY = (int) Math.round((y + step
						* Math.sin((i * this.orientation_delta_degree)
								* Math.PI / 180)));
				step++;
			}
			measurements[i] = (float) Math.sqrt(((checkX - x) * (checkX - x))
					+ ((checkY - y) * (checkY - y)));
			measurements[i] = 1 - measurements[i] / this.max_distance;
			measurement_points[i] = new Point(checkX, checkY);
		}
	}
	
	public SimpleEntry<List<Float>, List<Point>> getLaserDist(int x, int y){
		List<Float> measurements = new ArrayList<Float>();
		List<Point>  measurementPoints = new ArrayList<Point>();
		int checkX, checkY;
		int step;
		// double orientation_degree = this.orientation_delta_degree;
		for (int i = 0; i < this.orientation; i++) {
			step = 0;
			checkX = x;
			checkY = y;
			while (this.map_array(checkX, checkY) == Grid.GRID_EMPTY) {
				checkX = (int) Math.round((x + step
						* Math.cos((i * this.orientation_delta_degree)
								* Math.PI / 180)));
				checkY = (int) Math.round((y + step
						* Math.sin((i * this.orientation_delta_degree)
								* Math.PI / 180)));
				step++;
			}
			measurements.add(1 - (float) Math.sqrt(((checkX - x) * (checkX - x))
					+ ((checkY - y) * (checkY - y))) / this.max_distance);
			//measurements[i] = 1 - measurements[i] / this.max_distance;
			measurementPoints.add(new Point(checkX, checkY));
		}
		
		return new SimpleEntry<List<Float>, List<Point>>(measurements, measurementPoints);
	}

	/**
	 * @param listener
	 *            MouseAdapter
	 * 
	 */
	public void start_mouse(MouseAdapter listener) {
		this.showimage = new BufferedImage(map_image.getWidth(),
				map_image.getHeight(), BufferedImage.TYPE_INT_ARGB);

		Graphics2D drawing = this.showimage.createGraphics();
		drawing.drawImage(this.map_image, null, 0, 0);
		drawing.dispose();
		this.show_window = new JFrame("show image");
		this.show_window.addMouseMotionListener(listener);

		this.show_window_label_icon = new ImageIcon(this.showimage);
		this.show_window_label = new JLabel(this.show_window_label_icon);
		this.show_window.add(this.show_window_label);
		this.show_window.setSize(width, height);
		this.show_window.setVisible(true);

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		try {
			super.mouseMoved(e);
			position p = this.G[e.getX()][e.getY()];
			Graphics2D drawing = this.showimage.createGraphics();
			drawing.drawImage(this.map_image, null, 0, 0);
			drawing.setColor(Color.red);
			System.out.println(e.getPoint().toString());

			if (this.map_array(e.getX(), e.getY()) == GRID_EMPTY) {

				Point[] points = p.getMeasurement_points(-1);
				// after initial showimage
				for (int i = 0; i < points.length; i++) {
					drawing.drawLine(e.getX(), e.getY(), points[i].x,
							points[i].y);
				}
				System.out.print("points number" + points.length + "\t");

			} else {
				drawing.drawLine(e.getX(), e.getY(), e.getX(), e.getY());
			}

			this.show_window_label_icon = new ImageIcon(this.showimage);
			this.show_window_label = new JLabel(this.show_window_label_icon);
			this.show_window.add(show_window_label);
			this.show_window.setVisible(true);

		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public boolean map_array(int x, int y) {
		if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
			return this.map_array[x][y];
		} else {
			return GRID_OCCUPIED;
		}
	}

	public static void main(String[] args) throws IOException{

		//test distribution
		Grid grid = new Grid(4,4,"file:///home/wuser/backup/jpg/sim_map.jpg");
		grid.readmap();
		
		int situation[] = {1,2,4,8,16};
			for (int j : situation) {
				int number = j;
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
		
		//test getTable time period
		/*Configuration conf = HBaseConfiguration.create();
		Grid grid = new Grid(4,4,"file:///home/wuser/backup/jpg/test6.jpg");
		grid.readmap();
		HTable[] tables = new HTable[10];
		try {
			grid.setupTable(conf);
			
			int n = 10;
			long time = System.currentTimeMillis();
			for(int i = 0 ; i < n; i++){
				tables[i] = grid.getTable("test6.18.split");
				Result r = tables[i].get(new Get(Bytes.toBytes("r1")));
				System.out.println(r.toString());
			}
			time = System.currentTimeMillis() - time;
			System.out.println("time : " + time +" ms");
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			for(HTable t: tables)
				t.close();
			grid.closeTable();
		}*/
	}
}
