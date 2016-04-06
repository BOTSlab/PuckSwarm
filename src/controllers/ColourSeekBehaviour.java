package controllers;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import sensors.Blob;
import sensors.BlobFinder;
import sensors.Calibration.CalibDataPerPixel;
import sensors.STCameraImage;
import sensors.SensedType;
import sensors.StringOverlay;
import sensors.Suite;
import arena.Robot;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

public class ColourSeekBehaviour implements Behaviour, PropertiesListener {
	
	// Computed in 'computeDesired'.
	boolean ready;
	
	// If active, the blob currently being targeted.
	Blob target;
	
	// STImage coordinates of the position of the target in the last image.  These
	// are set to -1 if on the last time step target == null.
	int lastTargetX = -1, lastTargetY = -1;

	// Carrying status from the last iteration.
	boolean lastCarrying;
	
	Random random;
	
	STCameraImage image;
	
	// Parameters whose values are loaded from the current Experiment...
	
	static float K1;
	static float K2;
	
	// Minimum distance from other robots in the image plane required for
	// a potential target.
	public static int TARGET_MIN_DIST_ROBOT;

	// The method used for selecting targets.
	static String TARGETING_METHOD;
	
	// The amount of shift we can tolerate for a target in between frames.  A
	// negative value means that we don't care how much shift occurs.
	static float TARGET_SHIFT_DIST;
	
	// Whether we filter unreachable blobs.
	static boolean FILTER_UNREACHABLE;
	
	public ColourSeekBehaviour() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
	}
	
	@Override
	public void propertiesUpdated() {
		K1 = ExperimentManager.getCurrent().getProperty("SeekBehaviour.K1", -1, this);
		K2 = ExperimentManager.getCurrent().getProperty("SeekBehaviour.K2", -1, this);
		TARGET_MIN_DIST_ROBOT = ExperimentManager.getCurrent().getProperty("SeekBehaviour.TARGET_MIN_DIST_ROBOT", 10, this);
		TARGETING_METHOD = ExperimentManager.getCurrent().getProperty("SeekBehaviour.TARGETING_METHOD", "size", this);
		TARGET_SHIFT_DIST = ExperimentManager.getCurrent().getProperty("SeekBehaviour.TARGET_SHIFT_DIST", 10, this);
		FILTER_UNREACHABLE = ExperimentManager.getCurrent().getProperty("SeekBehaviour.FILTER_UNREACHABLE", false, this);
	}
	
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		image = suite.getCameraImage();
		
		SensedType carriedColour = image.getCarriedType();
		boolean carrying = SensedType.isPuckType(carriedColour);
		
		target = null;
		if (carrying)
			target = setTarget(image, carrying, carriedColour);
		else {
			// Try all colours to see which one yields the smallest target.
			Blob potentialTarget = null;
			for (int i=0; i<SensedType.NPUCK_COLOURS; i++) {
				potentialTarget = setTarget(image, carrying, SensedType.getPuckType(i));
				if (potentialTarget != null) {
					if (target == null)
						target = potentialTarget;
					else {
						if (potentialTarget.getGroundArea() < target.getGroundArea())
							target = potentialTarget;
					}
				}
			}
		}
		
		if (target == null)
			ready = false;
		else {
			// Determine whether we should start seeking.
			ready = false;
			float f = target.getGroundArea();
			if (!carrying) {
				if (K1 < 0)
					ready = true;
				
				float probability = (K1 / (K1 + f));
				probability *= probability;
				if (random.nextFloat() < probability)
					ready = true;
			} else {
				if (K2 < 0)
					ready = true;

				float probability = (f / (K2 + f));
				probability *= probability;
				if (random.nextFloat() < probability)
					ready = true;
			}	
		}
		
		lastCarrying = carrying;
		if (target == null) {
			lastTargetX = -1;
			lastTargetY = -1;
		} else {
			lastTargetX = target.getCentreX();
			lastTargetY = target.getCentreY();

			image.addOverlay(new StringOverlay(lastTargetX, lastTargetY, "X", Color.black));
		}
	}
		
	private Blob setTarget(STCameraImage image, boolean carrying, SensedType puckType) {

		BlobFinder finder;
		if (carrying)
			finder = new BlobFinder(image, puckType, 0, image.width-1, 0, image.height-1-image.getCalibration().getGripperHeight());
		else
			finder = new BlobFinder(image, puckType, 0, image.width-1, 0, image.height-1);
		ArrayList<Blob> blobs = finder.getBlobs();
		finder.filterBlobsNear(blobs, SensedType.ROBOT, TARGET_MIN_DIST_ROBOT);

		if (TARGETING_METHOD.equals("meridian"))
			return setTargetClosestToMeridian(blobs, carrying);
		else if (TARGETING_METHOD.equals("size"))
			return setTargetBySize(blobs, carrying, puckType);
		else {
			System.err.println("SeekBehaviour: TARGETING_METHOD: "
					+ TARGETING_METHOD + " invalid!");
			System.exit(-1);
			return null;
		}
	}

	// Set the target to the blob closest to the meridian.
	private Blob setTargetClosestToMeridian(ArrayList<Blob> blobs, boolean carrying) {
		int minDistToMeridian = Integer.MAX_VALUE;
		Blob potential = null;
		for (Blob b : blobs) {
			if (targetUnacceptable(b, carrying))
				continue;			
			
			if (Math.abs(image.getCalibration().getMeridian() - b.getX0()) < minDistToMeridian) {
				minDistToMeridian = Math.abs(image.getCalibration().getMeridian() - b.getX0());
				potential = b;
			}
			if (Math.abs(image.getCalibration().getMeridian() - b.getX1()) < minDistToMeridian) {
				minDistToMeridian = Math.abs(image.getCalibration().getMeridian() - b.getX1());
				potential = b;
			}
		}
		return potential;
	}

	// Set the target to the blob with the largest ground area (if carrying)
	// or the smallest (if not carrying).  To prevent dithering between
	// similarly sized targets, we also check that each new potential
	// target is within a threshold image distance of the last target.
	private Blob setTargetBySize(ArrayList<Blob> blobs, boolean carrying, SensedType puckType) {
		Blob potential = null;
		if (carrying) {
			float largestSize = -Float.MAX_VALUE;
			for (Blob b : blobs) {
				if (targetUnacceptable(b, carrying))
					continue;
				
				if (b.getGroundArea() > largestSize) {
					potential = b;
					largestSize = b.getGroundArea();
				}
			}
			
		} else {
			float smallestSize = Float.MAX_VALUE;
			for (Blob b : blobs) {
				if (targetUnacceptable(b, carrying))
					continue;

				if (b.getGroundArea() < smallestSize) {
					potential = b;
					smallestSize = b.getGroundArea();
				}
			}
		}
		
		return potential;
	}
	
	private boolean targetUnacceptable(Blob b, boolean carrying) {
		
		// Firstly, a target whose centre lies in the unreachable region is
		// no good.
		if (FILTER_UNREACHABLE && image.getCalibration().getCalibData(b.getCentreX(), b.getCentreY()).unreachable)
			return true;
		
		if (TARGET_SHIFT_DIST < 0 || lastTargetX == -1 || carrying != lastCarrying)
			// Either we are not configured to check target shift, there is
			// no last target to compare to, or there has been a change in 
			// whether the robot is carrying a puck or not.  In all three cases
			// we allow arbitrarily large shifts in the target position.
			return false;
		
		int dx = b.getCentreX() - lastTargetX;
		int dy = b.getCentreY() - lastTargetY;
		double dist = Math.sqrt(dx*dx + dy*dy);
		return (dist > TARGET_SHIFT_DIST);
	}

	@Override
	public boolean readyToStart() {
		return ready;
	}

	@Override
	public boolean readyToContinue() {
		return ready;
	}

	@Override
	public boolean willingToGiveUp() {
		return true;
	}

	@Override
	public Behaviour deactivate() {
		return null;
	}
	
	@Override
	public void activate() {
	}
	
	@Override
	public float getForwards() {
		return Robot.MAX_FORWARDS;
	}

	@Override
	public float getTorque() {
		// We get smoother motion if we use proportional control.  To get the
		// desired angle, we need the ground plane coordinates of the target's
		// centre.
		CalibDataPerPixel pixelCalib = image.getCalibration().getCalibData(target.getCentreX(), target.getCentreY());
		float turnAngle = (float) Math.atan2(pixelCalib.Yr, pixelCalib.Xr);
		return BehaviourUtils.getScaledTorque(turnAngle);
		//return Robot.MAX_TORQUE * turnAngle;
		/*
		if (target.getCentreX() < image.getCalibration().getMeridian())
			return Robot.MAX_TORQUE;
		else
			return -Robot.MAX_TORQUE;
			*/
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
		Vec2 c = debugDraw.getWorldToScreen(robotTransform.position);
		debugDraw.drawString(c.x, c.y, "Seek", Color3f.WHITE);
		if (target != null) {
			// Draw a line from the robot to the target.
			CalibDataPerPixel pixelCalib = image.getCalibration().getCalibData(target.getCentreX(), target.getCentreY());
			float Xr = pixelCalib.Xr;
			float Yr = pixelCalib.Yr;
			Vec2 posWrtBody = new Vec2(Xr, Yr);
			Vec2 globalPos = Transform.mul(robotTransform, posWrtBody);
			debugDraw.drawSegment(robotTransform.position, globalPos, Color3f.WHITE);
		}
	}
	
	public String getInfoString() {
		return "ColourSeek";
	}
}
