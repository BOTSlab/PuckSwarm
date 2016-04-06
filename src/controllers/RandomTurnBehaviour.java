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

public class RandomTurnBehaviour implements Behaviour {

	// The maximum time to spend turning in this behaviour (corresponds to the
	// maximum angle to turn). This should correspond to something <= to a 180
	// degree turn.
	public static final int MAX_TIME = 92;

	// The actual duration to remain in this behaviour.
	int randomTurnTime;

	// The direction of the turn.
	int turnSign;

	// The time at which this behaviour was entered and the current time step.
	int startCount, stepCount;

	Random rng = ExperimentManager.getCurrent().getRandom();

	STCameraImage image;

	@Override
	public void computeDesired(Suite suite, int stepCount) {
		// Nothing to do here. turnSign, randomTurnTime, and startCount are set
		// in activate.
		this.stepCount = stepCount;
		this.image = image;
	}

	@Override
	public boolean readyToStart() {
		// This should be a lower priority behaviour, such that it starts only
		// when other behaviours are unable to continue.
		return true;
	}

	@Override
	public boolean readyToContinue() {
		return !willingToGiveUp();
	}

	@Override
	public boolean willingToGiveUp() {
		return (stepCount - startCount >= randomTurnTime);
	}

	@Override
	public Behaviour deactivate() {
		return null;
	}

	public void activate() {
		startCount = stepCount;

		// Choose a turn direction away from the wall.
		turnSign = BehaviourUtils.getTurnSign(image, true, false);
		if (turnSign == 0) {
			if (rng.nextFloat() > 0.5)
				turnSign = 1;
			else
				turnSign = -1;
		}

		// Choose a random turn duration.
		randomTurnTime = (int) (rng.nextFloat() * MAX_TIME);
		//System.out.println("randomTurnTime: " + randomTurnTime);
	}

	@Override
	public float getForwards() {
		return 0;
	}

	@Override
	public float getTorque() {
		return turnSign * Robot.MAX_TORQUE;
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
		Vec2 c = debugDraw.getWorldToScreen(robotTransform.position);
		debugDraw.drawString(c.x, c.y, "RandomTurn: " + turnSign + ", "
				+ (stepCount - startCount), Color3f.WHITE);
	}
	
	public String getInfoString() {
		return "RT";
	}
}
