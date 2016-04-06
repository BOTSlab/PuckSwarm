package controllers;

import java.awt.Color;
import java.awt.Point;
import java.io.File;

import localmap.Cluster;
import localmap.LocalMap;
import localmap.MovementCommand;
import localmap.MovementUtils;
import localmap.VFHPlus;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import sensors.Pose;
import sensors.SensedType;
import sensors.StringOverlay;
import sensors.Suite;
import utils.FileUtils;
import arena.DistributionTaskManager;
import arena.MessageBoard;
import experiment.Experiment;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * Initial prototype of bucket brigading.  Forked from CacheConsController.
 */
public class BucketBrigadeController implements Controller, PropertiesListener {
	
	enum State {
		
		// Homing towards the source to collect a puck.
		PU_HOMING,
		
		// A searcher that is engaged in picking up a puck.  If successful it
		// transitions to a carrier.  If not, it goes back to scanning.
		PU_TARGET,
		
		// A carrier returns to the cache of the carried puck type.
		DE_HOMING,

		// We have just deposited and are now backing up.
		DE_BACKUP,
		
		// We have just dropped a puck for another to carry.  Stay in this state for a little
		// while so that we don't try to pick up the same puck again.
		POST_DROP};
		
	State state = State.PU_HOMING;
	
	// Tally of the number of iterations spent in each state over the last
	// suite.getStorageInterval() time steps.
	int[] recentStateCounts = new int[State.values().length];
	
	Cluster target;
	
	// Stores the type of puck most recently carried.  This is needed in EXILE state to 
	// determine which localizer's origin we should flee from.
	SensedType lastCarriedType;
	
	MovementCommand movement;
	
	// The value of stepCount upon activation.
	int stepCount, startCount;
	
	// Variables associated with the wandering aspect of this behaviour.
	VFHPlus vfh = null;	

	// The current pose obtained via APS.
	Pose robotPose;
	
	DistributionTaskManager manager;
	
	//
	// The following properties are set via the current Experiment...
	//
	
	// The amount of time to allow for a pick-up attempt.
	int PICK_UP_TIME;

	// The amount of time to spend backing up after completing a deposit.
	int BACKUP_TIME;

	// The time spent in the POST_DROP state.
    int POST_DROP_TIME;
	
	public BucketBrigadeController() {
		propertiesUpdated();

		manager = DistributionTaskManager.getInstance();
	}
	
	@Override
	public void propertiesUpdated() {
		Experiment e = ExperimentManager.getCurrent();

		PICK_UP_TIME = e.getProperty(
				"BucketBrigadeController.PICK_UP_TIME", 20, this);
		BACKUP_TIME = e.getProperty(
				"BucketBrigadeController.BACKUP_TIME", 5, this);
		POST_DROP_TIME = e.getProperty(
				"BucketBrigadeController.POST_DROP_TIME", 5, this);
	}
	
	/// BAD: The following three methods are copied into BHDController,
	/// ProbSeekController, and CacheConsController.  All three operate on the
	/// 'state' Enums of each class.  However, I am not sure how to write code
	/// that handles different Enum types coherently.
	
	/** Transition to a new state and post a message. */
	private void transition(State newState, String extraMessage) {
		state = newState;
		startCount = stepCount;
		MessageBoard.getMessageBoard().post("transition: " + state + " (" +
				extraMessage + ")");
	}
	
	/** As above, only no message is specified. */
	private void transition(State newState) {
		state = newState;
		startCount = stepCount;
		MessageBoard.getMessageBoard().post("transition: " + state);
	}
	
	/** Update 'recentStateCounts' and periodically store these counts to disk. */
	private void updateStateCounts(Suite suite) {
		int stateIndex = state.ordinal();
		recentStateCounts[stateIndex]++;
		
		if (stepCount % suite.getStorageInterval() == 0) {
			String filename = ExperimentManager.getOutputDir()
					+ File.separatorChar
					+ ExperimentManager.getCurrent().getStringCodeWithoutSeed()
					+ File.separatorChar
					+ ExperimentManager.getCurrent().getIndex()
					+ File.separatorChar
					+ "step" + String.format("%07d", stepCount)
					+ "_" + suite.getRobotName()
					+ "_stateCounts.txt";
			FileUtils.saveArray(recentStateCounts, filename);
	
			int n = State.values().length;
			for (int i=0; i<n; i++)
				recentStateCounts[i] = 0;
		}
	}
	
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		LocalMap localMap = suite.getLocalMap();
		if (vfh == null)
			vfh = new VFHPlus(localMap, true);
		
		// Get the robot's pose from APS.  This could be null if there is a problem
		// with localization.  One option in this case would be to stop dead.  However,
		// for now we choose to continue and only check for null if we need to use it.
		robotPose = suite.getAPS().getPose(suite.getRobotName());		
		
		boolean carrying = localMap.isCarrying();
		SensedType carriedType = localMap.getCarriedType();
		this.stepCount = stepCount;
		
		//
		// Handle state transitions...
		//
		switch (state) {
			case PU_HOMING:
				if (carrying) {
					transition(State.DE_HOMING, "Somehow acquired puck!");
				} else {
					target = localMap.getClosestPuckToPositionAsCluster(new Vec2(0,0), Float.MAX_VALUE);					
					if (target != null)
						transition(State.PU_TARGET, "Pick-up target acquired");
				}
				break;
			case PU_TARGET:
				if (carrying) {
					transition(State.DE_HOMING, "Pick-up succeeded");
				} else {
					// Try to maintain selection on the same target.
					target = localMap.getClosestPuckToPositionAsCluster(new Vec2(0,0), Float.MAX_VALUE);					
					if (target == null)
						// Give up on this pick-up attempt.
						transition(State.PU_HOMING, "Lost target");
					else if (stepCount - startCount > PICK_UP_TIME)
						// Give up on this pick-up attempt.
						transition(State.PU_HOMING, "Time out");
				}
				break;
			case DE_HOMING:
				if (!carrying)
					// The carried puck has presumably been delivered.
					transition(State.PU_HOMING);
				else if (seeRobotForPass(robotPose, localMap))
					// We will give up the carried puck.
					transition(State.DE_BACKUP);
				break;
			case DE_BACKUP:
				if (stepCount - startCount >= BACKUP_TIME)
                	transition(State.POST_DROP);
				break;
			case POST_DROP:
				if (carrying)
					transition(State.DE_HOMING, "Wandered into a puck!");
				else if (stepCount - startCount >= POST_DROP_TIME)
					transition(State.PU_HOMING);
				break;
		}
		
		//
		// Set forwards and turnAngle based on state.
		//
		if (state == State.PU_HOMING || state == State.POST_DROP) {
			if (robotPose == null)
				// Cannot localize.  Just stay still for the moment.
				movement = new MovementCommand(0, 0);
			else {
				Vec2 source = manager.getSource();
				float errorAngle = (float) Pose.getAngleFromTo(robotPose, 
											new Pose(source.x, source.y, 0));
				
				movement = MovementUtils.applyVFH(vfh, localMap, errorAngle, false);
			}

		} else if (state == State.PU_TARGET) {
			movement = new MovementCommand(1, 
					(float) Math.atan2(target.centroid.y, target.centroid.x));
			Point tp = localMap.getGridPoint(target.centroid.x, target.centroid.y);
			localMap.getOccupancy().addOverlay(new StringOverlay(tp.x, tp.y, "X", Color.black));
			
		} else if (state == State.DE_HOMING) {
			if (robotPose == null)
				// Cannot localize.  Just stay still for the moment.
				movement = new MovementCommand(0, 0);
			else {
				Vec2 destination = manager.getDestinations()[SensedType.getPuckIndex(carriedType)];
				float errorAngle = (float) Pose.getAngleFromTo(robotPose, 
											new Pose(destination.x, destination.y, 0));
				
				movement = MovementUtils.applyVFH(vfh, localMap, errorAngle, false);
			}

		} else if (state == State.DE_BACKUP) {
			movement = new MovementCommand(-1, 0);
						
		}
		
		if (carrying)
			lastCarriedType = carriedType;
		
		updateStateCounts(suite);
	}
	
	/**
	 * Check if there is another robot near the centre of our field of view (defined by
	 * a threshold) which is also closer to the destination.  Ideally this other robot would
	 * be heading in the opposite direction (towards us) and would be unladen, but
	 * we have no good way of checking for these conditions right now.
	 * 
	 * Precondition: We are carrying.
	 */
	static boolean seeRobotForPass(Pose robotPose, LocalMap localMap) {
		Vec2 closestRobot = localMap.getClosestRobotVec2();
		if (closestRobot == null)
			return false;

		// Actually, we will consider passing to the closest robot only if we are
		// sufficiently far from the source.
		DistributionTaskManager manager = DistributionTaskManager.getInstance();
		float x = (float) robotPose.getX();
		float y = (float) robotPose.getY();
		float theta = (float) robotPose.getTheta();
		float myDistanceToSource = MathUtils.distance(new Vec2(x, y), manager.getSource());		
		if (myDistanceToSource < 50)
			return false;
		
		// closestRobot is in the robot reference frame.  We translate this to the global
		// frame to get a vector that can be compared to the length of the destination
		// vector.
		float alpha = (float) Math.atan2(closestRobot.y, closestRobot.x);
		float d = closestRobot.length();	
		Vec2 hisPosition = new Vec2((float)(x + d*Math.cos(theta+alpha)),
			   								         (float)(y + d*Math.sin(theta+alpha)));
		
		Vec2 destination = manager.getDestinations()[SensedType.getPuckIndex(localMap.getCarriedType())];

		float myDistanceToGoal = MathUtils.distance(new Vec2(x, y), destination);
		float hisDistanceToGoal = MathUtils.distance(hisPosition, destination);
		
		return hisDistanceToGoal < myDistanceToGoal &&
				Math.abs(alpha) < Math.toRadians(30);
	}

	@Override
	public float getForwards() {
		return movement.getForwards();
	}

	@Override
	public float getTorque() {
		return movement.getTorque();
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
		if (target != null && state == State.PU_HOMING || state == State.PU_TARGET) {
			// Draw a line from the robot to the target.
			float Xr = target.centroid.x;
			float Yr = target.centroid.y;
			Vec2 posWrtBody = new Vec2(Xr, Yr);
			Vec2 globalPos = Transform.mul(robotTransform, posWrtBody);
			debugDraw.drawSegment(robotTransform.position, globalPos, Color3f.WHITE);
		}
		
		Vec2[] destinations = manager.getDestinations();
		for (int k=0; k<destinations.length; k++) {
			debugDraw.drawCircle(destinations[k], 5, robotColor);
			debugDraw.drawSolidCircle(destinations[k], 5, null, SensedType.getPuckType(k).color3f);
		}
	}
	
	public String getInfoString() {
		return "BucketBrigade: " + state;
	}
}
