package arena;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;
import org.jbox2d.testbed.framework.TestbedSettings;
import org.jbox2d.testbed.framework.TestbedTest;

import sensors.SimSuite;
import experiment.ExperimentManager;

public class PuckSwarmTest extends TestbedTest {

	Arena arena;
	
	int updateCount;
	boolean alreadyActivated, resetHit;
	java.awt.Robot captureRobot;

	public static final char SLASH = File.separatorChar;
	public static final String RESULTS_DIR = System.getProperty("user.home") + SLASH + "PuckSwarm";
	
	public static final int STEP_INTERVAL = 3;
	
	@Override
	public void initTest(boolean argDeserialized) {
		if (argDeserialized)
			return;

		setTitle("PuckSwarm");
		setCamera(new Vec2(0.0f, 0.0f), 3.5f);

		arena = new Arena(getWorld(), getDebugDraw(), true);
		
		try {
			captureRobot = new java.awt.Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void step(TestbedSettings settings) {

		// Activate the ExperimentManager (only available once) when the
		// "Activate Experiments" checkbox is first hit.
		if (!alreadyActivated && settings.getSetting("Activate Experiments").enabled) {
			ExperimentManager.activate(true, "PuckSwarm");
			String experimentCode = ExperimentManager.getCurrent().getStringCode();
			System.out.println(experimentCode);
			addTextLine(experimentCode);
			recreateArena();
			alreadyActivated = true;
		}

		// If ExperimentManager is activated, check to see if the current Experiment
		// is over.  If so, store the result and start the next one.
		if (ExperimentManager.isActive() && 
			arena.getStepCount() > ExperimentManager.getCurrent().
				getProperty("Arena.maxStepCount", 10000, null))
		{
			//ExperimentManager.store();
			if (ExperimentManager.hasNext())
				ExperimentManager.next();
			System.out.println(ExperimentManager.getCurrent().getStringCode());
			recreateArena();
		}
		
		if (resetHit) {
			recreateArena();
			resetHit = false;
		}
		
		int forcedTurn = 0;
		if (settings.getSetting("Forced Turn").enabled)
			forcedTurn = 1;
		if (!settings.pause || settings.singleStep) {
			if (updateCount % STEP_INTERVAL == 0)
				arena.step(settings.getSetting("Allow Thinking").enabled,
						   settings.getSetting("Allow Forwards").enabled,
						   settings.getSetting("Allow Turning").enabled,
						   settings.getSetting("Forced March").enabled, forcedTurn,
						   settings.getSetting("Show Cameras").enabled);
			else
				arena.coast(settings.getSetting("Allow Thinking").enabled,
						   settings.getSetting("Allow Forwards").enabled,
						   settings.getSetting("Allow Turning").enabled);
			updateCount++;
		}
		
		super.step(settings);
		arena.draw();
		
		if (settings.getSetting("Pause on Messages").enabled)
			MessageBoard.getMessageBoard().popUpDisplay();
		else
			MessageBoard.getMessageBoard().consoleDisplay();
		
		int stepCount = arena.getStepCount();
		setTitle("PuckSwarm: " + stepCount);
		
		// Capture periodic images of the simulation.
//		if (stepCount < 5 || stepCount % 250 == 0 || (stepCount-5) % 250 == 0 ||
//		(stepCount-15) % 250 == 0)
//		captureScreenshot();
		
//		if (stepCount % 10000 == 0)
//			captureScreenshot();
	}

	private void recreateArena() {
		// Destroy contents of world.
		World w = getWorld();
		for (Body b = w.getBodyList(); b != null; b = b.getNext()) {
			w.destroyBody(b);
		}
		arena.dispose();

		// Create anew.
		updateCount = 0;
		Robot.nameIndex = 0;
		SimSuite.jframeY = 25;
		arena = new Arena(w, getDebugDraw(), false);
	}

	private void captureScreenshot() {
		String filename = ExperimentManager.getOutputDir() + File.separatorChar + ExperimentManager.getCurrent().getStringCode() + "_step" + String.format("%05d", arena.getStepCount()) + ".png";
		
		try {
			//Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
			Rectangle screenRect = new Rectangle(33, 210, 570, 360);
			BufferedImage capture = captureRobot.createScreenCapture(screenRect);
			ImageIO.write(capture, "png", new File(filename));
		} catch (Exception e) {
			System.err.println("PuckSwarmTest: Problem capturing screenshot!");
		}
	}
	
	@Override
	public String getTestName() {
		return "PuckSwarm";
	}

	@Override
	public void reset() {
		resetHit = true;
	}
}