package controllers;

import java.awt.Color;
import java.awt.Point;
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

import sensors.Localizer;
import sensors.Odometer;
import sensors.Odometer.Mode;
import sensors.SensedType;
import sensors.SimSuite;
import sensors.StringOverlay;
import sensors.Suite;
import utils.AngleUtils;
import arena.MessageBoard;
import controllers.cluster.ClusterTargetSelector;
import controllers.cluster.ExtremaSelector;
import controllers.cluster.MemorySelector;
import controllers.cluster.PieSelector;
import experiment.Experiment;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * Home Point Metaclustering as described in SI 2012 paper.
 */
public class HPMBehaviour implements Behaviour, PropertiesListener {
	
	enum State {
		
		// Look for pucks to collect.
		PU_SCAN,
		
		// A searcher that is engaged in picking up a puck.  If successful it
		// transitions to a carrier.  If not, it goes back to scanning.
		PU_TARGET,
		
		// A carrier returns home to place its puck.
		HOMING,

		// Puck is being pushed into final position.
		DE_PUSH,
		
		// We have just deposited and and are now backing up.
		DE_BACKUP,
		
		// Move away from the home to encourage picking up non-home cluster pucks.
		EXILE,
		
		// A state entered if we inadvertently pick up a puck of a colour that has not been
		// Initialised.  The action is just to back up for SPIT_OUT_TIME.
		SPIT_OUT};
	State state = State.PU_SCAN;
	
	Cluster target;
	
	// For selecting pick-up targets
	ClusterTargetSelector targetSelector;
	
	// We use a separate (but identically initialised) home point selectors for each color
	ClusterTargetSelector[] homeSelectors = new ClusterTargetSelector[SensedType.NPUCK_COLOURS], 
			initialHomeSelectors = new ClusterTargetSelector[SensedType.NPUCK_COLOURS];
	
	// An array of localizers --- one per puck colour.  The entries will remain null until
	// an initial home point is selected for each puck colour.
	Odometer[] localizers = new Odometer[SensedType.NPUCK_COLOURS];

	// Stores the type of puck most recently carried.  This is needed in EXILE state to 
	// determine which localizer's origin we should flee from.
	SensedType lastCarriedType;
	
	MovementCommand movement;
	
	// The value of stepCount upon activation.
	int stepCount, startCount;
	
	// Variables associated with the wandering aspect of this behaviour.
	VFHPlus vfh = null;	
	Random random;
	
	//
	// The following properties are set via the current Experiment...
	//
	
	// Name of the class to be used to select clusters as targets.
	String CLUSTER_TARGET_SELECTOR;
	
	// Name of the class to be used to select new home areas.
	String HOME_TARGET_SELECTOR;
	
	String INITIAL_HOME_TARGET_SELECTOR;
	
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

	// The minimum distance required between home points.
	float HOME_SEPARATION_DISTANCE;
	
	public HPMBehaviour() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
	}
	
	@Override
	public void propertiesUpdated() {
		Experiment e = ExperimentManager.getCurrent();
		CLUSTER_TARGET_SELECTOR = e.getProperty(
				"HPMBehaviour.CLUSTER_TARGET_SELECTOR", "ExtremaSelector", this);
		HOME_TARGET_SELECTOR = e.getProperty(
				"HPMBehaviour.HOME_TARGET_SELECTOR", "ExtremaSelector", this);
		INITIAL_HOME_TARGET_SELECTOR = e.getProperty(
				"HPMBehaviour.INITIAL_HOME_TARGET_SELECTOR", "ExtremaSelector", this);
		PICK_UP_TIME = e.getProperty(
				"HPMBehaviour.PICK_UP_TIME", 100, this);
		PUSH_IN_TIME = e.getProperty(
				"HPMBehaviour.PUSH_IN_TIME", 3, this);
		BACKUP_TIME = e.getProperty(
				"HPMBehaviour.BACKUP_TIME", 10, this);
		PREDICTION_DISTANCE_SQD = e.getProperty(
				"HPMBehaviour.PREDICTION_DISTANCE_SQD", 10f*10f, this);
		WANDER_ST_DEV = e.getProperty(
				"HPMBehaviour.ST_DEV", 0.5f, this);
		EXILE_TIME = e.getProperty(
				"HPMBehaviour.EXILE_TIME", 50, this);
		SPIT_OUT_TIME = e.getProperty(
				"HPMBehaviour.SPIT_OUT_TIME", 20, this);
		HOME_SEPARATION_DISTANCE = e.getProperty(
				"HPMBehaviour.HOME_SEPARATION_DISTANCE", 50f, this);

		if (CLUSTER_TARGET_SELECTOR.equals("PieSelector")) {
			float K1 = ExperimentManager.getCurrent().getProperty("HPMBehaviour.K1", 0.2f, this);
			float K2 = ExperimentManager.getCurrent().getProperty("HPMBehaviour.K2", 3f, this);
			targetSelector = new PieSelector(K1, K2);
		} else if (CLUSTER_TARGET_SELECTOR.equals("ExtremaSelector")) {
			float K1 = ExperimentManager.getCurrent().getProperty("HPMBehaviour.K1", 1f, this);
			float K2 = ExperimentManager.getCurrent().getProperty("HPMBehaviour.K2", 5f, this);
			targetSelector = new ExtremaSelector(K1, K2);
		} else if (CLUSTER_TARGET_SELECTOR.equals("MemorySelector")) {
			float DECAY = ExperimentManager.getCurrent().getProperty("HPMBehaviour.DECAY", 1, this);
			targetSelector = new MemorySelector(DECAY);
		} else {
			System.err.println("Invalid setting for CLUSTER_TARGET_SELECTOR");
			System.exit(-1);
		}

		if (HOME_TARGET_SELECTOR.equals("ExtremaSelector")) {
			float K3 = ExperimentManager.getCurrent().getProperty("HPMBehaviour.K3", 100f, this);
			for (int k=0; k<SensedType.NPUCK_COLOURS; k++)
				homeSelectors[k] = new ExtremaSelector(0, K3);
		} else if (HOME_TARGET_SELECTOR.equals("MemorySelector")) {
			float DECAY = ExperimentManager.getCurrent().getProperty("HPMBehaviour.DECAY", 1, this);
			for (int k=0; k<SensedType.NPUCK_COLOURS; k++)
				homeSelectors[k] = new MemorySelector(DECAY);
		} else {
			System.err.println("Invalid setting for HOME_TARGET_SELECTOR");
			System.exit(-1);
		}

		if (INITIAL_HOME_TARGET_SELECTOR.equals("ExtremaSelector")) {
			float INITIAL_K3 = ExperimentManager.getCurrent().getProperty("HPMBehaviour.INITIAL_K3", 10f, this);
			for (int k=0; k<SensedType.NPUCK_COLOURS; k++)
				initialHomeSelectors[k] = new ExtremaSelector(0, INITIAL_K3);
		} else if (INITIAL_HOME_TARGET_SELECTOR.equals("MemorySelector")) {
			float INITIAL_DECAY = ExperimentManager.getCurrent().getProperty("HPMBehaviour.INITIAL_DECAY", 1, this);
			for (int k=0; k<SensedType.NPUCK_COLOURS; k++)
				initialHomeSelectors[k] = new MemorySelector(INITIAL_DECAY);
		} else {
			System.err.println("Invalid setting for INITIAL_HOME_TARGET_SELECTOR");
			System.exit(-1);
		}
	}
	
	/// Transition to a new state and post a message.
	private void transition(State newState, String extraMessage) {
		MessageBoard.getMessageBoard().post("transition: " + state + " -> " + newState + 
				" (" +	 extraMessage + ")");
		state = newState;
		startCount = stepCount;
	}
	
	private void transition(State newState) {
		transition(newState, "");
	}
	
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		LocalMap localMap = suite.getLocalMap();
		if (vfh == null)
			vfh = new VFHPlus(localMap, true);
		boolean carrying = localMap.isCarrying();
		SensedType carriedType = localMap.getCarriedType();
		this.stepCount = stepCount;
		
		// Allow the home selectors to have a look at the clusters of each type in the local map
		// and potentially select a new or initial home point for each type.
		for (int k=0; k<SensedType.NPUCK_COLOURS; k++) {
			Cluster newHomeCluster = null;
			if (localizers[k] == null) {
				initialHomeSelectors[k].inspect(localMap, localMap.getClustersOfType(k), carrying, carriedType);
				newHomeCluster = initialHomeSelectors[k].selectTarget(true, false);
				/*
				// Check if this new home point (if there is one---that is if newHomeCluster is
				// not null) is within the threshold distance of any other home point.
				for (int otherK=0; newHomeCluster != null && otherK<SensedType.NPUCK_COLOURS; otherK++)
					if (k != otherK && localizers[otherK] != null) {
						Vec2 otherHome = getHomePositionInRobotCoords(otherK);
						if (MathUtils.distance(newHomeCluster.centroid, otherHome) < HOME_SEPARATION_DISTANCE)
							// The proposed new home point is too close to another of a different type.
							newHomeCluster = null;
					}
				*/
			} else {
				homeSelectors[k].inspect(localMap, localMap.getClustersOfType(k), carrying, carriedType);
				newHomeCluster = homeSelectors[k].selectTarget(true, false);
			}
			if (newHomeCluster != null)
				initializeHomePoint(suite, newHomeCluster);	
		}

		// Mask puck colours that don't yet have a corresponding home point.  This prevents
		// them from being selected as targets for pick-up.
		for (int k=0; k < SensedType.NPUCK_COLOURS; k++)
			if (localizers[k] == null)
				localMap.postFilterClusters(SensedType.getPuckType(k));

		targetSelector.inspect(localMap, localMap.getFilteredClusters(), carrying, carriedType);
		
		//
		// Handle state transitions...
		//
		if (carrying && localizers[SensedType.getPuckIndex(carriedType)] == null)
			transition(State.SPIT_OUT, "Picked up an uninitialized color!");
		else {
			switch (state) {
				case PU_SCAN:
					if (carrying) {
						transition(State.HOMING, "Somehow acquired puck!");
					} else {
						target = targetSelector.selectTarget(carrying, !carrying);
						if (target != null) {
							if (localizers[target.puckType] == null)
								// A home point hasn't yet been selected for this type of puck.
								target = null;
							else if (homeInTargetCluster(localMap, target))
								// The home lies inside the targeted cluster.  That's
								// not allowed so we will keep scanning.
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
						if (target != null && localizers[target.puckType] == null)
							// A home point hasn't yet been selected for this type of puck.
							target = null;
						if (target == null)
							// Give up on this pick-up attempt.
							transition(State.PU_SCAN, "Lost target");
						else if (homeInTargetCluster(localMap, target)) {
							// The home lies in the target cluster.  Maybe this
							// wasn't true when we entered PU_TARGET but it is now.
							target = null;
							transition(State.PU_SCAN, "Home lies in target cluster");
						} else if (stepCount - startCount > PICK_UP_TIME)
							// Give up on this pick-up attempt.
							transition(State.PU_SCAN, "Time out");
					}
					break;
				case HOMING:
					if (!carrying)
						transition(State.PU_SCAN, "Somehow lost puck!");
					else if (carriedPuckInHomeCluster(localMap))
						transition(State.DE_PUSH);
					break;
				case DE_PUSH:
					if (stepCount - startCount >= PUSH_IN_TIME)
						transition(State.DE_BACKUP);
					break;
				case DE_BACKUP:
					if (stepCount - startCount >= BACKUP_TIME) {
						((SimSuite) suite).depositCycles++;
						transition(State.EXILE);
					} 
					break;
				case EXILE:
					if (stepCount - startCount >= EXILE_TIME)
						transition(State.PU_SCAN);
					break;
				case SPIT_OUT:
					if (stepCount - startCount >= SPIT_OUT_TIME)
						transition(State.PU_SCAN);
					break;
			}
		}
		
		//
		// Set forwards and turnAngle based on state.
		//
		if (state == State.PU_SCAN) {
			// Wander!
			float randomTurn = WANDER_ST_DEV * (float) random.nextGaussian();
			movement = MovementUtils.applyVFH(vfh, localMap, randomTurn);					

		} else if (state == State.PU_TARGET) {
			movement = new MovementCommand(1, 
					(float) Math.atan2(target.centroid.y, target.centroid.x));
			Point tp = localMap.getGridPoint(target.centroid.x, target.centroid.y);
			localMap.getOccupancy().addOverlay(new StringOverlay(tp.x, tp.y, "X", Color.black));
			
		} else if (state == State.HOMING) {		
			float errorAngle = localizers[SensedType.getPuckIndex(carriedType)].getHomeAngle();
			
			// Prevent avoidance of the home cluster by removing all of the carried puck's type
			// from the occupancy grid.
			localMap.postFilterOccupancy(carriedType);
			
			movement = MovementUtils.applyVFH(vfh, localMap, errorAngle);					

		} else if (state == State.DE_PUSH) {
			movement = new MovementCommand(1, 0);
			
		} else if (state == State.DE_BACKUP) {
			movement = new MovementCommand(-1, 0);
						
		} else if (state == State.EXILE) {			
			float errorAngle = localizers[SensedType.getPuckIndex(lastCarriedType)].getHomeAngle();
			// Reverse this angle
			errorAngle = (float) AngleUtils.constrainAngle(errorAngle + Math.PI);
			movement = MovementUtils.applyVFH(vfh, localMap, errorAngle);
			
		}  else if (state == State.SPIT_OUT) {
			float randomTurn = WANDER_ST_DEV * (float) random.nextGaussian();
			movement = new MovementCommand(-1, randomTurn);
		}
		
		if (carrying)
			lastCarriedType = carriedType;
	}

	private void initializeHomePoint(Suite suite, Cluster newHomeCluster) {
		int type = newHomeCluster.puckType;
		MessageBoard.getMessageBoard().post("New home!");
		if (localizers[type] == null) {
			Odometer odo = new Odometer(Mode.IDEAL);
			localizers[type] = odo;
			suite.addLocalizerUpdater(odo, SensedType.getPuckColorName(type));
		}
		localizers[type].resetTo(newHomeCluster.centroid);
	}

	private boolean homeInTargetCluster(LocalMap localMap, Cluster targetCluster) {
		Vec2 home = getHomePositionInRobotCoords(targetCluster.puckType);

		// Now see if any pucks in the target cluster lie within threshold
		// distance of the home position.
		Set<Vec2> set = targetCluster.subgraph.vertexSet();
		for (Vec2 v : set)
			if (MathUtils.distance(v, home) < LocalMap.CLUSTER_DISTANCE_THRESHOLD)
				return true;

		return false;
	}

	private boolean carriedPuckInHomeCluster(LocalMap localMap) {
		Cluster carriedCluster = localMap.getCarriedCluster();
		if (carriedCluster == null)
			// Should only be the case if another robot is nearby, causing
			// this robot's carried cluster to be filtered out.
			return false;
				
		Vec2 home = getHomePositionInRobotCoords(SensedType.getPuckIndex(localMap.getCarriedType()));
		
		// Now see if any pucks in the carried cluster lie within threshold
		// distance.
		Set<Vec2> set = carriedCluster.subgraph.vertexSet();
		for (Vec2 v : set)
			if (MathUtils.distance(v, home) < LocalMap.CLUSTER_DISTANCE_THRESHOLD)
				return true;

		return false;
	}

	/// Get the home position in robot-coordinates.
	private Vec2 getHomePositionInRobotCoords(int puckType) {
		Localizer localizer = localizers[puckType];
		
		float xo = localizer.getX();
		float yo = localizer.getY();
		float d = (float) Math.sqrt(xo*xo + yo*yo);
		float alpha = (float) Math.atan2(yo, xo);
		float phi = (float)(alpha + Math.PI - localizer.getTheta());
		Vec2 home = new Vec2(d * (float) Math.cos(phi),
							 d * (float) Math.sin(phi));
		return home;
	}
	
	@Override
	public boolean readyToStart() {
		return true;
	}

	@Override
	public boolean readyToContinue() {
		return true;
	}

	@Override
	/// Expect this to be the only behaviour.
	public boolean willingToGiveUp() {
		return false;
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
			if (localizers[k] != null) {
				debugDraw.drawCircle(localizers[k].getHomeTransform().position, 5, robotColor);
				debugDraw.drawSolidCircle(localizers[k].getHomeTransform().position, 5, null, SensedType.getPuckType(k).color3f);
			}
	}
	
	public String getInfoString() {
		return "MetaCluster: " + state + ", " + targetSelector.getStatusString();
	}

	@Override
	public void storeFinalStatistics(String filename) {
		// TODO Auto-generated method stub
		
	}	
}
