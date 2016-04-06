package arena;

import org.jbox2d.common.Vec2;

/**
 * Singleton to encapsulate global information related to bucket brigading.
 * @author av
 */
public class DistributionTaskManager {

	private Vec2 source;
	private Vec2[] destinations;
	
	private int[] addedCounts;
	private int[] removedCounts;
	
	public static final float ADDITION_PROBABILITY = 0.01f;

	private static DistributionTaskManager instance = null;

	private DistributionTaskManager() {
		// BAD: Constants that appear elsewhere in the code but are difficult to access.
		float arenaWidth =  1.5f*187.0f;
		float arenaHeight = 187.0f;
		int nTypes = 3;		
		
		source = new Vec2(-arenaWidth/ 2f + 15, 0);		

		// Place destinations evenly along the right-hand wall of the arena.
		float x = arenaWidth/2 - 15;
		destinations = new Vec2[nTypes];
		if (nTypes == 1)
			destinations[0] = new Vec2(x, 0);
		else {
			float adjustedHeight = arenaHeight - 25;
			float stepLength = adjustedHeight / (nTypes-1);
			for (int type = 0; type<nTypes; type++) {
				float y = -adjustedHeight/2f + (type) * stepLength;
				destinations[type] = new Vec2(arenaWidth/2 - 15, y);
			}
		}
		
		addedCounts = new int[nTypes];
		removedCounts = new int[nTypes];		
	}

	public static DistributionTaskManager getInstance() {
		if (instance == null)
			instance = new DistributionTaskManager();
		return instance;
	}

	public Vec2 getSource() {
		return source;
	}

	public Vec2[] getDestinations() {
		return destinations;
	}
	
	public void incrementAddedCount(int puckType) {
		addedCounts[puckType]++;
	}
	
	public void incrementRemovedCount(int puckType) {
		removedCounts[puckType]++;
	}
	
	public int[] getAddedCounts() {
		return addedCounts;
	}
	
	public int[] getRemovedCounts() {
		return removedCounts;
	}
	
	public int getTotalAdded() {
		int total = 0;
		for (int count : addedCounts)
			total += count;
		return total;
	}
	
	public int getTotalRemoved() {
		int total = 0;
		for (int count : removedCounts)
			total += count;
		return total;
	}
}