package controllers;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;

import sensors.Suite;

public interface Controller {
	/**
	 * Computed the desired movement (to be later retrieved with getForwards
	 * and getTorque).
	 */
	void computeDesired(Suite suite, int stepCount);
	
	/**
	 * \pre 'computeDesired' has already been called.
	 */
	float getForwards();
	
	/**
	 * \pre 'computeDesired' has already been called.
	 */
	float getTorque();

	void draw(Transform robotTransform, Color3f robotColor, DebugDraw debugDraw);

	/*
	 * Return a short string describing what the controller is doing.
	 */
	String getInfoString();
}
