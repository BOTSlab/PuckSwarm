package controllers;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.util.Random;

import localmap.Cluster;
import localmap.LocalMap;
import localmap.MovementCommand;
import localmap.MovementUtils;
import localmap.VFHPlus;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import sensors.StringOverlay;
import sensors.Suite;
import utils.FileUtils;
import arena.MessageBoard;
import arena.Robot;
import controllers.BHDController.State;
import controllers.cluster.ClusterTargetSelector;
import controllers.cluster.ExtremaSelector;
import controllers.cluster.MemorySelector;
import controllers.cluster.PieSelector;
import experiment.Experiment;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * Benchmark clustering algorithm for SI 2012 paper.
 */
public class ProbSeekController implements Controller, PropertiesListener {
	
	enum State {
		PU_SCAN, 	// Pick-up scan
		PU_TARGET,	// Pick-up target
		DE_SCAN,	// Deposit scan
		DE_TARGET,
		DE_PUSH,
		DE_BACKUP,
		DE_TURN};
	State state = State.PU_SCAN;
	
	// Tally of the number of iterations spent in each state over the last
	// suite.getStorageInterval() time steps.
	int[] recentStateCounts = new int[State.values().length];
	
	// In both PU_TARGET states we target a particular 
	// cluster.  That position should correspond to an individual puck 
	// (PU_TARGET) or a cluster (DE_TARGET).
	Cluster target;
	
	ClusterTargetSelector targetSelector;
	
	MovementCommand movement;
	
	// The value of stepCount upon activation.
	int stepCount, startCount;
	
	// Variables associated with the wandering aspect of this behaviour.
	VFHPlus vfh = null;	
	Random random;
	
	// Associated with DE_TURN
	int randomTurnDir, randomTurnTime;
	int TURNTIME_MIN = Robot.ONE_EIGHTY_TIME/2;
	
	//
	// The following properties are set via the current Experiment...
	//
	
	// Constants governing pick-up (K1) and deposit (K2) targeting.
	float K1, K2;
	
	// Whether to always target isolated pucks for pick-up.
	boolean ALWAYS_TARGET_ISOLATED;
	
	// The amount of time to allow for a pick-up attempt.
	int PICK_UP_TIME;

	// The amount of time to allow for a placement attempt.
	int PLACEMENT_TIME;

	// The amount of time to spend continuing to push after completing a deposit.
	int PUSH_IN_TIME;

	// The amount of time to spend backing up after completing a deposit.
	int BACKUP_TIME;

	// The maximimum squared distance allowed between a previously acquired target's
	// predicted position and the closest matching current position (the target
	// could be a cluster centroid or a single puck).
	float PREDICTION_DISTANCE_SQD;

	// The standard deviation of the random process that influences wandering.
	float WANDER_ST_DEV;
	
	public ProbSeekController() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
	}
	
	@Override
	public void propertiesUpdated() {
		Experiment e = ExperimentManager.getCurrent();
		
		K1 = ExperimentManager.getCurrent().getProperty("ProbSeekController.K1", 1f, this);
		K2 = ExperimentManager.getCurrent().getProperty("ProbSeekController.K2", 8f, this);
		
		ALWAYS_TARGET_ISOLATED = ExperimentManager.getCurrent().getProperty("ProbSeekController.K2", false, this);
		
		targetSelector = new ExtremaSelector(K1, K2, ALWAYS_TARGET_ISOLATED);
		
		PICK_UP_TIME = e.getProperty(
				"ProbSeekController.PICK_UP_TIME", 20, this);
		PLACEMENT_TIME = e.getProperty(
				"ProbSeekController.PLACEMENT_TIME", 40, this);
		PUSH_IN_TIME = e.getProperty(
				"ProbSeekController.PUSH_IN_TIME", 1, this);
		BACKUP_TIME = e.getProperty(
				"ProbSeekController.BACKUP_TIME", 5, this);
		PREDICTION_DISTANCE_SQD = e.getProperty(
				"ProbSeekController.PREDICTION_DISTANCE_SQD", 10f*10f, this);
		WANDER_ST_DEV = e.getProperty(
				"ProbSeekController.ST_DEV", 0.5f, this);		
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
		boolean carrying = localMap.isCarrying();
		this.stepCount = stepCount;
		
		targetSelector.inspect(localMap, localMap.getFilteredClusters(), carrying, localMap.getCarriedType());
		
		//
		// Handle state transitions...
		//
		switch (state) {
			case PU_SCAN:
				if (carrying) {
					transition(State.DE_SCAN, "Somehow acquired puck!");
				} else {
					target = targetSelector.selectTarget(carrying, !carrying);
					if (target != null)
						transition(State.PU_TARGET, "Pick-up target acquired");
				}
				break;
			case PU_TARGET:
				if (carrying) {
					transition(State.DE_SCAN, "Pick-up succeeded");
				} else {
					// Try to maintain selection on the same target.
					target = matchTarget(localMap, target, PREDICTION_DISTANCE_SQD);
					if (target == null)
						// Give up on this pick-up attempt.
						transition(State.PU_SCAN, "Lost target");
					else if (stepCount - startCount > PICK_UP_TIME)
						// Give up on this pick-up attempt.
						transition(State.PU_SCAN, "Time out");
				}
				break;
			case DE_SCAN:
				if (!carrying) {
					transition(State.PU_SCAN, "Somehow lost puck!");
				} else {
					target = targetSelector.selectTarget(carrying, !carrying);
					if (target != null)
						transition(State.DE_TARGET);
				}
				break;
			case DE_TARGET:
				if (!carrying) {
					transition(State.PU_SCAN, "Somehow lost puck!");
				} else if (localMap.getCarriedCluster() != null && localMap.getCarriedCluster().size > 1) {
					// We have made contact with a cluster of size 2 or more.
					transition(State.DE_PUSH, "Contacted cluster");
				} else {
					// Try to maintain selection on the same target.
					target = matchTarget(localMap, target, PREDICTION_DISTANCE_SQD);
					if (target == null)
						// Give up on this placement attempt.
						transition(State.DE_SCAN, "Lost target");
					else if (stepCount - startCount > PLACEMENT_TIME)
						// Give up on this placement attempt.
						transition(State.DE_SCAN, "Time out");
				}
				break;
			case DE_PUSH:
				if (stepCount - startCount >= PUSH_IN_TIME)
					transition(State.DE_BACKUP);
				break;
			case DE_BACKUP:
				if (stepCount - startCount >= BACKUP_TIME) {
					transition(State.DE_TURN);
					initiateRandomTurn(localMap);
				} break;
			case DE_TURN:
				if (stepCount - startCount >= randomTurnTime) {
					transition(State.PU_SCAN);
				}
				break;
		}
		
		//
		// Set forwards and turnAngle based on state.
		//
		if (state == State.PU_SCAN || state == State.DE_SCAN) {
			// Wander!
			float randomTurn = WANDER_ST_DEV * (float) random.nextGaussian();
			movement = MovementUtils.applyVFH(vfh, localMap, randomTurn);					

		} else if (state == State.PU_TARGET || state == State.DE_TARGET) {
			movement = new MovementCommand(1, 
					(float) Math.atan2(target.centroid.y, target.centroid.x));
			Point tp = localMap.getGridPoint(target.centroid.x, target.centroid.y);
			localMap.getOccupancy().addOverlay(new StringOverlay(tp.x, tp.y, "X", Color.black));
			
		} else if (state == State.DE_PUSH) {
			movement = new MovementCommand(1, 0);
			
		} else if (state == State.DE_BACKUP) {
			movement = new MovementCommand(-1, 0);
			
		} else if (state == State.DE_TURN) {
			movement = new MovementCommand(0, (float) (Math.PI/2 * randomTurnDir));
		}

		updateStateCounts(suite);
	}

	/**
	 * Determine the new target position, given the current LocalMap and the
	 * last target position.  We return null if the predicted position is too 
	 * dissimilar.
	 */
	public static Cluster matchTarget(LocalMap localMap, Cluster lastTarget, float sqdDistanceThreshold) {
		Point tp = localMap.getGridPoint(lastTarget.centroid.x, lastTarget.centroid.y);
		localMap.getOccupancy().addOverlay(new StringOverlay(tp.x, tp.y, "P", Color.black));
		
		// Find the closest puck or cluster to the prediction.
		Cluster newTarget = null;
		if (localMap.isCarrying())
			newTarget = localMap.getClosestClusterToPosition(lastTarget.centroid, sqdDistanceThreshold, true);
		else
			newTarget = localMap.getClosestPuckToPositionAsCluster(lastTarget.centroid, sqdDistanceThreshold);
		
		if (newTarget == null || !localMap.isReachable(newTarget.centroid))
			return null;
		else
			return newTarget;
	}
	
	private void initiateRandomTurn(LocalMap localMap) {
		randomTurnDir = localMap.getFreeerSide();

		// Choose a random turn duration.
		randomTurnTime = TURNTIME_MIN + random.nextInt(Robot.ONE_EIGHTY_TIME - TURNTIME_MIN + 1);
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
		if (target != null && state == State.PU_SCAN || state == State.PU_TARGET) {
			// Draw a line from the robot to the target.
			float Xr = target.centroid.x;
			float Yr = target.centroid.y;
			Vec2 posWrtBody = new Vec2(Xr, Yr);
			Vec2 globalPos = Transform.mul(robotTransform, posWrtBody);
			debugDraw.drawSegment(robotTransform.position, globalPos, Color3f.WHITE);
		}
	}
	
	public String getInfoString() {
		return "ProbSeek: " + state + ", " + targetSelector.getStatusString();
	}
}
