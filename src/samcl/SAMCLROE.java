package samcl;

import java.io.IOException;

import javax.swing.JFrame;

import robot.RobotState;

public class SAMCLROE extends SAMCL{

	@Override
	public synchronized void run(RobotState robot, JFrame samcl_window)
			throws IOException {
		// TODO Auto-generated method stub
		super.run(robot, samcl_window);
	}

	public SAMCLROE(int orientation, String map_filename, float delta_energy,
			int nt, float xI, float aLPHA, int tournament_presure)
			throws IOException {
		super(orientation, map_filename, delta_energy, nt, xI, aLPHA,
				tournament_presure);
	}

}
