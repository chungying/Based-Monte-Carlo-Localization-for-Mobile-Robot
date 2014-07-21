package util.metrics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import samcl.Position;



public class ReadWriteTextFile {

	/**
	 * Fetch the entire contents of a text file, and return it in a String.
	  * This style of implementation does not throw Exceptions to the caller.
	  *
	  * @param aFile is a file which already exists and can be read.
	  */
	static public String getContents(File aFile) {
		//...checks on aFile are elided
		StringBuilder contents = new StringBuilder();
	
		try {
		
			//use buffering, reading one line at a time
			//FileReader always assumes default encoding is OK!
			BufferedReader input =  new BufferedReader(new FileReader(aFile));
			try {
				String line = null; //not declared within while loop
				/*
				 * readLine is a bit quirky :
				 * it returns the content of a line MINUS the newline.
				 * it returns null only for the END of the stream.
				 * it returns an empty String if two newlines appear in a row.
				 */
				int x = 2;
				int y = 3;
				int z = 2;
				while (( line = input.readLine()) != null){
					contents.append(line);
					contents.append(System.getProperty("line.separator"));
					String[] strs = line.split("\t");
					
					for(int i = 0 ; i < x; i++){
						for(int j = 0 ; j < y ; j++){
							for(int k = 0 ; k < z; k++){
								System.out.print(strs[i*y*z + j*z + k]+"\t");
							}
							System.out.println();
						}
						System.out.println();
					}
				}
			}
			finally {
				input.close();
			}
		}
		catch (IOException ex){
			ex.printStackTrace();
		}
		return contents.toString();
	}

	/**
	* Change the contents of text file in its entirety, overwriting any
	* existing text.
	*
	* This style of implementation throws all exceptions to the caller.
	*
	* @param aFile is an existing file which can be written to.
	* @throws IllegalArgumentException if param does not comply.
	* @throws FileNotFoundException if the file does not exist.
	* @throws IOException if problem encountered during write.
	*/
	static public void setContents(File aFile, String aContents)
			throws FileNotFoundException, IOException {
		if (aFile == null) {
			throw new IllegalArgumentException("File should not be null.");
		}
		if (!aFile.exists()) {
			throw new FileNotFoundException ("File does not exist: " + aFile);
		}
		if (!aFile.isFile()) {
			throw new IllegalArgumentException("Should not be a directory: " + aFile);
		}
		if (!aFile.canWrite()) {
			throw new IllegalArgumentException("File cannot be written: " + aFile);
		}
		
		//use buffering
		Writer output = new BufferedWriter(new FileWriter(aFile));
		try {
			//FileWriter always assumes default encoding is OK!
			output.write( aContents );
		}
		finally {
			output.close();
		}
	}
	
	static public void setContents(File aFile, Position[][] Grid)
			throws FileNotFoundException, IOException {
		if (aFile == null) {
			throw new IllegalArgumentException("File should not be null.");
		}
		if (!aFile.exists()) {
			throw new FileNotFoundException ("File does not exist: " + aFile);
		}
		if (!aFile.isFile()) {
			throw new IllegalArgumentException("Should not be a directory: " + aFile);
		}
		if (!aFile.canWrite()) {
			throw new IllegalArgumentException("File cannot be written: " + aFile);
		}
		
		//use buffering
		Writer output = new BufferedWriter(new FileWriter(aFile));
		try {
			//FileWriter always assumes default encoding is OK!
			for(int i = 0; i < Grid.length; i++){
				for(int j = 0 ; j < Grid[i].length; j++){
					for(float f: Grid[i][j].getMeasurements(-1)){
						output.write(String.valueOf(f)+"\t");
					}
				}
			}
		}
		finally {
			output.close();
		}
	}

	/** Simple test harness.   */
	public static void main (String[] args) throws IOException {
//		List<String> strs = new ArrayList();
//		strs.add("1");
//		strs.add("2");
//		strs.add("3");
//		strs.add("4");
//		strs.add("5");
//		strs.add("6");
//		strs.add("21");
//		strs.add("22");
//		strs.add("23");
//		strs.add("24");
//		strs.add("25");
//		strs.add("26");
		
		Position[][] grid = new Position[2][3];
		
		for(int i = 0; i < 2;i++){
			for(int j = 0 ; j < 3; j++){
				float[] f = {(i*3*2+j*2+1), (i*3*2+j*2+2)};
				Position p = new Position(f);
				grid[i][j] = p;
				System.out.println(p.toString());
			}
		}
		
		File testFile = new File("/home/w514/test.txt");
		System.out.println("Original file contents: " + getContents(testFile));
		//setContents(testFile, "The content of this file has been overwritten...\n");
		setContents(testFile, grid);
		System.out.println("New file contents: " + getContents(testFile));
	}
}
