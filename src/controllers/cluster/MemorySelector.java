package controllers.cluster;

import localmap.Cluster;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * Monitor the size of the largest cluster seen.  Deposit only if a cluster is
 * at least of this size.  Pickup from the smallest cluster in view whose size
 * is smaller than the largest seen.
 */
public class MemorySelector extends ClusterTargetSelector {

	private float largestSeen;
	
	public float DECAY;
	
	public MemorySelector(float DECAY) {
		this.DECAY = DECAY;
		largestSeen = 0;
	}
	
	@Override
	public void inspect() {
		// Update largestSeen
		largestSeen *= DECAY;
		if (largestSeen < 1)
			largestSeen = 1;
		for (Cluster cluster : clusters)
			if (cluster.size > largestSeen)
				largestSeen = cluster.size;
	}

	@Override
	protected Cluster selectCluster(boolean preferLargest) {

		if (clusters.size() == 0)
			return null;

		if (preferLargest) {
			// The only possible target is the largest cluster in view with 
			// size >= largestSeen
			Cluster largestCluster = null;
			for (Cluster cluster : clusters)
				if (cluster.size >= largestSeen)
					largestCluster = cluster;
			return largestCluster; // (Could be null)
		} else {
			// Determine the smallest cluster in view smaller than largestSize.
			Cluster smallestCluster = null;
			int smallestSize = (int) Math.ceil(largestSeen);
			for (Cluster cluster : clusters)
				if (cluster.size < smallestSize) {
					smallestSize = cluster.size;
					smallestCluster = cluster;
				}
			return smallestCluster; // (Could be null)
		}
	}

	@Override
	public String getStatusString() {
		return "largestSeen: " + largestSeen;
	}
}
