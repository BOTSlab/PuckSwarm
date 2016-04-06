package analysis;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import org.jbox2d.common.MathUtils;

import sensors.SensedType;
import utils.DataTableUtils;
import utils.FileUtils;
import utils.RunningStats;
import arena.PositionList;
import de.erichseifert.gral.data.DataTable;

/**
 * A FORK OF Analyze.java FOR ROBOT AGGREGATION EXPERIMENTS IN V-REP / CREATES.
 * 
 * Take the stored puck position lists (in .txt files) and do the same extraction of clusters 
 * as in LocalMap then compute various measures and statistics.  Data on the experiments
 * is extracted from .properties files in the results directory.  Then compute the statistics on
 * cluster sizes and store the results.  The following files comprise the output:
 * - Files named 'METHOD__X.csv' where X represents the repetition number.
 * - Files named 'METHOD__avg.csv' which presents results averaged over all trials.
 * - Files named 'METHOD__stepsToCompletion.csv' which gives the number of steps
 *    required to reach TARGET_PERCENT_COMPLETION for each trial.
 * 
 */
public class AnalyzeAggregate {
	
	static String ANALYZE_DIR;
	static int REPETITIONS;
	
	public static final float TARGET_PERCENT_COMPLETION = 100f;
	private static final char SLASH = File.separatorChar;
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		if (args.length != 2) {
			System.err.println("Two arguments required: ANALYZE_DIR (String) and REPETITIONS (int)");
			System.exit(-1);
		}
		ANALYZE_DIR = args[0];
		REPETITIONS = Integer.valueOf(args[1]);
		System.out.println("\nanalyzing: " + ANALYZE_DIR);
		
		// Get a list of all .properties files (each one corresponding to an
		// experiment).  Extract the stored properties for each and place in 'experiments'.
		ArrayList<String> propFiles = FileUtils.getMatchedFilenames(".*.properties", ANALYZE_DIR);
		propFiles.remove("common.properties");
		ArrayList<Properties> experiments = new ArrayList<Properties>();
		for (String filename : propFiles) {
			try {
				Properties properties = new Properties();
				properties.load(new FileInputStream(ANALYZE_DIR + File.separatorChar + filename));
				experiments.add(properties);
			} catch (Exception e) {
				System.out.println("e" + e);
				System.err.println("Analyze: Problem loading: " + filename);
			}
		}
		
		// Read some necessary properties from 'common.properties'.
		/*
		int foundRepetitions = 0;
		try {
			Properties common = new Properties();
			common.load(new FileInputStream(ANALYZE_DIR + File.separatorChar + "common.properties"));
			foundRepetitions = Integer.valueOf(common.getProperty("repetitions"));
		} catch (Exception e) {
			System.err.println("Analyze: Problem loading common properties");
		}

		System.out.println("Found repetitions: " + foundRepetitions);
		*/
		System.out.println("Using repetitions: " + REPETITIONS);
		
		// For each experiment (i.e. each code), repetition index, and colour create a table
		// and fill with appropriate statistics.
		
		for (Properties experiment : experiments) {
			processExperiment(experiment);
			
		} // code
	}

	private static void processExperiment(Properties properties) {
		// Extract the code and nPucks and nPuckTypes from the properties file.
		String code = "";
		int nPucks = 0, nPuckTypes = 0;
		try {
			code = properties.getProperty("code");
			nPucks = Integer.valueOf(properties.getProperty("Arena.nPucks"));
			nPuckTypes = Integer.valueOf(properties.getProperty("Arena.nPuckTypes"));			
			
		} catch (Exception e) {
			System.err.println("Analyze: Problem loading needed properties");
		}
			
		System.out.print("code: " + code + "\t\t");
		//System.out.println("nPucks: " + nPucks);
		//System.out.println("nPuckTypes: " + nPuckTypes);
		
		DataTable[][] tables = new DataTable[REPETITIONS][nPuckTypes];
		for (int i=0; i<REPETITIONS; i++)
			tables[i] = new DataTable[nPuckTypes];
					
		for (int i=0; i<REPETITIONS; i++) {
			for (int k=0; k<nPuckTypes; k++)
				fillTable(code, tables, i, k, SensedType.getPuckColorName(k));
		} // i
		
		//storeAverageTable(tables, ANALYZE_DIR + File.separatorChar + code + "__avg_" + colorName + ".csv");
		
		computeEnsembleData(code, tables, REPETITIONS, nPucks);
	}

	@SuppressWarnings("unchecked")
	private static void fillTable(String code, DataTable[][] tables,
			int i, int k, String colorName) {
		
		String dirName = ANALYZE_DIR + SLASH + code + SLASH + i;
		
		tables[i][k] = new DataTable(Integer.class,    // stepCount 
								    Integer.class, 	// number of clusters
								    Double.class, 	// mean cluster size
								    Double.class, 	// std. cluster size
								    Double.class, 	// min cluster size
								    Double.class,	// max cluster size
								    	Double.class);   // total distance between robots (for aggregation)

		// Now go through all completed text files in order of stepCount.
//		ArrayList<String> txtFiles = FileUtils.getMatchedFilenames("step\\d*_" + colorName + "_pucks.txt", dirName);
		ArrayList<String> txtFiles = FileUtils.getMatchedFilenames("step\\d*_bots2d.txt", dirName);
		Collections.sort(txtFiles);
		for (String txtFile : txtFiles) {
			int stepInStr = txtFile.indexOf("step");
			int stepCount = Integer.valueOf(txtFile.substring(stepInStr + 4, stepInStr + 11));
			
			PositionList list = PositionList.load(dirName + SLASH + txtFile);
			RunningStats stats = list.getClusterStats();
			
			// Compute total distance
			double td = 0.0;
			int n =  list.size();
			for (int I = 0; I<n-1; I++)
				for (int j= k+1; j<n; j++)
					td += MathUtils.distance(list.get(I), list.get(j));
			
			if (stats == null)
				tables[i][k].add(stepCount, 0, 0., 0., 0., 0., 0.);
			else
				tables[i][k].add(stepCount, stats.getNumberOfValues(), stats.getMean(), stats.getStandardDeviation(), stats.getMin(), stats.getMax(), td);
		}
		/*
		if (txtFiles.size() > 0) {
			DataTableUtils.storeTable(ANALYZE_DIR + File.separatorChar + code + "__" + i + "_" + colorName + ".csv", tables[i]);
			System.out.println("completed repetition " + i + ": " + code);
		} else
			missingData = true;
		*/
	}

	@SuppressWarnings("unchecked")
	private static void computeEnsembleData(String code, DataTable[][] tables,
			int repetitions, int nPucks) {
		
		// Check that all tables have the same number of rows.
		int nRows = tables[0][0].getRowCount();
		for (int i=0; i<repetitions; i++)
			for (int k=0; k<tables[i].length; k++)
				if (tables[i][k] != null && tables[i][k].getRowCount() != nRows) {
					System.err.println("Analyze: Not all tables have an equal number of rows!");
					return;
				}
		
		// Create individual per-repetition (per-i) tables to hold PC and TD.
		DataTable[] pcTables = new DataTable[repetitions];
		for (int i=0; i<repetitions; i++)
			pcTables[i] = new DataTable(Integer.class,   // stepCount 
														Double.class); 	// % completion
															
		DataTable avgPCTable = new DataTable(Integer.class,   // stepCount 
																	  Double.class); 	// % completion

		DataTable[] tdTables = new DataTable[repetitions];
		for (int i=0; i<repetitions; i++)
			tdTables[i] = new DataTable(Integer.class,   // stepCount 
														Double.class); 	// total distance
															
		DataTable avgTDTable = new DataTable(Integer.class,   // stepCount 
																	  Double.class); 	// total distance

		// The statistics computed below are:
		// - percent completion for each time step
		// - average percent completion for each time step over all trials
		// - the number of steps before TARGET_PERCENT_COMPLETION is first reached.
		// - the time-averaged percentage completion for each trial
		
		// This array stores the number of steps required for each repetition to reach
		// TARGET_PERCENT_COMPLETION.  We initialize with -1 to indicate failure to reach.
		int[] stepsToCompletion = new int[repetitions];
		for (int i=0; i<repetitions; i++)
			stepsToCompletion[i] = -1;
		
		// This one stores the time averaged PC for each trial
		double[] timeAveragedPC = new double[repetitions];
		
		// This one stores the time averaged TD for each trial
		double[] timeAveragedTD = new double[repetitions];

		for (int row=0; row<nRows; row++) {			
			int stepCount = tables[0][0].get(0, row).intValue();
			double avgPercentCompletion = 0;
			double avgTotalDistance = 0;

			for (int i=0; i<repetitions; i++) {
				
				// Compute percentage completion (PC)
				double sumMaxClusterSize = 0;
				for (int k=0; k<tables[i].length; k++) {
					if (tables[i][k] != null)
						sumMaxClusterSize += tables[i][k].get(5, row).doubleValue();
				}
				double percentCompletion = 100.0 * sumMaxClusterSize / nPucks;
				
				// Add PC to the ensemble tables and average it
				pcTables[i].add(stepCount, percentCompletion);
				avgPercentCompletion += percentCompletion;
				
				// Compute steps to completion (STC)
				if (stepsToCompletion[i] == -1 && 
					percentCompletion >= TARGET_PERCENT_COMPLETION)
					stepsToCompletion[i] = stepCount;
				
				// Compute time averaged completion (TAPC)
				timeAveragedPC[i] += percentCompletion / 100.0;
				
				// Add TD to the ensemble tables and average it
				double td = tables[i][0].get(6, row).doubleValue();
				tdTables[i].add(stepCount, td);
				avgTotalDistance += td;

				// Compute time averaged total distance (TATD)
				timeAveragedTD[i] += td;
			}
			avgPCTable.add(stepCount, avgPercentCompletion / repetitions);
			avgTDTable.add(stepCount, avgTotalDistance / repetitions);
		}
		
		for (int i=0; i<repetitions; i++)
			timeAveragedPC[i] /= nRows;
				
		for (int i=0; i<repetitions; i++)
			DataTableUtils.storeTable(ANALYZE_DIR + File.separatorChar + code + 
					"__PC_" + i + ".csv", pcTables[i]);
		DataTableUtils.storeTable(ANALYZE_DIR + File.separatorChar + code + 
				"__APC.csv", avgPCTable);
		
		for (int i=0; i<repetitions; i++)
			DataTableUtils.storeTable(ANALYZE_DIR + File.separatorChar + code + 
					"__TD_" + i + ".csv", tdTables[i]);
		DataTableUtils.storeTable(ANALYZE_DIR + File.separatorChar + code + 
				"__ATD.csv", avgTDTable);
		
		// Store the stepsToCompletion array as another .csv.  However, in this case the
		// rows represent individual trials.
		DataTable stepsToCompletionTable = new DataTable(Integer.class);
		for (int i=0; i<repetitions; i++)
			stepsToCompletionTable.add(stepsToCompletion[i]);
		DataTableUtils.storeTable(ANALYZE_DIR + File.separatorChar + code + 
				"__stepsToCompletion.dat", stepsToCompletionTable);

		/*
		DataTable timeWeightedCompletionTable = new DataTable(Double.class);
		for (int i=0; i<repetitions; i++)
			timeWeightedCompletionTable.add(timeWeightedCompletion[i]);
		DataTableUtils.storeTable(ANALYZE_DIR + File.separatorChar + code + 
				"__timeWeightedCompletion.dat", timeWeightedCompletionTable);
		*/
		DataTable timeAveragedPCTable = new DataTable(Double.class);
		for (int i=0; i<repetitions; i++)
			timeAveragedPCTable.add(timeAveragedPC[i]);
		DataTableUtils.storeTable(ANALYZE_DIR + File.separatorChar + code + 
				"__timeAveragedPC.dat", timeAveragedPCTable);
		
		// Print out the average timeWeightedCompletion.
		/*
		double avgTWC = 0;
		for (int i=0; i<repetitions; i++)
			avgTWC += timeWeightedCompletion[i];
		System.out.print("avgTWC: " + avgTWC / repetitions + "\t\t");
		*/
		double avgTAPC = 0;
		for (int i=0; i<repetitions; i++)
			avgTAPC += timeAveragedPC[i];
		System.out.print("avgTAPC: " + avgTAPC / repetitions + "\t\t");

		double avgTATD = 0;
		for (int i=0; i<repetitions; i++)
			avgTATD += timeAveragedTD[i];
		System.out.print("avgTATD: " + avgTATD / repetitions + "\t\t");
		
		// Print out the average stepsToCompletion (if there are no -1's)
		double avgSTC = 0;
		int incompletions = 0;
		for (int i=0; i<repetitions; i++) {
			if (stepsToCompletion[i] == -1)
				incompletions++;
			else
				avgSTC += stepsToCompletion[i];				
		}
		if (incompletions == 0)
			System.out.println("avgSTC: " + avgSTC / repetitions);
		else
			System.out.println("incompletions: " + incompletions + " / " + repetitions);
	}

	/*
	private static void storeAverageTable(DataTable[] tables, String filename) {
		int nRows = tables[0].getRowCount();
		for (DataTable table : tables)
			if (table.getRowCount() != nRows) {
				System.err.println("Analyze: Cannot average unequal tables!");
				return;
			}
		
		@SuppressWarnings("unchecked")
		DataTable meanTable = new DataTable(Double.class,   // stepCount 
											Double.class, 	// number of clusters
										    Double.class, 	// mean cluster size
										    Double.class, 	// std. cluster size
										    Double.class, 	// min cluster size
										    Double.class);	// max cluster size

		
		for (int row=0; row<nRows; row++) {
			
			// First compute the sum over all tables for this row.
			Double[] means = { new Double(0), new Double(0), new Double(0),
					 		   new Double(0), new Double(0), new Double(0) };
			for (DataTable table : tables) {
				Number[] thisRow = (Number[]) table.getRow(row).toArray(null);
				
				for (int col=0; col<means.length; col++) {
					means[col] = means[col] + thisRow[col].doubleValue();
				}
			}
			
			// Divide by the number of tables
			for (int col=0; col<means.length; col++)
				means[col] = means[col] / tables.length;
			
			meanTable.add(means);
		}

		DataTableUtils.storeTable(filename, meanTable);					
	}
	*/
}
