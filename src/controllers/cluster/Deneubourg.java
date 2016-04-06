package controllers.cluster;

/**
 * Used to represent the probabilities for deposit and pickup
 * discussed in "The Dyanmics of Collective Sorting: Robot-like Ants and 
 * Ant-like Robots" Deneubourg et al, 1991.
 */
public class Deneubourg {

	/**
	 * Probability to pick up given local density f.
	 */
	public static float pickupProb(float f, float K1) {
		float p = (K1 / (K1 + f));
		return p * p;
	}

	/**
	 * Probability to deposit given local density f.
	 */
	public static float depositProb(float f, float K2) {
		float p = (f / (K2 + f));
		return p * p;
	}
}
