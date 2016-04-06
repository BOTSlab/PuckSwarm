package arena;

import java.io.File;

/**
 * Storage for a few harmless globals.
 */
public class Settings {

	public static String getStepCountString(int stepCount) {
		return String.format("%07d", stepCount);
	}

	//public static final char SLASH = File.separatorChar;
	//public static final String RESULTS_DIR = System.getProperty("user.home") + SLASH + "PuckSwarm";
	//public static final String CALIB_DIR = System.getProperty("user.home") + SLASH + "work" + SLASH + "data" + SLASH + "srv1" + SLASH + "calib";

	// Interval at which data should be stored.
//	public static final int STORAGE_INTERVAL = 500;
	
	
}
