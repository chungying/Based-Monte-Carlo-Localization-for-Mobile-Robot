package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Command {

	public static String excuteCommand(String command) {
		Process p;
		StringBuffer output = new StringBuffer();
		try {
			p = Runtime.getRuntime().exec(command/*"hadoop fs -ls "+"/user/w514/hfiles"*/);
		
			p.waitFor();
			InputStream input = p.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			
			
			String line="";
			while((line=reader.readLine())!=null){
				output.append(line+"\n");
			}
			
		} catch (IOException e) {
			output.append(e.toString());
			e.printStackTrace();
		} catch (InterruptedException e) {
			output.append(e.toString());
			e.printStackTrace();
		}
		return output.toString();

	}
	
	

}
