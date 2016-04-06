package controllers;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import arena.Robot;

import sensors.APS;
import sensors.Localizer;
import sensors.Pose;
import sensors.STImage;
import sensors.Odometer;
import sensors.Suite;
import sensors.Odometer.Mode;

public class TestAPSController implements Controller {
	
	String statusString;

	@Override
	public void computeDesired(Suite suite, int stepCount) {
		APS aps = suite.getAPS();
		Pose pose = aps.getPose(suite.getRobotName());
		if (pose == null)
			statusString = "Not localized!";
		else
				statusString = "x: " + pose.getX() + ", y: " + pose.getY() 
					+ ", theta: " + pose.getTheta();
		System.out.println("TestAPS: " + statusString);
	}

	@Override
	public float getForwards() {
		return 0;
	}

	@Override
	public float getTorque() {
		return 0;
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
		Vec2 c = debugDraw.getWorldToScreen(robotTransform.position);
		debugDraw.drawString(c.x, c.y, statusString, Color3f.WHITE);
	}

	@Override
	public String getInfoString() {
		return "TestAPSController";
	}
}
