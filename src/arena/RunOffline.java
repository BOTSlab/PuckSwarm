	package arena;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;

import sensors.SimSuite;
import experiment.ExperimentManager;

public class RunOffline {
	
//	private static String outputDir;
	private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private static World world;
	private static Arena arena;
	private static long startTime = -1;
	private static int updateCount;
	
	public static void main(String[] args) throws SecurityException, IOException {
		
		if (args.length > 2) {
			System.err.println("If no arguments are specified then we launch " + 
					"using the hardcoded experiments defined in " + 
					"ExperimentManager.  If 1 argument is specified then this " +
					"should be a directory containing existing .properties files " +
					"which define the experiments to run required.");
			System.exit(-1);
		}
		
		boolean createExperiments = true;
		String outputDir = "";
		if (args.length == 1) {
			createExperiments = false;
			outputDir = args[0];
		}

		// Activate the ExperimentManager.
		ExperimentManager.activate(createExperiments, outputDir);
		
		FileHandler handler = new FileHandler(getLogFileName());
		handler.setFormatter(new SimpleFormatter());
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);
		
		String experimentCode = ExperimentManager.getCurrent().getStringCode();
		logger.info(experimentCode);
		recreateArena();
		
		while (ExperimentManager.isActive())
			step();
	}
	
	private static String getLogFileName() {
		DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy_HH.mm.ss");
		Date date = new Date();
		return ExperimentManager.getOutputDir() + File.separatorChar + "log_" + 
			dateFormat.format(date) + ".txt";
	}

	private static void step() {
		// Check to see if the current Experiment is over.  If so, store the 
		// result and start the next one.
		if (arena.getStepCount() > ExperimentManager.getCurrent().
				getProperty("Arena.maxStepCount", 10000, null))
		{
			//ExperimentManager.store();
			if (ExperimentManager.hasNext())
				ExperimentManager.next();
			else
				return;
			recreateArena();
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info(ExperimentManager.getCurrent().getStringCode());
		}
		
		if (updateCount % PuckSwarmTest.STEP_INTERVAL == 0)
			arena.step(true, true, true, false, 0, false);
		else
			arena.coast(true, true, true);
		updateCount++;

		//world.step(1/60f, 8, 100);
		world.step(1/14f, 3, 100);
	}

	private static void recreateArena() {
		// Create anew.
		updateCount = 0;
		Robot.nameIndex = 0;
		SimSuite.jframeY = 25;
		if (startTime != -1) {
			long elapsed = (System.currentTimeMillis() - startTime) / 60000; 
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("elapsed: " + elapsed + " minutes");
		}
		
		// A whole new world!  (Reusing an existing world led to a memory leak!)
		world = new World(new Vec2(0,0), false);

		arena = new Arena(world, null, false);
		
		startTime = System.currentTimeMillis();
	}
}