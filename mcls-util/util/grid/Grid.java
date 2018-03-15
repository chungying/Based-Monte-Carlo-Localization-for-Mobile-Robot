/**
 * 
 */
package util.grid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import util.gui.ImagePanel;
import util.Transformer;
import util.gui.FrameOwner;
import util.gui.GraphicTools;
import util.gui.WindowFrame;
import util.imageprocess.PgmImage;
import util.pf.Particle;
import util.pf.sensor.laser.LaserSensor;
import util.robot.RobotState;

/**
 * @author jolly
 * 
 */
public class Grid implements FrameOwner, Closeable {
	private int RPCcount = 0;
	public int getRPCcount(){
		int c = RPCcount;
		RPCcount=0;
		return c;
	}
	public enum Counters {
		READMAP, READ_SUCCEED, READ_FAILED
	}

	/**
	 * nested class
	 * TODO this should be replaced by util.grid.Position
	 */
	public static class position {
		public int sensor_number;
		public List<Point> measurement_points;
		public List<Float> circle_measurements;
		public List<Float> energy;

		/**
		 * @param z
		 * @return <pre>
		 * if z >= 0
		 * return the range of the measurements of this orientation
		 * else
		 * return all of the measurements
		 * </pre>
		 */
		private List<Float> getMeasurements(int z) {
			if (z >= 0) {
				List<Float> measurements = new ArrayList<Float>();
				//int bias = (this.sensor_number - 1) / 2;
				int globalIndex;
				for (int i = 0; i < this.sensor_number; i++) {
					globalIndex = Transformer.local2global(i, z, this.circle_measurements.size());
					//globalIndex = ((z - bias + i + this.circle_measurements.length) % this.circle_measurements.length);
					measurements.add(this.circle_measurements.get(globalIndex));
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
			return energy.get(z);
		}

		/**
		 * @param z
		 * @return <pre>
		 * if z >= 0
		 * return the points of the measurements of this orientation
		 * else
		 * return all of the points of the measurements
		 */
		public List<Point> getMeasurement_points(int z) {
			List<Point> mpts = null;
			if (z >= 0) {
				mpts = new ArrayList<Point>();
				//int bias = (this.sensor_number - 1) / 2;
				int globalIndex;
				for (int i = 0; i < this.sensor_number; i++) {
					globalIndex = Transformer.local2global(i, z, this.circle_measurements.size());
					//globalIndex = ((z - bias + i + this.circle_measurements.length) % this.circle_measurements.length);
					mpts.add(this.measurement_points.get(globalIndex));
				}
				
			} else {
				mpts = this.measurement_points;
			}
			return mpts;
		}

		
		private void setup(float max_dist_sensor) {
			this.sensor_number = (this.circle_measurements.size() / 2) + 1;
			this.energy = new ArrayList<Float>();
			List<Float> zt = null;
			for (int i = 0; i < this.circle_measurements.size(); i++) {
				//Unit of measurements is pixel.
				zt = this.getMeasurements(i);
				this.energy.add(Transformer.CalculateEnergy(zt, max_dist_sensor));
				
			}
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
		public position(List<Float> measurements, float max_dist, List<Point> measurement_points) {
			super();
			this.circle_measurements = measurements;
			this.measurement_points = measurement_points;
			this.setup(max_dist);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("position [circle_measurements=[");
			Iterator<Float> it = circle_measurements.iterator(); 
			sb.append(it.next().toString());
			while(it.hasNext()){
				sb.append(", ");
				sb.append(it.next().toString());
			}
			sb.append("], energy=[");
			it = energy.iterator();
			sb.append(it.next().toString());
			while(it.hasNext()){
				sb.append(", ");
				sb.append(it.next().toString());
			}
			sb.append("]");
			return sb.toString();
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
	

	/**
	 * windows
	 */
	public JFrame show_window;
//	private JFrame map_window;
	public JLabel show_window_label;
	public ImageIcon show_window_label_icon;
//	public MouseAdapter mouse_adapter;

	public BufferedImage map_image;
	public BufferedImage showimage;
	public BufferedImage temp_image;
//	public MouseListener mouseListener;
	/*public MouseMotionListener mouse = new MouseMotionListener() {

		@Override
		public void mouseDragged(MouseEvent e) {

		}

		@Override
		public void mouseMoved(MouseEvent e) {

		}

	};*/

	public boolean[][] map_array;
	public position[][] G = null;

	@ParametersDelegate
	public LaserSensor laser = new LaserSensor();

	@Parameter(names = {"-cl","--cloud"}, description = "if there on the cloud is or not, default is false", required = false, arity = 1)
	public boolean onCloud = false;
	
	@Parameter(names = {"-i","--image"}, 
			description = "the image file of the map", 
			required = true, arity = 1)
	public String mapFilename = "";
	
	/**
	 * @param filename
	 *            :jpg,png...
	 */
	public Grid(String filename) {
		this.mapFilename = filename;
	}
	

	/**
	 * 
	 */
	public Grid(){}

	private HConnection connection = null;
	private byte[] family = null;
	private byte[] energy = null;
	
	public HTable getTable(String tablename) throws IOException{
		if(!this.onCloud)
			return null;
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
	
	private boolean closed = false; 
	@Override
	public void close() throws IOException {
		if(!closed){
			closed = true;
			System.out.println("closing " + this.getClass().getName());
			if(connection!=null)
				connection.close();
			if(windowFrame!=null){
				windowFrame.dispose();
			}
		}
	}

	public void setupHBaseConnection(Configuration conf) throws IOException {
		System.out.println("set up table");
		this.family = Bytes.toBytes("distance");
		this.energy = Bytes.toBytes("energy");
		//setup connection to afford the getTable(tableName);
		connection = HConnectionManager.createConnection(conf);
		
	}
	
	private ImagePanel imagePanel = null;
	private JScrollPane sp = null;
	private Graphics2D grap = null;
	private WindowFrame windowFrame = null;
	public boolean windowClosedFlag = false;
	@Parameter(names = "--visualization", help = false, required = false, arity=1)
	public boolean visualization=true;

	public void setupCloseableObjs(Closeable... closeObjs){
		if (windowFrame == null)
			setupFrame(visualization);
		if(windowFrame != null)
			windowFrame.setupCloseList(closeObjs);
	}
	
	@Override
	public void setupFrame(boolean visualization) {
		if(visualization==false){
			windowClosedFlag = true;
			return;
		}
		if (windowFrame == null) {
			//TODO check availibility of headless mode
			//1. initialization
			//1.3 setup window
			windowFrame = new WindowFrame("intell research lab Seattle", this);
			windowFrame.setLayout(new BorderLayout());
			int width = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width;
			int height = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
			if(this.width>width && this.height > height)
				windowFrame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
			//1.1 reading image for setting up panel
			//1.2 setup graphics for drawing panel
			BufferedImage image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
			grap = image.createGraphics();
			grap.drawImage(this.map_image, null, 0, 0);
			imagePanel = new ImagePanel(image);
			imagePanel.setPreferredSize(new Dimension(this.width, this.height));
			//1.4 setup scroll panel 
			sp = new JScrollPane(imagePanel);
			windowFrame.getContentPane().add(sp, BorderLayout.CENTER);
//			sp.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener(){
//				@Override
//				public void adjustmentValueChanged(AdjustmentEvent arg0) {
//					System.out.println("horizontal bar value: " + sp.getHorizontalScrollBar().getValue());
//					System.out.println("vertical bar value: " + sp.getVerticalScrollBar().getValue());
//				}
//			});
//			sp.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener(){
//				@Override
//				public void adjustmentValueChanged(AdjustmentEvent arg0) {
//					System.out.println("horizontal bar value: " + sp.getHorizontalScrollBar().getValue());
//					System.out.println("vertical bar value: " + sp.getVerticalScrollBar().getValue());
//				}
//			});
			windowClosedFlag = false;
			windowFrame.pack();
			windowFrame.setVisible(visualization);
		}
	}


	@Override
	public void setFrameLocation(int x, int y) {
		windowFrame.setLocation(x, y);
		
	}


	public void drawPoints(List<Particle> set){
		GraphicTools.drawBatchPoint(grap, set, 1, Color.PINK);
	}
	
	/**
	 * 
	 * @param x in pixels, int
	 * @param y in pixels, int
	 * @param th in degree, double
	 */
	public void drawHypothesisRobot(int x, int y, double th) {
		drawHypothesisRobot( x, y, th, Color.GREEN);
	}
	
	public void drawHypothesisRobot(int x, int y, double th, Color color) {
		drawHypothesisRobot(x, y, th, 4, color);
	}
	
	public void drawHypothesisRobot(int x, int y, double th,int size, Color color) {
		if(visualization==false)
			return;
		GraphicTools.drawRobot(grap, x, y, th, size, color);
	}
	
	public void drawLaserPoint(int x, int y){
		if(visualization==false)
			return;
		GraphicTools.drawPoint(grap, x, y, -1.0, 4, Color.RED);
	}
	
	public void drawConfidence(int x, int y, double th, double[][] cov){
		if(visualization==false)
			return;
		GraphicTools.drawConfidentZone(grap, x, y, th, cov);
	}
	public void drawBestSolution(int x, int y, double th){
		if(visualization==false)
			return;
		GraphicTools.drawRobot(grap, x, y, th, 8, Color.BLUE);
	}

	public void drawRobot(RobotState robot){
		if(visualization==false || robot==null)
			return;
		//2. drawing Robot
		GraphicTools.drawRobot(grap, (int)Math.round(robot.X), (int)Math.round(robot.Y), robot.H, 10, Color.RED);
		//3. drawing sensor hits
//		if(robot.getMeasurement_points()!=null){
//			for(Point p: robot.getMeasurement_points()){
//				drawLaserPoint(p.x, p.y);
//			}	
//		}
	}
	
	public void refresh(){
		if(visualization==false)
			return;
		//1. refreshing image
		grap.drawImage(this.map_image, null, 0, 0);
	}
	
	public void repaint(){
		if(visualization==false)
			return;
		//final step of drawing
		imagePanel.repaint();
	}

	

	public void scanFromHbase(HTable table, List<Particle> ser_set, float lowerBoundary,
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
		 
		for (Result[] results = scanner.next(caching); 
				results.length != 0;
				results = scanner.next(caching)) {
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
					Particle p = new Particle(x, y, Transformer.Z2Th(z, this.laser.getOrientation()));
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
				Particle p = src.get(i);
				byte[] resultValue = results[i].getValue(
						fam,
						Bytes.toBytes("data")
						);
				List<Float> measurements = new ArrayList<Float>();
				if(resultValue!=null){
					for (int sensorIndex = 0; sensorIndex < this.laser.rangeCount(); sensorIndex++) {
						measurements.add( 
							Bytes.toFloat(
									//the beam value of byte array
									Transformer.getBA(
											//global index
											Transformer.local2global(
													sensorIndex, 
													Transformer.th2Z(p.getTh(), this.laser.getOrientation()), 
													this.laser.getOrientation()),
													resultValue)
							));
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
				List<Float> measurements = new ArrayList<Float>();
					//int bias = (this.sensor_number - 1) / 2;
					for (int sensorIndex = 0; sensorIndex < this.laser.rangeCount(); sensorIndex++) {
						globalIndex = 
								Transformer.local2global(
										sensorIndex, 
										Transformer.th2Z(p.getTh(), this.laser.getOrientation()), 
										this.laser.getOrientation());
						/*globalIndex = (Transformer.th2Z(p.getTh(), this.orientation) 
								- bias + j + this.orientation) 
								% this.orientation;*/
						value = results[i].getValue(fam,Bytes.toBytes(String.valueOf(globalIndex)));
						//System.out.println(Bytes.toString(value));
						measurements.add(Float.valueOf(Bytes.toString(value)));
					}
					p.setMeasurements(measurements);
				//}else
					//throw new IOException("There is no result!!");
			}
		}else
			throw new IOException("the length is different.");

	}

	private List<Float> getSingleFromCloud2(HTable table, int X, int Y, int Z) throws Exception {
		this.RPCcount++;
		String rowkey = Transformer.xy2RowkeyString(X, Y);
		Get get = new Get(Bytes.toBytes(rowkey));
		get.addColumn(this.family, Bytes.toBytes("data"));
		Result result = table.get(get);
		byte[] BA = result.getValue(this.family, Bytes.toBytes("data"));
		List<Float> measurements = null;
		if(Z>=0){
			measurements = new ArrayList<Float>();
			//int bias = (this.sensor_number - 1) / 2;
			int globalIndex;
			for (int sensorIndex = 0; sensorIndex < this.laser.rangeCount(); sensorIndex++) {
				globalIndex = Transformer.local2global(sensorIndex, Z, this.laser.getOrientation());
				//globalIndex = ((Z - bias + i + this.orientation) % this.orientation);
				measurements.add(Bytes.toFloat(Transformer.getBA(globalIndex, BA)));
			}
		}else{
			measurements = new ArrayList<Float>();
			for (int i = 0; i < this.laser.getOrientation(); i++) {
				measurements.add(Bytes.toFloat(Transformer.getBA(i, BA)));
			}
			
		}
		return measurements;
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
			float[] measurements = new float[this.laser.rangeCount()];
			float temp = 0f;
			//int bias = (this.sensor_number - 1) / 2;
			int globalIndex;
			for (int sensorIndex = 0; sensorIndex < this.laser.rangeCount(); sensorIndex++) {
				globalIndex = Transformer.local2global(sensorIndex, Z, this.laser.getOrientation());
				//globalIndex = ((Z - bias + i + this.orientation) % this.orientation);
				// System.out.println("index: " + index);
				temp = Float.parseFloat(Bytes.toString(result.getValue(
						this.family, Bytes.toBytes(String.valueOf(globalIndex)))));
				// System.out.println("temparary float = " + temp);
				measurements[sensorIndex] = temp;

			}
			return measurements;
		} else {
			float[] measurements = new float[this.laser.getOrientation()];
			for (int i = 0; i < this.laser.getOrientation(); i++) {
				measurements[i] = Bytes.toFloat(result.getValue(
						this.family/* Bytes.toBytes("distance") */,
						Bytes.toBytes(String.valueOf(i))));
			}
			return measurements;
		}
	}
	
	public boolean assignMeasurementsAnyway(List<Particle> src) throws Exception{
		int count=0;
		for(Particle p: src){
			boolean status = map_array[p.getX()][p.getY()] == Grid.GRID_OCCUPIED;
			if(status){
				p.setMeasurements(null);
				count++;
			}else{
				p.setMeasurements(this.getMeasurementsAnyway( null, false, p.getX(), p.getY(), p.getTh()));
			}
			
//			if( !p.isIfmeasurements()){
//				throw new Exception("cannot get measurements for this particle.");
//			}
		}
		if(count==src.size()){
			System.out.println/*throw new Exception*/("all of particles have no measurements.");
			return false;
		}
		return true;
	}
	
	public List<Float> getMeasurementsAnyway(HTable table, boolean onCloud, double x, double y, double head)
			throws Exception{
		if(this.G!=null || (onCloud==true && table!=null)){
			return this.getMeasurements(table, onCloud, x, y, head);
		}else{
			List<Float> M = this.getLaserDist((int)Math.round(x), (int)Math.round(y), this.laser.range_max).getKey();
			return Transformer.drawMeasurements(M.toArray(new Float[M.size()]), Transformer.th2Z(head, laser.getOrientation()));
		}
	}
	
	/**
	 * 
	 * @param x in pixels
	 * @param y in pixels
	 * @param head in degree
	 * @return
	 */
	public List<Float> getMeasurementsOnTime(double x, double y, double head){
		return getMeasurementsOnTime((int)Math.round(x), (int)Math.round(y), Transformer.th2Z(head, laser.getOrientation()));
	}
	
	/**
	 * 
	 * @param x in pixels
	 * @param y in pixels
	 * @param z
	 * @return
	 */
	public List<Float> getMeasurementsOnTime(int x, int y, int z){
		List<Float> M = this.getLaserDist(x, y, this.laser.range_max).getKey();
		return Transformer.drawMeasurements(M.toArray(new Float[M.size()]), z);
	}
	
	
	/*public double angle_min=0; 		//start angle of the scan [rad]
	public double angle_max=0;     	//end angle of the scan [rad]
	public double angle_increment=Math.toRadians(45);	// angular distance between measurements [rad]
	@Deprecated
	public float[] getMeasurementsOnTime(Pose pos){
		
		int count=(int)Math.floor((angle_max-angle_min)/angle_increment) +1;
		float[] ranges=new float[count];
		for(int i = 0 ; i < count;i++){
			ranges[i] = (float)map_calc_range(pos.X,pos.Y,Math.toRadians(pos.H)+angle_min + (i*angle_increment),100);
		}
		return ranges;
	}*/
	
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
	private List<Float> getMeasurements(HTable table, boolean onCloud, double x, double y, double head)
			throws Exception {
//		assert(this.orientation==this.laser.getOrientation());
		if (onCloud) {
			//refer to pre_compute() for the measurements from cloud servers.
			return this.getSingleFromCloud2(table, 
					(int)Math.round(x), (int)Math.round(y), Transformer.th2Z(head, this.laser.getOrientation()));
		} 
		else// if( this.map_array(x,y)==Grid.GRID_EMPTY ) {
			//refer to pre_compute() for the measurements computed in advance.
			return this.G[(int)Math.round(x)][(int)Math.round(y)].getMeasurements(Transformer.th2Z(head,this.laser.getOrientation()));
	}


	public SimpleEntry<List<Float>, List<Point>> getLaserDist(int x, int y){
	//		assert(this.max_distance==this.laser.range_max);
			return getLaserDist(x, y, this.laser.range_max);
		}


	/**
	 * @param x x-axis value of the position
	 * @param y y-axis value of the position
	 * @return a set of distances between each point and (x,y), unit is pixel(s).
	 * This function will replace getlaserdist(...).
	 */
	public SimpleEntry<List<Float>, List<Point>> getLaserDist(int x, int y, float max_dist){
//		assert(this.orientation==this.laser.getOrientation());
//		assert(this.orientation_delta_degree==this.laser.angle_resolution);
		List<Float> measurements = new ArrayList<Float>();
		List<Point>  measurementPoints = new ArrayList<Point>();
		int checkX, checkY;
		int step;
		// double orientation_degree = this.orientation_delta_degree;
		for (int i = 0; i < this.laser.getOrientation(); i++) {
			step = 0;
			checkX = x;
			checkY = y;
			while (this.map_array(checkX, checkY) == Grid.GRID_EMPTY) {
				step++;//begin from 1 unit length.
				checkX = (int) Math.round((x + step
						* Math.cos((i * this.laser.angle_resolution)
								* Math.PI / 180)));
				checkY = (int) Math.round((y + step
						* Math.sin((i * this.laser.angle_resolution)
								* Math.PI / 180)));
				//check max_dist
				if(step>=max_dist)
					break;
			}
			//Unit of measurements has to be changed into pixel. done!
			/*measurements.add(1 - (float) Math.sqrt(((checkX - x) * (checkX - x))
					+ ((checkY - y) * (checkY - y))) / this.max_distance);*/
			measurements.add((float) Math.sqrt(((checkX - x) * (checkX - x))
					+ ((checkY - y) * (checkY - y))));

			measurementPoints.add(new Point(checkX, checkY));
		}
		
		return new SimpleEntry<List<Float>, List<Point>>(measurements, measurementPoints);
	}

	private double map_scale=1;

	public double map_calc_range( double ox, double oy, double oa, double max_range)
	{
	  // Bresenham raytracing
	  int x0,x1,y0,y1;
	  int x,y;
	  int xstep, ystep;
	  boolean steep;
	  int tmp;
	  int deltax, deltay, error, deltaerr;

	  x0 = (int) Math.round(ox/this.map_scale);
	  y0 = (int) Math.round(oy/this.map_scale);;
	  
	  x1 = (int) Math.round((ox + max_range * Math.cos(oa))/this.map_scale);
	  y1 = (int) Math.round((oy + max_range * Math.sin(oa))/this.map_scale);;
	  
	  if(Math.abs(y1-y0) > Math.abs(x1-x0))
	    steep = true;
	  else
	    steep = false;

	  if(steep)
	  {
	    tmp = x0;
	    x0 = y0;
	    y0 = tmp;

	    tmp = x1;
	    x1 = y1;
	    y1 = tmp;
	  }

	  deltax = Math.abs(x1-x0);
	  deltay = Math.abs(y1-y0);
	  error = 0;
	  deltaerr = deltay;

	  x = x0;
	  y = y0;

	  if(x0 < x1)
	    xstep = 1;
	  else
	    xstep = -1;
	  if(y0 < y1)
	    ystep = 1;
	  else
	    ystep = -1;

	  if(steep)
	  {
	    if(this.map_array(y,x) != Grid.GRID_EMPTY)
	      return Math.sqrt((x-x0)*(x-x0) + (y-y0)*(y-y0)) * this.map_scale;
	  }
	  else
	  {
	    if(this.map_array(x,y) != Grid.GRID_EMPTY)
	      return Math.sqrt((x-x0)*(x-x0) + (y-y0)*(y-y0)) * this.map_scale;
	  }

	  while(x != (x1 + xstep * 1))
	  {
	    x += xstep;
	    error += deltaerr;
	    if(2*error >= deltax)
	    {
	      y += ystep;
	      error -= deltax;
	    }

	    if(steep)
	    {
	      if(this.map_array(y,x) != Grid.GRID_EMPTY)
	        return Math.sqrt((x-x0)*(x-x0) + (y-y0)*(y-y0)) * this.map_scale;
	    }
	    else
	    {
	      if(this.map_array(x,y) != Grid.GRID_EMPTY)
	        return Math.sqrt((x-x0)*(x-x0) + (y-y0)*(y-y0)) * this.map_scale;
	    }
	  }
	  return max_range;
	}
	
	public boolean map_array(int x, int y) {
		if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
			return this.map_array[x][y];
		} else {
			return GRID_OCCUPIED;
		}
	}

	/**
	 * @param listener
	 *            MouseAdapter
	 * 
	 */
	/*public void start_mouse(MouseAdapter listener) {
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

	}*/

	/*@Override
	public void mouseMoved(MouseEvent e) {
		try {
			super.mouseMoved(e);
			position p = this.G[e.getX()][e.getY()];
			Graphics2D drawing = this.showimage.createGraphics();
			drawing.drawImage(this.map_image, null, 0, 0);
			drawing.setColor(Color.red);
			System.out.println(e.getPoint().toString());

			if (this.map_array(e.getX(), e.getY()) == GRID_EMPTY) {

				List<Point> points = p.getMeasurement_points(-1);
				// after initial showimage
				for (int i = 0; i < points.size(); i++) {
					drawing.drawLine(e.getX(), e.getY(), points.get(i).x,
							points.get(i).y);
				}
				System.out.print("points number" + points.size() + "\t");

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
	}*/

//	private void readmap(float max_dist, String filename) throws IOException {
//		this.map_filename = filename;
//		this.readmap(max_dist);
//	}

	//using default filename
	public void readMapImageFromLocal() throws IOException {
		try {
			if(mapFilename.contains(".pgm") || mapFilename.contains(".PGM"))
				this.map_image = new PgmImage(mapFilename).img;
			else
				this.map_image = ImageIO.read(new URL(this.mapFilename));
			this.convert2OccupiedMap(this.map_image);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
		this.checkRangeMax();
	}

	// for MapReduce, before calculate the data, task must read the map information from filesystem.
	//for hbase2hbase
	public void readMapImageFromHadoop(String filename,
			@SuppressWarnings("rawtypes") Context context) {
		//TODO requiring max_dist of sensors.
		readMapImageFromHadoop(filename, context.getConfiguration());
	
	}

	//for hbase2hfile
	public void readMapImageFromHadoop(String filename, Configuration conf) {
		FileSystem fs =null;
		try {
			// context.getCounter(Counters.READMAP).increment(1);
			fs = FileSystem.get(URI.create(filename), conf);
			FSDataInputStream inputstream = fs.open(new Path(filename));
			if(filename.contains(".pgm") || filename.contains(".PGM"))
				map_image = (new PgmImage(inputStream)).img;
			else
				map_image = ImageIO.read(inputstream);
			// context.getCounter(Counters.READ_SUCCEED).increment(1);
			//TODO requiring max_dist of sensors.
			this.convert2OccupiedMap(this.map_image);
			
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				fs.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.checkRangeMax();
	
	}

	/**
	 * extract information of map image for raycasting.
	 * max_distance of sensor is required, default value is diagonal length of map.
	 * @param map_image2 
	 */
	private void convert2OccupiedMap(BufferedImage map_image2) {
		this.width = map_image2.getWidth();
		this.height = map_image2.getHeight();
		map_array = new boolean[this.width][this.height];
		Color black = new Color(0, 0, 0);
		Color white = new Color(255,255,255);
		Color pixel;
		int count1=0,count2=0,count3=0;
		for (int x = 0; x < map_image2.getWidth(); x++) {
			for (int y = 0; y < map_image2.getHeight(); y++) {
				pixel = new Color(map_image2.getRGB(x, y));
				if (pixel.equals(black)){
					map_array[x][y] = Grid.GRID_OCCUPIED;
					count1++;
				}
				else if (pixel.equals(white)){
					map_array[x][y] = Grid.GRID_EMPTY;
					count2++;
				}
				else{
					map_array[x][y] = Grid.GRID_OCCUPIED;
					count3++;
				}
			}
		}
		assert(count1!=0||count3!=0);
		assert(count2!=0);
	}


	private double totalProgress;
	private double currentProgress;
	

	/**
	 * 
	 */
	public void pre_compute() {
		if (this.onCloud) {
			//TODO check what part of initialization of HBase connection should be moved to here.
		}
		else{
			this.totalProgress = this.width * this.height;
			int progress = 0;
			this.G = new position[this.width][this.height];
			for (int x = 0; x < this.width; x++) {
				for (int y = 0; y < this.height; y++) {
					this.currentProgress = x * y;
					if (Math.round(this.currentProgress / this.totalProgress) > progress) {
						progress++;
						System.out.println("pre-caching progress" + progress + "%");
					}

					List<Float> measurements = new ArrayList<Float>();
					List<Point> measurement_points = new ArrayList<Point>();
					if (x == 0) {

						for (int k = 0; k < this.laser.getOrientation(); k++) {
							measurements.add(0f);
							measurement_points.add(new Point(x, y));
						}
					} else {
						// Unit of G has to be changed into pixel. done!
						this.getlaserdist(x, y, measurements, measurement_points, this.laser.range_max);

					}
					this.G[x][y] = new position(measurements, this.laser.range_max, measurement_points);
				}
			} 
		}
	}

	/**
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * 
	 * This function is mainly invoked by the cloud-based program. TODO this.laser in the cloud might not be initialized.
	 */
	public void pre_compute(int x, int y, int width, int height) {
//		assert(this.orientation==this.laser.getOrientation());
//		assert(this.max_distance==this.laser.range_max);
		try {
			this.G = new position[width][height];
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					List<Float> measurements = new ArrayList<Float>();
					List<Point> measurement_points = new ArrayList<Point>();
					if (i + x == 0 || i + x >= this.width || j + y == 0
							|| j + y >= this.height) {//edge of the picture would not be calculated.
						
						for (int k = 0; k < this.laser.getOrientation(); k++) {
							measurements.add(0f);
							measurement_points.add(new Point(i + x, j + y));
						}
						//Unit of G has to be changed into pixel. done!
						//this.G[i][j] = new position(measurements, this.max_distance, measurement_points);
						
					} else {
						//Unit of G has to be changed into pixel. done!
						this.getlaserdist(i + x, j + y,
								measurements, measurement_points, this.laser.range_max);
						
					}
					this.G[i][j] = new position(measurements, this.laser.range_max, measurement_points);
				}
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * @param x in unit of pixels
	 * @param y in unit of pixels
	 * @param measurements in unit of pixels
	 * @param measurement_points
	 * This will be replaced by getLaserDist(...). 
	 */
	@Deprecated
	private void getlaserdist(int x, int y, List<Float> measurements,
			List<Point> measurement_points, float max_dist) {
//		assert(this.orientation==this.laser.getOrientation());
//		assert(this.orientation_delta_degree==this.laser.angle_resolution);
		if(measurements.size()!=0)
			measurements.clear();
		if(measurement_points.size()!=0)
			measurement_points.clear();
		int checkX, checkY;
		int step;
		// double orientation_degree = this.orientation_delta_degree;
		for (int i = 0; i < this.laser.getOrientation(); i++) {
			step = 0;
			checkX = x;
			checkY = y;
			while (this.map_array(checkX, checkY) == Grid.GRID_EMPTY) {
				step++;//begin from 1 unit length
				checkX = (int) Math.round((x + step
						* Math.cos((i * this.laser.angle_resolution)
								* Math.PI / 180)));
				checkY = (int) Math.round((y + step
						* Math.sin((i * this.laser.angle_resolution)
								* Math.PI / 180)));
				//check max_dist
				if(step>=max_dist)
					break;
			}
			measurements.add((float) Math.sqrt(((checkX - x) * (checkX - x))
					+ ((checkY - y) * (checkY - y))));
			//Unit of measurements has to be changed into pixel. done!
			//measurements[i] = 1 - measurements[i] / this.max_distance;
			
			measurement_points.add(new Point(checkX, checkY));
		}
	}


	private void checkRangeMax() {
//		assert(this.max_distance==this.laser.range_max);
		if(laser.range_max<=0){
			//diagonal length of map image
			this.laser.range_max = (float) Point2D.Float.distance(0.0, 0.0, (double) this.width, (double) this.height);
		}
	}


	public void setupGrid() throws Exception {
		if(this.onCloud){
			//retrieve map image from the cloud and initialize this.table
			System.out.println("cloud setup");
			Logger.getRootLogger().setLevel(Level.WARN);
			Configuration conf = HBaseConfiguration.create();
			this.setupHBaseConnection(conf);
			this.readMapImageFromHadoop(this.mapFilename, conf);
			
			
		}else{
			//retrieve map image from local filesystem
			System.out.println("local setup");
			this.readMapImageFromLocal();
			
		}
	}
}
