package controllers.cluster;

import java.util.ArrayList;
import java.util.Set;

import localmap.Cluster;
import localmap.LocalMap;

import org.jbox2d.common.Vec2;

import sensors.SensedType;

public abstract class ClusterTargetSelector {
	
	private LocalMap localMap;
	protected ArrayList<Cluster> clusters;

	/**
	 * Do any preprocessing necessary prior to target selection via 
	 * 'selectTarget'.  This method will be called every iteration, as opposed
	 * to 'selectTarget' which is only called when we are ready to home in
	 * on a target.
	 */
	public void inspect(LocalMap localMap, ArrayList<Cluster> clusters, boolean carrying, SensedType carriedColor) {
		this.localMap = localMap;
		this.clusters = clusters;
		inspect();
	}

	/**
	 * This internal method is required to customise pre-processing behaviour.
	 */
	protected abstract void inspect();
	
	/**
	 * Consider the visible clusters and potentially select the one as a target
	 * (could be a target for pick-up, deposit, or as a new home location).  If
	 * preferLargest is true then larger clusters will be selected
	 * in preference to smaller clusters (and vice versa).  If isolatePuck
	 * is true then one puck is selected from the target cluster and this one-puck
	 * cluster is returned.
	 * 
	 * Returns null to indicate no suitable target is in view.  Otherwise the 
	 * return value is a position in robot-centric coordinates.
	 * 
	 * @pre: inspect previously called for the current input.
	 */
	public Cluster selectTarget(boolean preferLargest, boolean isolatePuck) {
		// Apply the method for whatever concrete class this happens to be.
		Cluster targetCluster = selectCluster(preferLargest);
		
		if (targetCluster == null)
			return null;
		else if (!isolatePuck) {
			return targetCluster;
		} else {
			Cluster onePuckCluster = selectPickupPuckCluster(targetCluster);
			if (localMap.isReachable(onePuckCluster.centroid)) {
				return onePuckCluster;
			} else
				return null;
		}
	}

	/**
	 * This internal method is required to customise cluster selection behaviour.
	 * @param preferLargest True if we prefer large clusters as targets
	 */
	protected abstract Cluster selectCluster(boolean preferLargest);
	
	/**
	 * From the given cluster, select the puck which is most promising for
	 * pick-up.  For now we select the left- or right-most puck which has
	 * the smaller degree (in terms of the cluster's graph).  If the degree
	 * is equal the left-most is chosen.  The returned cluster will contain
	 * just the one puck.
	 */
	protected Cluster selectPickupPuckCluster(Cluster cluster) {
		// Select the two pucks with largest and smallest Yr value.
		Set<Vec2> set = cluster.subgraph.vertexSet();
		float largestYr = Float.NEGATIVE_INFINITY;
		float smallestYr = Float.POSITIVE_INFINITY;
		Vec2 largestYrVec = null, smallestYrVec = null;
		for (Vec2 v : set) {
			if (v.y > largestYr) {
				largestYr = v.y;
				largestYrVec = v;
			}
			if (v.y < smallestYr) {
				smallestYr = v.y;
				smallestYrVec = v;
			}
		}
		
		// Of these two, choose the one with the smaller degree.
		if (cluster.subgraph.degreeOf(largestYrVec) <= 
			cluster.subgraph.degreeOf(smallestYrVec))
			return new Cluster(largestYrVec, cluster.puckType);
		else
			return new Cluster(smallestYrVec, cluster.puckType);
	}
	
	/**
	 * Return a string describing something of the selector's internal state.
	 */
	public abstract String getStatusString();
	
	/**
	 * Obtain the probability of selecting the given cluster based on
	 * Deneubourg et al's formulae with the given constants.
	 */
	protected static float getProb(Cluster c, boolean preferLargest, float k1, float k2) {
		if (preferLargest) {
			if (k2 < 0)
				return 1f;
			else
				return Deneubourg.depositProb(c.size, k2);
		} else {
			if (k1 < 0)
				return 1f;
			else
				return Deneubourg.pickupProb(c.size, k1);
		}
	}	
}
