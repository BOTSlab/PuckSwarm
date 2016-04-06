package sensors;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.collision.AABB;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;

import arena.Arena;
import arena.DotPuck;
import arena.Enclosure;
import arena.HiddenFixture;
import arena.Puck;
import arena.Robot;

public class PointSensor implements Sensor {

	Vec2 posWrtBody; // Position of the PointSensor with respect to robotBody.
	Body robotBody;
	Enclosure enclosure;
	World world;
	DebugDraw debugDraw;
		
	boolean useSegmentSampling;
	
	// The start and end of the sampling segment (valid only if 
	// useSegmentSampling is true).
	Vec2 segmentStart, segmentEnd;
	
	int nSegmentPoints;

	SensedType sensedType = SensedType.NOTHING;
	
	Vec2 globalPos = new Vec2();
	
	public static final float RADIUS_OF_LARGEST_OBJECT_SQD = Robot.SRV_LENGTH * Robot.SRV_LENGTH;

	/**
	 * Plain constructor.  Segment sampling will not be used.
	 */
	public PointSensor(Vec2 posWrtBody, Body robotBody, Arena arena) {
		this.posWrtBody = posWrtBody;
		this.robotBody = robotBody;
		this.enclosure = arena.getEnclosure();
		this.world = arena.getWorld();
		this.debugDraw = arena.getDebugDraw();
	}
		
	/**
	 * Constructor augmented with furtherNeighbour argument.  Segment sampling 
	 * MAY be used.
	 */
	/*
	public PointSensor(Vec2 posWrtBody, Body robotBody, Arena arena, Vec2 furtherNeighbour) {
		this(posWrtBody, robotBody, arena);

		if (MathUtils.distance(furtherNeighbour,  posWrtBody) > DotPuck.WIDTH) {
			useSegmentSampling = true;

			// How many points to test along the segment?
			nSegmentPoints = 4;
		
			segmentStart = posWrtBody;
			Vec2 between = furtherNeighbour.sub(posWrtBody);
			Vec2 V = (Vec2) between.clone();
			V.normalize();
			V.mulLocal(MathUtils.distance(furtherNeighbour,  posWrtBody) / (float) (nSegmentPoints));
			segmentEnd = furtherNeighbour.sub(V);
			
		}
	}
	*/

	public void sense() {
		//if (useSegmentSampling)
		//	segmentSense();
		//else
			pointSense();
	}

	private void pointSense() {

		// If the sensor lies outside the arena, it is considered WALL.
		Transform.mulToOut(robotBody.getTransform(), posWrtBody, globalPos);
		if (!enclosure.inFreeSpace(globalPos, 0)) {
			sensedType = SensedType.WALL;
			return;
		}

		sensedType = SensedType.NOTHING;
		for (Body b = world.getBodyList(); b != null; b = b.getNext()) {
			// The sensor should not detect the robot's body.  Also, we add a
			// quick test of distance to improve efficiency.
			if (b == robotBody ||
				MathUtils.distanceSquared(b.getPosition(), globalPos) > RADIUS_OF_LARGEST_OBJECT_SQD)
				continue;
			for (Fixture f = b.getFixtureList(); f != null; f = f.getNext()) {
				// We first test for overlap of the sensor point and the AABB
				// for the fixture.  Only if that is successful do we do the
				// more expensive, but accurate test.
				Object userData = f.getUserData();
				if (!(userData instanceof HiddenFixture) &&
					possibleOverlap(f.getAABB()) &&
					f.getShape().testPoint(b.getTransform(), globalPos)) {
					setSensedType(userData);
					return;
				}
			}
		}
	}
	
	public boolean possibleOverlap(final AABB aabb) {
		return (globalPos.x <= aabb.upperBound.x &&
			globalPos.x >= aabb.lowerBound.x &&
			globalPos.y <= aabb.upperBound.y &&
			globalPos.y >= aabb.lowerBound.y);
	}
	/*
	public void segmentSense() {

		// If the sensor lies outside the arena, it is considered WALL.
		Vec2 globalPos = Transform.mul(robotBody.getTransform(), posWrtBody);
		if (!enclosure.insideArena(globalPos, 0)) {
			sensedType = SensedType.WALL;
			return;
		}
		
		// Form an axis-aligned bounding box from the two ends of the segment.
		Vec2 startGlobal = Transform.mul(robotBody.getTransform(), segmentStart);
		Vec2 endGlobal = Transform.mul(robotBody.getTransform(), segmentEnd);
		AABB aabb = new AABB(new Vec2(Math.min(startGlobal.x, endGlobal.x) - Puck.WIDTH, 
								 	  Math.min(startGlobal.y, endGlobal.y) - Puck.WIDTH),
						     new Vec2(Math.max(startGlobal.x, endGlobal.x) + Puck.WIDTH,
						    		  Math.max(startGlobal.y, endGlobal.y) + Puck.WIDTH));
		
		Vec2[] segmentPoints = new Vec2[nSegmentPoints];
		for (int i=0; i<nSegmentPoints; i++) {
			float t = i / (float) (nSegmentPoints-1);
			segmentPoints[i] = new Vec2(
					startGlobal.x + t * (endGlobal.x - startGlobal.x),
					startGlobal.y + t * (endGlobal.y - startGlobal.y));
		}
		
		sensedType = SensedType.NOTHING;
		for (Body b = world.getBodyList(); b != null; b = b.getNext()) {
			// The sensor should not detect the robot's body.  Also, we add a
			// quick test of distance to improve efficiency.
			if (//b == robotBody ||
				MathUtils.distanceSquared(b.getPosition(), globalPos) > RADIUS_OF_LARGEST_OBJECT_SQD)
				continue;
			for (Fixture f = b.getFixtureList(); f != null; f = f.getNext()) {
				// We first test for overlap of the sensor point and the AABB
				// for the fixture.  Only if that is successful do we do the
				// more expensive, but accurate tests.
//				if (AABB.testOverlap(f.getAABB(), aabb) ) {					
					for (Vec2 point : segmentPoints) {
						Object userData = f.getUserData();
						if (//!(userData instanceof HiddenFixture) &&
					        f.getShape().testPoint(b.getTransform(), point)) {
					    	setSensedType(userData);
							if (sensedType != SensedType.HIDDEN)
								return;
					    }
					}
//				}
			}
		}

	}
	*/

	// We have hit something. Determine and return its type.
	private void setSensedType(Object userData) {
		if (userData == null) {
			sensedType = SensedType.WALL;
		} else if (userData instanceof Puck) {
			sensedType = ((Puck) userData).getPuckType();
		} else if (userData instanceof Robot) {
			sensedType = SensedType.ROBOT;
		} 
//		else if (userData instanceof HiddenFixture) {
//			sensedType = SensedType.NOTHING;
//		}
	}
	
	public SensedType getSensedType() {
		return sensedType;
	}

	public void draw() {
		float radius = 0.2f;
		
		Vec2 globalPos = Transform.mul(robotBody.getTransform(), posWrtBody);
		debugDraw.drawSolidCircle(globalPos, radius, null, sensedType.color3f);
		
		if (useSegmentSampling) {
			Vec2 startGlobal = Transform.mul(robotBody.getTransform(), segmentStart);
			Vec2 endGlobal = Transform.mul(robotBody.getTransform(), segmentEnd);

			for (int i=0; i<nSegmentPoints; i++) {
				float t = i / (float) (nSegmentPoints-1);
				Vec2 v = new Vec2(
						startGlobal.x + t * (endGlobal.x - startGlobal.x),
						startGlobal.y + t * (endGlobal.y - startGlobal.y));
				debugDraw.drawSolidCircle(v, radius/2, null, Color3f.BLUE);
			}
		}
	}
}
