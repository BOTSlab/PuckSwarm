package arena;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;

import sensors.SensedType;

public class Puck implements Entity {
	Body body;
	SensedType puckType;
	DebugDraw debugDraw;
	Fixture mainFixture;
	
	public static float WIDTH = 6.3f;
	public static float HALF_WIDTH = WIDTH/2;

	// The minimum distance from the boundary to place a Robot.
	public static float MIN_BOUNDARY_DISTANCE = WIDTH/2;
	
	public Puck(float x, float y, float angle, SensedType puckType, World world, DebugDraw debugDraw) {
		this.debugDraw = debugDraw;
		this.puckType = puckType;
		
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DYNAMIC;
		bodyDef.position.set(x, y);
		bodyDef.angle = angle;
		bodyDef.allowSleep = false;
		bodyDef.linearDamping = 10f;
		// For experiments with projectile bucket brigading
		//bodyDef.linearDamping = 2.0f;
		bodyDef.angularDamping = 10f;
		bodyDef.bullet = false;
		body = world.createBody(bodyDef);

		CircleShape circleShape = new CircleShape();
		circleShape.m_radius = WIDTH/2;
		mainFixture = body.createFixture(circleShape, 5.0f);
		mainFixture.setUserData(this);
		
		setFilterBits(mainFixture);
		
		body.setUserData(this);
	}
	
	protected void setFilterBits(Fixture f) {
		// The puck should not collide with the opening of the gripper.
		f.m_filter.categoryBits = 0x04;
		f.m_filter.maskBits = 0x05;
	}
	
	public void draw() {
		debugDraw.drawSolidCircle(body.getPosition(), WIDTH/2, new Vec2(0,0), puckType.color3f);
	}
	
	public SensedType getPuckType() {
		return puckType;
	}
}
