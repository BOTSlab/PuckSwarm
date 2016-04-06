package controllers;

import java.util.Random;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import sensors.Camera;
import sensors.STCameraImage;
import sensors.SensedType;
import sensors.Suite;
import sensors.StoredCalibration;
import arena.Robot;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * Determine the proportions of same-coloured pucks in the backup zone of the
 * image.  If the proportion for any colour exceeds a threshold then we will
 * respond.  That response depends on whether the robot is carrying a puck
 * and whether the carried puck matches the cluster's colour:
 *  - If carried colour = cluster colour then we deposit the puck by moving
 *    forwards to approach the cluster, then backing up and turning away by a 
 *    random angle.
 *  - Otherwise (if there is no puck being carried or the carried colour does
 *    not match the cluster colour) then we turn away by a random angle.
 *
 * Note: This is the first colour-aware Behaviour.  Not sure how colour-awareness
 * should be indicated...
 */
public class ColourBackupBehaviour implements Behaviour, PropertiesListener {	
	
	enum State {INACTIVE, APPROACHING, BACKING_UP, TURNING};
	State state = State.INACTIVE;
	
	Random random;
	
	float[] proportions;
	
	// The value of stepCount upon activation and the current value.
	int startCount, stepCount;

	// The direction and duration to turn while in TURNING state.
	int turnDir, turnTime;
	
	// Maximum cluster sizes (in ground area) seen.  Used if CLUSTER_MEMORY.
	float[] maxClusterSizes;
	
	boolean propertiesUpdated;
	
	// Parameters whose values are loaded from the current Experiment...

	// These define the image coordinates of the rectangular region sampled
	// for pucks.
	public static int REGION_WIDTH, REGION_HEIGHT, BOTTOM_Y;	
	static int regionArea;

	// The threshold minimum proportion of pucks required to trigger APPROACHING
	// if the cluster is pure AND the robot is carrying a matching puck.
	static float THRESHOLD_IF_CARRYMATCH;

	// The threshold minimum proportion of pucks required to trigger TURNING
	// if the cluster is pure AND the robot is NOT carrying a matching puck.
	static float THRESHOLD_IFNOT_CARRYMATCH;
	
	// Determines whether we should count the proportion of pixels in the backup
	// zone (false) or the proportion of the total ground area included in the
	// backup zone.
	static boolean USE_GROUND_AREA;
	
	// The number of time steps to approach
	static int APPROACH_TIME;
	
	// The number of time steps to back up.
	static int BACKUP_TIME;
	
	// The minimum turning time.
	static int TURNTIME_MIN;
	
	// Whether we maintain a memory of the maximum cluster size (in ground area).
	static boolean CLUSTER_MEMORY;
	
	public ColourBackupBehaviour() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
				
		proportions = new float[SensedType.NPUCK_COLOURS];
		maxClusterSizes = new float[SensedType.NPUCK_COLOURS];
	}
	
	@Override
	public void propertiesUpdated() {
		REGION_WIDTH = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.REGION_WIDTH", 50, this);
		REGION_HEIGHT = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.REGION_HEIGHT", 15, this);
		BOTTOM_Y = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.BOTTOM_Y", 90, this);
		USE_GROUND_AREA = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.USE_GROUND_AREA", false, this);
		THRESHOLD_IF_CARRYMATCH = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.THRESHOLD_IF_CARRYMATCH", 0.25f, this);
		//THRESHOLD_IFNOT_CARRYMATCH = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.THRESHOLD_IFNOT_CARRYMATCH", 0.6f, this);
		THRESHOLD_IFNOT_CARRYMATCH = 2 * THRESHOLD_IF_CARRYMATCH;
		APPROACH_TIME = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.APPROACH_TIME", 3, this);
		BACKUP_TIME = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.BACKUP_TIME", 5, this);
		TURNTIME_MIN = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.TURNTIME_MIN", Robot.ONE_EIGHTY_TIME/3, this);
		CLUSTER_MEMORY = ExperimentManager.getCurrent().getProperty("BackupAndTurnBehaviour.CLUSTER_MEMORY", false, this);
		propertiesUpdated = true;
	}
	
	private void completePropertiesUpdate(STCameraImage image) {	
		// Determine the area (in pixels or ground area) of the backup zone.
		if (!USE_GROUND_AREA)
			regionArea = REGION_WIDTH * REGION_HEIGHT;
		else {
			int x0 = image.getCalibration().getMeridian() - REGION_WIDTH/2;
			int x1 = image.getCalibration().getMeridian() + REGION_WIDTH/2;
			int y0 = BOTTOM_Y - REGION_HEIGHT + 1;
			int y1 = BOTTOM_Y;
			regionArea = 0;
			for (int i=x0; i<=x1; i++)
				for (int j=y0; j<=y1; j++)
					regionArea += image.getCalibration().getCalibData(i, j).groundArea;
		}
		propertiesUpdated = false;
	}
		
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		STCameraImage image = suite.getCameraImage();
		
		if (propertiesUpdated)
			completePropertiesUpdate(image);
		
		this.stepCount = stepCount;

		switch (state) {
			case INACTIVE:
				// Check the backup zone and either enter APPROACHING (i.e.
				// intending to deposit), TURNING (i.e. leave cluster alone),
				// or INACTIVE (i.e. there is no cluster, or at least not one
				// worth preserving).
				state = changeStateBasedOnBackupZone(image);
				if (state == State.TURNING)
					initiateTurning(image);
				startCount = stepCount;
				break;
			case APPROACHING:
				if (stepCount - startCount >= APPROACH_TIME) {
					state = State.BACKING_UP;
					startCount = stepCount;
				}
				break;
			case BACKING_UP:
				if (stepCount - startCount >= BACKUP_TIME) {
					state = State.TURNING;
					startCount = stepCount;
					initiateTurning(image);
				}
				break;
			case TURNING:
				if (stepCount - startCount >= turnTime) {
					state = State.INACTIVE;
				}
				break;
		}
	}
	
	private State changeStateBasedOnBackupZone(STCameraImage image) {
		int x0 = image.getCalibration().getMeridian() - REGION_WIDTH/2;
		int x1 = image.getCalibration().getMeridian() + REGION_WIDTH/2;
		int y0 = BOTTOM_Y - REGION_HEIGHT + 1;
		int y1 = BOTTOM_Y;
		
		// Calculate the proportion of the backup zone filled by each puck
		// colour.
		Color3f[] puckColors = SensedType.getPuckColours();
		for (int k=0; k<proportions.length; k++) {
			proportions[k] = 0;
			for (int i=x0; i<=x1; i++)
				for (int j=y0; j<=y1; j++) {
					if (SensedType.isPuckOfColor(image.pixels[i][j], puckColors[k])) {
						if (USE_GROUND_AREA)
							proportions[k] += image.getCalibration().getCalibData(i, j).groundArea;
						else
							proportions[k]++;
					}
				}
			proportions[k] /= regionArea;
		}
		
		// Determine the highest and second-highest proportion values.
		float highest = 0, secondHighest = 0;
		int highestK = 0;
		for (int k=0; k<proportions.length; k++) {
			float p = proportions[k];
			if (p > highest) {
				secondHighest = highest;
				highest = p;
				highestK = k;
			} else if (p < highest && p > secondHighest) {
				secondHighest = p;
			}
		}
		
		// If the cluster is not pure, we will remain INACTIVE (perhaps resulting
		// in a destructive collision with the cluster).
		if (secondHighest != 0) {
//System.out.println("Impure!");
			return State.INACTIVE;
		}

		// What is the predominant colour of the cluster
		SensedType clusterType = SensedType.getPuckType(highestK);

		// Is there a match between the carried puck and the cluster.
		//boolean carryMatch = BehaviourUtils.isCarrying(image, clusterType);
		boolean carryMatch = image.getCarriedType() == clusterType;

		// Update the memory of maxClusterSizes
		if (CLUSTER_MEMORY) {
			if (highest > maxClusterSizes[highestK])
				maxClusterSizes[highestK] = highest;
			else
				// This is not the highest seen...  Let it be destroyed.
				return State.INACTIVE;
		}
		
		if (carryMatch) {
			// We will either deposit to enlarge the cluster, or remain inactive
			// if the cluster size is lower than the threshold.
			if (highest > THRESHOLD_IF_CARRYMATCH) {
//System.out.println("Approach!");
				return State.APPROACHING;
			} else {
//System.out.println("Colour match but threshold not met: " + highest);
				return State.INACTIVE;
			}
		} else {
			// We are not carrying a matching puck for this cluster.  So
			// we either turn away (i.e. leave the cluster untouched) or remain
			// inactive.
			if (highest > THRESHOLD_IFNOT_CARRYMATCH) {
//System.out.println("Turn!");
				return State.TURNING;
			} else {
//System.out.println("Colour does NOT match and threshold not met: " + highest);
				return State.INACTIVE;
			}
		}		
	}
	
	private void initiateTurning(STCameraImage image) {
		// Choose a turn direction away from the wall.
		turnDir = BehaviourUtils.getTurnSign(image, true, false);
		if (turnDir == 0) {
			if (random.nextFloat() > 0.5)
				turnDir = 1;
			else
				turnDir = -1;
		}

		// Choose a random turn duration.
		turnTime = TURNTIME_MIN + random.nextInt(Robot.ONE_EIGHTY_TIME - TURNTIME_MIN + 1);
	}

	@Override
	public boolean readyToStart() {
		return state != State.INACTIVE;
	}

	@Override
	public boolean readyToContinue() {
		return state != State.INACTIVE;
	}

	@Override
	public boolean willingToGiveUp() {
		return state == State.INACTIVE;
	}

	@Override
	public Behaviour deactivate() {
		return null;
	}
	
	@Override
	public void activate() {
		// Activation work already done in computeDesired.
	}
	
	@Override
	public float getForwards() {
		switch (state) {
			case APPROACHING:
				return Robot.MAX_FORWARDS;
			case BACKING_UP:
				return -Robot.MAX_FORWARDS;
			default:
				return 0;
		}
	}

	@Override
	public float getTorque() {
		if (state == State.TURNING)
			return turnDir * Robot.MAX_TORQUE;
		else
			return 0;
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
		Vec2 c = debugDraw.getWorldToScreen(robotTransform.position);
		debugDraw.drawString(c.x, c.y, "Colour-backup", Color3f.WHITE);
		for (int i=0; i<proportions.length; i++)
			debugDraw.drawString(c.x, c.y + (i+1)*10, maxClusterSizes[i] + "", Color3f.WHITE);
	}
	
	public String getInfoString() {
		String s = "ColourBackUp: props: ";
		for (int i=0; i<proportions.length; i++)
			s += proportions[i] + ", ";
		return s;
	}
}
