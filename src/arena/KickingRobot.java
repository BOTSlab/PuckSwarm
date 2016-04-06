package arena;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.joints.LimitState;
import org.jbox2d.dynamics.joints.PrismaticJoint;
import org.jbox2d.dynamics.joints.PrismaticJointDef;

import controllers.KickingController;

public class KickingRobot extends Robot {

	// Body of the kicker.
	Body kickerBody;
	
	PrismaticJoint prisJoint;
	
	enum KickState {KICKING, RETURNING};
		
	KickState state = KickState.RETURNING;
	
	public KickingRobot(float x, float y, float angle, Arena arena,
			boolean enableDisplay, boolean presetCaches) {
		super(x, y, angle, arena, enableDisplay, presetCaches);
		
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DYNAMIC;
		bodyDef.position.set(x, y);
		bodyDef.angle = angle;
		bodyDef.allowSleep = false;

		kickerBody = world.createBody(bodyDef);
		createKickerFixtures();
		PrismaticJointDef prisJointDef = new PrismaticJointDef();
		prisJointDef.bodyA = body;
		prisJointDef.bodyB = kickerBody;
		prisJointDef.enableLimit = true;
		prisJointDef.lowerTranslation = 0f;
		prisJointDef.upperTranslation = 10f;
		prisJointDef.collideConnected = false;
		prisJointDef.enableMotor = true;
		prisJointDef.maxMotorForce = 100000;
		prisJoint = (PrismaticJoint) world.createJoint(prisJointDef);
	}
	
	private void createKickerFixtures() {
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.density = 5.0f;
		fixtureDef.filter.categoryBits = 0x01;
		fixtureDef.filter.maskBits = 0x07;
		
		// X-coordinates below are shifted by this amount to shift the origin
		// of the kicker towards the front of the robot.
		float x = 0.5f;

		Vec2[] bodyVertices = {
				new Vec2(x - 1f, -1f),				
				new Vec2(x + 1f, -1f),
				new Vec2(x + 1f, 1f),
				new Vec2(x - 1f, 1f)
		};
		PolygonShape bodyShape = new PolygonShape();
		bodyShape.set(bodyVertices, bodyVertices.length);
		fixtureDef.shape = bodyShape;
		fixtureDef.userData = this;
		kickerBody.createFixture(fixtureDef);
	}
	
	public void move(boolean allowForwards, boolean allowTurning) {
		super.move(allowForwards, allowTurning);
		
		// Handle kick state transitions
		if (state == KickState.RETURNING) {
			if (controller instanceof KickingController &&
					((KickingController) controller).getKick())
				// Kick!
				state = KickState.KICKING;
		} else if (state == KickState.KICKING) {
			if (prisJoint.m_limitState == LimitState.AT_UPPER)
				state = KickState.RETURNING;
		}
		
		// Handle control over the prismatic joint.
		if (state == KickState.KICKING) {
			prisJoint.setMotorSpeed(10000);
		} else if (state == KickState.RETURNING) {
			float speed = -10 * prisJoint.getJointTranslation();
			prisJoint.setMotorSpeed(speed);			
		}
	}	
}
