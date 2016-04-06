package controllers;

import java.util.EnumMap;

import localmap.MovementCommand;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import controllers.ProbSeekController.State;

import arena.Robot;

import sensors.APS;
import sensors.Localizer;
import sensors.Pose;
import sensors.STImage;
import sensors.Odometer;
import sensors.Suite;
import sensors.Odometer.Mode;

/**
 * Execute a simple movement which is useful for comparing the physical robots'
 * motion with that of our simulated robots.  The movement corresponds to a square
 * with side length of approximately 30 cm.
 * 
 * Side note: Experimenting with the use of the State pattern here.  In this case, each
 * state is simple enough to be described using the movement, duration of movement,
 * and the follow-on state for each possible state.  For more advanced implementations
 * one would consider defining subclasses of State, each of which would have some
 * means of accessing the "context" class, which would provide common methods
 * and data members needed across different states.
 * @author av
 *
 */
public class TestMovementController implements Controller {

	// Names of all possible states.
	enum StateName {
		FORWARD_1,
		LEFT_1,
		FORWARD_2,
		LEFT_2,
		FORWARD_3,
		LEFT_3,
		FORWARD_4,
		DONE;
	};
	
	// Characterize each possible state with its movement, duration of movement,
	// and the next state to enter once the movement has completed.
	class State {
		MovementCommand cmd;
		int duration;
		StateName nextStateName;
		
		State(MovementCommand cmd, int duration, StateName nextStateName) {
			this.cmd = cmd;
			this.duration = duration;
			this.nextStateName = nextStateName;
		}
	}
	
	// Dictionary of all possible states.
	EnumMap<StateName, State> stateDict = new EnumMap<StateName, State>(StateName.class);
	
	// The current state.
	State state;
	
	// Time steps since the entry into the current state.
	int stepsInState = 0;

	public TestMovementController() {
		
		MovementCommand straight = new MovementCommand(1, 0);
		MovementCommand left = new MovementCommand(1, (float) Math.PI/2);
		//MovementCommand right = new MovementCommand(1, -(float) Math.PI/2);
		MovementCommand still = new MovementCommand(0, 0);
		int turnDuration = 5;
		int straightDuration = 7;
		
		// Add all possible states to stateDict.
		stateDict.put(StateName.FORWARD_1, 
				new State(straight, straightDuration,	StateName.LEFT_1));
		stateDict.put(StateName.LEFT_1, 
				new State(left, turnDuration, StateName.FORWARD_2));
		stateDict.put(StateName.FORWARD_2, 
				new State(straight, straightDuration, StateName.LEFT_2));
		stateDict.put(StateName.LEFT_2, 
				new State(left, turnDuration, StateName.FORWARD_3));
		stateDict.put(StateName.FORWARD_3, 
				new State(straight, straightDuration, StateName.LEFT_3));
		stateDict.put(StateName.LEFT_3, 
				new State(left, turnDuration, StateName.FORWARD_4));
		stateDict.put(StateName.FORWARD_4, 
				new State(straight, straightDuration, StateName.DONE));
		stateDict.put(StateName.DONE, 
				new State(still, Integer.MAX_VALUE, null));
		
		state = stateDict.get(StateName.FORWARD_1);
	}
	
	@Override
	public void computeDesired(Suite suite, int stepCount) {
		if (stepsInState == state.duration) {
			state = stateDict.get(state.nextStateName);
			stepsInState = 0;
			System.out.println(suite.getAPS().getPose(suite.getRobotName()));
		}
		stepsInState++;
	}

	@Override
	public float getForwards() {
		return state.cmd.getForwards();
	}

	@Override
	public float getTorque() {
		return state.cmd.getTorque();
	}

	@Override
	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
	}

	@Override
	public String getInfoString() {
		return "TestMovementController";
	}
}
