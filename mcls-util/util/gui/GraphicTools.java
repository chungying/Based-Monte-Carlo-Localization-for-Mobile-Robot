package util.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import util.pf.Particle;

public class GraphicTools {
	static public void drawConfidentZone(Graphics2D grap, int x, int y, double th, double[][] cov){
		EigenDecomposition E = new EigenDecomposition(new Array2DRowRealMatrix(
				new double[][]{{cov[0][0], cov[0][1]},{cov[1][0],cov[1][1]}}));
		double[] egvalues = E.getRealEigenvalues();
		RealMatrix egvectors = E.getV();
		int majorAxis = egvalues[0]>egvalues[1]?0:1;
		double w = 2*Math.sqrt(9.21*egvalues[majorAxis]);
		double h = 2*Math.sqrt(9.21*egvalues[(majorAxis+1)%2]);
		double theda = Math.atan2(egvectors.getEntry(majorAxis, 1), egvectors.getEntry(majorAxis, 0));
		Shape ellipse = (new AffineTransform(Math.cos(theda), Math.sin(theda), -Math.sin(theda), Math.cos(theda), x, y)).createTransformedShape(new Ellipse2D.Double(-w/2, -h/2, w, h));
		grap.setColor(Color.BLACK);
		grap.draw(ellipse);
	}
	static public void drawPoint(Graphics2D grap, int x, int y, double head, int radius, Color color){
		grap.setColor(color);
		grap.fillOval(x-(radius/2), y-(radius/2), radius, radius);
	}
	
	static public void drawRobot(Graphics2D grap, int x, int y, double head, int radius, Color color){
		grap.setColor(color);
		grap.drawOval(x-(radius/2), y-(radius/2), radius, radius);
		grap.drawLine(x, y, 
				x+(int)Math.round(2*radius*Math.cos(Math.toRadians(head))), 
				y+(int)Math.round(2*radius*Math.sin(Math.toRadians(head))));
	}
	
	static public void drawBatchPoint(Graphics2D grap, List<Particle> particles, int radius, Color color){
		grap.setColor(color);
		for (Particle p : particles) {
			grap.drawOval(p.getX(), p.getY(), radius, radius);
		}
	}
}
