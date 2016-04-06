package arena;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;

import org.jbox2d.common.Vec3;

import sensors.Pose;

/**
 * Like PositionLIst, only for (x, y, theta) poses.  BAD: Should unify with PositionList.
 */
public class PoseList {
	private ArrayList<Pose> list = new ArrayList<Pose>();
	
	public Pose get(int i) {
		return list.get(i);
	}
	
	public void add(Pose p) {
		list.add(p);
	}
	
	public void add(float x, float y, float theta) {
		list.add(new Pose(x, y, theta));
	}

	public void save(String filename) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));
			int n = list.size();
			for (int i=0; i<n; i++) {
				Pose p = list.get(i);
				out.write(p.getX() + ", " + p.getY() + ", " + p.getTheta() + "\n");
			}
			out.close();
		} catch (Exception e) {
			System.err.println("PoseList: Problem saving!");
			System.exit(-1);
		}
	}
	
	/**
	 * Load the given file into a new PoseList and return it.
	 */
	public static PoseList load(String filename) {
		try {
			PoseList pList = new PoseList();
			
			Scanner scanner = new Scanner(new File(filename));
			scanner.useDelimiter(System.getProperty("line.separator"));
			while (scanner.hasNext()) {
				String s = scanner.next();
				Scanner lineScanner = new Scanner(s);
				lineScanner.useDelimiter("\\s*,\\s*");

				float x = lineScanner.nextFloat();
				float y = lineScanner.nextFloat();
				float theta = lineScanner.nextFloat();
				pList.add(x, y, theta);
				
				lineScanner.close();
			}
			scanner.close();

			return pList;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public int size() {
		return list.size();
	}
}
