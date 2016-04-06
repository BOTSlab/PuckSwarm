package controllers.cluster;

import java.util.Random;

import localmap.Cluster;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * Each visible cluster is assigned a probability of selection based on
 * Deneubourg et al's formulae.  These probabilities, along with the probability
 * of no selection, are arranged in a conceptual pie graph.  A single random
 * number is then used to select the corresponding "slice".
 */
public class PieSelector extends ClusterTargetSelector {

	Random random;

	// Parameters governing the probability of pick-up / deposit.
	public float K1, K2;
		
	public PieSelector(float K1, float K2) {
		this.K1 = K1;
		this.K2 = K2;
		random = ExperimentManager.getCurrent().getRandom();
	}
	
	@Override
	public void inspect() {
	}

	@Override
	protected Cluster selectCluster(boolean preferLargest) {
		
		float r = random.nextFloat();
		float probSoFar = 0;
		for (Cluster cluster : clusters) {
			probSoFar += getProb(cluster, preferLargest, K1, K2);
			if (r <= probSoFar)
				// Select this cluster!
				return cluster;
		}
		
		return null;
	}
	
	@Override
	public String getStatusString() {
		return "";
	}
}
