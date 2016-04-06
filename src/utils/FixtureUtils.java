package utils;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.FixtureDef;

public class FixtureUtils {
	/**
	 * Create a partial arc of a circle composed of fixtures to be attached to
	 * the given body.
	 * 
	 * @param cx		x-coordinate of circle centre
	 * @param cy		y-coordinate of circle centre
	 * @param radius	radius of circle
	 * @param angle0	starting angle for arc (should be < angle1)
	 * @param angle1	ending angle for arg (should be > angle0)
	 * @param n			number of segments to create
	 * @param body		body to attach fixtures to
	 * @param def		holds characteristics of fixtures to create
	 */
	public static void createArc(float cx, float cy, float radius, float angle0, float angle1, int n, Body body, FixtureDef def) {
		assert angle0 < angle1;
		
		float totalAngle = angle1 - angle0;
		
		double angleDelta = totalAngle / n;
		double angle = angle0;
		float x0 = cx + radius * (float) Math.cos(angle);
		float y0 = cy + radius * (float) Math.sin(angle);
		for (int i = 0; i < n; i++) {
			angle += angleDelta;
			float x1 = cx + radius * (float) Math.cos(angle);
			float y1 = cy + radius * (float) Math.sin(angle);

			PolygonShape shape = new PolygonShape();
			shape.setAsEdge(new Vec2(x0, y0), new Vec2(x1, y1));

			def.shape = shape;
			body.createFixture(def);
			x0 = x1;
			y0 = y1;
		}
	}
	
	/**
	 * Create a rounded rectangle and add its fixtures to the given body.  The parameters
	 * leftX, rightX, topY, bottomY define the rectangle without consideration of the
	 * rounded corners.  The radius of the rounded corners is given by radius.
	 */
	public static void createRoundedRectangle(float leftX, float rightX, float topY, float bottomY, float radius, Body body) {
		
		int nSegments = 20;
		FixtureUtils.createArc(leftX + radius, topY - radius, radius, AngleUtils.PI_OVER_TWOf, AngleUtils.PIf, nSegments, body, new FixtureDef());
		FixtureUtils.createArc(rightX - radius, topY - radius, radius, 0, AngleUtils.PI_OVER_TWOf, nSegments, body, new FixtureDef());
		FixtureUtils.createArc(leftX + radius, bottomY + radius, radius, AngleUtils.PIf, 3*AngleUtils.PIf/2, nSegments, body, new FixtureDef());
		FixtureUtils.createArc(rightX - radius, bottomY + radius, radius, 3*AngleUtils.PIf/2, 2*AngleUtils.PIf, nSegments, body, new FixtureDef());

		PolygonShape topEdge = new PolygonShape();
		topEdge.setAsEdge(new Vec2(leftX + radius, topY), new Vec2(rightX - radius, topY));
		body.createFixture(topEdge, 0.0f);
		
		PolygonShape bottomEdge = new PolygonShape();
		bottomEdge.setAsEdge(new Vec2(leftX + radius, bottomY), new Vec2(rightX - radius, bottomY));
		body.createFixture(bottomEdge, 0.0f);

		PolygonShape leftEdge = new PolygonShape();
		leftEdge.setAsEdge(new Vec2(leftX, bottomY + radius), new Vec2(leftX, topY - radius));
		body.createFixture(leftEdge, 0.0f);

		PolygonShape rightEdge = new PolygonShape();
		rightEdge.setAsEdge(new Vec2(rightX, bottomY + radius), new Vec2(rightX, topY - radius));
		body.createFixture(rightEdge, 0.0f);
	}
}
