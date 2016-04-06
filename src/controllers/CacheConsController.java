package controllers;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

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
import utils.AngleUtils;
import utils.FileUtils;
import arena.MessageBoard;
import arena.PositionList;
import controllers.cluster.ClusterTargetSelector;
import controllers.cluster.Deneubourg;
import controllers.cluster.ExtremaSelector;
import experiment.Experiment;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * CacheConsensus clustering and sorting method as described in SI 2012 paper.  This
 * was forked off HPMBehaviour.  This version is intended to be cleaner, removes some 
 * options, and introduces others.
 */
public class CacheConsController implements Controller, PropertiesListener {
	
	enum State {
		
		// Look for pucks to collect.
		PU_SCAN,
		
		// A searcher that is engaged in picking up a puck.  If successful it
		// transitions to a carrier.  If not, it goes back to scanning.
		PU_TARGET,
		
		// A carrier returns to the cache of the carried puck type.
		HOMING,

		// Puck is being pushed into final position.
		DE_PUSH,
		
		// We have just deposited and are now backing up.
		DE_BACKUP,
		
		// Move away from the cache point after a deposit to encourage picking up 
		// non-cache cluster pucks.
		EXILE,
		
		// A state entered if we inadvertently pick up a puck of a colour that has not been
		// Initialised.  The action is just to back up for SPIT_OUT_TIME.
		SPIT_OUT};

	State state = State.PU_SCAN;
	
	// Tally of the number of iterations spent in each state over the last
	// suite.getStorageInterval() time steps.
	int[] recentStateCounts = new int[State.values().length];
	
	Cluster target;
	
	// For selecting pick-up targets
	ClusterTargetSelector targetSelector;
	
	// An array of cache points --- one per puck colour.  The entries will remain null until
	// an initial home point is selected for each puck colour.  An entry can also be nullified
	// via the check for distance to another cache.  We represent as Poses just for
	// convenience.  They are really just points.
	Pose[] cachePoints = new Pose[SensedType.NPUCK_COLOURS];
	
	// Associated with the cache points defined above, are the remembered sizes of the
	// clusters attached to them.  It is important to stress "remembered" because
	// unobserved changes to a cache can occur due to the actions of other robots.
	float[] rememberedCacheSizes = new float[SensedType.NPUCK_COLOURS];

	// Stores the type of puck most recently carried.  This is needed in EXILE state to 
	// determine which localizer's origin we should flee from.
	SensedType lastCarriedType;
	
	MovementCommand movement;
	
	// The value of stepCount upon activation.
	int stepCount, startCount;
	
	// Reference to the suite.
	Suite suite = null;
	
	// Variables associated with the wandering aspect of this behaviour.
	VFHPlus vfh = null;	
	Random random;

	// The current pose obtained via APS.
	Pose robotPose;
	
	boolean presetCaches;
	
	//
	// The following properties are set via the current Experiment...
	//
	
	// Constants governing pick-up (K1) and deposit (K2) targeting.
	float K1, K2;
	
	// Whether to always target isolated pucks for pick-up.
	boolean ALWAYS_TARGET_ISOLATED;

	// The amount of time to allow for a pick-up attempt.
	int PICK_UP_TIME;

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

	// The time spent in the EXILE state after finishing a deposit cycle.
	int EXILE_TIME;
	
	// The amount of time to spend in SPIT_OUT.
	int SPIT_OUT_TIME;
	
	// The following parameter was introduced for the ECAL 2013 paper
	
	// Percentage of the full height of the enclosure at which to place the row of preset
	// caches.
	float PRESET_ROW_HEIGHT;
	
	// Ignore pucks when moving, thus making the controller less respectful of the clusters
	// of other agents.
	boolean IGNORE_PUCKS;
	
	enum CacheSelectionOption {
		// Cluster centres are selected as new cache points with a probability governed by 
		// their size and constant K_ABSOLUTE.
		PROB_ABSOLUTE,
		
		// Cluster centres are selected as new cache points with probability governed by 
		// the difference in size with the remembered cache size for this type and 
		// K_RELATIVE.
		PROB_RELATIVE,
		
		// New cache points are selected whenever their size is larger than the remembered
		// cache size for this type.
		RELATIVE,
		
		// Just like RELATIVE only the robot has a probability FORGET_PROB of
		// forgetting its caches on each iteration.
		RELATIVE_FORGET
	};
	
	CacheSelectionOption CACHE_SELECTION_OPTION;

	float K_ABSOLUTE, K_RELATIVE, FORGET_PROB;
	
	enum CacheSeparationOption {
		// Let the caches grow arbitrarily close to each other
		IGNORE, 
		
		// Whenever a new cache point is found, any existing cache points within
		// CACHE_SEPARATION_DISTANCE are nullified.
		NULLIFY_OLD, 
		
		// Only accept a new cache point which is at a minimum distance of
		// CACHE_SEPARATION_DISTANCE from existing cache points.
		ACCEPT_ISOLATED, 
		
		// Whenever a potential new cache point is found, compare its perceived size
		// with the stored size of any existing caches within CACHE_SEPARATION_DISTANCE.
		// If the new cache point is the largest, then accept it and nullify the others.
		// Otherwise, this cache point is not accepted.
		ACCEPT_LARGER
	};
	
	CacheSeparationOption CACHE_SEPARATION_OPTION;
	
	// Depending on the option selected above, this may be the minimum distance
	// between cache points.
	float CACHE_SEPARATION_DISTANCE;
	
	public CacheConsController(boolean presetCaches) {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
		
		// Added for ECAL workshop and "perfect information" extension to SI paper
		this.presetCaches = presetCaches;
		if (presetCaches) {
			
			// Constants that appear elsewhere in the code but are difficult to access.
			float arenaWidth = 187.0f;
			float arenaHeight = 187.0f;
			int nTypes = 2;
			
			// Place cache points evenly along the main diagonal of the arena.
			/*
			float diagonalLength = (float) Math.hypot(arenaWidth, arenaHeight);
			float stepLength = diagonalLength / (nTypes + 1);
			float cos_45 = (float) Math.cos(Math.PI/4f);
			for (int type = 0; type<nTypes; type++) {
				float x = -arenaWidth/2f + cos_45 * (type + 1) * stepLength;
				cachePoints[type] = new Pose(x, x, 0);
				rememberedCacheSizes[type] = 1000;
			}
			*/
			float L = 0.75f * arenaWidth / 2.0f;
			for (int type = 0; type<nTypes; type++) {
				float angle = (float) (type * (2*Math.PI) / nTypes + Math.PI/4f);
				float x = (float) (L*Math.cos(angle));
				float y = (float) (L*Math.sin(angle));
				cachePoints[type] = new Pose(x, y, 0);
				rememberedCacheSizes[type] = 1000;
			}
			
			
			// Setting IGNORE_PUCKS to true caused a reduction in performance
			// for the benchmark method "CacheCons_Informed" described in the
			// SI12 paper.  So we explicitly set it false here.
			IGNORE_PUCKS = false;
		}
	}
	
	@Override
	public void propertiesUpdated() {
		Experiment e = ExperimentManager.getCurrent();

		K1 = ExperimentManager.getCurrent().getProperty("CacheConsController.K1", 1f, this);
		K2 = ExperimentManager.getCurrent().getProperty("CacheConsController.K2", 8f, this);

		ALWAYS_TARGET_ISOLATED = ExperimentManager.getCurrent().getProperty("CacheConsController.K2", false, this);

		targetSelector = new ExtremaSelector(K1, K2, ALWAYS_TARGET_ISOLATED);
		
		PICK_UP_TIME = e.getProperty(
				"CacheConsController.PICK_UP_TIME", 20, this);
		PUSH_IN_TIME = e.getProperty(
				"CacheConsController.PUSH_IN_TIME", 1, this);
		BACKUP_TIME = e.getProperty(
				"CacheConsController.BACKUP_TIME", 5, this);
		PREDICTION_DISTANCE_SQD = e.getProperty(
				"CacheConsController.PREDICTION_DISTANCE_SQD", 10f*10f, this);
		WANDER_ST_DEV = e.getProperty(
				"CacheConsController.ST_DEV", 0.5f, this);
		EXILE_TIME = e.getProperty(
				"CacheConsController.EXILE_TIME", 20, this);
		SPIT_OUT_TIME = e.getProperty(
				"CacheConsController.SPIT_OUT_TIME", 10, this);

		CACHE_SELECTION_OPTION = CacheSelectionOption.valueOf(CacheSelectionOption.class, 
				e.getProperty("CacheConsController.CACHE_SELECTION_OPTION", "RELATIVE", this));

		K_ABSOLUTE = ExperimentManager.getCurrent().getProperty("CacheConsController.K_ABSOLUTE", 40f, this);
		K_RELATIVE = ExperimentManager.getCurrent().getProperty("CacheConsController.K_RELATIVE", 8f, this);
		FORGET_PROB = ExperimentManager.getCurrent().getProperty("CacheConsController.FORGET_PROB", 0.001f, this);
		
		CACHE_SEPARATION_OPTION = CacheSeparationOption.valueOf(CacheSeparationOption.class, 
				e.getProperty("CacheConsController.CACHE_SEPARATION_OPTION", "ACCEPT_LARGER", this));

		CACHE_SEPARATION_DISTANCE = e.getProperty(
				"CacheConsController.HOME_SEPARATION_DISTANCE", 50f, this);		

		// For ECAL
		IGNORE_PUCKS = ExperimentManager.getCurrent().getProperty(
				"CacheConsController.IGNORE_PUCKS", false, this);
		PRESET_ROW_HEIGHT = e.getProperty(
				"CacheConsController.PRESET_ROW_HEIGHT", 0.5f, this);		
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
		this.suite = suite;
		
		// Get the robot's pose from APS.  This could be null if there is a problem
		// with localization.  One option in this case would be to stop dead.  However,
		// for now we choose to continue and only check for null if we need to use it.
		robotPose = suite.getAPS().getPose(suite.getRobotName());		
		
		boolean carrying = localMap.isCarrying();
		SensedType carriedType = localMap.getCarriedType();
		this.stepCount = stepCount;
		
		checkForget();
		
		// Look at the unfiltered clusters and potentially select new cache points.
		if (robotPose != null) {
						
			// First go through all visible clusters (of each type) and determine if the cache
			// cluster is visible.  If so, update its remembered size.
			for (int k=0; k<SensedType.NPUCK_COLOURS; k++) {
				if (cachePoints[k] != null) {
					Cluster cacheCluster = null;
// BUG Discovered on 29 August 2013.
					for (Cluster c : localMap.getUnfilteredClustersOfType(k))
						if (cachePointInCluster(localMap, c))
							cacheCluster = c;
					if (cacheCluster != null) {
						rememberedCacheSizes[k] = Math.max(rememberedCacheSizes[k], cacheCluster.size);
MessageBoard.getMessageBoard().post(suite.getRobotName() + " " + "new cache size for type " + k + ": " + rememberedCacheSizes[k] + "\n");
					}
				}
			}
	
			for (int k=0; k<SensedType.NPUCK_COLOURS; k++) {
				// In all options for new cache selection, the candidate is the largest cluster in view.
				Cluster candidate = null;
				for (Cluster c : localMap.getUnfilteredClustersOfType(k))
					if (candidate == null || c.size > candidate.size)
						candidate = c;
				if (candidate == null)
					continue;
				
				// The candidate may or not be selected.
				if (!checkCacheSelection(candidate))
					continue;

				// Check for separation between the candidate and existing cache points.
				if (checkCacheSeparation(candidate))
					// Acceptable!
					initializeCache(candidate);	
			}
		}

		// Mask puck colours that don't yet have a corresponding cache.  This prevents
		// them from being selected as targets for pick-up.
		for (int k=0; k < SensedType.NPUCK_COLOURS; k++)
			if (cachePoints[k] == null)
				localMap.postFilterClusters(SensedType.getPuckType(k));

		targetSelector.inspect(localMap, localMap.getFilteredClusters(), carrying, carriedType);
		
		//
		// Handle state transitions...
		//
		if (carrying && cachePoints[SensedType.getPuckIndex(carriedType)] == null)
			transition(State.SPIT_OUT, "Picked up an uninitialized color!");
		else {
			switch (state) {
				case PU_SCAN:
					if (carrying) {
						transition(State.HOMING, "Somehow acquired puck!");
					} else {
						target = targetSelector.selectTarget(carrying, !carrying);
						if (target != null) {
							if (cachePoints[target.puckType] == null)
								// A cache point hasn't yet been selected for this type of puck.
								target = null;
							else if (cachePointInCluster(localMap, target))
								// The target lies within the cache cluster.
								target = null;
							else
								transition(State.PU_TARGET, "Pick-up target acquired");
						}
					}
					break;
				case PU_TARGET:
					if (carrying) {
						transition(State.HOMING, "Pick-up succeeded");
					} else {
						// Try to maintain selection on the same target.
						target = ProbSeekController.matchTarget(localMap, target, PREDICTION_DISTANCE_SQD);
						if (target == null)
							// Give up on this pick-up attempt.
							transition(State.PU_SCAN, "Lost target");
						else if (cachePoints[target.puckType] == null)
							// A cache point hasn't yet been selected for this type of puck.
							transition(State.PU_SCAN, "No cache point for this target");
						else if (cachePointInCluster(localMap, target))
							transition(State.PU_SCAN, "Potential target lies in cache");
						else if (stepCount - startCount > PICK_UP_TIME)
							// Give up on this pick-up attempt.
							transition(State.PU_SCAN, "Time out");
					}
					break;
				case HOMING:
					if (!carrying)
						transition(State.PU_SCAN, "Somehow lost puck");
					else if (cachePoints[SensedType.getPuckIndex(carriedType)] == null)
						transition(State.PU_SCAN, "Cache point was nullified");
					else if (carriedPuckInCacheCluster(localMap))
						// We hit the cache!
						transition(State.DE_PUSH);
					break;
				case DE_PUSH:
					if (stepCount - startCount >= PUSH_IN_TIME)
						transition(State.DE_BACKUP);
					break;
				case DE_BACKUP:
					if (stepCount - startCount >= BACKUP_TIME) {
						if (cachePoints[SensedType.getPuckIndex(lastCarriedType)] == null)
							transition(State.PU_SCAN, "Cache point was nullified");
                        else
                        	transition(State.EXILE);
					} 
					break;
				case EXILE:
					if (stepCount - startCount >= EXILE_TIME)
						transition(State.PU_SCAN, "Exile complete");
					else if (cachePoints[SensedType.getPuckIndex(lastCarriedType)] == null)
						transition(State.PU_SCAN, "Cache point was nullified");
					break;
				case SPIT_OUT:
					if (stepCount - startCount >= SPIT_OUT_TIME)
						transition(State.PU_SCAN, "Time-out elapsed");
					break;
			}
		}
		
		//
		// Set forwards and turnAngle based on state.
		//
		if (state == State.PU_SCAN) {
			// Wander!
			float randomTurn = WANDER_ST_DEV * (float) random.nextGaussian();
			movement = MovementUtils.applyVFH(vfh, localMap, randomTurn, IGNORE_PUCKS);					

		} else if (state == State.PU_TARGET) {
			movement = new MovementCommand(1, 
					(float) Math.atan2(target.centroid.y, target.centroid.x));
			Point tp = localMap.getGridPoint(target.centroid.x, target.centroid.y);
			localMap.getOccupancy().addOverlay(new StringOverlay(tp.x, tp.y, "X", Color.black));
			
		} else if (state == State.HOMING) {
			if (robotPose == null)
				// Cannot localize.  Just stay still for the moment.
				movement = new MovementCommand(0, 0);
			else {
				float errorAngle = (float) Pose.getAngleFromTo(robotPose, cachePoints[SensedType.getPuckIndex(carriedType)]);
				
				// Prevent avoidance of the cache cluster by removing all of the carried puck's type
				// from the occupancy grid.
				localMap.postFilterOccupancy(carriedType);
				
				movement = MovementUtils.applyVFH(vfh, localMap, errorAngle, IGNORE_PUCKS);
			}

		} else if (state == State.DE_PUSH) {
			movement = new MovementCommand(1, 0);
			
		} else if (state == State.DE_BACKUP) {
			movement = new MovementCommand(-1, 0);
						
		} else if (state == State.EXILE) {
			if (robotPose == null)
				// Cannot localize.  Just stay still for the moment.
				movement = new MovementCommand(0, 0);
			else {
				float errorAngle = (float) Pose.getAngleFromTo(robotPose, cachePoints[SensedType.getPuckIndex(lastCarriedType)]);
	
				// Reverse this angle
				errorAngle = (float) AngleUtils.constrainAngle(errorAngle + Math.PI);
				movement = MovementUtils.applyVFH(vfh, localMap, errorAngle, IGNORE_PUCKS);
			}
			
		}  else if (state == State.SPIT_OUT) {
			float randomTurn = WANDER_ST_DEV * (float) random.nextGaussian();
			movement = new MovementCommand(-1, randomTurn);
		}
		
		if (carrying)
			lastCarriedType = carriedType;
		
		// Periodically save cache points to disk.
		if (stepCount % suite.getStorageInterval() == 0) {
			String base = ExperimentManager.getOutputDir()
					+ File.separatorChar
					+ ExperimentManager.getCurrent().getStringCodeWithoutSeed()
					+ File.separatorChar
					+ ExperimentManager.getCurrent().getIndex()
					+ File.separatorChar
					+ "step" + String.format("%07d", stepCount);

			PositionList cachePointList = new PositionList();
			for (int k=0; k<SensedType.NPUCK_COLOURS; k++) {
				if (cachePoints[k] == null)
					// Indicates a null entry.
					cachePointList.add(Float.NaN, Float.NaN);
				else
					cachePointList.add((float)cachePoints[k].getX(), (float)cachePoints[k].getY());
			}
			
			cachePointList.save(base + "_" + suite.getRobotName() + "_cachePoints.txt");
		}

		updateStateCounts(suite);
	}

	private void checkForget() {
		if (CACHE_SELECTION_OPTION == CacheSelectionOption.RELATIVE_FORGET) {
			if (!presetCaches && FORGET_PROB != -1 && random.nextFloat() < FORGET_PROB) {
				// Forget all caches!
				for (int k=0; k<SensedType.NPUCK_COLOURS; k++) {
					cachePoints[k] = null;
					rememberedCacheSizes[k] = 0;
				}
			}
		}
	}

	/** Determine whether the given candidate should be accepted as a new cache. */
	private boolean checkCacheSelection(Cluster candidate) {
		switch (CACHE_SELECTION_OPTION) {
		case PROB_ABSOLUTE:
			if (random.nextFloat() < Deneubourg.depositProb(candidate.size, K_ABSOLUTE))
				return true;
			break;
		case PROB_RELATIVE:
			float diff = candidate.size - rememberedCacheSizes[candidate.puckType];
			if (random.nextFloat() < Deneubourg.depositProb(diff, K_RELATIVE))
				return true;
			break;
		case RELATIVE_FORGET:
		case RELATIVE:
			if (candidate.size > rememberedCacheSizes[candidate.puckType])
				return true;
			break;
		}
		
		return false;
	}
	
	/** 
	 * Check if the proposed new cache point is acceptable in relation to its distance to 
	 * existing cache points.  Return true if it is acceptable.
	 * 
	 * Side Effect: May nullify existing cache points.
	 */
	private boolean checkCacheSeparation(Cluster candidate) {
		assert candidate != null;

		// To simplify the code below, we first assemble an ArrayList of indices (puck type
		// index, k) for existing cache points which lie within the threshold distance.
		ArrayList<Integer> nearbyCacheIndices = new ArrayList<Integer>();
		for (int otherK=0; otherK<SensedType.NPUCK_COLOURS; otherK++)
			if (otherK != candidate.puckType && cachePoints[otherK] != null) {
				Vec2 otherCachePoint = getCachePointInRobotCoords(otherK);
				if (MathUtils.distance(candidate.centroid, otherCachePoint) < CACHE_SEPARATION_DISTANCE) {
					nearbyCacheIndices.add(otherK);
				}
			}
		
		switch (CACHE_SEPARATION_OPTION) {
		case IGNORE:
			// We are ignoring any concerns about cache separation. 
			return true;
		
		case NULLIFY_OLD:
			// Nullify existing nearby cache points.
			for (int otherK : nearbyCacheIndices) {
				cachePoints[otherK] = null;
				rememberedCacheSizes[otherK] = 0;
			}
			return true;
		
		case ACCEPT_ISOLATED:
			// Only accept the new cache point if there are no nearby existing cache points.
			return (nearbyCacheIndices.size() == 0);
		
		case ACCEPT_LARGER:
			// Check if this is larger than all existing cache points.
			boolean newOneIsLargest = true;
			for (int otherK : nearbyCacheIndices)
				if (candidate.size <= rememberedCacheSizes[otherK])
					newOneIsLargest = false;
			
			if (newOneIsLargest) {
				// We nullify the existing nearby caches.
				for (int otherK : nearbyCacheIndices) {
					cachePoints[otherK] = null;
					rememberedCacheSizes[otherK] = 0;
				}
			}
			
			return newOneIsLargest;
		}
		
		assert false;  // Should never reach here, but the compiler requires a final return.
		return true;
	}

	private void initializeCache(Cluster newCacheCluster) {
		assert robotPose != null;
		int type = newCacheCluster.puckType;
		MessageBoard.getMessageBoard().post("New cache point!");
		cachePoints[type] = Pose.getTranslated(robotPose, newCacheCluster.centroid.x, 
				newCacheCluster.centroid.y);
		rememberedCacheSizes[type] = newCacheCluster.size;
	}

	private boolean cachePointInCluster(LocalMap localMap, Cluster targetCluster) {
		if (robotPose == null)
			// Can't localize at the moment.  We'll assume that the target cluster is
			// not the cache cluster.
			return false;
		Vec2 cp = getCachePointInRobotCoords(targetCluster.puckType);

		// Now see if any pucks in the target cluster lie within threshold distance.
		Set<Vec2> set = targetCluster.subgraph.vertexSet();
		for (Vec2 v : set)
			if (MathUtils.distance(v, cp) < LocalMap.CLUSTER_DISTANCE_THRESHOLD)
				return true;

		return false;
	}

	private boolean carriedPuckInCacheCluster(LocalMap localMap) {
		Cluster carriedCluster = localMap.getCarriedCluster();
		if (carriedCluster == null)
			// Should only be the case if another robot is nearby, causing
			// this robot's carried cluster to be filtered out.
			return false;
		if (robotPose == null)
			// Can't localize at the moment.  We'll assume we're not at the cache yet.
			return false;
				
		Vec2 cp = getCachePointInRobotCoords(SensedType.getPuckIndex(localMap.getCarriedType()));
		
		// Now see if any pucks in the carried cluster lie within threshold distance.
		Set<Vec2> set = carriedCluster.subgraph.vertexSet();
		for (Vec2 v : set)
			if (MathUtils.distance(v, cp) < LocalMap.CLUSTER_DISTANCE_THRESHOLD)
				return true;

		return false;
	}

	/** Get the position of the cache point in robot-coordinates. */
	private Vec2 getCachePointInRobotCoords(int puckType) {
		assert robotPose != null;
		Pose cpPose = cachePoints[puckType];
		
		double d = Pose.getDistanceBetween(robotPose, cpPose);
		double alpha = Pose.getAngleFromTo(robotPose, cpPose);
		
		return new Vec2((float) (d * (float) Math.cos(alpha)),
							 	     (float) (d * (float) Math.sin(alpha)));
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
		
		for (int k=0; k<SensedType.NPUCK_COLOURS; k++)
			if (cachePoints[k] != null) {
				Vec2 v = new Vec2((float) cachePoints[k].getX(), (float) cachePoints[k].getY());
				debugDraw.drawCircle(v, 5, robotColor);
				debugDraw.drawSolidCircle(v, 5, null, SensedType.getPuckType(k).color3f);
			}
	}
	
	public String getInfoString() {
		return "CacheCons: " + state + ", sizes: " + Arrays.toString(rememberedCacheSizes);
	}
}
