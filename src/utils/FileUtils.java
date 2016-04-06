package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {
	/**
	 * Return a list of filenames that match the given regular expression in the
	 * given directory.
	 */
	public static ArrayList<String> getMatchedFilenames(String regex,
														String dirName) {
		ArrayList<String> matchedFilenames = new ArrayList<String>();
		Pattern pattern = Pattern.compile(regex);
		File dir = new File(dirName);
		String[] children = dir.list();
		if (children == null) {
			System.err.println("FileUtils: directory " + dirName + " does not exist!");
		} else {
			for (int i = 0; i < children.length; i++) {
				String filename = children[i];
				Matcher matcher = pattern.matcher(filename);
				if (matcher.find()) {
					matchedFilenames.add(filename);
				}
			}
		}
		return matchedFilenames;
	}

	/**
	 * Save the given array as a text file using one entry per line.
	 */
	public static void saveArray(int[] array, String filename) {
		try {
			
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));
			int n = array.length;
			for (int i=0; i<n; i++) {
				out.write(array[i] + "\n");
			}
			out.close();
		} catch (Exception e) {
			System.err.println("FileUtils: saveArray: Problem saving!");
			System.err.println("FileUtils: saveArray: filename: " + filename);
			System.exit(-1);
		}
	}
	
	/**
	 * Load an array from the given text file (saved as one number per line
	 * as by saveArray).
	 */
	public static int[] readArray(String filename) {
		try {
			ArrayList<Integer> arrayList = new ArrayList<Integer>();
			
			Scanner scanner = new Scanner(new File(filename));
			scanner.useDelimiter(System.getProperty("line.separator"));
			while (scanner.hasNext()) {
				String line = scanner.nextLine();
				arrayList.add(Integer.valueOf(line));
			}
			scanner.close();

			int n = arrayList.size();
			int[] array = new int[n];
			for (int i=0; i<n; i++)
				array[i] = arrayList.get(i);
			return array;
			
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
			return null;
		}
	}

}
