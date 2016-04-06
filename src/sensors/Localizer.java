package sensors;

import org.jbox2d.common.Vec2;

public interface Localizer {

	public void reset();
	
	public void resetTo(Vec2 location);

	public float getX();

	public float getY();

	public float getTheta();
	
	public float getDistanceHome();

	public float getHomeAngle();	
}
