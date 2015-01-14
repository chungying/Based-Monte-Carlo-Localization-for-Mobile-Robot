package util.metrics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import util.grid.Position;



public class ReadWriteTextFile {

	/**
	 * Fetch the entire contents of a text file, and return it in a String.
	  * This style of implementation does not throw Exceptions to the caller.
	  *
	  * @param mFile is a file which already exists and can be read.
	  * @param eFile is a file which already exists and can be read.
	  */
	static public Position[][] getContents(File mFile, File eFile) {
		//...checks on mFile are elided
		
		int x = 2;
		int y = 3;
		int z = 2;
		Position[][] grid = new Position[x][y];
		try {
		
			//use buffering, reading one line at a time
			//FileReader always assumes default encoding is OK!
			BufferedReader inputM =  new BufferedReader(new FileReader(mFile));
			BufferedReader inputE =  new BufferedReader(new FileReader(eFile));
			try {
				String line = null; //not declared within while loop
				/*
				 * readLine is a bit quirky :
				 * it returns the content of a line MINUS the newline.
				 * it returns null only for the END of the stream.
				 * it returns an empty String if two newlines appear in a row.
				 */
				
				/*while ( ( line=input.readLine() )!=null){
					contents.append(line);
					contents.append(System.getProperty("line.separator"));
					String[] strs = line.split("\t");
					for(String s: strs){
						System.out.print(s+"\t");
					}
					System.out.println();
				}*/
				
				System.out.println("M------------------------");
				for(int i = 0 ; i < x; i++){
					System.out.println(i+" : ");
					for(int j = 0 ; j < y ; j++){
						line = inputM.readLine();
						System.out.print("  "+j+" : ");
						float[] m = new float[z];
						String[] mStrs = line.split("\t");
						for(int k = 0; k < z; k++){
							System.out.print(mStrs[k]+"\t");
							m[k] = Float.valueOf(mStrs[k]);
						}
						Position p = new Position();
						p.setCircle_measurements(m);
						grid[i][j] = p;
						System.out.println();
					}
				}
				System.out.println("-----------------------");
				
				System.out.println("E------------------------");
				for(int i = 0 ; i < x; i++){
					System.out.println(i+" : ");
					for(int j = 0 ; j < y ; j++){
						line = inputE.readLine();
						System.out.print("  "+j+" : ");
						float[] e = new float[z];
						String[] eStrs = line.split("\t");
						for(int k = 0; k < z; k++){
							System.out.print(eStrs[k]+"\t");
							e[k] = Float.valueOf(eStrs[k]);
						}
						Position p = grid[i][j];
						p.setEnergy(e);
						grid[i][j] = p;
						System.out.println();
					}
				}
				System.out.println("-----------------------");
				
			}
			finally {
				inputM.close();
				inputE.close();
			}
		}
		catch (IOException ex){
			ex.printStackTrace();
		}
		return grid;
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
	
	static public void setContents(File mFile, File eFile, Position[][] Grid)
			throws FileNotFoundException, IOException {
		if (mFile == null) {
			throw new IllegalArgumentException("File should not be null.");
		}
		if (!mFile.exists()) {
			throw new FileNotFoundException ("File does not exist: " + mFile);
		}
		if (!mFile.isFile()) {
			throw new IllegalArgumentException("Should not be a directory: " + mFile);
		}
		if (!mFile.canWrite()) {
			throw new IllegalArgumentException("File cannot be written: " + mFile);
		}
		if (eFile == null) {
			throw new IllegalArgumentException("File should not be null.");
		}
		if (!eFile.exists()) {
			throw new FileNotFoundException ("File does not exist: " + eFile);
		}
		if (!eFile.isFile()) {
			throw new IllegalArgumentException("Should not be a directory: " + eFile);
		}
		if (!eFile.canWrite()) {
			throw new IllegalArgumentException("File cannot be written: " + eFile);
		}
		
		//use buffering
		Writer outputM = new BufferedWriter(new FileWriter(mFile));
		Writer outputE = new BufferedWriter(new FileWriter(eFile));
		try {
			//FileWriter always assumes default encoding is OK!
			for(int i = 0; i < Grid.length; i++){
				for(int j = 0 ; j < Grid[i].length; j++){
					for(float f: Grid[i][j].getMeasurements(-1)){
						outputM.write(String.valueOf(f)+"\t");
					}
					outputM.write(System.getProperty("line.separator"));
					
					for(float f: Grid[i][j].getEnergy()){
						outputE.write(String.valueOf(f)+"\t");
					}
					outputE.write(System.getProperty("line.separator"));
				}
			}
		}
		finally {
			outputM.close();
			outputE.close();
		}
	}

	/** Simple test harness.   */
	public static void main (String[] args) throws IOException {
		
		Position[][] grid = new Position[2][3];
		
		for(int i = 0; i < 2;i++){
			for(int j = 0 ; j < 3; j++){
				float[] f = {(i*3*2+j*2+1), (i*3*2+j*2+2)};
				Position p = new Position(f);
				grid[i][j] = p;
				//System.out.println(p.toString());
			}
		}
		
		File mFile = new File("/Users/ihsumlee/Jolly/measurement.txt");
		File eFile = new File("/Users/ihsumlee/Jolly/energy.txt");
		setContents(mFile, eFile, grid);
		for(Position[] ps: getContents(mFile, eFile)){
			for(Position p : ps){
				System.out.println(p.toString());
			}
		}

		
	}
}
