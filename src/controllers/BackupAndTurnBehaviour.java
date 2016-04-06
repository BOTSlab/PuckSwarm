package controllers;

import java.util.Random;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import experiment.ExperimentManager;
import experiment.PropertiesListener;

import arena.Robot;

import sensors.Camera;
import sensors.Odometer;
import sensors.STCameraImage;
import sensors.SensedType;
import sensors.Suite;
import sensors.StoredCalibration;

public class BackupAndTurnBehaviour implements Behaviour, PropertiesListener {	
	
	enum State {INACTIVE, BACKING_UP, TURNING};
	State state = State.INACTIVE;
	
	Random random;
	
	float proportion;
	
	// The value of stepCount upon activation and the current value.
	int startCount, stepCount;

	// The direction and duration to turn while in TURNING state.
	int turnDir, turnTime;
	
	// Parameters whose values are loaded from the current Experiment...

	// These define the image coordinates of the rectangular region sampled
	// for pucks.
	public static int REGION_WIDTH, REGION_HEIGHT, BOTTOM_Y;	
	static int regionArea;

	// The threshold proportion of PUCKS required to trigger backup.
	static float THRESHOLD;

	static boolean USE_GROUND_AREA;
	
	// The number of time steps to execute once triggered.
	static int BACKUP_TIME;
	
	// The minimum turning time.
	static int TURNTIME_MIN;
	
	public BackupAndTurnBehaviour() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
	}
	
	@Override
	public void propertiesUpdated() {
		REGION_WIDTH = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.REGION_WIDTH", 140, this);
		REGION_HEIGHT = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.REGION_HEIGHT", 40, this);
		BOTTOM_Y = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.BOTTOM_Y", 119, this);
		THRESHOLD = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.THRESHOLD", 0.25f, this);
		BACKUP_TIME = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.BACKUP_TIME", 30, this);
		TURNTIME_MIN = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.TURNTIME_MIN", 41, this);
		
		regionArea = REGION_WIDTH * REGION_HEIGHT;
	}
		
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		STCameraImage image = suite.getCameraImage();
		this.stepCount = stepCount;

		switch (state) {
			case INACTIVE:
				if (checkInitiateBackup(image)) {
					state = State.BACKING_UP;
					startCount = stepCount;
				}
				break;
			case BACKING_UP:
				if (stepCount - startCount >= BACKUP_TIME) {
					state = State.TURNING;
					startCount = stepCount;
					initiateTurning(image);
				}
				break;
			case TURNING:
				if (stepCount - startCount >= turnTime) {
					state = State.INACTIVE;
				}
				break;
		}
	}
	
	private boolean checkInitiateBackup(STCameraImage image) {
		proportion = 0;
		int x0 = image.getCalibration().getMeridian() - REGION_WIDTH/2;
		int x1 = image.getCalibration().getMeridian() + REGION_WIDTH/2;
		int y0 = BOTTOM_Y - REGION_HEIGHT + 1;
		int y1 = BOTTOM_Y;
		for (int i=x0; i<=x1; i++)
			for (int j=y0; j<=y1; j++) {
				if (image.pixels[i][j] == SensedType.RED_PUCK) {
					proportion++;
				}
			}
		proportion /= regionArea;
		return proportion > THRESHOLD;
	}
	
	private void initiateTurning(STCameraImage image) {
		// Choose a turn direction away from the wall.
		turnDir = BehaviourUtils.getTurnSign(image, true, false);
		if (turnDir == 0) {
			if (random.nextFloat() > 0.5)
				turnDir = 1;
			else
				turnDir = -1;
		}

		// Choose a random turn duration.
		turnTime = TURNTIME_MIN + random.nextInt(Robot.ONE_EIGHTY_TIME - TURNTIME_MIN + 1);
	}

	@Override
	public boolean readyToStart() {
		return state == State.BACKING_UP;
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
		if (state == State.BACKING_UP)
			return -Robot.MAX_FORWARDS;
		else
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
//		debugDraw.drawString(c.x, c.y, "BU: " + proportion + ", " + state, Color3f.WHITE);
debugDraw.drawString(c.x, c.y, "Back-up-and-turn", Color3f.WHITE);
	}
	
	public String getInfoString() {
		return "BU";
	}
}
