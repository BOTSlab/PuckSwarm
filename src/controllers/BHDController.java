package controllers;

import java.io.File;
import java.util.Random;

import localmap.LocalMap;
import localmap.MovementCommand;
import localmap.VFHPlus;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;

import sensors.Suite;
import utils.FileUtils;
import arena.MessageBoard;
import arena.Robot;
import experiment.Experiment;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * Benchmark clustering algorithm for SI 2012 paper.  This state-machine emulates the
 * simple clustering behaviour described in:
 * 
 * 		Beckers, Holland, and Deneubourg, "From Local Actions to Global Tasks: Stigmergy
 *      and Collective Robotics", 1994.
 *      
 * This controller shares much of its design with the more advanced controllers ProbSeek
 * and CacheCons.
 */
public class BHDController implements Controller, PropertiesListener {
	
	enum State {
		FORWARD,
		PUSH,
		BACKUP,
		TURN};
	State state = State.FORWARD;
	
	// Tally of the number of iterations spent in each state over the last
	// suite.getStorageInterval() time steps.
	int[] recentStateCounts = new int[State.values().length];
	
	MovementCommand movement;
	
	// The value of stepCount upon activation.
	int stepCount, startCount;
	
	// VFH+ is used for obstacle avoidance.
	VFHPlus vfh = null;	
	Random random;
	
	// Associated with TURN
	int randomTurnDir, randomTurnTime;
	int TURNTIME_MIN = Robot.ONE_EIGHTY_TIME/2;
	
	//
	// The following properties are set via the current Experiment...
	//
	
	// The amount of time to spend continuing to push after completing a deposit.
	int PUSH_IN_TIME;

	// The amount of time to spend backing up after completing a deposit.
	int BACKUP_TIME;
	
	public BHDController() {
		random = ExperimentManager.getCurrent().getRandom();
		propertiesUpdated();
	}
	
	@Override
	public void propertiesUpdated() {
		Experiment e = ExperimentManager.getCurrent();
		PUSH_IN_TIME = e.getProperty(
				"BHDController.PUSH_IN_TIME", 1, this);
		BACKUP_TIME = e.getProperty(
				"BHDController.BACKUP_TIME", 5, this);
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
		//boolean carrying = localMap.isCarrying();
		this.stepCount = stepCount;
		
		//
		// Handle state transitions...
		//
		switch (state) {
			case FORWARD:
				if (localMap.getCarriedCluster() != null && localMap.getCarriedCluster().size > 1) {
					// We have made contact with a cluster of size 2 or more.
					transition(State.PUSH, "Contacted cluster");
				} else {
					// We use VFH+ only to test whether the path ahead is free.  
					Float result = vfh.computeTurnAngle(localMap, 0, true);
					vfh.updateGUI();
					if (result == null || Math.abs(result.floatValue())  > Math.PI/4) {
						transition(State.TURN, "Obstacle ahead");
						initiateRandomTurn(localMap);
					}
				}
				break;
			case PUSH:
				if (stepCount - startCount >= PUSH_IN_TIME)
					transition(State.BACKUP);
				break;
			case BACKUP:
				if (stepCount - startCount >= BACKUP_TIME) {
					transition(State.TURN);
					initiateRandomTurn(localMap);
				} break;
			case TURN:
				if (stepCount - startCount >= randomTurnTime) {
					transition(State.FORWARD);
				}
				break;
		}
		
		//
		// Set forwards and turnAngle based on state.
		//
		if (state == State.FORWARD) {
			movement = new MovementCommand(1, 0);

		} else if (state == State.PUSH) {
			movement = new MovementCommand(1, 0);
			
		} else if (state == State.BACKUP) {
			movement = new MovementCommand(-1, 0);
			
		} else if (state == State.TURN) {
			movement = new MovementCommand(0, (float) (Math.PI/2 * randomTurnDir));
		}
		
		updateStateCounts(suite);
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
		vfh.draw(robotTransform, debugDraw);
	}
	
	public String getInfoString() {
		return "BHD: " + state;
	}
}
