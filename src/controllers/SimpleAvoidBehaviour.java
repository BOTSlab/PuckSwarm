package controllers;

import java.util.Random;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import sensors.STCameraImage;
import sensors.SensedType;
import sensors.Suite;
import arena.Robot;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * A simple avoidance behaviour to initiate a random
 * duration turn away from the wall or from obstacles.
 **/
public class SimpleAvoidBehaviour implements Behaviour, PropertiesListener {
	
	enum State {INACTIVE, TURNING};
	State state = State.INACTIVE;
	
	Random random;
	
	// The value of stepCount upon activation.
	int stepCount, startCount;

	// The direction and duration to turn while in TURNING state.
	int turnDir, turnTime;

	// Parameters whose values are loaded from the current Experiment...
	
	// ROBOT or WALL pixels closer than distance (that is, with ground plane
	// distances smaller than this threshold) will initiate a turn away from them.
	static int DIST_THRESHOLD;
	
	public SimpleAvoidBehaviour() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
	}
	
	@Override
	public void propertiesUpdated() {
		DIST_THRESHOLD = ExperimentManager.getCurrent().getProperty("SimpleAvoidBehaviour.DIST_THRESHOLD", 15, this);
	}
			
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		STCameraImage image = suite.getCameraImage();
		this.stepCount = stepCount;
		
		switch (state) {
			case INACTIVE:
				if (checkAndSetTurnDir(image)) {
					state = State.TURNING;
					startCount = stepCount;
					initiateTurning();
				}
				break;
			case TURNING:
				if (stepCount - startCount >= turnTime) {
					state = State.INACTIVE;
				}
				break;
		}
	}
	
	// Search all image pixels for the WALL or ROBOT pixel with the nearest
	// ground plane distance to the robot.  If there is no WALL or ROBOT pixel
	// or if it is greater than the threshold distance, then this behaviour
	// will be INACTIVE.  Otherwise, turn in the opposite direction of the
	// closest WALL or ROBOT pixel.
	private boolean checkAndSetTurnDir(STCameraImage image) {
		float closestDist = Float.MAX_VALUE;
		turnDir = 0;
		for (int i = 0; i < image.width; i++)
			for (int j = 0; j < image.height; j++) {
				sensors.Calibration.CalibDataPerPixel calib = image.getCalibration().getCalibData(i, j);
				if (calib.active) {
					if ((image.pixels[i][j] == SensedType.WALL) ||
					    (image.pixels[i][j] == SensedType.ROBOT)) {
						float dist = (float)Math.sqrt(calib.Xr*calib.Xr + calib.Yr*calib.Yr);
						if (dist < DIST_THRESHOLD) {
							closestDist = dist;
							if (i < image.getCalibration().getMeridian())
								turnDir = -1;
							else
								turnDir = 1;
						}
					}
				}
			}
		
		return turnDir != 0;
	}
	
	private void initiateTurning() {
		// turnDir already set.

		// Choose a random turn duration.
		turnTime = random.nextInt(Robot.ONE_EIGHTY_TIME + 1);
	}

	@Override
	public boolean readyToStart() {
		return state == State.TURNING;
	}

	@Override
	public boolean readyToContinue() {
		return !willingToGiveUp();
	}

	@Override
	public boolean willingToGiveUp() {
		return state == State.INACTIVE;
	}

	@Override
	public Behaviour deactivate() {
		return null;
	}
	
	@Override
	public void activate() {
		// Activation work already done in computeDesired.
	}
	
	@Override
	public float getForwards() {
		return 0;
	}

	@Override
	public float getTorque() {
		if (state == State.TURNING)
			return turnDir * Robot.MAX_TORQUE;
		else
			return 0;
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
		Vec2 c = debugDraw.getWorldToScreen(robotTransform.position);
		debugDraw.drawString(c.x, c.y, "SimpleAvoid: " + state + ", " + (stepCount - startCount) + " / " + turnTime, Color3f.WHITE);
	}
	
	public String getInfoString() {
		return "SA";
	}
}
