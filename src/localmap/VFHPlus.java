package localmap;

import java.awt.Container;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import sensors.STImage;
import sensors.SensedType;
import utils.AngleUtils;
import arena.Grid;
import arena.GridPanel;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * Implementation of VFH+ from "VFH+: Reliable Obstacle Avoidance for Fast Mobile
 * Robots" by Iwan Ulrich and Johann Borenstein, 1998.  References to "the original
 * paper" are to the following:	"The Vector Field Histogram - Fast Obstacle Avoidance for Mobile Robots" by
 * Borenstein and Koren, 1991.
 */
public class VFHPlus implements PropertiesListener {
	
	class CandidateDir {
		CandidateDir(int sector) {
			this.sector = sector;
			assert sector >= 0 && sector < N;
		}
		int sector;
		float cost;
	}
	
	/// Return type for 'checkTargetAngle'
	public class CheckTargetAngleResponse {
		public boolean targetAngleFree;
		public float outputAngle;
	}
	
	// Indicates whether or not we are moving towards a particular goal.  If
	// false, then the target specified
	boolean goalDirected;
	
	// Pre-computed data.
	Grid baseMagnitude, beta, gamma;
	
	// The polar histogram after each stage of processing.
	float[] primaryHistogram, binaryHistogram, maskedHistogram;
	
	// The candidate directions.
	ArrayList<CandidateDir> dirs = new ArrayList<CandidateDir>();
	
	// ...the best one.
	CandidateDir best;
	
	// Debug: histogram to show 1/cost for candidate directions
	float[] costHistogram;
	
	// The last result as a sector index and angle.
	int lastTurnSector;
	float lastTurnAngle;

	// Whether we should ignore pucks (i.e. not bother avoiding them).
	boolean ignorePucks;
	
	JFrame frame;
	GridPanel primaryPanel, binaryPanel, maskedPanel, costPanel;

	public static boolean enableDisplay;

	boolean propertiesUpdated;
	
	// Length of the polar histogram.
	public static int N = 72 + 1; // Make it odd so that the centre sector corresponds to 0
	
	// Angular range employed.
	public static float START_ANGLE = (float) (Math.PI/2);
	public static float STOP_ANGLE = (float) (-Math.PI/2);
	
	// Angular step (i.e. resolution).
	public static float ALPHA = (START_ANGLE - STOP_ANGLE) / (N-1);
	
	public static int ZERO_ANGLE_SECTOR = angleToIndex(0);
	
	// Maximum sensed distance squared (may be less than 
	// LocalMap.MAX_SENSED_DISTANCE_SQD).
	public static float MAX_SENSED_DISTANCE_SQD = (float) Math.pow(100, 2) / 3;
	
	// VFH+ parameters a and b.  Unlike the paper, we set these so that the base
	// magnitude scales down linearly (with distance squared) from 1 to 0 at
	// MAX_SENSED_DISTANCE_SQD.
	public static float A = 1f;
	public static float B = 1f / MAX_SENSED_DISTANCE_SQD;
	
	// Threshold for the width of a narrow/wide threshold.
	public static final int S_MAX = 8;

	// Weights for the candidate direction cost functions
	public static float MU_1; // High for goal-oriented
	public static float MU_2;  // High to prefer small turns
	public static float MU_3;  // High to prefer maintaining the same turn
	
	// Parameters whose values are loaded from the current Experiment...

	private static float SAFETY_DISTANCE_GAMMA, SAFETY_DISTANCE_MASK;

	// Hysteresis thresholds for forming the binary histogram.
	private static float TAU_LO, TAU_HI;
	
	public VFHPlus(LocalMap localMap, boolean goalDirected) {
		this.goalDirected = goalDirected;
		
		if (goalDirected) {
			MU_1 = 5;
			MU_2 = 2;
			MU_3 = 3;
		} else {
			MU_1 = 0;
			MU_2 = 2;
			MU_3 = 3;			
		}
		
		initDataStructures(localMap);
		
		propertiesUpdated();

		enableDisplay = !ExperimentManager.isActive();
enableDisplay = false;
		if (enableDisplay) {
			primaryPanel = new GridPanel("Primary Histogram", N, 1, 0);	
			binaryPanel = new GridPanel("Binary Histogram", N, 1, 0);	
			maskedPanel = new GridPanel("Masked Histogram", N, 1, 0);	
			costPanel = new GridPanel("Candidate Directions", N, 1, GridPanel.DRAW_NEGATIVE);
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					frame = new JFrame("VFH+");
					frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					Container pane = frame.getContentPane();
					pane.setLayout(new GridLayout(0, 1));
					
					pane.add(primaryPanel);
					pane.add(binaryPanel);
					pane.add(maskedPanel);
					pane.add(costPanel);
					
					frame.pack();
					frame.setVisible(true);
				}
			});
		}
	}

	@Override
	public void propertiesUpdated() {
		SAFETY_DISTANCE_GAMMA = ExperimentManager.getCurrent().getProperty("VFHPlus.SAFETY_DISTANCE_GAMMA", 2f, this);
		SAFETY_DISTANCE_MASK = ExperimentManager.getCurrent().getProperty("VFHPlus.SAFETY_DISTANCE_MASK", 5f, this);
		TAU_LO = ExperimentManager.getCurrent().getProperty("VFHPlus.TAU_LO", 0.7f, this) * A;
		TAU_HI = ExperimentManager.getCurrent().getProperty("VFHPlus.TAU_HI", 0.85f, this) * A;
		propertiesUpdated = true;
	}
	
	/**
	 * Resets some key variables that are retained between calls to computeTurnAngle.
	 * This function should be called if the robot experiences large motion in
	 * between calls to computeTurnAngle;
	 */
	public void reset() {
		for (int k=0; k<N; k++)
			binaryHistogram[k] = 0;
		lastTurnAngle = 0;
		lastTurnSector = angleToIndex(lastTurnAngle);
	}
	
	private void initDataStructures(LocalMap localMap) {
		int w = localMap.getWidth();
		int h = localMap.getHeight();
		beta = new Grid(w, h);
		gamma = new Grid(w, h);
		baseMagnitude = new Grid(w, h);		
		
		for (int i=0; i<w; i++) {
			for (int j=0; j<h; j++) {
				Vec2 v = localMap.getGroundPlane(i, j);
				
				float dSqd = v.x*v.x + v.y*v.y;
				
				//if (dSqd > LocalMap.MAX_SENSED_DISTANCE_SQD)
				//	continue;

				// Eq. (2): Base value for magnitude (actual occupancy certainty
				// added in 'computeHistogram').
				baseMagnitude.data[i][j] = Math.max(0, A - B*dSqd);

				// Eq. (1): Cell angle
				beta.data[i][j] = (float) Math.atan2(v.y, v.x);
				
				// Eq. (4): Enlargement angle
				gamma.data[i][j] = (float) (Math.asin((SAFETY_DISTANCE_GAMMA)/Math.sqrt(dSqd)));
			}
		}
		
		// Create the histograms.
		primaryHistogram = new float[N];
		binaryHistogram = new float[N];
		maskedHistogram = new float[N];
		
		// Debug histograms
		costHistogram = new float[N];
	}	
		
	/**
	 * Return the desired turn angle given the current localMap and target
	 * angle.  Note that 'targetAngle' is given with respect to the robot's
	 * current heading.  If this instance was constructed with goalDirected
	 * set to false then 'targetAngle' is ignored.  A return value of null
	 * indicates that no turn angle is deemed collision-free.
	 */
	public Float computeTurnAngle(LocalMap localMap, float targetAngle, boolean ignorePucks) {
		if (propertiesUpdated) {
			initDataStructures(localMap);
			propertiesUpdated = false;
		}
		
		this.ignorePucks = ignorePucks;
		computeHistogramsAndDirs(localMap, targetAngle);
		
		if (dirs.isEmpty())
			// No direction is safe!  We must be stuck somehow.
			return null;

		// Choose the best candidate direction in terms of least cost.
		best = dirs.get(0);
		for (CandidateDir dir : dirs)
			if (dir.cost < best.cost)
				best = dir;
		
		lastTurnSector = best.sector;
		lastTurnAngle = indexToAngle(best.sector);
		return new Float(lastTurnAngle);
	}
	
	/**
	 * Unlike 'computeTurnAngle' this method checks whether the given targetAngle
	 * is viable.  If not it yields a more appropriate angle in the returned
	 * structure.  A return value of null indicates that no turn angle is 
	 * deemed collision-free.
	 */
	public CheckTargetAngleResponse checkTargetAngle(LocalMap localMap, float targetAngle, boolean ignorePucks) {
		if (propertiesUpdated) {
			initDataStructures(localMap);
			propertiesUpdated = false;
		}

		this.ignorePucks = ignorePucks;
		CheckTargetAngleResponse returnValue = new CheckTargetAngleResponse();
		
		computeHistogramsAndDirs(localMap, targetAngle);
		
		if (dirs.isEmpty())
			// No direction is safe!  We must be stuck somehow.
			return null;

		// Choose the best candidate direction as the one that matches the
		// target angle, if available.  If this one is not available, choose
		// the least cost direction.
		best = null;
		int targetSector = angleToIndex(targetAngle);
		for (CandidateDir dir : dirs)
			if (dir.sector == targetSector) {
				best = dir;
				returnValue.targetAngleFree = true;
			}
		if (best == null) {
			// Choose lowest cost dir.
			best = dirs.get(0);
			for (CandidateDir dir : dirs)
				if (dir.cost < best.cost)
					best = dir;
		}
		
		lastTurnSector = best.sector;
		lastTurnAngle = indexToAngle(best.sector);
		returnValue.outputAngle = lastTurnAngle;
		return returnValue;
	}

	private void computeHistogramsAndDirs(LocalMap localMap, float targetAngle) {
		STImage occ = localMap.getOccupancy();
		
		// Compute histograms.
		computePrimary(occ);
		computeBinary();
		computeMasked(localMap, occ);
		
		computeCandidateDirections(targetAngle);

		if (enableDisplay) {
			for (int i=0; i<N; i++)
				costHistogram[i] = 0;
			for (CandidateDir dir : dirs)
				costHistogram[dir.sector] = dir.cost;
		}
	}
	
	private void computePrimary(STImage occ) {
		// Zero primary histogram
		for (int i=0; i<N; i++)
			primaryHistogram[i] = 0;

		// Fill primary histogram from occupancy grid.
		int w = occ.width;
		int h = occ.height;
		for (int i=0; i<w; i++) {
			for (int j=0; j<h; j++) {
				if (occ.pixels[i][j] == SensedType.NOTHING ||
					(ignorePucks && SensedType.isPuckType(occ.pixels[i][j])))
					continue;

				// Fill sectors all sectors that correspond to the enlarged obstacle
				// (Roughly equivalent with Eq. 6).
				int startK = angleToIndex(beta.data[i][j] + gamma.data[i][j]);
				int stopK = angleToIndex(beta.data[i][j] - gamma.data[i][j]) + 1;
				for (int k=startK; k<=stopK; k++) {
					if (k >= 0 && k < N)
						// The following is closer to what is in the paper, but
						// leads to very large values in the primary histogram
						// and makes it difficult to find good values for TAU_HI
						// and TAU_LO:
						//
						// primaryHistogram[k] += baseMagnitude.data[i][j];
						//
						// This keeps the histogram values bounded.
						primaryHistogram[k] = Math.max(primaryHistogram[k], baseMagnitude.data[i][j]);
				}
			}
		}
	}
		
	private void computeBinary() {	
		// Compute binary histogram: Eq. (7)
		for (int k=0; k<N; k++) {
			if (primaryHistogram[k] > TAU_HI)
				binaryHistogram[k] = 1;
			else if (primaryHistogram[k] < TAU_LO)
				binaryHistogram[k] = 0;
			// Otherwise the previous value is retained.
		}
	}
	
	private void computeMasked(LocalMap localMap, STImage occ) {
		// Compute masked histogram: Eq. (8) - (11) ...
		
		// Left and right trajectory centers...  Ideally these should be computed
		// from the robot's current speed and ability to turn.
		Vec2 leftCenter = new Vec2(-6.5f,  // Camera lies 6.5 cm ahead of the robot's center
								   12.5f); // Approximate turning radius
		Vec2 rightCenter = new Vec2(leftCenter.x, -leftCenter.y);
		float safetyRadiusSqd = (float) Math.pow(leftCenter.y + SAFETY_DISTANCE_MASK, 2);
		
		// Determine left and right limit angles based on trajectory centers and
		// active cells from the occupancy grid.
		float leftLimit = START_ANGLE;
		float rightLimit = STOP_ANGLE;
		int w = occ.width;
		int h = occ.height;
		for (int i=0; i<w; i++)
			for (int j=0; j<h; j++) {
				if (occ.pixels[i][j] == SensedType.NOTHING ||
					(ignorePucks && SensedType.isPuckType(occ.pixels[i][j])))
					continue;

				Vec2 v = localMap.getGroundPlane(i, j);
				float b = beta.data[i][j];
				if (b < 0 && b > rightLimit && // Now condition 1: Eq. (10a)
						MathUtils.distanceSquared(v, rightCenter) < safetyRadiusSqd)
					rightLimit = b;
				if (b > 0 && b < leftLimit && // Now condition 2: Eq. (10b)
						MathUtils.distanceSquared(v, leftCenter) < safetyRadiusSqd)
					leftLimit = b;
			}
		//System.out.println("leftLimit: " + leftLimit + ", rightLimit: " + rightLimit);
		
		// Eq. (11)
		for (int k=0; k<N; k++) {
			maskedHistogram[k] = 0;
			
			if (binaryHistogram[k] == 0) {
				float sectorAngle = indexToAngle(k);
				if (sectorAngle > leftLimit || sectorAngle < rightLimit)
					maskedHistogram[k] = 1;
			} else
				maskedHistogram[k] = 1;
		}
	}
	
	private void computeCandidateDirections(float targetAngle) {
		int targetSector = angleToIndex(targetAngle);
		dirs.clear();
		
		// Determine left and right borders for all openings in the masked 
		// histogram and add the candidate directions for each opening.
		boolean opening = maskedHistogram[0] == 0;
		int left = 0;
		for (int k=1; k<N; k++) {
			if (opening) {
				// Look for the right border.
				if (maskedHistogram[k-1] == 0 && maskedHistogram[k] == 1) {
					addCandidateDirs(left, k-1, targetSector);
					opening = false;
				}
			} else {
				// Start looking for the left border of this new opening.
				if (maskedHistogram[k-1] == 1 && maskedHistogram[k] == 0) {
					left = k;
					opening = true;
				}
			}
		}
		if (opening && maskedHistogram[N-1] == 0)
			// The right border of the final opening (if it exists) is N-1.
			addCandidateDirs(left, N-1, targetSector);
		
		// Now compute scores.
		for (CandidateDir dir : dirs) {
//			dir.cost = MU_1 * getAbsSectorAngleDiff(dir.sector, targetSector) +
//					   MU_2 * getAbsSectorAngleDiff(dir.sector, N/2 + 1) +
//					   MU_3 * getAbsSectorAngleDiff(dir.sector, lastTurnSector);
			double sectorAngle = indexToAngle(dir.sector);
			dir.cost = (float)(MU_1 * AngleUtils.getAngularDifference(sectorAngle, targetAngle) +
					   		   MU_2 * AngleUtils.getAngularDifference(sectorAngle, 0) +
					   		   MU_3 * AngleUtils.getAngularDifference(sectorAngle, lastTurnAngle));
		}
	}

	/// From Eq. (16)
	private int getAbsSectorAngleDiff(int c1, int c2) {
		return Math.min(Math.abs(c1-c2), 
						Math.min( Math.abs(c1-c2-N), Math.abs(c1-c2+N) ) );
	}

	/// Add candidate directions (1-3) for this opening.
	private void addCandidateDirs(int left, int right, int targetSector) {
		int width = right - left + 1;
		if (width < S_MAX)
			dirs.add(new CandidateDir((left + right)/2));
		else {
			dirs.add(new CandidateDir(left + S_MAX/2));
			dirs.add(new CandidateDir(right - S_MAX/2));
			
			if (goalDirected) {
				// Add the target direction if it lies in this sector
				if (targetSector >= left && targetSector <= right)
					dirs.add(new CandidateDir(targetSector));
			} else {
				// Add the zero angle and the last chosen target sector if they
				// lie in this sector.  (We don't add both if they are the same).
				if (ZERO_ANGLE_SECTOR >= left && ZERO_ANGLE_SECTOR <= right)
					dirs.add(new CandidateDir(ZERO_ANGLE_SECTOR));
				if (lastTurnSector != ZERO_ANGLE_SECTOR &&
				    lastTurnSector >= left && lastTurnSector <= right)
					dirs.add(new CandidateDir(lastTurnSector));
			}
		}
	}

	public static final float indexToAngle(int k) {
		return START_ANGLE - k * ALPHA;
	}
	
	public static final int angleToIndex(float angle) {
		return (int)((START_ANGLE - angle) / ALPHA);
	}

	/**
	 * The draw method below is tied to the simulation.  This method just
	 * updates the panels displaying the internal workings of VFH+.
	 */
	public void updateGUI() {
		if (!enableDisplay)
			return;
		
		primaryPanel.takeForDisplay(new Grid(primaryHistogram));
		binaryPanel.takeForDisplay(new Grid(binaryHistogram));
		maskedPanel.takeForDisplay(new Grid(maskedHistogram));
		costPanel.takeForDisplay(new Grid(costHistogram));
		primaryPanel.repaint();
		binaryPanel.repaint();
		maskedPanel.repaint();
		costPanel.repaint();
	}
		
	public void draw(Transform robotTransform, DebugDraw debugDraw) {
		if (!enableDisplay)
			return;
		
		// Draw the candidate directions.
		for (CandidateDir dir : dirs) {
			float length = 15f;
			float x1 = (float) (length * Math.cos(indexToAngle(dir.sector))); 
			float y1 = (float) (length * Math.sin(indexToAngle(dir.sector))); 
			Vec2 posWrtBody = new Vec2(x1, y1);
			Vec2 globalPos = Transform.mul(robotTransform, posWrtBody);
			if (dir == best)
				debugDraw.drawSegment(robotTransform.position, globalPos, Color3f.GREEN);
			else
				debugDraw.drawSegment(robotTransform.position, globalPos, Color3f.RED);
		}
	}
}
