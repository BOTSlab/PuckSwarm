package experiment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Properties;

import utils.FileUtils;

/**
 * An all-static class providing management of Experiments to client classes.
 */
public class ExperimentManager {

	// static var's for the management of multiple Experiment instances.
	private static ExperimentBlock currentBlock = null;
	private static int blockIndex = 0;
	private static ArrayList<ExperimentBlock> blocks;
	private static boolean finished = false;
	private static boolean activated = false;
	
	static String outputDir = "/Users/av/DATA/CONSTRUCT/LIVE";

	// The liveExperiment experiment will be active whenever there is no other
	// active experiment (e.g. prior to 'activate' being called). It is created
	// in 'getCurrent' only if needed.
	private static Experiment liveExperiment = null;

	// Used when creating all Experiment possiblities.
	private static int index = 0;

	static final String COMMON_PROPERTIES_FILENAME = "common.properties";

	private static void createExperiments() {
		//
		// VARYING PARAMETERS:
		//
		// The experiments below all utilize the basic configuration:
		// 40 pucks, 2 types, 4 robots, scale 1
		//

		// Varying ProbSeek's K2: WINNER: K2 = 8 (highest TWC and tied for 
		// lowest avgSTC with K2 = 4)
		/*
		outputDir = "VARY_K2";
		String namesAndValues[][] = {
				{ "controllerType", 		"ProbSeek" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "ProbSeekController.K2", 	"-1", "1", "2", "4", "8", "16"}
		};
		*/

		// Varying CacheCons's CACHE_SELECTION_OPTION and related parameters
		// such as K_ABSOLUTE and K_RELATIVE.
		// WINNER: RELATIVE, avgTWC: 0.993331592039801, avgSTC: 1000.0
		/*
		outputDir = "VARY_SELECTION_1";
		String namesAndValues[][] = {
				{ "controllerType", 		"CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "CacheConsController.CACHE_SELECTION_OPTION", "PROB_ABSOLUTE" },
				{ "CacheConsController.K_ABSOLUTE", 	"-1", "10", "20", "40", "80", "160", "320" }
		};
		*/
		/*
		
		// WINNER: K_RELATIVE = 8, avgTWC: 0.9780950248756219, avgSTC: 3100.0
		outputDir = "VARY_SELECTION_2";
		String namesAndValues[][] = {
				{ "controllerType", 		"CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "CacheConsController.CACHE_SELECTION_OPTION", "PROB_RELATIVE"},
				{ "CacheConsController.K_RELATIVE", 	"-1", "1", "2", "4", "8", "16", "32"}
		};
		
		// WINNER: 
		outputDir = "VARY_SELECTION_3";
		String namesAndValues[][] = {
				{ "controllerType", 		"CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "CacheConsController.CACHE_SELECTION_OPTION", "RELATIVE"}
		};
		*/
		
		// Varying CacheCons CACHE_SEPARATION_OPTION
		// WINNER: ACCEPT_LARGER, avgTWC: 0.9992146766169153, avgSTC: 800.0
		/*
		outputDir = "VARY_SEPARATION";
		String namesAndValues[][] = {
				{ "controllerType", 		"CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "CacheConsController.CACHE_SEPARATION_OPTION", "IGNORE", "NULLIFY_OLD", "ACCEPT_ISOLATED", "ACCEPT_LARGER"}
		};
		*/
		/*
		outputDir = "VARY_SELECTION_4";
		String namesAndValues[][] = {
				{ "controllerType", 		"CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "CacheConsController.CACHE_SELECTION_OPTION", "RELATIVE_FORGET"},
				{ "CacheConsController.FORGET_PROB", "-1", "0.0001f", "0.001f", "0.01f", "0.1f"}
		};
		*/

		//
		// EXPERIMENTS:
		//
		/*
		// Vary nPucks
		outputDir = "NPUCKS";
		String namesAndValues[][] = {
				{ "controllerType", 		"ProbSeek", "CacheCons" },
				{ "Arena.nPucks",			"20", "40", "80", "160" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"}
		};
		*/
		
		// Vary nPuckTypes
		/*
		outputDir = "NPUCKTYPES_1";
		String namesAndValues[][] = {
				{ "controllerType", 		"BHD", "ProbSeek", "CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"1" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"}
		};
		*/
		/*
		outputDir = "NPUCKTYPES_2";
		String namesAndValues[][] = {
				{ "controllerType", 		"ProbSeek", "CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2", "4", "8" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"}
		};
		*/
		/*
		outputDir = "NPUCKTYPES_IGNORE";
		String namesAndValues[][] = {
				{ "controllerType", 		"CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2", "4", "8" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "CacheConsController.CACHE_SEPARATION_OPTION", "IGNORE"}
		};
		*/
		
		/*
		// Vary scale
		outputDir = "SCALE";
		String namesAndValues[][] = {
				{ "controllerType", 		"ProbSeek", "CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1", "2", "4", "8" }
		};
		*/
		
		// Vary nRobots
		/*
		outputDir = "NROBOTS";
		String namesAndValues[][] = {
				{ "controllerType", 		"ProbSeek", "CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"1", "2", "4", "8" },
				{ "RoundedRectangleEnclosure.scale", "1" }
		};
		outputDir = "NROBOTS_37";
		String namesAndValues[][] = {
				{ "controllerType", 		"ProbSeek", "CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"3", "7" },
				{ "RoundedRectangleEnclosure.scale", "1" }
		};
		*/
		/*
		outputDir = "NROBOTS_64";
		String namesAndValues[][] = {
				{ "controllerType", 		"ProbSeek", "CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"64" },
				{ "RoundedRectangleEnclosure.scale", "1" }
		};
		*/
		
		/*
		outputDir = "SHIFT_PS";
		String namesAndValues[][] = {
				{ "controllerType", 		"ProbSeek" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"1" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "Arena.puckShiftStep",	"10000" },
		};
		*/
		
		/*
		outputDir = "SHIFT_CC";
		String namesAndValues[][] = {
				{ "controllerType", 		"CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"1" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "CacheConsController.CACHE_SELECTION_OPTION", "RELATIVE", "PROB_RELATIVE", "PROB_ABSOLUTE", "RELATIVE_FORGET"},
				{ "Arena.puckShiftStep",	"10000" },
		};
		*/
		/*
		outputDir = "SHUFFLE_PS";
		String namesAndValues[][] = {
				{ "controllerType", 		"ProbSeek" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"1" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "Arena.puckShuffleStep",	"10000" },
		};
		*/
		/*
		outputDir = "SHUFFLE_CC";
		String namesAndValues[][] = {
				{ "controllerType", 		"CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"1" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "CacheConsController.CACHE_SELECTION_OPTION", "RELATIVE", "PROB_RELATIVE", "PROB_ABSOLUTE", "RELATIVE_FORGET"},
				{ "Arena.puckShuffleStep",	"10000" },
		};
		*/
		

		/*
		outputDir = "STANDARD_REV";
		String namesAndValues[][] = {
				{ "controllerType", 		"ProbSeek", "CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"4" }
		};
		*/

		
		outputDir = "INFORMED_CIRCLE_2";
		String namesAndValues[][] = {
				{ "controllerType", 		"CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"4" },
				{ "Arena.nInformedRobots", "0", "4" }
		};
		
		/*
		outputDir = "INFORMED_CIRCLE_4";
		String namesAndValues[][] = {
				{ "controllerType", 		"CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"4" },
				{ "Arena.nRobots", 			"4" },
				{ "Arena.nInformedRobots", "0", "4" }
		};
		*/
		/*
		outputDir = "INFORMED_CIRCLE_8";
		String namesAndValues[][] = {
				{ "controllerType", 		"CacheCons" },
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"8" },
				{ "Arena.nRobots", 			"4" },
				{ "Arena.nInformedRobots", "0", "4" }
		};
		*/
		
		/*
		outputDir = "ECAL_4";
		String namesAndValues[][] = {
//				{ "Arena.nPucks",			"40" },
//				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"4" },
				{ "Arena.nInformedRobots", 			"0", "1", "2", "3", "4"},
				{ "CacheConsController.PRESET_ROW_HEIGHT", "0.5f"}
//				{ "CacheConsController.IGNORE_PUCKS", "false" }
		};
		*/

		/*
		outputDir = "WORKSHOP_4";
		String namesAndValues[][] = {
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"4" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "Arena.nInformedRobots", 			"1", "2", "3", "4"},
				{ "CacheConsController.CACHE_SELECTION_OPTION", "RELATIVE", "RELATIVE_FORGET"}
		};
		*/
		
		/*
		outputDir = "WORKSHOP_8";
		String namesAndValues[][] = {
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"8" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "Arena.nInformedRobots", 			"1", "2", "3", "4"},
				{ "CacheConsController.CACHE_SELECTION_OPTION", "RELATIVE", "RELATIVE_FORGET"}
		};
		*/

		/*
		outputDir = "WORKSHOP_12";
		String namesAndValues[][] = {
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"12" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "Arena.nInformedRobots", 			"1", "2", "3", "4", "5", "6"},
				{ "CacheConsController.CACHE_SELECTION_OPTION", "RELATIVE", "RELATIVE_FORGET"}
		};
		*/

		/*
		outputDir = "WORKSHOP_16";
		String namesAndValues[][] = {
				{ "Arena.nPucks",			"40" },
				{ "Arena.nPuckTypes",		"2" },
				{ "Arena.nRobots", 			"16" },
				{ "RoundedRectangleEnclosure.scale", "1"},
				{ "Arena.nInformedRobots", 			"2", "4", "6", "8"},
				{ "CacheConsController.CACHE_SELECTION_OPTION", "RELATIVE", "RELATIVE_FORGET"}
		};
		*/
		
		// Make sure the output directory exists.
		(new File(outputDir)).mkdirs();
		
		// Set and store the common properties for all experiments.
		int repetitions = 20;
// ECAL:
//int repetitions = 5;
		int startIndex = 0;
		Properties common = new Properties();
		common.put("repetitions", repetitions + "");
		common.put("startIndex", startIndex + "");
//		common.put("Arena.maxStepCount", "100000");
// ECAL:
common.put("Arena.maxStepCount", "20000");
		
// ECAL:
//common.put("Arena.maxStepCount", "10000");

// STANDARD_REV:
//common.put("Arena.maxStepCount", "20000");
		
		try {
			common.store(new FileOutputStream(outputDir + File.separatorChar + COMMON_PROPERTIES_FILENAME), null);
		} catch (Exception e) {
			System.err.println("ExperimentManager: Problem storing properties");
		}	
		
		ArrayList<String> filenames = new ArrayList<String>();
		depthFirstExpand(0, namesAndValues, new Properties(), filenames);

		createExperimentBlocks(filenames, repetitions, startIndex);
	}
	
	private static void readExistingExperiments() {
		// Get a list of all .properties files (each one corresponding to an
		// experiment).  Extract the code for each and place in 'codes'.
		ArrayList<String> filenames = FileUtils.getMatchedFilenames(".*.properties", outputDir);
		filenames.remove("common.properties");
		for (String code : filenames)
			System.out.println("Found code: " + code);
		
		// Read some necessary properties from 'common.properties'.
		int repetitions = 0;
		int startIndex = 0;
		try {
			Properties common = new Properties();
			common.load(new FileInputStream(outputDir + File.separatorChar + "common.properties"));
			repetitions = Integer.valueOf(common.getProperty("repetitions"));
			if (common.containsKey("startIndex"))
				startIndex = Integer.valueOf(common.getProperty("startIndex"));
		} catch (Exception e) {
			System.err.println("ExperimentManager: Problem loading common properties");
		}
		System.out.println("Found repititions: " + repetitions);
		
		createExperimentBlocks(filenames, repetitions, startIndex);
	}
	
	private static void createExperimentBlocks(ArrayList<String> filenames, int repetitions, int startIndex) {
		blocks = new ArrayList<ExperimentBlock>();
		for (String filename : filenames)
			blocks.add(new ExperimentBlock(repetitions, startIndex, outputDir, filename));

		currentBlock = blocks.get(0);		
	}
	

	/**
	 * Recursive method to expand all possibilities of property values and 
	 * create the .properties files for each experiment.  These files go into
	 * 'filenames'.
	 */
	private static void depthFirstExpand(int depth, String[][] namesAndValues,
										 Properties underConstruction, 
										 ArrayList<String> filenames) {
		if (depth == namesAndValues.length) {
			// Base case: Complete 'underConstruction' by setting its
			// description and loading in all common properties.

			String code = setCodeString(namesAndValues, underConstruction);

			// Incorporate common properties.
			/*
			Set<String> keys = common.stringPropertyNames();
			for (String key : keys)
				underConstruction.setProperty(key, common.getProperty(key));
			*/

			// Save the properties describing this experiment.
			String filename = outputDir + File.separatorChar + code + ".properties";
			try {
				underConstruction.store(new FileOutputStream(filename), null);
				System.out.println("Created experiment file: " + filename);				
			} catch (Exception e) {
				System.err.println("ExperimentManager: Problem storing properties");
			}
			filenames.add(filename);
			index++;
			return;
		}

		// Extract the first name from names.
		String name = namesAndValues[depth][0];

		// Now go through the values...
		for (int i = 1; i < namesAndValues[depth].length; i++) {
			String v = namesAndValues[depth][i];
			Properties props = (Properties) underConstruction.clone();
			props.setProperty(name, v + "");
			depthFirstExpand(depth + 1, namesAndValues, props, filenames);
		}
	}

	private static String setCodeString(String[][] namesAndValues,
			Properties props) {

		// Create the code string by concatenating the values of the properties
		// in the order that they appear in 'namesAndValues'.
		String code = "";
		for (int i = 0; i < namesAndValues.length; i++) {
			code += props.getProperty(namesAndValues[i][0]);
			if (i != namesAndValues.length - 1)
				code += "_";
		}

		// Replace any '-' characters with 'm' (just because it makes filenames
		// easier to manipulate from the command-line).
		code = code.replace('-', 'm');

		props.setProperty("code", code);
		
		return code;
	}

	public static Experiment getCurrent() {
		if (activated && !finished)
			return currentBlock.getCurrent();
		else {
			// ExperimentManager hasn't been activated, so we assume we are in
			// live mode (i.e. interacting with the physical robots or
			// simulator in interactive mode).
			if (liveExperiment == null) {
				////liveExperiment = new EditableExperiment();
				//String code = JOptionPane.showInputDialog("Provide a code name for this experiment (e.g. ProbSeek):");
				//int seed = Integer.valueOf(JOptionPane.showInputDialog("Provide a seed for this experiment (e.g. 0):"));	
				String code = "0";
				int seed = 0;

				liveExperiment = new Experiment(code, seed, outputDir);
//liveExperiment = new Experiment("null", 0, outputDir);
			}
			return liveExperiment;
		}
	}

	public static boolean hasNext() {
		int nb = blocks.size();
		finished = (blockIndex == nb - 1) && !blocks.get(nb - 1).hasNext();
		return !finished;
	}

	public static void next() {
		if (currentBlock.hasNext())
			currentBlock.next();
		else {
			// Switch to a new block.
			blockIndex++;
			currentBlock = blocks.get(blockIndex);
		}
	}

	/**
	 * Prior to the initial call to activate, the liveExperiment experiment will always
	 * be returned by getCurrent.
	 * @param createExperiments	Set to true to create experiments from scratch.  
	 * 							If true 'oDir' will be ignored.  If false
	 * 							directory 'oDir' should already contain 
	 * 							.properties files describing the experiments.
	 * @param oDir		Output directory (valid only if !createExperiments)
	 */
	public static void activate(boolean createExperiments, String oDir) {
		outputDir = oDir;
		activated = true;
		if (createExperiments)
			createExperiments();
		else
			readExistingExperiments();
	}

	public static boolean isActive() {
		return activated && !finished;
	}
	
	public static String getOutputDir() {
		return outputDir;
	}
}
