package controllers;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import sensors.Suite;
import arena.Robot;

public class TestTurnBehaviour implements Behaviour {
	
	int stepCount;
	
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		this.stepCount = stepCount; 
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
	}

	@Override
	public float getForwards() {
		return 0;
	}

	@Override
	public float getTorque() {
		if (stepCount < Robot.ONE_EIGHTY_TIME)
			return Robot.MAX_TORQUE;
		else
			return 0;
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
		Vec2 c = debugDraw.getWorldToScreen(robotTransform.position);
		debugDraw.drawString(c.x, c.y, "Straight", Color3f.WHITE);
	}
	
	public String getInfoString() {
		return "St";
	}
}
