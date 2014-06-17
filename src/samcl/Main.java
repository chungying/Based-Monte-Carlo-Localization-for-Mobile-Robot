package samcl;


import java.io.IOException;
import java.util.Arrays;

import com.beust.jcommander.JCommander;

public class Main {
	
	public static void main(String[] args) throws IOException {
		
		SAMCL samcl = new SAMCL(
				18, //orientation
				//"file:///home/w514/map.jpg",//map image file
				//TODO file name
				"hdfs:///user/eeuser/map1024.jpeg",
				(float) 0.005, //delta energy
				100, //total particle
				(float) 0.001, //threshold xi
				(float) 0.6, //rate of population
				10);//competitive strength
		
		new JCommander(samcl, args);
		
		if(!samcl.onCloud){
			if (!Arrays.asList(args).contains("-i") && !Arrays.asList(args).contains("--image")) {
				String filepath = "file://" + System.getProperty("user.home") + "/sim_map.jpg";
				System.out.println(filepath);
				samcl.map_filename = filepath;
				
			}
			System.out.println("start to pre-caching");
			samcl.setup();
			samcl.Pre_caching();
		}else
			samcl.setup();
		samcl.run();
//		int counter = 0;
//		while(true){
//			counter++;
//			samcl.run();
//			System.out.println("end.....");
//		}
		
	}
	
/*	public static void preCachingTime(SAMCL samcl, int times){
		for (int i = 0; i < times; i++) {
			System.out.println("start SAMCL");
			Date start_time = new Date();
			long start_tick = System.currentTimeMillis();
			System.out.println("end : " + start_time.toString());
			samcl.Pre_caching();
			Date end_time = new Date();
			long end_tick = System.currentTimeMillis();
			long diff = end_tick - start_tick;
			System.out.println("end : " + end_time.toString());
			System.out.println("waste time : " + String.valueOf(diff) + " ms");
		}
	}*/

}
