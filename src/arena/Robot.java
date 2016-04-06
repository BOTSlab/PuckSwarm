package arena;

import java.awt.Color;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

import sensors.SimSuite;
import utils.AngleUtils;
import utils.FixtureUtils;
import controllers.Controller;
import controllers.ControllerUtils;

public class Robot implements Entity {
	Body body;
	World world; // Necessary for creating the body and sensing.
	DebugDraw debugDraw;
	Color3f color3f;
	Color color;

	String name;
	
	// The robot's label will contain its name and a string from the controller.
	String infoString = "";
	
	// This robot's suite of sensors 
	SimSuite simSuite;
	
	// The controller, or "brain".
	Controller controller;		
	
	public static final float SRV_LENGTH = 22f;
	public static final float SRV_WIDTH = 11.3f;
	
	public static final float MAX_FORWARDS = 5*150000; // 5 * 200000

	public static final float MAX_TORQUE = 5*475000; // 5*500000

	// The number of time steps that correspond to an approximate 180 degree turn.
	public static final int ONE_EIGHTY_TIME = 10;
	
	// The minimum distance from the boundary to place a Robot.
	public static float MIN_BOUNDARY_DISTANCE = (SRV_LENGTH)/2;
	
	// Index used to create unique names for all robots.
	public static int nameIndex = 0;
	
	private static Color3f[] color3fs = {
		Color3f.GREEN, Color3f.BLUE, new Color3f(1, 0, 1),
		new Color3f(0, 1, 1), new Color3f(1, 1, 0), Color3f.RED
	};
	private static Color[] colors = {
		Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.RED
	};
	
	public Robot(float x, float y, float angle, Arena arena, boolean enableDisplay,
						 boolean presetCaches) {
		this.debugDraw = arena.getDebugDraw();
		this.world = arena.getWorld();
		
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DYNAMIC;
		bodyDef.position.set(x, y);
		bodyDef.angle = angle;
		bodyDef.allowSleep = false;
		bodyDef.linearDamping = 10f;
		bodyDef.angularDamping = 10f;
		bodyDef.bullet = false;
		body = world.createBody(bodyDef);

		createFixtures();
		
		body.setUserData(this);
		
		// Create robot's name and color3f based on nameIndex.
		name = "R" + nameIndex;
		color3f = color3fs[nameIndex % color3fs.length];
		color = colors[nameIndex % colors.length];
		nameIndex++;
		
		simSuite = new SimSuite(name, body, arena, enableDisplay);

		controller = ControllerUtils.create(presetCaches);
	}
	
	private void createFixtures() {
		// This FixtureDef will be used for each part of the robot below.
		// Prior to creating the actual fixture, the shape will be changed
		// for each part.
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.density = 5.0f;
		fixtureDef.filter.categoryBits = 0x01;
		fixtureDef.filter.maskBits = 0x07;
		
		// X-coordinates below are shifted by this amount to shift the origin
		// to the centre of the camera.
		float shiftX = -3.88f;

		// The main body consisting of a rectangle with a triangular piece at
		// the rear.  Note that this rectangle does not go right up to the 
		// "shoulder" of the gripper section.
		float rightX = shiftX + 6.4f;
		Vec2[] bodyVertices = {
				new Vec2(shiftX - 8.7f, 0f),
				new Vec2(shiftX - 7.7f, -5.7f),				
				new Vec2(rightX, -5.7f),
				new Vec2(rightX, 5.7f),
				new Vec2(shiftX - 7.7f, 5.7f)
		};
		PolygonShape bodyShape = new PolygonShape();
		bodyShape.set(bodyVertices, bodyVertices.length);
		fixtureDef.shape = bodyShape;
		fixtureDef.userData = this;
		body.createFixture(fixtureDef);
		
		// The gripper will not be sensed.
		fixtureDef.userData = new HiddenFixture();

		// "Shoulder" pieces to connect up to the gripper.
		PolygonShape topShoulder = new PolygonShape();
		PolygonShape botShoulder = new PolygonShape();
		float innerShoulderY = 4.5f;
		Vec2[] topShoulderVertices = {
				new Vec2(rightX, innerShoulderY),				
				new Vec2(shiftX + 11.5f, 3.29f),
				new Vec2(rightX, 5.7f)
		};
		Vec2[] botShoulderVertices = {
				new Vec2(rightX, -5.7f),
				new Vec2(shiftX + 11.5f, -3.29f),
				new Vec2(rightX, -innerShoulderY)				
		};
		topShoulder.set(topShoulderVertices, topShoulderVertices.length);
		botShoulder.set(botShoulderVertices, botShoulderVertices.length);
		fixtureDef.shape = topShoulder;
		body.createFixture(fixtureDef);
		fixtureDef.shape = botShoulder;
		body.createFixture(fixtureDef);
				
		// Create the puck enclosure arc (the "gripper" itself).
		float startDegrees = 65f;
		float stopDegrees = 360f - startDegrees;
		FixtureUtils.createArc(shiftX + 10f, 0f, 3.6f, startDegrees * AngleUtils.TO_RADf, stopDegrees * AngleUtils.TO_RADf, 10, body, fixtureDef);
		
		// Now create the "bow" of the gripper which is passable to pucks.
		fixtureDef.filter.categoryBits = 0x02;
		fixtureDef.filter.maskBits = 0x03;
		//FixtureUtils.createArc(10f, 0f, 3.6f, -startDegrees * AngleUtils.TO_RADf, startDegrees * AngleUtils.TO_RADf, 5, body, fixtureDef);
		CircleShape circleShape = new CircleShape();
		circleShape.m_radius = 3.6f;
		circleShape.m_p.x = shiftX + 10.0f;
		fixtureDef.shape = circleShape;
		body.createFixture(fixtureDef);
	}
	
	public void sense() {
		simSuite.sense();
	}
		
	public void think(int stepCount) {
		controller.computeDesired(simSuite, stepCount);
		
		infoString = name + " : " + controller.getInfoString();
	}
	
	public void move(boolean allowForwards, boolean allowTurning) {
		Vec2 f = body.getWorldVector(new Vec2(controller.getForwards(), 0.0f));
		Vec2 p = body.getWorldPoint(body.getLocalCenter());
		if (allowForwards)
			body.applyForce(f, p);
		if (allowTurning)
			body.applyTorque(controller.getTorque());
	}
	
	/**
	 * Forces the robot to move forward and turn at a constant rate (useful
	 * for debugging and setting controller parameters).  The 'direction'
	 * parameter governs the direction of turn (1 for CCW, 0 for straight,
	 * -1 for CW).
	 */
	public void forcedMarch(int direction) {
		Vec2 f = body.getWorldVector(new Vec2(MAX_FORWARDS, 0.0f));
		Vec2 p = body.getWorldPoint(body.getLocalCenter());
		body.applyForce(f, p);	
		body.applyTorque(direction*MAX_TORQUE);
	}	
	
	public void draw() {
		Transform T = body.getTransform();
		controller.draw(T, color3f, debugDraw);
		simSuite.draw();
		
		// Draw the robot's label.
		Vec2 c = debugDraw.getWorldToScreen(T.position.add(new Vec2(0, 5)));
		debugDraw.drawString(c.x, c.y, infoString, color3f);

		//Vec2 d = debugDraw.getWorldToScreen(T.position.add(new Vec2(10, 5)));
		//debugDraw.drawString(d.x, d.y, T.position.toString(), Color3f.WHITE);
		
		// Draw the avoidance zones as defined for the CRV paper (final version).
		/*
		if (name.equalsIgnoreCase("R0")) {
			Vec2 center = Transform.mul(T, new Vec2(SRV_LENGTH/2 + 5, 0));
			debugDraw.drawCircle(center, 6, Color3f.BLUE);
			debugDraw.drawCircle(center, 20, Color3f.GREEN);
		}
		*/
	}

	public void dispose() {
		simSuite.dispose();
	}
}
