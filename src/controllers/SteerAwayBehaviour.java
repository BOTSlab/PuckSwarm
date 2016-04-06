package controllers;

import java.util.Random;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import sensors.STCameraImage;
import sensors.Suite;
import arena.Robot;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * SIMPLIFIED AVOIDANCE BEHAVIOUR FOR FINAL VERSION OF CRV 2012 PAPER.
 */
public class SteerAwayBehaviour implements Behaviour, PropertiesListener {
	
	// In STEER_AWAY state we turn gently away from the wall or from other robots.
	// In BIG_TURN, the robot makes a random duration turn away from the direction
	// of the wall.
	enum State {STEER_AWAY, BIG_TURN};
	State state = State.STEER_AWAY;
	
	int turnSign;

	// The value of stepCount upon activation.
	int stepCount, startCount;

	// The duration to turn while in BIG_TURN state.
	int turnTime;
	
	Random random;
	
	public static float BIG_TURN_PROB;
	
	public static boolean AVOID_PUCKS_AS_ROBOTS, AVOID_PUCKS_AS_WALLS;
	
	public SteerAwayBehaviour() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
	}
	
	@Override
	public void propertiesUpdated() {
		BIG_TURN_PROB = ExperimentManager.getCurrent().getProperty("SteerAwayBehaviour.BIG_TURN_PROB", 0.01f, this);
		AVOID_PUCKS_AS_ROBOTS = ExperimentManager.getCurrent().getProperty("SteerAwayBehaviour.AVOID_PUCKS_AS_ROBOTS", false, this);
		AVOID_PUCKS_AS_WALLS = ExperimentManager.getCurrent().getProperty("SteerAwayBehaviour.AVOID_PUCKS_AS_WALLS", false, this);
	}
	
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		STCameraImage image = suite.getCameraImage();
		this.stepCount = stepCount;
		
		switch (state) {
			case STEER_AWAY:
				if (checkAndSetTurnSign(image)) {
					state = State.BIG_TURN;
					startCount = stepCount;
					initiateTurning();
				}
				break;
			case BIG_TURN:
				if (stepCount - startCount >= turnTime)
					state = State.STEER_AWAY;
				break;
		}
	}

	/**
	 * Return false if we should stay in STEER_AWAY and true to enter BIG_TURN.
	 * Also set 'turnSign'.
	 */
	private boolean checkAndSetTurnSign(STCameraImage image) {
		int wallResult = BehaviourUtils.getTurnSign(image, true, false, AVOID_PUCKS_AS_WALLS, false);
		int otherRobotResult = BehaviourUtils.getTurnSign(image, false, false, false, AVOID_PUCKS_AS_ROBOTS);

		if (wallResult != 0) {
			turnSign = wallResult;
			return random.nextFloat() < BIG_TURN_PROB;
		}
		if (otherRobotResult != 0) {
			turnSign = otherRobotResult;
			return random.nextFloat() < BIG_TURN_PROB;
		}
	
		turnSign = 0;
		return false;
	}

	private void initiateTurning() {
		// turnSign already set.

		// Choose a random turn duration.
		turnTime = random.nextInt(Robot.ONE_EIGHTY_TIME + 1);
	}

	@Override
	public boolean readyToStart() {
		return state == State.BIG_TURN || turnSign != 0;
	}

	@Override
	public boolean readyToContinue() {
		return state == State.BIG_TURN || turnSign != 0;
	}

	@Override
	public boolean willingToGiveUp() {
		return state != State.BIG_TURN;
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
		if (state == State.STEER_AWAY)
			return 0.5f * Robot.MAX_FORWARDS;
		else
			return 0;
	}

	@Override
	public float getTorque() {
		return turnSign * Robot.MAX_TORQUE;
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
		Vec2 c = debugDraw.getWorldToScreen(robotTransform.position);
		debugDraw.drawString(c.x, c.y, "Steer-away", Color3f.WHITE);
	}
	
	public String getInfoString() {
		return "SteerAway";
	}	
}
