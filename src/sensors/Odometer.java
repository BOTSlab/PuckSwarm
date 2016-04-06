/**
 * Simulates pose estimation via simple integration.  Construct with different
 * Mode values for ideal and simulated odometric error.
 */
package sensors;

import java.util.Random;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Mat22;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import utils.AngleUtils;

import arena.Arena;

import experiment.ExperimentManager;

public class Odometer implements Localizer, LocalizerUpdater {
	
	Transform robotTransform, lastRobotTransform;
	
	public enum Mode {
		IDEAL, // The true pose of the robot is simply copied into (x, y, theta) 
		ZERO_ERROR, // The true difference between the last and current poses is
		            // integrated without any synthetic error.  Still, numerical
					// error will creep in.
		INCLUDE_ERROR // The true difference between the last and current poses
					  // is corrupted by noise and then integrated.  The noise
					  // is governed by the ALPHA parameters.
	}
	Mode mode;

	Arena arena;
	DebugDraw debugDraw;
	
	Pose curPose, lastPose;
	
	// Last true position and angle (used to generate the estimate).
	Vec2 lastTruePosition;
	double lastTrueAngle;

	// True pose at which the last 'reset' occured (for visualization only).
	Transform trueResetTransform;
	
	// Used to simulate odometric error.
	Random random;
	
	// Parameters that govern the odometric error in INCLUDE_ERROR mode.
	public static final double ALPHA1 = 0.001;
	public static final double ALPHA2 = 0.001;
	public static final double ALPHA3 = 0.001;
	public static final double ALPHA4 = 0.001;

	public Odometer(Mode mode) { 
		this.mode = mode;
	}

	//
	// OVERRIDES FOR LOCALIZERUPDATER
	//
	
	/**
	 * Called once to initialize.
	 */
	@Override
	public void init(Transform robotTransform, Arena arena) {
		this.robotTransform = robotTransform;
		this.arena = arena;
		this.debugDraw = arena.getDebugDraw();
		random = ExperimentManager.getCurrent().getRandom();
		
		lastTruePosition = robotTransform.position.clone();
		
		reset();		
	}

	/**
	 * Allow the pose estimate to be updated from the most recent motion.
	 */
	@Override
	public void update() {
		if (mode == Mode.IDEAL) {
			double resetAngle = trueResetTransform.getAngle();
			curPose.theta = robotTransform.getAngle() - resetAngle;
			double dx = robotTransform.position.x - trueResetTransform.position.x;
			double dy = robotTransform.position.y - trueResetTransform.position.y;
			double d = Math.sqrt(dx*dx + dy*dy);
			double alpha = Math.atan2(dy, dx) - resetAngle;
			curPose.x = d * Math.cos(alpha);
			curPose.y = d * Math.sin(alpha);
			return;
		}
		
		// The actual movement is parameterized by a translation d and a rotation
		// deltaTheta.
		double dx = robotTransform.position.x - lastTruePosition.x;
		double dy = robotTransform.position.y - lastTruePosition.y;
		double d = Math.sqrt(dx*dx + dy*dy);
		double deltaTheta = robotTransform.getAngle() - lastTrueAngle;
		
		if (mode == Mode.ZERO_ERROR) {
			// Nothing to do: d and deltaTheta are unperturbed.
		} else if (mode == Mode.INCLUDE_ERROR) {
			// Simulate odometric error using the method proposed by Thrun et al
			// in "Probabilistic Robotics".  One difference is that we parameterize
			// the motion with (d, deltaTheta) whereas Thrun et al include an
			// additional rotation component.
			double dSqd = d * d;
			double deltaThetaSqd = deltaTheta * deltaTheta;
			double rotError = sample(ALPHA1 * deltaThetaSqd + ALPHA2 * dSqd);
			double transError = sample(ALPHA3 * dSqd + ALPHA4 * deltaThetaSqd);
			d += transError;
			deltaTheta += rotError;
		}
		
		// Integrate motion (or simulated erroneous motion).
		curPose.theta = lastPose.theta + deltaTheta;
		curPose.x = lastPose.x + d * Math.cos(curPose.theta);
		curPose.y = lastPose.y + d * Math.sin(curPose.theta);
		
		// Prepare for the next time step.
		lastPose.x = curPose.x;
		lastPose.y = curPose.y;
		lastPose.theta = curPose.theta;
		
		lastTruePosition = robotTransform.position.clone();
		lastTrueAngle = robotTransform.getAngle();
	}

	/**
	 * Generate a random sample from a Gaussian distribution with the given
	 * variance.
	 */
	private double sample(double variance) {
		return Math.sqrt(variance) * random.nextGaussian();
	}

	@Override
	public void draw() {
		// Draw the reset position
		drawTransform(trueResetTransform, "O");

		// Now determine the screen position of the estimate
		Transform estimateTransform = new Transform();
		estimateTransform.set(Transform.mul(trueResetTransform, 
											new Vec2((float)curPose.x, (float)curPose.y)),
							  (float) curPose.theta + trueResetTransform.getAngle());
		drawTransform(estimateTransform, "");
	}
	
	@Override
	public Transform getHomeTransform() {
		return trueResetTransform;
	}

	//
	// OVERRIDES FOR LOCALIZER
	//
	
	/**
	 * Reset our estimate of the robot's position
	 */
	@Override
	public void reset() {
		curPose = new Pose(0, 0, 0);
		lastPose = new Pose(0, 0, 0);
		
		// For Mode.IDEAL and visualization in 'draw'.
		trueResetTransform = new Transform(robotTransform);		
	}

	/**
	 * Reset to position the origin at the given position w.r.t. the robot.
	 */
	@Override
	public void resetTo(Vec2 p) {
		curPose = new Pose(-p.x, -p.y, 0);
		lastPose = new Pose(-p.x, -p.y, 0);
		
		// For Mode.IDEAL and visualization in 'draw'.
		Vec2 pInWorld = Transform.mul(robotTransform, p);
		trueResetTransform = new Transform(pInWorld, robotTransform.R);			
	}

	@Override
	public float getX() {
		return (float) curPose.x;
	}

	@Override
	public float getY() {
		return (float) curPose.y;
	}

	@Override
	public float getTheta() {
		return (float) curPose.theta;
	}
	
	@Override
	public float getDistanceHome() {
		return (float) Math.sqrt(curPose.x * curPose.x + curPose.y * curPose.y);
	}

	@Override
	public float getHomeAngle() {		
		// The angle towards the origin is:	atan2(y, x) - theta + pi
		return (float) AngleUtils.constrainAngle(
				Math.atan2(curPose.y, curPose.x) - curPose.theta + Math.PI);
	}
	
	//
	// TO BE REMOVED? DEPRECATED?
	// 

	public Pose getPose() {
		return curPose;
	}

	public Pose getLastPose() {
		return lastPose;
	}

	public Transform getTransform() {
		return new Transform(new Vec2((float)curPose.x, (float)curPose.y), 
							 Mat22.createRotationalTransform((float)curPose.theta));
	}

	public Transform getLastTransform() {
		return new Transform(new Vec2((float)lastPose.x, (float)lastPose.y), 
				 Mat22.createRotationalTransform((float)lastPose.theta));
	}

	public static Transform getInverseTransform(Transform t) {
		// Utilizing equation (2.45) from "Introduction to Robotics" 3rd Edition
		// by John J. Craig.
		
		Mat22 Rinv = t.R.invert();
		
		Vec2 newV = Mat22.mul(Rinv, t.position).mul(-1);
		
		return new Transform(newV, Rinv);
	}
	
	/**
	 * Adapted from the function of the same name in DebugDrawJ2D to increase
	 * the size of the axes drawn.
	 */
	public void drawTransform(Transform origin, String label) {
		float scale = 2.0f;

		Vec2 xAxis = new Vec2(origin.position.x + scale * origin.R.col1.x,
							  origin.position.y + scale * origin.R.col1.y);
		Vec2 yAxis = new Vec2(origin.position.x + scale * origin.R.col2.x,
							  origin.position.y + scale * origin.R.col2.y);

		debugDraw.drawSegment(origin.position, xAxis, Color3f.RED);
		debugDraw.drawSegment(origin.position, yAxis, Color3f.GREEN);

		Vec2 v = debugDraw.getWorldToScreen(origin.position
				.add(new Vec2(-1, -4)));
		debugDraw.drawString(v.x, v.y, label, Color3f.WHITE);
	}

	/**
	 * For testing purposes...
	 */
	public void randomReset() {
		float arenaWidth = 0.65f * arena.getEnclosure().getWidth();
		float arenaHeight = 0.85f * arena.getEnclosure().getHeight();
		Vec2 v = new Vec2((float) random.nextFloat() * arenaWidth - arenaWidth / 2.0f,
						  (float) random.nextFloat() * arenaHeight - arenaHeight / 2.0f);
		
		curPose = new Pose(0, 0, 0);
		lastPose = new Pose(0, 0, 0);
		
		// For Mode.IDEAL and visualization in 'draw'.
		Mat22 rotMatrix = new Mat22();
		rotMatrix.setIdentity();
		trueResetTransform = new Transform(v, rotMatrix);			
	}

}
