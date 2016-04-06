package arena;

import java.util.Random;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

import experiment.ExperimentManager;

import utils.AngleUtils;
import utils.FixtureUtils;

public class OvalEnclosure implements Enclosure {
	// Rectangular dimensions of the arena in cm.
	private float WIDTH;
	private float HEIGHT;

	// Parameters of the oval arena. R is the radius of the two semicircles
	// D is the distance between the two semicircle centres.
	private float R;
	private float D;

	private Vec2 leftCircle;
	private Vec2 rightCircle;
	
	Random rng = ExperimentManager.getCurrent().getRandom();
	
	private Body body;

	public OvalEnclosure(World world) {
		BodyDef bd = new BodyDef();
		body = world.createBody(bd);
		
		int scale = ExperimentManager.getCurrent().getProperty(
				"OvalEnclosure.scale", 1, null);

		WIDTH = scale*177.0f;
		HEIGHT = scale*111.0f;
		R = HEIGHT / 2;
		D = WIDTH - 2 * R;
		leftCircle = new Vec2(-D/2, 0);
		rightCircle = new Vec2(D/2, 0);

		FixtureUtils.createArc(D/2, 0, R, -AngleUtils.PI_OVER_TWOf, AngleUtils.PI_OVER_TWOf, 40, body, new FixtureDef());
		FixtureUtils.createArc(-D/2, 0, R, AngleUtils.PI_OVER_TWOf, 1.5f * AngleUtils.PIf, 40, body, new FixtureDef());

		PolygonShape topEdge = new PolygonShape();
		topEdge.setAsEdge(new Vec2(-D/2, R), new Vec2(D/2, R));
		body.createFixture(topEdge, 0.0f);
		
		PolygonShape bottomEdge = new PolygonShape();
		bottomEdge.setAsEdge(new Vec2(-D/2, -R), new Vec2(D/2, -R));
		body.createFixture(bottomEdge, 0.0f);
	}
	
	@Override
	public Vec2 getRandomInside(float minDistance) {
		// Try a large number of times to find a satisfactory point.
		for (int i=0; i<100000; i++) {
			// Generate a random position that lies within the WIDTH x HEIGHT
			// rectangular area.
			Vec2 v = new Vec2((float) rng.nextFloat() * WIDTH - WIDTH / 2.0f,
							  (float) rng.nextFloat() * HEIGHT - HEIGHT / 2.0f);
			
			if (inFreeSpace(v, minDistance))
				return v;
		}
		
		// We will reach here only if no satisfactory point could be found
		// which should occur only if the arena is blocked full.
		return null;
	}
	
	@Override
	public boolean inFreeSpace(Vec2 v, float minDistance) {		
		if (MathUtils.distance(v, leftCircle) < R - minDistance)
			// We are close enough to the left circle
			return true;
		else if (MathUtils.distance(v, rightCircle) < R - minDistance)
			// We are close enough to the left circle
			return true;
		else if (v.x < D/2 && v.x > -D/2 && 
				 v.y < R - minDistance && v.y > -R + minDistance)
			// We are within the center rectangular area and not too close
			// to the top or bottom
			return true;
		return false;
	}

	@Override
	public float getWidth() {
		return WIDTH;
	}

	@Override
	public float getHeight() {
		return HEIGHT;
	}

	@Override
	public Body getBody() {
		return body;
	}
}
