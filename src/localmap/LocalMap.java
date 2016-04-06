package localmap;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;


import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jgraph.graph.DefaultEdge;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.UndirectedSubgraph;

import experiment.ExperimentManager;
import experiment.PropertiesListener;

import sensors.Blob;
import sensors.BlobFinder;
import sensors.Calibration;
import sensors.StringOverlay;
import sensors.Calibration.CalibDataPerPixel;
import sensors.STCameraImage;
import sensors.STImage;
import sensors.SensedType;
import arena.DotPuck;
import arena.Puck;

/**
 * A LocalMap represents information acquired from a single STImage in egocentric
 * coordinates.
 */
public class LocalMap implements PropertiesListener {

	Calibration calib;
	
	// We use an STImage as the occupancy grid to represent.
	STImage occupancy;
	
	// Location of perceived pucks This is an array of ArrayLists --- one for 
	// each puck colour.
	ArrayList<Vec2>[] pucks;
	
	// Whether or not the robot is carrying a puck, the location and
	// colour of that puck, and the cluster that this puck belongs to.
	boolean carrying;
	Vec2 carriedV;
	SensedType carriedType = SensedType.NOTHING;
	Cluster carriedCluster;
		
	// Clusters extracted from the current localMap.  'clusters' will be further filtered
	// by 'filterClusters', but we will retain a copy called 'rawClusters' of the clusters
	// before filtration.
	ArrayList<Cluster> clusters = new ArrayList<Cluster>(),
											rawClusters = new ArrayList<Cluster>();
	
	private int width;
	private int height;
	
	float minXr, maxXr, minYr, maxYr;

	// Resolution of the occupancy grid.  This specifies the square size of
	// each grid cell in ground-plane coordinates.
	public static float CELL_SIZE = 1;
	
	// Threshold distance between pucks for them to be considered part of the
	// same cluster.
//	public static float CLUSTER_DISTANCE_THRESHOLD = 1.5f * Puck.WIDTH;
public static float CLUSTER_DISTANCE_THRESHOLD = 1.5f;
	
	public static final float ROBOT_THRESHOLD_DISTANCE_SQD = (float) Math.pow(10, 2);

	// Parameters whose values are loaded from the current Experiment...
	
	// Thresholds applied to the area of a blob in the gripper hole to determine 
	// if it is carried or not.  We use the LO threshold if we are already
	// carrying and the HI threshold if we are not.
	float CARRY_THRESHOLD_LO, CARRY_THRESHOLD_HI;

	/**
	 * The width and height attributes are initialised based on the camera's
	 * calibration parameters.
	 * @param calib 
	 */
	public LocalMap(Calibration calib) {
		this.calib = calib;
		minXr = Float.POSITIVE_INFINITY;
		maxXr = Float.NEGATIVE_INFINITY;
		minYr = Float.POSITIVE_INFINITY;
		maxYr = Float.NEGATIVE_INFINITY;
		for (int i=0; i<calib.getImageWidth(); i++)
			for (int j=0; j<calib.getImageHeight(); j++) {
				CalibDataPerPixel pixelCalib = calib.getCalibData(i, j);
				if (pixelCalib != null) { 
					if (pixelCalib.Xr*pixelCalib.Xr + pixelCalib.Yr*pixelCalib.Yr > calib.getMaxSensedDistanceSqd())
						continue;

					minXr = Math.min(minXr, pixelCalib.Xr);
					maxXr = Math.max(maxXr, pixelCalib.Xr);
					minYr = Math.min(minYr, pixelCalib.Yr);
					maxYr = Math.max(maxYr, pixelCalib.Yr);
				}
			}
		
		// Note that for height we utilize not the minimum Xr, but 0.
		width = (int) (0.5 + (maxYr - minYr) / CELL_SIZE) + 1;
		height = (int) (0.5 + (maxXr) / CELL_SIZE) + 1;
		
		occupancy = new STImage(width, height);
		
		pucks = (ArrayList<Vec2>[])new ArrayList[SensedType.NPUCK_COLOURS];
		for (int k=0; k<SensedType.NPUCK_COLOURS; k++)
			pucks[k] = new ArrayList<Vec2>();

		propertiesUpdated();
	}
	
	/**
	 * This copy constructor is useful for the purpose of displaying a LocalMap (in a
	 * LocalMapImagePanel).
	 * @param other
	 */
	public LocalMap(LocalMap other) {
		this(other.calib);
		copyFrom(other);
	}

	public void copyFrom(LocalMap other) {
		// Need to copy the following variables
		//STImage occupancy;
		//ArrayList<Vec2>[] pucks;
		//boolean carrying;
		//Vec2 carriedV;
		//SensedType carriedType = SensedType.NOTHING;
		//Cluster carriedCluster;			
		//ArrayList<Cluster> clusters;
		
		for (int i=0; i<width; i++)
			for (int j=0; j<height; j++)
				occupancy.pixels[i][j] = other.occupancy.pixels[i][j];
		
		for (int k=0; k<SensedType.NPUCK_COLOURS; k++) {
			pucks[k].clear();
			pucks[k].addAll(other.pucks[k]);
		}

		carrying = other.carrying;
		carriedV = other.carriedV;
		carriedType = other.carriedType;
		carriedCluster = other.carriedCluster;
		
		clusters.clear();
		clusters.addAll(other.clusters);
	}
	
	@Override
	public void propertiesUpdated() {
		CARRY_THRESHOLD_LO = ExperimentManager.getCurrent().getProperty("LocalMap.CARRY_THRESHOLD_LO", 0.1f, this);
		CARRY_THRESHOLD_HI = ExperimentManager.getCurrent().getProperty("LocalMap.CARRY_THRESHOLD_HI", 0.4f, this);
	}
	
	/*
	@Override
	public Object clone() {
		LocalMap copy = new LocalMap(calib);
		
		if (occupancy != null) {
			copy.occupancy = (STImage) occupancy.clone();
			copy.pucks = pucks.clone();		
			copy.carriedCluster = carriedCluster;
			copy.clusters = (ArrayList<Cluster>) clusters.clone();
		}
		
		return copy;
	}
	*/
	
	public void update(STCameraImage image) {
		// Fill occupancy grid and extract pucks and clusters.
		extractOccupancy(image);
		extractPucks(image);
		clusters.clear();
		for (int k=0; k<SensedType.NPUCK_COLOURS; k++)
			extractClusters(pucks[k], k, clusters);
		
		// Determine the cluster that the carried puck belongs to
		carriedCluster = null;
		if (carrying) {
			for (Cluster cluster : clusters) {
				Set<Vec2> set = cluster.subgraph.vertexSet();
				for (Vec2 v : set)
					if (carriedV.equals(v))
						carriedCluster = cluster;
			}
		}
		
		rawClusters.clear();
		rawClusters.addAll(clusters);

		filterClusters();
		
		occupancy.addOverlay(new StringOverlay(width/4, height - 10, "LocalMap: Carrying: " + carriedType, Color.BLACK));
	}

	private void extractOccupancy(STCameraImage image) {
		occupancy.setAll(SensedType.NOTHING);
		
		for (int i=0; i<image.width; i++)
			for (int j=0; j<image.height; j++) {
				CalibDataPerPixel pixelCalib = calib.getCalibData(i, j);
				if (pixelCalib != null && !pixelCalib.gripperHole) { 
					if (pixelCalib.Xr*pixelCalib.Xr + pixelCalib.Yr*pixelCalib.Yr > calib.getMaxSensedDistanceSqd())
						continue;

					SensedType type = image.pixels[i][j];
					if (type == SensedType.HIDDEN)
						// We don't see HIDDEN things (D-uh!)
						continue;

					Point p = getGridPoint(pixelCalib.Xr, pixelCalib.Yr);
					occupancy.pixels[p.x][p.y] = type;
				}				
		}
	}

	/**
	 * Extract puck centers and place them in 'pucks'.  If we are carrying
	 * a puck then that puck is also added to 'pucks'.
	 */
	@SuppressWarnings("unchecked")
	private void extractPucks(STCameraImage image) {

		boolean lastCarrying = carrying;
		carrying = false;
		carriedType = SensedType.NOTHING;
		
		for (int k=0; k<SensedType.NPUCK_COLOURS; k++) {
			pucks[k].clear();
			
			SensedType puckType = SensedType.getPuckType(k);
			BlobFinder finder = new BlobFinder(image, puckType, 0, image.width-1, 0, image.height-1);
			ArrayList<Blob> blobs = finder.getBlobs();
			
			// Filter distant blobs and those outside the calibrated area.
			Iterator<Blob> it = blobs.iterator();
			while (it.hasNext()) {
				Blob blob = it.next();
				CalibDataPerPixel pixelCalib = calib.getCalibData(blob.getCentreX(), blob.getCentreY());
				if (pixelCalib == null || pixelCalib.Xr*pixelCalib.Xr + pixelCalib.Yr*pixelCalib.Yr > calib.getMaxSensedDistanceSqd())
					it.remove();
			}
			
			// Now check for blobs in the gripper hole.  Note that the presence
			// of a blob in the hole does not indicate yet that it is being
			// carried.  We apply a check for area of this blob further below.
			// In case multiple blobs are found, the largest is set as holeBlob.
			Blob holeBlob = null;
			for (Blob blob : blobs)
				if (calib.getCalibData(blob.getCentreX(), blob.getCentreY()).gripperHole) {
					if (holeBlob == null || blob.getArea() > holeBlob.getArea())
						holeBlob = blob;
				}
			
			// Remove any blobs within the gripper hole, except for the one found
			// above.
			it = blobs.iterator();
			while (it.hasNext()) {
				Blob blob = it.next();
				if (blob == holeBlob)
					continue;
				if (calib.getCalibData(blob.getCentreX(), blob.getCentreY()).gripperHole)
					it.remove();					
			}
			
			if (holeBlob != null) {

				// It is possible that a puck that is partly in the gripper creates
				// two blobs (separated by the gripper body).  If so we remove the
				// non-hole blob that is identified as lying less than half the 
				// puck width to the carried blob.
				CalibDataPerPixel holeCalib = calib.getCalibData(holeBlob.getCentreX(), holeBlob.getCentreY());
				it = blobs.iterator();
				while (it.hasNext()) {
					Blob blob = it.next();
					if (blob == holeBlob)
						continue;
					CalibDataPerPixel thisCalib = calib.getCalibData(blob.getCentreX(), blob.getCentreY());
					float dx = thisCalib.Xr - holeCalib.Xr;
					float dy = thisCalib.Yr - holeCalib.Yr;
					if (Math.sqrt(dx*dx + dy*dy) < DotPuck.INNER_WIDTH)
						it.remove();					
				}
			
				// Check if the hole blob is large enough to be considered carried.
				float holeFraction = holeBlob.getArea() / (float) calib.getNHolePixels();
				if ( (lastCarrying && holeFraction > CARRY_THRESHOLD_LO) ||
					(!carrying && holeFraction > CARRY_THRESHOLD_HI)) {
					carrying = true;
					carriedType = SensedType.getPuckType(k);				
					carriedV = new Vec2(holeCalib.Xr, holeCalib.Yr);
				}
			}
			
			// Finally add all remaining blobs as pucks (including the hole blob
			// whether it is considered carried or not).
			for (Blob blob : blobs) {			
				CalibDataPerPixel pixelCalib = calib.getCalibData(blob.getCentreX(), blob.getCentreY());
				Vec2 v = new Vec2(pixelCalib.Xr, pixelCalib.Yr);
				pucks[k].add(v);
			}			
		}
	}
	
	/**
	 * From 'pucks' extract clusters of pucks and add them to 'clusters'.  A 
	 * cluster is defined as a connected component of the graph formed by the 
	 * pucks.  The nodes of this graph are the pucks represented in 'pucks' 
	 * with an edge defined between any pair that lie within 
	 * CLUSTER_DISTANCE_THRESHOLD of each other.  The type of puck is given by k.
	 */
	public static void extractClusters(ArrayList<Vec2> pucks, int k,
									   					  ArrayList<Cluster> clusters) {
		UndirectedGraph<Vec2, DefaultEdge> g =
	            new SimpleGraph<Vec2, DefaultEdge>(DefaultEdge.class);
		
		// Create nodes which represent the position pairs in 'pucks[k]'.
		for (Vec2 v : pucks)
			g.addVertex(v);
						
		// Add edges by considering all possible pairs of vertices and creating
		// an edge when the two positions lie within CLUSTER_DISTANCE_THRESHOLD
		Set<Vec2> vertexSet = g.vertexSet();
		for (Vec2 vi : vertexSet) {
			for (Vec2 vj : vertexSet) {
				if (!vi.equals(vj)) {
					if (MathUtils.distance(vi, vj) < CLUSTER_DISTANCE_THRESHOLD)
						g.addEdge(vi, vj);
				}
			}
		}

		// Now extract the connected components.
		@SuppressWarnings({ "rawtypes", "unchecked" })
		ConnectivityInspector inspector = new ConnectivityInspector(g);
		@SuppressWarnings("unchecked")
		List<Set<Vec2>> sets = inspector.connectedSets();
		
		// For each connected component, determine the centroid and add the
		// cluster to 'clusters'.
		for (Set<Vec2> set : sets) {
			int size = set.size();
			
			// Compute the centroid.
			Vec2 centroid = new Vec2();
			for (Vec2 v : set)
				centroid.addLocal(v);
			centroid.mulLocal(1f / size);
			
			UndirectedSubgraph<Vec2, DefaultEdge> subgraph =
					new UndirectedSubgraph<Vec2, DefaultEdge>(g, set, null);
					
			Cluster cluster = new Cluster(centroid, size, k, subgraph);
			clusters.add(cluster);
		}
	}
	
	/**
	 * Filter the clusters in rawClusters, adding the acceptable ones to clusters.
	 */
	private void filterClusters() {
		// Remove clusters whose centroid is further than the threshold distance.
		ListIterator<Cluster> it = clusters.listIterator();
		while (it.hasNext()) {
			Cluster cluster = it.next();
			if (cluster.centroid.lengthSquared() > calib.getMaxClusterDistanceSqd())
				it.remove();
		}
				
		// Filter out clusters which are in proximity to other robots.
		for (int i=0; i<occupancy.width; i++)
			for (int j=0; j<occupancy.height; j++) {
				if (occupancy.pixels[i][j] == SensedType.ROBOT) {
					Vec2 robotV = getGroundPlane(i, j);
					it = clusters.listIterator();
					while (it.hasNext()) {
						Cluster cluster = it.next();
						Set<Vec2> set = cluster.subgraph.vertexSet();
						for (Vec2 puckV : set) {
							if (MathUtils.distanceSquared(robotV, puckV) < ROBOT_THRESHOLD_DISTANCE_SQD) {
								it.remove();
								break;
							}
						}
					}
				}
			}
		
		// Filter out other infeasible clusters.  A cluster may be infeasible
		// for any of the following reasons:
		// - The cluster of the carried puck
		// - Unreachable clusters.
		// - If the robot is carrying, then clusters of a different type.
		// - If the robot is carrying, then any cluster with a puck lying closer than the
		//    threshold distance to a cluster of a different colour.
		it = clusters.listIterator();
		while (it.hasNext()) {
			Cluster cluster = it.next();
			
			boolean remove = false;
			if (carrying && cluster == carriedCluster)
				remove = true;
			else if (!isReachable(cluster.centroid))
				remove = true;
			else if (carrying && SensedType.getPuckIndex(carriedType) != cluster.puckType)
				remove = true;
			else if (carrying) {
				// Go through all of the original list of clusters and see if any have a different
				// colour and are neighbouring.  If so, then both clusters should be filtered out.
				// BAD: Should remove both at once, but as it stands the loop below will run
				// for both clusters.
				for (Cluster other : clusters) {
					if (cluster != other && cluster.puckType != other.puckType && cluster.isNeighbourTo(other)) {
						remove = true;
						break;
					}
				}
			}
			
			if (remove)
				it.remove();
		}
	}
	
	/**
	 * Converts ground-plane coordinates (Xr, Yr) to a pair of integer coordinates
	 * on the occupancy grid.
	 */
	public Point getGridPoint(float Xr, float Yr) {
		int i = (int)(0.5 + (maxYr - Yr) / CELL_SIZE);
		int j = (int)(0.5 + (Xr) / CELL_SIZE);
		return new Point(i, j);
	}

	/**
	 * Converts grid coordinates (i, j) to ground-plane coordinates (Xr, Yr).
	 */
	public Vec2 getGroundPlane(int i, int j) {
		return new Vec2(CELL_SIZE * j, maxYr - CELL_SIZE * i);
	}

	public STImage getOccupancy() {
		return occupancy;
	}
	
	public ArrayList<Cluster> getFilteredClusters() {
		return clusters;
	}
	
	public boolean isCarrying() {
		return carrying;
	}
	
	public SensedType getCarriedType() {
		return carriedType;
	}
	
	public Cluster getCarriedCluster() {
		return carriedCluster;
	}	

	/**
	 * Return a cluster containing just the closest puck to 'v' subject to the given
	 * squared distance threshold.  Return null if there is no puck within 
	 * threshold distance.
	 */
	public Cluster getClosestPuckToPositionAsCluster(Vec2 v, float sqdDistanceThreshold) {
		// We could select the closest puck in 'pucks' but these have not been
		// filtered for proximity to other robots (if we filtered the list of
		// pucks then other robots could mask out parts of clusters, not entire
		// clusters as we are currently doing).  So instead we look for the
		// closest individual puck that belongs to a cluster.
		Vec2 closest = null;
		int closestType = -1;
		float smallestDistSqd = Float.POSITIVE_INFINITY;
		for (Cluster cluster : clusters) {
			Set<Vec2> vertexSet = cluster.subgraph.vertexSet();
			
			for (Vec2 puckV : vertexSet) {
				float ds = MathUtils.distanceSquared(v, puckV);
				if (ds < sqdDistanceThreshold && ds < smallestDistSqd) {
					smallestDistSqd = ds;
					closest = puckV;
					closestType = cluster.puckType;
				}
			}
		}
		if (closest != null)
			return new Cluster(closest, closestType);
		else
			return null;
	}

	/**
	 * As above only applied on cluster centroids.  The argument 'excludeCarried'
	 * implies that we should not consider the cluster that the carried puck
	 * belongs to.
	 */
	public Cluster getClosestClusterToPosition(Vec2 v, float sqdDistanceThreshold, boolean excludeCarried) {
		Cluster closest = null;
		float smallestDistSqd = Float.POSITIVE_INFINITY;
		for (Cluster cluster : clusters) {
			if (cluster == carriedCluster)
				// We cannot select the cluster of the carried puck itself.
				continue;

			float ds = MathUtils.distanceSquared(v, cluster.centroid);
			if (ds < sqdDistanceThreshold && ds < smallestDistSqd) {
				smallestDistSqd = ds;
				closest = cluster;
			}
		}
		return closest;
	}

	/**
	 * Get the cluster that contains the given position.  Return null if there
	 * is no such cluster.  "Containing" means that one of the pucks within
	 * a cluster lies within the threshold distance to the given position.
	 */
	public Cluster getContainingCluster(Vec2 v) {
		for (Cluster cluster : clusters) {
			Set<Vec2> set = cluster.subgraph.vertexSet();
			for (Vec2 cv : set)
				if (MathUtils.distance(v, cv) < LocalMap.CLUSTER_DISTANCE_THRESHOLD)
					return cluster;
		}
		return null;
	}

	/**
	 * Determine if the given position is reachable.
	 */
	public boolean isReachable(Vec2 v) {
		double circleYr = 12.5; // Approximate turning radius of SRV-1 
		double circleXr = 0; // Camera lies 6.5cm forward of robot's centre.
		double radius = circleYr + 0;
		
		double dXr = v.x - circleXr;
		// Get minimum difference in Yr between either left or right circle.
		double dYr = Math.min(Math.abs(circleYr - v.y), Math.abs(circleYr + v.y));
		
		return Math.sqrt(dXr*dXr + dYr*dYr) > radius;
	}
	
	/**
	 * Determine which half of the occupancy grid is more free.  Return 1 if
	 * the left is more free or equally free.  Otherwise return -1.  Pucks are 
	 * ignored in this process.
	 */
	public int getFreeerSide() {
		int w = occupancy.width;
		int w_2 = occupancy.width / 2;
		int h = occupancy.height;
		int leftSum = 0, rightSum = 0;
		for (int i=0; i<w_2; i++)
			for (int j=0; j<h; j++) {
				SensedType type = occupancy.pixels[i][j];
				if (type == SensedType.ROBOT || type == SensedType.WALL)
					leftSum++;
			}
		for (int i=w_2; i<w; i++)
			for (int j=0; j<h; j++) {
				SensedType type = occupancy.pixels[i][j];
				if (type == SensedType.ROBOT || type == SensedType.WALL)
					rightSum++;
			}
		
		if (leftSum <= rightSum)
			return 1;
		else
			return -1;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/**
	 * Remove detected clusters of the given type.  This is intended to be called after update.
	 */
	public void postFilterClusters(SensedType puckType) {
		ListIterator<Cluster> it = clusters.listIterator();
		while (it.hasNext()) {
			Cluster cluster = it.next();
			if (SensedType.getPuckType(cluster.puckType) == puckType)
				it.remove();
		}
	}

	/**
	 * Remove entries of the given type from the occupancy grid.
	 */
	public void postFilterOccupancy(SensedType type) {
		for (int i=0; i<width; i++)
			for (int j=0; j<height; j++)
				if (occupancy.pixels[i][j] == type)
					occupancy.pixels[i][j] = SensedType.NOTHING;
	}

	public ArrayList<Cluster> getClustersOfType(int k) {
		ArrayList<Cluster> rightClusters = new ArrayList<Cluster>();
		for (Cluster c : clusters)
			if (c.puckType == k)
				rightClusters.add(c);
		return rightClusters;
	}
	
	public ArrayList<Cluster> getUnfilteredClustersOfType(int k) {
		ArrayList<Cluster> rightClusters = new ArrayList<Cluster>();
		for (Cluster c : rawClusters)
			if (c.puckType == k)
				rightClusters.add(c);
		return rightClusters;
	}	
	
	/**
	 * Get a vector to the closest robot---actually the closest ROBOT
	 * cell in the occupancy grid.  Return null if there is no robot in sight.
	 */
	public Vec2 getClosestRobotVec2() {
		float closestDistance = Float.MAX_VALUE;
		Vec2 closestRobot = null;
		for (int i=0; i<occupancy.width; i++)
			for (int j=0; j<occupancy.height; j++) {
				if (occupancy.pixels[i][j] == SensedType.ROBOT) {
					Vec2 vec = getGroundPlane(i, j);
					if (vec.length() < closestDistance) {
						closestDistance = vec.length();
						closestRobot = vec;
					}
				}
			}
		return closestRobot;
	}
}
