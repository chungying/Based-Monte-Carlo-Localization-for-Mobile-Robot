package mcl;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.google.protobuf.ServiceException;

import robot.RobotState;
import samcl.SAMCL;
import util.metrics.Particle;
import util.metrics.Transformer;

public class MCL extends SAMCL{
	
	public static void main(String[] args) throws ServiceException, Throwable{
		//TODO finish the main function
		final MCL mcl = new MCL(false, 18, "file:///home/w514/jpg/test6.jpg", 0.001f, 100, 0.01f, 0.3f, 10);
		mcl.setup();
		mcl.Pre_caching();
		
		RobotState robot = new RobotState(19,19, 0, mcl.precomputed_grid, null, null); 
		Thread t = new Thread(robot);
		t.start();
		
		final JFrame mcl_window = new JFrame("mcl_window");
		mcl_window.setSize(mcl.precomputed_grid.width, mcl.precomputed_grid.height);
		mcl_window.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("close table!!!!!!!!!!!!!!!!!!!!!!!");
				if (JOptionPane.showConfirmDialog(mcl_window,
						"Are you sure to close this window?", "Really Closing?", 
			            JOptionPane.YES_NO_OPTION,
			            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
					try {
						if(mcl.onCloud)
							mcl.precomputed_grid.closeTable();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					System.exit(0);
				}
			}
		
		});
		mcl.run(robot, mcl_window);
		mcl.close();
	}
	
	@Override
	public Particle Determining_size(List<Particle> src) {
		this.Nl = this.Nt;
		this.Ng = 0;
		return Transformer.minParticle(src);
	}

	@Override
	public void Global_drawing(List<Particle> src, List<Particle> dst) {
		//Do nothing in MCL
		//super.Global_drawing(src, dst);
	}



	@Override
	public void batchWeight(List<Particle> src, float[] robotMeasurements)
			throws IOException, ServiceException {
		for(Particle p : src){
			List<Float> M = this.precomputed_grid.getLaserDist(p.getX(), p.getY()).getKey();
			Float[] m = M.toArray(new Float[M.size()]);
			p.setMeasurements(Transformer.drawMeasurements(m, p.getZ()));
			this.WeightParticle(p, robotMeasurements);
		}
	}

	public MCL(boolean cloud, int orientation, String map_filename,
			float delta_energy, int nt, float xI, float aLPHA,
			int tournament_presure) throws IOException {
		super(cloud, orientation, map_filename, delta_energy, nt, xI, aLPHA,
				tournament_presure);
	}

}
