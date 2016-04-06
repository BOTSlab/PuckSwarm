package arena;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class Grid {
	public float[][] data;
	public int width, height;
	
	public Grid(int width, int height) {
		this.width = width;
		this.height = height;
		data = new float[width][height];
	}
	
	/**
	 * Construct from a 2D array.  Note that we make a deep copy of the array.
	 */
	public Grid(float array[][]) {
		this.width = array[0].length;
		this.height = array.length;
		data = new float[width][height];
		for (int i=0; i<width; i++)
			for (int j=0; j<height; j++)
				data[i][j] = array[i][j];
	}

	/**
	 * Construct from a 1D array.  Note that we make a deep copy of the array.
	 */
	public Grid(float array[]) {
		this.width = array.length;
		this.height = 1;
		data = new float[width][1];
		for (int i=0; i<width; i++)
			data[i][0] = array[i];
	}

	@Override
	public Object clone() {
		Grid copy = new Grid(width, height);
		for (int i=0; i<width; i++)
			for (int j=0; j<height; j++)
				copy.data[i][j] = data[i][j];
		return copy;
	}

	public void clear() {
		for (int i=0; i<width; i++)
			for (int j=0; j<height; j++)
				data[i][j] = 0;
	}

	public float getSum() {
		float sum = 0;
		for (int i=0; i<width; i++)
			for (int j=0; j<height; j++)
				sum += data[i][j];
		return sum;
	}

	public void save(String filename) {
		try {
			FileWriter fstream = new FileWriter(filename);
			BufferedWriter out = new BufferedWriter(fstream);
			for (int j=0; j<height; j++) {
				for (int i=0; i<width; i++) {
					if (i < width - 1)
						out.write((int)data[i][j] + ", ");
					else
						out.write((int)data[i][j] + "");
				}
				out.write("\n");
			}
			out.close();
		} catch (Exception e) {
			System.err.println("Grid: Problem saving grid!");
		}
	}
	
	public static Grid load(String filename) {
		try {
			// Read the contents of the file into the following flexible-size
			// structure.  The data will be created at the right size and filled
			// afterward.
			ArrayList<ArrayList<Float>> allData = new ArrayList<ArrayList<Float>>();
			
			Scanner scanner = new Scanner(new File(filename));
			scanner.useDelimiter(System.getProperty("line.separator"));
			while (scanner.hasNext()) {
				ArrayList<Float> lineData = new ArrayList<Float>();
				String s = scanner.next();
				if (s.charAt(0) == '#')
					// Comment line.
					continue;
				Scanner lineScanner = new Scanner(s);
				lineScanner.useDelimiter("\\s*,\\s*");

				while (lineScanner.hasNext()) {
					float value = lineScanner.nextFloat();
					lineData.add(new Float(value));
				}
				
				allData.add(lineData);
				lineScanner.close();
			}
			scanner.close();

			int width = allData.get(0).size();
			int height = allData.size();
			Grid grid = new Grid(width, height);
			for (int j=0; j<height; j++)
				for (int i=0; i<width; i++)
					grid.data[i][j] = allData.get(j).get(i);
			return grid;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setAll(float value) {
		for (int i=0; i<width; i++)
			for (int j=0; j<height; j++)
				data[i][j] = value;
	}

	public float getMax() {
		float max = -Float.MAX_VALUE;
		for (int i=0; i<width; i++)
			for (int j=0; j<height; j++)
				if (data[i][j] > max)
					max = data[i][j];
		return max;
	}
}
