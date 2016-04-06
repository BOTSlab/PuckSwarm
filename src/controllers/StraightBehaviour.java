package controllers;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import arena.Robot;

import sensors.STImage;
import sensors.Odometer;
import sensors.Suite;

public class StraightBehaviour implements Behaviour {
	
	@Override
	public void computeDesired(Suite suite, int stepCount) {
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
		return Robot.MAX_FORWARDS;
	}

	@Override
	public float getTorque() {
		return 0;
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
		Vec2 c = debugDraw.getWorldToScreen(robotTransform.position);
		debugDraw.drawString(c.x, c.y, "Straight", Color3f.WHITE);
	}
	
	public String getInfoString() {
		return "Straight";
	}
}
