package experiment;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Random;

public class Experiment {

	protected String code;
	protected Random random;
	protected int seed;
	protected String propertiesFilename;
	protected Properties properties;
	
	private static final char SLASH = File.separatorChar;

	/**
	 * Construct an experiment from a given .properties file.
	 */
	Experiment(int seed, String outputDir, String propertiesFilename) {
		this.random = new Random(seed);
		this.seed = seed;
		this.propertiesFilename = propertiesFilename;

		properties = new Properties();
		try {
			properties.load(new FileInputStream(ExperimentManager.outputDir + File.separatorChar + ExperimentManager.COMMON_PROPERTIES_FILENAME));
			properties.load(new FileInputStream(propertiesFilename));
		} catch (Exception e) {
			System.err.println("Experiment: Problem loading: " + propertiesFilename);
		}
		code = properties.getProperty("code");
		
		// Create directories for output files.
		String subDirName = outputDir + SLASH + getStringCodeWithoutSeed();
		(new File(subDirName)).mkdirs();
		String subSubDirName = subDirName + SLASH + seed;
		(new File(subSubDirName)).mkdirs();		
	}
	
	/**
	 * Construct an experiment without a .properties file but given a code.
	 */
	Experiment(String code, int seed, String outputDir) {
		this.code = code;
		this.random = new Random(seed);
		this.seed = seed;

		// Create a properties object but leave it as empty.
		properties = new Properties();
		
		// Create directories for output files.
		String subDirName = outputDir + SLASH + getStringCodeWithoutSeed();
		(new File(subDirName)).mkdirs();
		String subSubDirName = subDirName + SLASH + seed;
		(new File(subSubDirName)).mkdirs();
	}

	public String getStringCode() {
		return getStringCodeWithoutSeed() + "__" + seed;
	}

	public String getStringCodeWithoutSeed() {
		return code;
	}

	public int getIndex() {
		return seed;
	}
	
	public Random getRandom() {
		return random;
	}

	public int getProperty(String key, int value, PropertiesListener listener) {
		return Integer.valueOf(properties.getProperty(key, value + ""));
	}

	public float getProperty(String key, float value, PropertiesListener listener) {
		return Float.valueOf(properties.getProperty(key, value + ""));
	}

	public boolean getProperty(String key, boolean value, PropertiesListener listener) {
		return Boolean.valueOf(properties.getProperty(key, value + ""));
	}

	public String getProperty(String key, String value, PropertiesListener listener) {
		return properties.getProperty(key, value);
	}
}
