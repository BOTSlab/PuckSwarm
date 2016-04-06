package controllers;

import localmap.LocalMap;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;

import sensors.SensedType;
import sensors.Suite;

/**
 * Test of kicking mechanism.
 */
public class TestKickController implements KickingController {

	boolean applyKick;
	
	public TestKickController() {
	}
		
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		LocalMap localMap = suite.getLocalMap();
		
		// Kick out red pucks.
		applyKick = localMap.isCarrying() && localMap.getCarriedType() == SensedType.RED_PUCK;
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
	}
	
	public String getInfoString() {
		return "TestKick";
	}

	@Override
	public boolean getKick() {
		return applyKick;
	}
}
