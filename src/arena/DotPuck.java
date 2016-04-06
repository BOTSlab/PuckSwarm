/**
 * A DotPuck is a Puck, except that it consists of two fixtures: a larger circle
 * that is not sensed (i.e. it has no userData set) and a smaller inner circle
 * that shows a particular colour.
 */

package arena;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;

import sensors.SensedType;

public class DotPuck extends Puck {

	public static float INNER_WIDTH = 2/3f * Puck.WIDTH;
	
	public DotPuck(float x, float y, float angle, SensedType puckType, World world, DebugDraw debugDraw) {
		super(x, y, angle, puckType, world, debugDraw);
		
		CircleShape circleShape = new CircleShape();
		circleShape.m_radius = INNER_WIDTH/2;
		Fixture innerFixture = body.createFixture(circleShape, 5.0f);
		
		// The outer circle will not be sensed.
		mainFixture.setUserData(new HiddenFixture());
		innerFixture.setUserData(this);
		
		setFilterBits(innerFixture);
	}

	public void draw() {
		debugDraw.drawSolidCircle(body.getPosition(), WIDTH/2, new Vec2(0,0), Color3f.WHITE);
		debugDraw.drawSolidCircle(body.getPosition(), INNER_WIDTH/2, new Vec2(0,0), puckType.color3f);
	}
}
