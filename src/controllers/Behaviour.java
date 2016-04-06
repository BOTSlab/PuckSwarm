package controllers;

public interface Behaviour extends Controller {
	/**
	 * Assuming this behaviour was not active in the last time step, determine
	 * if it is now ready to be activated.
	 */
	public boolean readyToStart();

	/**
	 * Assuming this behaviour was active in the last time step, determine if it
	 * is ready to continue as the active behaviour.
	 */
	public boolean readyToContinue();

	/**
	 * Is this behaviour willing to give up control. Some behaviours may want to
	 * maintain control for a fixed period of time.
	 */
	public boolean willingToGiveUp();

	/**
	 * If this behaviour has just given up control (by returning willingToGiveUp
	 * as true) then it may select another Behaviour to become active
	 * automatically. If so that behaviour is returned from this method. If null
	 * is returned, there is considered to be no designated follow up behaviour.
	 */
	public Behaviour deactivate();

	/**
	 * Sets the behaviour to be active. This will be called only for a newly
	 * active behaviours. That is, if behaviour b was active during the last
	 * time step, its activate method will not be called during this time step.
	 */
	public void activate();
}
