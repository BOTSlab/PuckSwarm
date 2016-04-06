package localmap;

import arena.Robot;

public class MovementCommand {
	/// A value in the range [-Robot.MAX_FORWARDS, Robot.MAX_FORWARDS] indicating 
	/// commanded forward speed (negative values indicate travelling in reverse.
	private float forwards;
	
	/// A value in the range [-Robot.MAX_TORQUE, Robot.MAX_TORQUE] indicating 
	/// the commanded torque.
	private float torque;
	
	/**
	 * Constructor.
	 * 
	 * @param forwardSpeed Desired forward speed in the range [-1, 1].
	 * @param turnAngle Desired angle to turn towards in radians.
	 */
	public MovementCommand(float forwardSpeed, float turnAngle) {
		this.forwards = Robot.MAX_FORWARDS * forwardSpeed * MovementUtils.getSpeedScale(turnAngle);
		this.torque = Robot.MAX_TORQUE * MovementUtils.getTorqueScale(turnAngle);
	}

	public float getForwards() {
		return forwards;
	}

	public float getTorque() {
		return torque;
	}
}

