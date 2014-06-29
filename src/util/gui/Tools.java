package util.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import util.metrics.Particle;

public class Tools {
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
