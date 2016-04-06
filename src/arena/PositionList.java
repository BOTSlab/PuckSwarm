package arena;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;

import localmap.Cluster;
import localmap.LocalMap;

import org.jbox2d.common.Vec2;

import utils.RunningStats;

/**
 * A list of 2-D positions.  More convenient than an ArrayList<Vec2> or such
 * because we also have a 'save' and 'load' methods.
 */
public class PositionList {
	private ArrayList<Vec2> list = new ArrayList<Vec2>();
	
	public Vec2 get(int i) {
		return list.get(i);
	}
	
	public void add(Vec2 v) {
		list.add(v);
	}
	
	public void add(float x, float y) {
		list.add(new Vec2(x, y));
	}

	public void save(String filename) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));
			int n = list.size();
			for (int i=0; i<n; i++) {
				Vec2 p = list.get(i);
				out.write(p.x + ", " + p.y + "\n");
			}
			out.close();
		} catch (Exception e) {
			System.err.println("PositionList: Problem saving!");
			System.exit(-1);
		}
	}
	
	/**
	 * Load the given file into a new PositionList and return it.
	 */
	public static PositionList load(String filename) {
		try {
			PositionList pList = new PositionList();
			
			Scanner scanner = new Scanner(new File(filename));
			scanner.useDelimiter(System.getProperty("line.separator"));
			while (scanner.hasNext()) {
				String s = scanner.next();
				Scanner lineScanner = new Scanner(s);
				lineScanner.useDelimiter("\\s*,\\s*");

				float x = lineScanner.nextFloat();
				float y = lineScanner.nextFloat();
				pList.add(x, y);
				
				lineScanner.close();
			}
			scanner.close();

			return pList;
			
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Get a RunningStats object to describe the clusters extracted from this list.  Return null
	 * to indicate that no clusters were found.
	 */
	public RunningStats getClusterStats() {
		RunningStats stats = new RunningStats();
		ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		LocalMap.extractClusters(list, 0, clusters);
		for (Cluster cluster : clusters)
			stats.push(cluster.size);

		if (clusters.size() == 0)
			return null;
		
		return stats;
	}

	public int size() {
		return list.size();
	}
}
