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

public class TurnSignBehaviour implements Behaviour, PropertiesListener {
	
	// In TURN_SIGN state we turn gently away from the wall or from other robots.
	// In BIG_TURN, the robot makes a random duration turn away from the direction
	// of the wall.
	enum State {TURN_SIGN, BIG_TURN};
	State state = State.TURN_SIGN;
	
	int turnSign;

	// The value of stepCount upon activation.
	int stepCount, startCount;

	// The duration to turn while in BIG_TURN state.
	int turnTime;
	
	Random random;
	
	// Parameters whose values are loaded from the current Experiment...

	static float WALL_ESCAPE_PROB;
	

	public TurnSignBehaviour() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
	}
	@Override
	public void propertiesUpdated() {
		WALL_ESCAPE_PROB = ExperimentManager.getCurrent().getProperty("TurnSignBehaviour.WALL_ESCAPE_PROB", 0.1f, this);
	}
	
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		STCameraImage image = suite.getCameraImage();		
		this.stepCount = stepCount;
		
		switch (state) {
			case TURN_SIGN:
				if (checkAndSetTurnSign(image)) {
					state = State.BIG_TURN;
					startCount = stepCount;
					initiateTurning();
				}
				break;
			case BIG_TURN:
				if (stepCount - startCount >= turnTime) {
					state = State.TURN_SIGN;
				}
				break;
		}
	}

	/**
	 * Return false if we should stay in TURN_SIGN and true to enter BIG_TURN.
	 * Also set 'turnSign'.
	 */
	private boolean checkAndSetTurnSign(STCameraImage image) {
		int wallTurnSign = BehaviourUtils.getTurnSign(image, true, true);
		int otherRobotTurnSign = BehaviourUtils.getTurnSign(image, false, false);

		// Independent of the variables computed above, there is a small
		// probability of entering BIG_TURN on every iteration when we
		// are close to a wall and not turning to avoid another robot.
		if (wallTurnSign != 0 && otherRobotTurnSign == 0
				&& random.nextFloat() < WALL_ESCAPE_PROB) {
			turnSign = wallTurnSign;
			return true;
		}

		if (wallTurnSign == 0 && otherRobotTurnSign == 0)
			// Smooth sailing ahead! Do not turn.
			turnSign = 0;
		else if (wallTurnSign != 0 && otherRobotTurnSign == 0)
			// There are no other robots nearby but the wall is close.
			turnSign = wallTurnSign;
		else if (wallTurnSign == 0 && otherRobotTurnSign != 0)
			// The wall is not nearby, but other robots are close.
			turnSign = otherRobotTurnSign;
		else {
			// We are close to both the wall and other robots. We now consider
			// two subcases:
			if (wallTurnSign == otherRobotTurnSign) {
				// There is agreement on which direction to turn.
				turnSign = wallTurnSign;
			} else {
				// There is no agreement. Enter BIG_TURN
				turnSign = wallTurnSign;
				return true;
			}
		}

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
		if (state == State.TURN_SIGN)
			return Robot.MAX_FORWARDS;
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
		debugDraw.drawString(c.x, c.y, "TS: " + state + ", " + (stepCount - startCount) + " / " + turnTime, Color3f.WHITE);
	}
	
	public String getInfoString() {
		return "TS";
	}
}
