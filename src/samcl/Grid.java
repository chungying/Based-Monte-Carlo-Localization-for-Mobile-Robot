/**
 * 
 */
package samcl;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
				int bias = (this.sensor_number - 1) / 2;
				int index;
				for (int i = 0; i < this.sensor_number; i++) {
					index = ((z - bias + i + this.circle_measurements.length) % this.circle_measurements.length);
					measurements[i] = this.circle_measurements[index];
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
				int bias = (this.sensor_number - 1) / 2;
				int index;
				for (int i = 0; i < this.sensor_number; i++) {
					index = ((z - bias + i + this.circle_measurements.length) % this.circle_measurements.length);
					measurements[i] = this.measurement_points[index];
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
	public position[][] G;

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

	// private Configuration conf = null;
	private HTable table = null;
	private byte[] family = null;

	public void closeTable() throws IOException {
		this.table.close();
	}

	public void setupTable(Configuration conf) throws IOException {
		// TODO table name
		System.out.println("set up table");
		this.table = new HTable(conf, "map.512.4.split");
		this.family = Bytes.toBytes("distance");
	}

	public void scan(List<Particle> sER_set, float lowerBoundary,
			float upperBoundary) throws IOException {
		sER_set.clear();

		String lower = String.valueOf(lowerBoundary);
		String upper = String.valueOf(upperBoundary);
		Scan scan = new Scan(Bytes.toBytes(lower), Bytes.toBytes(upper));
		scan.addFamily(Bytes.toBytes("energy"));

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

		ResultScanner scanner = this.table.getScanner(scan);
		String str = "";
		String[] pose = null;
		for (Result[] results = scanner.next(caching); results.length != 0; results = scanner
				.next(caching)) {
			// TODOdone count RPC
			this.RPCcount++;
			for (Result result : results) {
				// List<Cell> Cells = result.listCells();
				//TODO ******most important!!!!
				for (Cell cell : result.rawCells()) {
					str = Bytes.toString(CellUtil.cloneQualifier(cell));
					str = str.replace("(", "");
					str = str.replace(")", "");
					pose = str.split(",");

					// System.out.println("(X, Y, Z) = "
					// + "(" + (Integer.parseInt(pose[1])-10000) + ","
					// + (Integer.parseInt(pose[0])-10000) + ","
					// + (Bytes.toString(CellUtil.cloneQualifier(cell))) + ")"
					// );
					Particle p = new Particle(
							Integer.parseInt(pose[1]) - 10000,
							Integer.parseInt(pose[0]) - 10000,
							Integer.parseInt(Bytes.toString(CellUtil
									.cloneValue(cell))));
					if (p.underSafeEdge(width, height, safe_edge))
						sER_set.add(p);
					// System.out.println(p.toString());
					// System.out.println("----------------------------------------------------");
				}
			}
		}
	}

	public void getBatchFromCloud(List<Particle> src)
			throws IOException {
		// first step: setup List<Get>
		// HTable, Particles
		List<Get> gets = new ArrayList<Get>();
		byte[] fam = Bytes.toBytes("distance");
		for (Particle p : src) {
			String str = Transformer.XY2String(p.getX(), p.getY());
			Get get = new Get(Bytes.toBytes(str));
			get.addFamily(fam);
			gets.add(get);
		}

		// second: fetch from the Results to the List<Particles>
		// Particles(X, Y, Z), Results
		Result[] results = this.table.get(gets);
		if (results.length == src.size()) {
			for (int i = 0; i < results.length; i++) {
				convertResultToParticle(src.get(i), results[i], fam);
			}
		}else
			throw new IOException("the length is different.");

	}

	private void convertResultToParticle(Particle particle, Result result,
			byte[] fam) throws IOException {
		if (!result.isEmpty()) {
			float[] measurements = new float[this.sensor_number];
			int bias = (this.sensor_number - 1) / 2;
			int index;
			for (int i = 0; i < this.sensor_number; i++) {
				index = ((particle.getZ() - bias + i + this.orientation) % this.orientation);
				measurements[i] = Float.valueOf(Bytes.toString(result.getValue(
						fam, Bytes.toBytes(String.valueOf(index)))));
			}
			particle.setMeasurements(measurements);
		}else
			throw new IOException("There is no result!!");

	}

	private float[] getFromCloud(int X, int Y, int Z) throws IOException {
		// TODOdone count RPC times
		this.RPCcount++;

		String rowkey = Transformer.XY2String(X, Y);
		// System.out.print("family:"+Bytes.toString(family)+"\t");
		// System.out.println("rowkey: " + rowkey);
		Get get = new Get(Bytes.toBytes(rowkey));
		get.addFamily(this.family);
		Result result = this.table.get(get);
		// List<Cell> cells = result.listCells();
		if (Z >= 0) {
			float[] measurements = new float[this.sensor_number];
			float temp = 0f;
			int bias = (this.sensor_number - 1) / 2;
			int index;
			for (int i = 0; i < this.sensor_number; i++) {

				index = ((Z - bias + i + this.orientation) % this.orientation);
				// System.out.println("index: " + index);
				temp = Float.parseFloat(Bytes.toString(result.getValue(
						this.family, Bytes.toBytes(String.valueOf(index)))));
				// System.out.println("temparary float = " + temp);
				measurements[i] = temp;

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

	public float[] getMeasurements(boolean onCloud, int x, int y, double head)
			throws IOException {
		return this.getMeasurements(onCloud, x, y, Transformer.th2Z(head,
				this.orientation, this.orientation_delta_degree));

	}

	/**
	 * @return <pre>
	 * if z >= 0
	 * return the range of the measurements of this orientation
	 * else
	 * return all of the measurements
	 * </pre>
	 */
	public float[] getMeasurements(boolean oncloud, int X, int Y, int Z)
			throws IOException {
		if (oncloud) {
			// System.out.println("get from CLOUD!!!!!");
			// System.out.println("(X,Y,Z) = ("+X+","+Y+","+Z+")");
			return this.getFromCloud(X, Y, Z);
		} else {
			// System.out.println("get from local!!!!!");
			// System.out.println("(X,Y,Z) = ("+X+","+Y+","+Z+")");
			return this.G[X][Y].getMeasurements(Z);
		}
	}

	public void readmap() {
		try {
			map_image = ImageIO.read(new URL(this.map_filename));
			this.convert();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// private Context context;
	// TODO for debug mode, there could use counter to debug
	public void readmap(String filename,
			@SuppressWarnings("rawtypes") Context context) {
		try {
			// context.getCounter(Counters.READMAP).increment(1);
			FileSystem fs = FileSystem.get(URI.create(filename),
					context.getConfiguration());
			FSDataInputStream inputstream = fs.open(new Path(filename));
			map_image = ImageIO.read(inputstream);
			// context.getCounter(Counters.READ_SUCCEED).increment(1);
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

	public void readmap(String filename) throws MalformedURLException {
		this.map_filename = filename;
		try {
			map_image = ImageIO.read(new URL(this.map_filename));
			this.convert();
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
				// System.out.println(pixel.toString());
			}
		}

		// JLabel label = new JLabel(new ImageIcon(map_image));
		// map_window = new JFrame("map image");
		// map_window.add(label);
		// map_window.setSize(map_image.getWidth(), map_image.getHeight());
		// map_window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// map_window.setVisible(false);
	}

	/**
	 * 
	 */
	public void pre_compute() {
		this.G = new position[this.width][this.height];
		for (int x = 1; x < this.width - 1; x++) {
			// System.out.println( Math.round( ( x / (double) this.width )*100 )
			// );

			for (int y = 1; y < this.height - 1; y++) {
				if (x == 0) {
					this.G[x][y] = new position();
				} else {
					float[] temp = new float[this.orientation];
					Point[] measurement_points = new Point[this.orientation];

					this.getlaserdist(x, y, temp, measurement_points);
					this.G[x][y] = new position(temp, measurement_points);
				}
			}
			// System.out.print("\n");
			// System.out.println("("+x+","+(this.height-100)+","+(0)+")"+(G[x][this.height-100].toString()));
		}
	}

	public void pre_compute(int x, int y, int width, int height) {
		try {
			this.G = new position[width][height];
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					float[] temp = new float[this.orientation];
					Point[] measurement_points = new Point[this.orientation];
					// System.out.println("orien= "+ this.orientation);
					// System.out.println("senso= "+ this.sensor_number);
					// System.out.println("temp = "+ temp.length);
					// System.out.println("point= "+ measurement_points.length);
					if (i + x == 0 || i + x >= this.width || j + y == 0
							|| j + y >= this.height) {// TODO checkout the condition
														// condition
						for (int k = 0; k < measurement_points.length; k++) {
							// System.out.println("i+x = "+(i+x));
							// System.out.println("j+y = "+(j+y));
							// System.out.println("k   = "+k);
							measurement_points[k] = new Point(i + x, j + y);
						}
						this.G[i][j] = new position(temp, measurement_points);
					} else {
						this.getlaserdist(i + x, j + y, temp,
								measurement_points);
						this.G[i][j] = new position(temp, measurement_points);
					}
				}
				// System.out.print("\n");
				// System.out.println("("+x+","+(this.height-100)+","+(0)+")"+(G[x][this.height-100].toString()));
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

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
}
