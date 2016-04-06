package arena;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

public interface Enclosure {
	/**
	 * Generate a random (x, y) position that lies no closer than minDistance
	 * to the boundary.
	 */
	public Vec2 getRandomInside(float minDistance);
	
	/**
	 * Return true if the given point lies within the enclosure with at least
	 * the given minimum distance to the boundary.
	 */
	public boolean inFreeSpace(Vec2 v, float minDistance);
	
	public float getWidth();
	public float getHeight();
	public Body getBody();
}