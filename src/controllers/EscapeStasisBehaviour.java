package controllers;

import java.util.Random;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import sensors.Camera;
import sensors.Odometer;
import sensors.STCameraImage;
import sensors.SensedType;
import sensors.Suite;
import arena.Robot;
import experiment.ExperimentManager;
import experiment.PropertiesListener;
import experiment.PropertyListener;

/**
 * Detects the number of unchanged pixels from the last time step and activates
 * a random escape behaviour based on this number.
 */
public class EscapeStasisBehaviour implements Behaviour, PropertiesListener {
	
	// Copy of image from the last time step.
	STCameraImage lastImage = null;
	
	// The value of stepCount upon activation and the current value.
	int startCount, stepCount;
	
	// Computed in 'computeDesired'.
	boolean ready;
	
	// The current randomly selected forwards and torque amounts.
	float forwards, torque;
	
	Random random;
	
	// Parameters whose values are loaded from the current Experiment...
	
	// Modulates the probability of triggering.
	float K;

	// The number of time steps to execute once triggered.
	int ESCAPE_TIME;
		
	public EscapeStasisBehaviour() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
	}
	
	@Override
	public void propertiesUpdated() {
		K = ExperimentManager.getCurrent().getProperty("EscapeBehaviour.K", 1e-6f, this);
		ESCAPE_TIME = ExperimentManager.getCurrent().getProperty("EscapeBehaviour.ESCAPE_TIME", 60, this);
	}
	
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		STCameraImage image = suite.getCameraImage();
		this.stepCount = stepCount;
		ready = false;
		
		if (lastImage == null) {
			lastImage = (STCameraImage) image.clone();
			return;
		}
		
		// Determine the proportion of image pixels which have changed from
		// the last time step (not including inactive pixels).
		float prop = 0;
		for (int i=0; i<image.width; i++)
			for (int j=0; j<image.height; j++)
				if (image.getCalibration().getCalibData(i, j).active &&
					image.pixels[i][j] != lastImage.pixels[i][j])
					prop++;
		prop /= image.getCalibration().getNActivePixels();
		//System.out.println("prop:" + prop);

		float probability = (K / (K + prop));
		probability *= probability;
		if (random.nextFloat() < probability)
			ready = true;
		
		lastImage = (STCameraImage) image.clone();
	}

	@Override
	public boolean readyToStart() {
		return ready;
	}

	@Override
	public boolean readyToContinue() {
		return !willingToGiveUp();
	}

	@Override
	public boolean willingToGiveUp() {
		return (stepCount - startCount >= ESCAPE_TIME);
	}

	@Override
	public Behaviour deactivate() {
		return null;
	}
	
	@Override
	public void activate() {
		startCount = stepCount;
		forwards = (2 * random.nextFloat() - 1) * Robot.MAX_FORWARDS;
		torque = (2 * random.nextFloat() - 1) * Robot.MAX_TORQUE;
	}
	
	@Override
	public float getForwards() {
		return forwards;
	}

	@Override
	public float getTorque() {
		return torque;
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
		Vec2 c = debugDraw.getWorldToScreen(robotTransform.position);
		debugDraw.drawString(c.x, c.y, "EscapeS: " + (stepCount - startCount), Color3f.WHITE);
	}

	@Override
	public String getInfoString() {
		return "ES";
	}
}
