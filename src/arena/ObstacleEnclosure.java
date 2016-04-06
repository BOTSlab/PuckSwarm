package arena;

import java.util.ArrayList;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;

import utils.FixtureUtils;

/* 
 * Incorporates rectangular obstacles.
 */
public class ObstacleEnclosure extends RoundedRectangleEnclosure {

	ArrayList<PolygonShape> obstacles;
	
	public ObstacleEnclosure(World world) {
		super(world);
		
		// Obstacles
		obstacles = new ArrayList<PolygonShape>();
		float half_width = WIDTH/10f;
		float half_height = HEIGHT/5f;
		float centreX = 0;
		float centreY = 0;
		addObstacle(half_width, half_height, centreX, centreY);
	}

	private void addObstacle(float half_width, float half_height,
			float centreX, float centreY) {
		
		// We create a polygon representing the obstacle.  This is not actually added to
		// the world, but only used in 'inFreeSpace'.  It is just a rectangle.
		PolygonShape obs = new PolygonShape();
		obs.setAsBox(half_width, half_height, new Vec2(centreX, centreY), 0);
		obstacles.add(obs);
		//obs.setAsBox(WIDTH/10f, HEIGHT/5f);
		//body.createFixture(obs, 0.0f);
		
		// The fixutres required for a rounded rectangle are actually incorporated into
		// the enclosure's body here.
		FixtureUtils.createRoundedRectangle(centreX-half_width, centreX+half_width, centreY+half_height, centreY-half_height, 5f, body);		
	}
		
	@Override
	public Vec2 getRandomInside(float minDistance) {
		// Try a large number of times to find a satisfactory point.
		for (int i=0; i<100000; i++) {
			// Generate a random position that lies within the WIDTH x HEIGHT
			// rectangular area.
			Vec2 v = new Vec2((float) rng.nextFloat() * WIDTH - WIDTH / 2.0f,
							  			  (float) rng.nextFloat() * HEIGHT - HEIGHT / 2.0f);			
			
			Vec2[] testPoints = { v,
					new Vec2(v.x - minDistance, v.y),
					new Vec2(v.x + minDistance, v.y),
					new Vec2(v.x, v.y - minDistance),
					new Vec2(v.x, v.y + minDistance) };
			
			for (Vec2 point : testPoints)
				if (!inFreeSpace(point, minDistance))
					continue;
			return v;
		}
		
		// We will reach here only if no satisfactory point could be found
		// which should occur only if the arena is blocked full.
		return null;
	}

	@Override
	public boolean inFreeSpace(Vec2 v, float minDistance) {
		if (!super.inFreeSpace(v,  minDistance))
			return false;
		
		Transform xf = new Transform();
		xf.setIdentity();
		for (PolygonShape obs : obstacles) {
			if (obs.testPoint(xf, v))
				return false;
		}
		return true;
	}
}
