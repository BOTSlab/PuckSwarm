package controllers;

import java.util.Random;

import localmap.VFHPlus;
import localmap.VFHPlus.CheckTargetAngleResponse;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import sensors.Suite;
import arena.Robot;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * This behaviour utilizes VFH+ to avoid obstacles (walls and robots).  It
 * only becomes active if there is an obstacle to avoid (as opposed to
 * VFHWanderBehaviour which is always active).
 */
public class VFHSteerAwayBehaviour implements Behaviour, PropertiesListener {
	
	VFHPlus vfh = null;
	
	Random random;
	
	float actualTurn;
	
	// In STEER_AWAY state we turn gently away from the wall or from other robots.
	// In BIG_TURN, the robot makes a random duration turn away from the direction
	// of the wall.
	enum State {INACTIVE, STEER_AWAY, BIG_TURN};
	State state = State.STEER_AWAY;
	
	// If stuck, then we will move randomly backwards.
	boolean stuck;
	
	// The value of stepCount upon activation.
	int stepCount, startCount;

	// The duration to turn while in BIG_TURN state.
	int bigTurnTime;
	
	public static float BIG_TURN_PROB;

	public VFHSteerAwayBehaviour() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
	}
	
	@Override
	public void propertiesUpdated() {
		BIG_TURN_PROB = ExperimentManager.getCurrent().getProperty("VFHSteerAwayBehaviour.BIG_TURN_PROB", 0.01f, this);
	}
	
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		if (vfh == null)
			vfh = new VFHPlus(suite.getLocalMap(), true);
		
		CheckTargetAngleResponse result = vfh.checkTargetAngle(suite.getLocalMap(), 0, true);
		
		switch (state) {
			case BIG_TURN:
				if (stepCount - startCount >= bigTurnTime)
					state = State.STEER_AWAY;
				else
					break;
			case INACTIVE:
			case STEER_AWAY:
				if (result == null) {
					// There is no valid turn angle, according to VFH+.
					stuck = true;
					state = State.STEER_AWAY;
					actualTurn = 0.5f * (float) random.nextGaussian();
					
				} else if (random.nextFloat() < BIG_TURN_PROB) {
					stuck = false;
					state = State.BIG_TURN;
					startCount = stepCount;
					bigTurnTime = random.nextInt(Robot.ONE_EIGHTY_TIME + 1);
					actualTurn = (float) (suite.getLocalMap().getFreeerSide() * Math.PI/2);
					
				} else {
					stuck = false;
					if (result.targetAngleFree)
						state = State.INACTIVE;
					else
						state = State.STEER_AWAY;
					actualTurn = result.outputAngle;
					
				}
		}
		
		vfh.updateGUI();
	}

	@Override
	public boolean readyToStart() {
		return state != State.INACTIVE;
	}

	@Override
	public boolean readyToContinue() {
		return state != State.INACTIVE;
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
		vfh.reset();
	}
	
	@Override
	public float getForwards() {
		if (state == State.BIG_TURN)
			return 0;
		
		int direction = 1;
		if (stuck)
			direction = -1;
		double maxTurnAngle = Math.PI/2;
		double speedScale = (maxTurnAngle - Math.abs(actualTurn)) / maxTurnAngle; 
		return (float) (speedScale * direction * Robot.MAX_FORWARDS);
	}

	@Override
	public float getTorque() {
		return BehaviourUtils.getScaledTorque(actualTurn);
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
		//Vec2 c = debugDraw.getWorldToScreen(robotTransform.position);
		//debugDraw.drawString(c.x, c.y, "Wander", Color3f.WHITE);
		
		vfh.draw(robotTransform, debugDraw);
		
		// Draw the turn direction.
		float length = 20f;
		float x1 = (float) (length * Math.cos(actualTurn)); 
		float y1 = (float) (length * Math.sin(actualTurn)); 
		Vec2 posWrtBody = new Vec2(x1, y1);
		Vec2 globalPos = Transform.mul(robotTransform, posWrtBody);
		debugDraw.drawSegment(robotTransform.position, globalPos, Color3f.WHITE);
	}
	
	public String getInfoString() {
		return "VFHSteerAway: " + state;
	}	
}
