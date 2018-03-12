package main;

import com.beust.jcommander.JCommander;

import util.recorder.Record;
import util.runner.MCLRunner;

public class Run {
	public static void main(String[] args){
		try {
			MCLRunner runner = new MCLRunner();
			JCommander j = new JCommander();
			j.setAcceptUnknownOptions(true);
			j.addObject(runner);
			j.parse(args);
			runner.run(args);
			Record.statistics();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
