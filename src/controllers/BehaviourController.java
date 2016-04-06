package controllers;

import java.util.ArrayList;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;

import sensors.Suite;
import utils.RunningStats;

public class BehaviourController implements Controller {
	ArrayList<Behaviour> behaviourList = new ArrayList<Behaviour>();

	Behaviour lastChosen, chosen;

	// Used to track the time spent in each behaviour.
	ArrayList<RunningStats> stats = new ArrayList<RunningStats>();

	// Used to display result in 'draw'.
	private String statsString = "";
	
	/**
	 * Add a new Behaviour. The order in which behaviours are added determines
	 * their level of priority (earliest added = highest priority).
	 */
	public void addBehaviour(Behaviour b) {
		behaviourList.add(b);
		stats.add(new RunningStats());
	}

	/**
	 * From amongst the behaviours that are ready to be active (i.e. they report
	 * isReady = true) choose the highest priority behaviour and compute its
	 * desired movement.
	 */
	public void computeDesired(Suite suite, int stepCount) {
		// Stats will be 1 time step out of date.
		updateStats(stepCount);

		chosen = null;
		if (lastChosen != null) {
			lastChosen.computeDesired(suite, stepCount);
			if (!lastChosen.willingToGiveUp()) {
				//System.out.println("BehaviourController: " + lastChosen.toString() + " not willing to give up!");
				chosen = lastChosen;
				return;
			} else if (lastChosen.deactivate() != null){
				chosen = lastChosen.deactivate();
				//System.out.println("BehaviourController: " + chosen.toString() + " selected as follow up");
				chosen.computeDesired(suite, stepCount);
				chosen.activate();
				lastChosen = chosen;
				return;
			}
		}

		for (Behaviour b : behaviourList) {
			if (b != lastChosen)
				// computeDesired was already called for lastChosen above.
				b.computeDesired(suite, stepCount);
			
			if (b == lastChosen && b.readyToContinue()) {
				chosen = b;
				//System.out.println("BehaviourController: " + chosen.toString() + " ready to continue");
				break;
			} else if (b != lastChosen && b.readyToStart()) {
				chosen = b;
				//System.out.println("BehaviourController: " + chosen.toString() + " activating");
				chosen.activate();
				break;
			}
		}
		
		lastChosen = chosen;
	}

	/**
	 * The only stats we keep track of so far are the proportion of time spent on
	 * each behaviour.  This information goes into 'statsString' which may be
	 * displayed when draw is called.
	 */
	private void updateStats(int stepCount) {
		int n = behaviourList.size();
		
		statsString = "";
		for (int i=0; i<n; i++) {
			RunningStats stat = stats.get(i);
			if (behaviourList.get(i) == chosen)
				stat.push(1);
			else
				stat.push(0);

			statsString += behaviourList.get(i).getInfoString() + ": " + String.format("%.3g", stat.getMean());
			if (i != n - 1)
				statsString += ", ";
		}		
	}

	public float getForwards() {
		if (chosen != null)
			return chosen.getForwards();
		return 0;
	}

	public float getTorque() {
		if (chosen != null)
			return chosen.getTorque();
		return 0;
	}

	public void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw) {
//		Vec2 c = debugDraw.getWorldToScreen(robotTransform.position.add(new Vec2(0, 5)));
//		debugDraw.drawString(c.x, c.y, statsString, Color3f.WHITE);
		
		if (chosen != null)
			chosen.draw(robotTransform, robotColor, debugDraw);
	}
	
	public String getInfoString() {
		if (chosen != null)
			return chosen.getInfoString();
		return "";
	}
}
