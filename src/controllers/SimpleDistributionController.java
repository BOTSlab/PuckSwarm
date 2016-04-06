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
 * Individual distribution behaviour.  No bucket brigading.  Forked from BucketBrigadeController.
 */
public class SimpleDistributionController implements Controller, PropertiesListener {
	
	enum State {
		
		// Homing towards the source to collect a puck.
		PU_HOMING,
		
		// A searcher that is engaged in picking up a puck.  If successful it
		// transitions to a carrier.  If not, it goes back to scanning.
		PU_TARGET,
		
		// A carrier returns to the cache of the carried puck type.
		DE_HOMING,
		
		// A carrier that is spitting out its puck by backing up.
		SO_BACKUP,
		
		// After spitting out a puck, the robot will home towards the source
		// but should ignore pucks for a short period, which is represented by this state.
		SO_HOMING
	};
		
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
	
	// Position at which the last carried puck was collected.
	Vec2 pickupPoint;
	
	DistributionTaskManager manager;
	
	//
	// The following properties are set via the current Experiment...
	//
	
	// The amount of time to allow for a pick-up attempt.
	int PU_TARGET_TIME;

	// The amount of time to back-up to try and spit out a puck after DE_HOMING_TIME
	// has elapsed.
	int SO_BACKUP_TIME;

	// The amount of time to stay in SO_HOMING before transitioning to PU_HOMING.
	int SO_HOMING_TIME;

	public SimpleDistributionController() {
		propertiesUpdated();

		manager = DistributionTaskManager.getInstance();
	}
	
	@Override
	public void propertiesUpdated() {
		Experiment e = ExperimentManager.getCurrent();

		PU_TARGET_TIME = e.getProperty(
				"SimpleDistributionController.PU_TARGET_TIME", 20, this);
		SO_BACKUP_TIME = e.getProperty(
				"SimpleDistributionController.SO_BACKUP_TIME", 2, this);
		SO_HOMING_TIME = e.getProperty(
				"SimpleDistributionController.SO_HOMING_TIME", 10, this);
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
					pickupPoint = robotPose.getAsVec2();
				} else {
					target = localMap.getClosestPuckToPositionAsCluster(new Vec2(0,0), Float.MAX_VALUE);					
					if (target != null)
						transition(State.PU_TARGET, "Pick-up target acquired");
				}
				break;
			case PU_TARGET:
				if (carrying) {
					transition(State.DE_HOMING, "Pick-up succeeded");
					pickupPoint = robotPose.getAsVec2();
				} else {
					// Try to maintain selection on the same target.
					target = localMap.getClosestPuckToPositionAsCluster(new Vec2(0,0), Float.MAX_VALUE);					
					if (target == null)
						// Give up on this pick-up attempt.
						transition(State.PU_HOMING, "Lost target");
					else if (stepCount - startCount > PU_TARGET_TIME)
						// Give up on this pick-up attempt.
						transition(State.PU_HOMING, "Time out");
				}
				break;
			case DE_HOMING:
				if (!carrying) {
					// The carried puck has presumably been delivered.
					transition(State.PU_HOMING);
				} /*else {
					Vec2 current = robotPose.getAsVec2();
					Vec2 source = manager.getSource();
					Vec2 destination = manager.getDestinations()[SensedType.getPuckIndex(carriedType)];
					float distanceCarried = MathUtils.distance(pickupPoint, current);
					float benchmarkDistance = 0.5f * MathUtils.distance(source, destination);
					
					if (distanceCarried > benchmarkDistance)
						// Give up on this delivery attempt.
						transition(State.SO_BACKUP, "Time out");
				} */
				break;
			case SO_BACKUP:
				if (stepCount - startCount > SO_BACKUP_TIME)
					transition(State.SO_HOMING, "Time out");
				break;
			case SO_HOMING:
				if (carrying)
					transition(State.DE_HOMING, "Somehow acquired puck!");
				else if (stepCount - startCount > SO_HOMING_TIME)
					// Give up on this delivery attempt.
					transition(State.PU_HOMING, "Time out");
				break;
		}
		
		//
		// Set forwards and turnAngle based on state.
		//
		if (state == State.PU_HOMING || state == State.SO_HOMING) {
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
			float targetAngle = (float) Math.atan2(target.centroid.y, target.centroid.x);
			movement = MovementUtils.applyVFH(vfh, localMap, targetAngle, true);			
			
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

		} else if (state == State.SO_BACKUP) {
			movement = new MovementCommand(-1, 0);

		}
		
		if (carrying)
			lastCarriedType = carriedType;
		
		updateStateCounts(suite);
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
		return "SimpleDistribution: " + state;
	}
}
