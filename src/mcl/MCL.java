package mcl;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.beust.jcommander.JCommander;
import com.google.protobuf.ServiceException;

import robot.RobotState;
import samcl.SAMCL;
import util.gui.RobotController;
import util.gui.Window;
import util.metrics.Particle;
import util.metrics.Transformer;

public class MCL extends SAMCL{
	
	public static void main(String[] args) throws ServiceException, Throwable{
		//for debug mode
		if(args.length==0){
			String[] targs = {/*"-cl",*/
					//"-i","file:///Users/ihsumlee/Jolly/jpg/white.jpg"
					"-i","file:///home/w514/jpg/test6.jpg"
					,"-o","4"
					,"-rl","true"
					,"-rx","30"
					,"-ry","30"
					,"-p","10"
					};
			args = targs;
		}
		//TODO finish the main function,1:add robot controller
		final MCL mcl = new MCL(false,
				18,
				"file:///home/w514/jpg/test6.jpg", 
				0.001f, 
				100, 
				0.01f, 
				0.3f, 
				10);
		JCommander jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(mcl);
		jc.parse(args);
		
		mcl.setup();
		mcl.Pre_caching();
		
		RobotState robot = new RobotState(19,19, 0, mcl.precomputed_grid, null, null); 
		jc = new JCommander();
		jc.setAcceptUnknownOptions(true);
		jc.addObject(robot);
		jc.parse(args);
		robot.setInitModel(robot.getUt());
		robot.setInitPose(robot.getPose());
		Thread t = new Thread(robot);
		t.start();
		
		RobotController robotController = new RobotController("robot controller", robot, mcl);
		Window window = new Window("mcl image", mcl,robot);
		
		for(int i = 0; i < 10; i ++){
			window.setTitle("mcl image:"+String.valueOf(i));
			mcl.run(robot, window);
			robot.lock();
			robot.initRobot();
			robot.unlock();
		}
		
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
