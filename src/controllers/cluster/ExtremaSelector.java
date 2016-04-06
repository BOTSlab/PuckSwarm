package controllers.cluster;

import java.util.Random;

import localmap.Cluster;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * Out of the set of visible clusters, the largest (if carrying) or the smallest
 * (if not carrying) is selected and assigned a probability of selection based on
 * Deneubourg et al's formulae.
 */
public class ExtremaSelector extends ClusterTargetSelector {

	Random random;

	// Parameters governing the probability of pick-up / deposit.
	float K1, K2;
	
	// If true, then when we are targeting small clusters, an isolated puck
	// will always be targeted.
	boolean alwaysTargetIsolated;
		
	public ExtremaSelector(float K1, float K2, boolean alwaysTargetIsolated) {
		this.K1 = K1;
		this.K2 = K2;
		this.alwaysTargetIsolated = alwaysTargetIsolated;
		random = ExperimentManager.getCurrent().getRandom();
	}
	
	public void inspect() {
	}
		
	@Override
	protected Cluster selectCluster(boolean preferLargest) {
		if (clusters.size() == 0)
			return null;
		
		Cluster extremeCluster = clusters.get(0);
		for (Cluster cluster : clusters) {
			if (preferLargest && cluster.size > extremeCluster.size)
				extremeCluster = cluster;
			else if (!preferLargest && cluster.size < extremeCluster.size)
				extremeCluster = cluster;
			else if (cluster.size == extremeCluster.size &&
						Math.abs(cluster.centroid.y) < Math.abs(extremeCluster.centroid.y))
				// In the event of ties, choose the cluster closer to the mid-line.
				extremeCluster = cluster;
		}

		if (!preferLargest && alwaysTargetIsolated && extremeCluster.size == 1)
			return extremeCluster;
		
		if (random.nextFloat() < getProb(extremeCluster, preferLargest, K1, K2))
			return extremeCluster;
		else
			return null;
	}
	
	@Override
	public String getStatusString() {
		return "";
	}
}
