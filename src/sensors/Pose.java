package sensors;

import org.jbox2d.common.Vec2;

import utils.AngleUtils;

public class Pose {
	double x, y, theta;

	public Pose(double x, double y, double theta) {
		this.x = x;
		this.y = y;
		this.theta = theta;
	}
	
	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getTheta() {
		return theta;
	}

	/**
	 * Return the distance between the two poses.
	 */
	public static double getDistanceBetween(Pose a, Pose b) {
		double dx = b.x - a.x;
		double dy = b.y - a.y;
		return Math.sqrt(dx*dx + dy*dy);
	}

	/**
	 * Return the relative angle that pose b is seen from pose a.  The result
	 * will be in the range (-pi, pi].
	 */
	public static double getAngleFromTo(Pose a, Pose b) {
		double dx = b.x - a.x;
		double dy = b.y - a.y;
		return AngleUtils.constrainAngle(Math.atan2(dy, dx) - a.theta);
	}
	
	/**
	 * Get a pose obtained by starting at 'a' and translating by the given (xt, yt)
	 * coordinate pair which are described in a's coordinate frame.
	 */
	public static Pose getTranslated(Pose a, float xt, float yt) {
		double dist = Math.sqrt(xt*xt + yt*yt);
		double angle = Math.atan2(yt, xt) + a.theta;
		return new Pose(a.x + dist * Math.cos(angle), a.y + dist * Math.sin(angle), a.theta);
	}
	
	public String toString() {
		return "x: " + x + ", y: " + y + ", theta: " + theta;
	}

	public Vec2 getAsVec2() {
		return new Vec2((float)x, (float)y);
	}
}
