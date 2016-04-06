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

public class RoundedRectangleEnclosure implements Enclosure {
	// Rectangular dimensions of the arena in cm.
	protected float WIDTH;
	protected float HEIGHT;

	// R is the radius of the rounded corners.
	protected float R;

	// The four corners.
	protected Vec2 upperLeft, upperRight, lowerLeft, lowerRight;
	
	protected Random rng = ExperimentManager.getCurrent().getRandom();
	
	protected Body body;

	public RoundedRectangleEnclosure(World world) {
		BodyDef bd = new BodyDef();
		body = world.createBody(bd);
		
		float scale = ExperimentManager.getCurrent().getProperty(
				"RoundedRectangleEnclosure.scale", 1f, null);

		WIDTH = scale*187.0f;
		// For experiments with projectile bucket brigading
		//WIDTH = 1.5f*scale*187.0f;
		HEIGHT = scale*187.0f;
		R = scale * 15.5f;
		
		upperLeft = new Vec2(-WIDTH/2 + R, HEIGHT/2 - R); // UL
		upperRight = new Vec2(WIDTH/2 - R, HEIGHT/2 - R); // UR
		lowerLeft = new Vec2(-WIDTH/2 + R, -HEIGHT/2 + R);  // LL
		lowerRight = new Vec2(WIDTH/2 - R, -HEIGHT/2 + R);  // LR
		FixtureUtils.createRoundedRectangle(-WIDTH/2, WIDTH/2, HEIGHT/2, -HEIGHT/2, R, body);
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
		float ax = Math.abs(v.x);
		float ay = Math.abs(v.y);
		if (ax < WIDTH/2 - R - minDistance && ay < HEIGHT/2 - minDistance)
			// The point is within a central rectangular region which extends horizontally between
			// the corner circle centres and vertically to the entire extent of the enclosure.
			return true;
		else if (ax > WIDTH/2 - R && ax < WIDTH/2 - minDistance && ay < HEIGHT/2 - R - minDistance)
			// The point is within one of two rectangular regions on the left and right.
			return true;
		else	if (MathUtils.distance(v, upperLeft) < R - minDistance)
			return true;
		else	if (MathUtils.distance(v, upperRight) < R - minDistance)
			return true;
		else	if (MathUtils.distance(v, lowerLeft) < R - minDistance)
			return true;
		else	if (MathUtils.distance(v, lowerRight) < R - minDistance)
			return true;
		else
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
