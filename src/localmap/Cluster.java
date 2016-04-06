package localmap;

import java.util.Set;

import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jgraph.graph.DefaultEdge;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.UndirectedSubgraph;

public class Cluster {
	public Vec2 centroid;
	public int size;
	public int puckType;
	public UndirectedSubgraph<Vec2, DefaultEdge> subgraph;

	public Cluster(Vec2 centroid, int size, int k, 
				   UndirectedSubgraph<Vec2, DefaultEdge> subgraph) {
		this.centroid = centroid;
		this.size = size;
		this.puckType = k;
		this.subgraph = subgraph;
	}
	
	/**
	 * Construct a cluster that consists only of a single puck.
	 */
	public Cluster(Vec2 centroid, int k) {
		this.centroid = centroid;
		this.size = 1;
		this.puckType = k;
		
		UndirectedGraph<Vec2, DefaultEdge> g =
	            new SimpleGraph<Vec2, DefaultEdge>(DefaultEdge.class);
		g.addVertex(centroid);

		this.subgraph =
				new UndirectedSubgraph<Vec2, DefaultEdge>(g, null, null);
	}
	
	/**
	 * Return true if this cluster is a neighbour to the given cluster.  A pair of clusters are
	 * neighbours if any member of one lies within the threshold distance of another.  In
	 * general this will not be the case for clusters of the same type (because they would
	 * have been extracted as a single cluster).  However, clusters of different types may 
	 * be neighbours.
	 */
	public boolean isNeighbourTo(Cluster other) {
		Set<Vec2> thisSet = subgraph.vertexSet();
		Set<Vec2> otherSet = other.subgraph.vertexSet();
		for (Vec2 thisPuck : thisSet)
			for (Vec2 otherPuck : otherSet)
				if (MathUtils.distance(thisPuck, otherPuck) < LocalMap.CLUSTER_DISTANCE_THRESHOLD)
					return true;
		return false;
	}
}