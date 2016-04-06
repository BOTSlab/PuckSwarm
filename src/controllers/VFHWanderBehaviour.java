package controllers;

import java.util.Random;

import localmap.VFHPlus;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import sensors.Suite;
import arena.Robot;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * This behaviour utilizes VFH+ to avoid obstacles (walls, robots, and pucks)
 * as it wanders about randomly.  This behaviour is always ready to activate
 * and should therefore have other behaviours ahead of it in priority (otherwise,
 * it would always be active).
 */
public class VFHWanderBehaviour implements Behaviour, PropertiesListener {
	
	VFHPlus vfh = null;
	
	Random random;
	
	float desiredRandomTurn;

	float actualTurn;
	
	// If stuck, then we will move randomly backwards.
	boolean stuck;
	
	// The standard deviation of the random process that influences wandering.
	public static float ST_DEV;
	
	public VFHWanderBehaviour() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
	}
	
	@Override
	public void propertiesUpdated() {
		ST_DEV = ExperimentManager.getCurrent().getProperty("VFHWanderBehaviour.ST_DEV", 0.5f, this);
	}
	
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		if (vfh == null)
			vfh = new VFHPlus(suite.getLocalMap(), true);
		
		desiredRandomTurn = ST_DEV * (float) random.nextGaussian();
		/*
		if (desiredRandomTurn > Math.PI/4)
			desiredRandomTurn = 0.1f * random.nextFloat();		
		if (desiredRandomTurn < -Math.PI/4)
			desiredRandomTurn += 0.1f * random.nextFloat();
		*/

		Float result = vfh.computeTurnAngle(suite.getLocalMap(), desiredRandomTurn, false);
		if (result == null) {
			// There is no valid turn angle, according to VFH+.
			stuck = true;
			actualTurn = desiredRandomTurn;
		} else {
			stuck = false;
			actualTurn = result.floatValue();
		}
		
		vfh.updateGUI();		
	}

	@Override
	public boolean readyToStart() {
		return true;
	}

	@Override
	public boolean readyToContinue() {
		return true;
	}

	@Override
	public boolean willingToGiveUp() {
		return true;
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
		int direction = 1;
		if (stuck)
			direction = -1;
		double maxTurnAngle = Math.PI/2;
		double speedScale = (maxTurnAngle - Math.abs(actualTurn)) / maxTurnAngle; 
		return (float) (speedScale * direction * Robot.MAX_FORWARDS);
	}

	@Override
	public float getTorque() {
		return actualTurn * Robot.MAX_TORQUE;
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
		return "VFHWander";
	}
}
