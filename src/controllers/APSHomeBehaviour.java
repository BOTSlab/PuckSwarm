package controllers;

import java.util.Random;

import localmap.MovementCommand;
import localmap.MovementUtils;
import localmap.VFHPlus;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import sensors.APS;
import sensors.Pose;
import sensors.Suite;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * APS-based homing demo controller.
 */
public class APSHomeBehaviour implements Behaviour, PropertiesListener {
	
	boolean active;
	
	VFHPlus vfh = null;
	
	Random random;
	float randomHeading;
	
	MovementCommand movement;

	APS aps;
	
	public static float DISTANCE_THRESHOLD;
	
	public APSHomeBehaviour() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
	}
	
	@Override
	public void propertiesUpdated() {
		DISTANCE_THRESHOLD = ExperimentManager.getCurrent().getProperty("OdoHomeBehaviour.DISTANCE_THRESHOLD", 5f, this);
	}
	
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		if (vfh == null)
			vfh = new VFHPlus(suite.getLocalMap(), true);
		aps = suite.getAPS();
		
		Pose robotPose = aps.getPose(suite.getRobotName());
		Pose dummyPose = aps.getPose("DUMMY");
		if (robotPose == null || dummyPose == null)
			return;
		
		// Are we far enough away from the goal that we should be active?
		active = Pose.getDistanceBetween(robotPose, dummyPose) > DISTANCE_THRESHOLD;
		if (!active)
			return;
		
		// The angle towards the origin is:
		// 		atan2(y, x) - theta + pi
		float errorAngle = (float) Pose.getAngleFromTo(robotPose, dummyPose);		
		System.out.println("errorAngle: " + errorAngle);
		
		// DE_HOMING:
		/*
		Float result = vfh.computeTurnAngle(suite.getLocalMap(), (float) errorAngle, false);
		float turnAngle;
		if (result != null)
			turnAngle = result.floatValue();
		else
			turnAngle = errorAngle;
		
		movement = new MovementCommand(1, turnAngle);
		*/
		movement = MovementUtils.applyVFH(vfh, suite.getLocalMap(), errorAngle);
		
		// NON-RANDOM WANDERING:
		//turnAngle = vfh.computeTurnAngle(suite.getLocalMap(), 0);
		
		// RANDOMIZED WANDERING:
		/*
		randomHeading += 0.05f * (float) random.nextGaussian();
		if (randomHeading > Math.PI/4)
			randomHeading -= 0.1f * random.nextFloat();		
		if (randomHeading < -Math.PI/4)
			randomHeading += 0.1f * random.nextFloat();
		turnAngle = vfh.computeTurnAngle(suite.getLocalMap(), randomHeading);
		*/
	}

	@Override
	public boolean readyToStart() {
		return active;
	}

	@Override
	public boolean readyToContinue() {
		return active;
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
		return movement.getForwards();
	}

	@Override
	public float getTorque() {
		return movement.getTorque();
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
		Vec2 c = debugDraw.getWorldToScreen(robotTransform.position);
		debugDraw.drawString(c.x, c.y, "APSHome", Color3f.WHITE);
		
		vfh.draw(robotTransform, debugDraw);
	}
	
	public String getInfoString() {
		return "AH";
	}
}
