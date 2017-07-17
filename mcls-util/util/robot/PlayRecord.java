package util.robot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.ros.message.MessageFactory;
import org.ros.message.Time;
import org.ros.node.NodeConfiguration;
import org.ros.time.TimeProvider;

import geometry_msgs.PoseStamped;
import geometry_msgs.PoseWithCovariance;
import geometry_msgs.Twist;
import geometry_msgs.TwistStamped;
import geometry_msgs.Vector3;
import nav_msgs.Odometry;
//TODO the format of micro second.

public class PlayRecord {
	public static double cvt(String secs, String micro, boolean moveright){//123456789.123456
		if(moveright)
			return Double.parseDouble(secs)+Double.parseDouble(micro)/10000000.0;
		else
			return Double.parseDouble(secs)+Double.parseDouble(micro)/1000000.0;
	}
	
	public static void main(String[] args) throws Exception{
		NodeConfiguration conf = NodeConfiguration.newPrivate();
		MessageFactory fac = conf.getTopicMessageFactory();
		
		Scanner scanner = null;
		String[] read2Lines = {"",""};
		String readline = "";
		String previousTime = "";
		String  preRobotTime = "", preLaserTime = "";
		String[] tokens = null, sensorTokens = null, originalTime = null;
		int linecounter = 0;
		int zerocount = 0;
		boolean timeFlag = false, senFlag = false;
		Time time = null;
		double microTimeD = 0, preOdomTimeD = 0, preLaserTimeD=0;
		try{
			scanner = new Scanner( new BufferedReader(new FileReader(
					//"/Users/Jolly/workspace/dataset/intellab_dataset"
					"/Users/Jolly/workspace/dataset/intellab/intel.clf.txt"
					)));
			
			//initialization
			/*read2Lines[0] = scanner.nextLine();
			read2Lines[1] = scanner.nextLine();
			linecounter++;
			tokens = read2Lines[0].split(" ");
			tokens = tokens[tokens.length-1].split(":");
			preRobotTime = tokens[tokens.length-1];
			*/
			for(int i = 0 ; i < 11;i++) scanner.nextLine();			
			//main loop
			while(scanner.hasNextLine()){
				readline = scanner.nextLine();
				//read2Lines[0] = scanner.nextLine();
				//read2Lines[1] = scanner.nextLine();
				linecounter++;
				//tokens = read2Lines[0].split(" ");
				tokens = readline.split(" ");
				
				//time extraction
				originalTime = tokens[tokens.length-3].split("\\.");
				boolean moveright = false;
				char[] ch = originalTime[1].toCharArray();
				if(ch[ch.length-1] == '0'){
					moveright = true;
					microTimeD = cvt(originalTime[0],originalTime[1], moveright);
					double freqTrue = 1/(microTimeD-preOdomTimeD);
					
					moveright = false;
					microTimeD = cvt(originalTime[0],originalTime[1], moveright);
					double freqFalse = 1/(microTimeD-preOdomTimeD);
					if(freqTrue  <0 ){
						moveright = false;
						if(freqFalse<0)
							new Exception("error");
					}else if (freqFalse<0){
						moveright = true;
						if(freqTrue<0)
							new Exception("error");
					}else if (Math.abs(freqTrue - 10)<Math.abs(freqFalse - 10)){
						moveright = true;
					}else
						moveright = false;
				}else{
					moveright = false;
				}
				microTimeD = cvt(originalTime[0],originalTime[1], moveright);
				
				if(tokens[0].contains("ODOM")){
					System.out.println(microTimeD + " - " + preOdomTimeD + " = " +(microTimeD-preOdomTimeD) + " sec = " + 1/(microTimeD-preOdomTimeD) +" Hz.");
					if(1/(microTimeD-preOdomTimeD)<0 || 1/(microTimeD-preOdomTimeD)>25)
						System.in.read();
					preOdomTimeD = microTimeD;
				}else if (tokens[0].contains("FLASER")){
					preLaserTimeD = microTimeD;
				}
				
				//old version
				if(tokens[0].contains("@SENS")){
					//System.out.println(tokens[tokens.length-1]);
					
					
					
					//SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSSSSS");
					//Date date = sdf.parse(tokens[tokens.length-1]);
					//time = new Time();
					//time.secs = 0;
					//time.nsecs = ;
					//System.out.println("time: " + date.getTime() + ", " +time.toString());//in microsecond		
					//System.out.println(Time.fromNano((int)(date.getTime()%1000000)*1000));
					timeFlag = true;
				}
				
				sensorTokens = read2Lines[1].split(" ");
				if(sensorTokens[0].contains("#ROBOT")){
					//System.out.print("odometry:");
					//1System.out.println(tokens.length);
					senFlag = true;
					String[] strs = null;
					System.out.print("Robot "+ tokens[tokens.length-1]+": ");
					float t1,t2;
					char[] chrs = tokens[tokens.length-1].toCharArray();
					if(chrs[chrs.length-1] == '0'){
						//current time
						strs = tokens[tokens.length-1].split(":");
						strs = strs[strs.length-1].split("\\.");
						t1 = Float.parseFloat(strs[0])+Float.parseFloat(strs[1])/10000000.0f;						
						zerocount++;
						System.out.print(" zero "+t1);
					}else{
						//current time
						strs = tokens[tokens.length-1].split(":");
						t1 = Float.parseFloat(strs[strs.length-1]);
						System.out.print(" noth "+t1 /*strs[strs.length-1]*/);
					}
					t2 = Float.parseFloat(preRobotTime);
					System.out.println(" - " + t2+ " = " +(t1-t2) + " sec = " + 1/(t1-t2) +" Hz.");
					
					
					//PoseStamped pose = fac.newFromType(PoseStamped._TYPE);
					//System.out.println(pose.getHeader().getFrameId());
					//pose.getHeader().setFrameId("robot");
					
					//pose.getHeader().setStamp(time );
					//System.out.println(pose.getHeader().getFrameId());
					//pose.getPose().getPosition().setX(0.0);
					//pose.getPose().getPosition().setY(0.0);
					//pose.getPose().getOrientation().setZ(0.0);
					
					//preRobotTime = tokens[tokens.length-1];
					preRobotTime = String.valueOf(t1);
					//System.out.println("preRobot" + preRobotTime);
				}
				else if(sensorTokens[0].contains("#LASER")){
					//System.out.print("laser:");
					//System.out.println(tokens.length);
					senFlag = true;
					preLaserTime = tokens[tokens.length-1];
				}
				
			}
		}finally{
			if(scanner!=null)
				scanner.close();
		}
		
	}
}
