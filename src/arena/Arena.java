package arena;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;

import sensors.SensedType;
import utils.RunningStats;
import experiment.Experiment;
import experiment.ExperimentManager;

public class Arena {
	World world;
	DebugDraw debugDraw;

	// Maintain lists of the robots and pucks created.
	ArrayList<Robot> robots = new ArrayList<Robot>();
	ArrayList<Puck> pucks = new ArrayList<Puck>();

	int stepCount;

	public Enclosure enclosure;

	Random rng = ExperimentManager.getCurrent().getRandom();

	RunningStats carryingStats = new RunningStats();

	boolean allowRobotDisplay;
	public boolean showCamera;

	public static int STORAGE_INTERVAL = 100;
// STANDARD_REV:
//	public static int STORAGE_INTERVAL = 1;

	private static final char SLASH = File.separatorChar;
	
	public Arena(World world, DebugDraw debugDraw, boolean allowRobotDisplay) {
		this.world = world;
		this.debugDraw = debugDraw;
		this.allowRobotDisplay = allowRobotDisplay;
		
		//enclosure = new OvalEnclosure(world);
		enclosure = new RoundedRectangleEnclosure(world);
		// For experiments with projectile bucket brigading
		//enclosure = new ObstacleEnclosure(world);
		
		boolean gridConfig = ExperimentManager.getCurrent().getProperty(
				"Arena.gridConfig", false, null);
		if (gridConfig)
			this.createGridConfiguration();
		else {
			// Create some randomly positioned initial pucks and robots.
			int nPucks = ExperimentManager.getCurrent().getProperty(
					"Arena.nPucks", 10, null);
			int nPuckTypes = ExperimentManager.getCurrent().getProperty(
					"Arena.nPuckTypes", 2, null);
			if (nPuckTypes > SensedType.NPUCK_COLOURS) {
				System.err.println("Arena: nPuckTypes too large!");
				System.exit(-1);
			}
			int nPucksPerType = nPucks / nPuckTypes;
			float w = enclosure.getWidth();
			
			boolean modifiedPuckPlacement = ExperimentManager.getCurrent().getProperty(
					"Arena.modifiedPuckPlacement", false, null);
			if (modifiedPuckPlacement) {
				// Intended to mitigate the possibility of off-type pucks near the
				// predetermined cache postion for CacheCons_Informed.  For revision 2 of the SI12
				// paper.
				for (int k=0; k<nPuckTypes; k++) {
					SensedType puckType = SensedType.getPuckType(k);
					for (int i = 0; i < nPucksPerType; i++) {
						if (k == 0)
							// Red pucks must start on the left.
							createPuck(puckType, -w/2, 0);
						else
							// Green pucks must start on the right
							createPuck(puckType, 0, w/2);
					}
				}
				
			} else {
				for (int k=0; k<nPuckTypes; k++) {
					SensedType puckType = SensedType.getPuckType(k);
					for (int i = 0; i < nPucksPerType; i++)
						createPuck(puckType, -w/2, w/2);				
				}	
			}

			int nRobots = ExperimentManager.getCurrent().getProperty(
					"Arena.nRobots", 4, null);
			// Added for ECAL workshop and "perfect information" extension to SI paper
			int nInformedRobots = ExperimentManager.getCurrent().getProperty(
					"Arena.nInformedRobots", 0, null);
			for (int i = 0; i < nRobots; i++) {
				boolean presetCaches = i < nInformedRobots;
				createRobot(presetCaches);
			}
			
			// For experiments with object distribution
			/*
			//float w = enclosure.getWidth();
			float h = enclosure.getHeight();
			for (float y = -h/4; y < h/4; y+=h/(2*nRobots))
				//robots.add(new KickingRobot(0.25f*w, y, (float)Math.PI, this, allowRobotDisplay, false));
				robots.add(new Robot(0.25f*w, y, (float)Math.PI, this, allowRobotDisplay, false));
			*/
		}

		// Set gravity to zero.
		world.setGravity(new Vec2(0.0f, 0.0f));
	}

	// For placing some pucks in a grid configuration with one robot viewing
	// these pucks.
	private void createGridConfiguration() {
		/*
		ArrayList<Vec2> gridCoords = new ArrayList<Vec2>();
		
		float min = -20;
		float max = 20;
		int n = 2;
		float delta = (max - min) / n;
		for (float x=min; x<=max; x+= delta)
			for (float y=min; y<=max; y+= delta)
		gridCoords.add(new Vec2(x, y));
		
		for (Vec2 pos : gridCoords)
			pucks.add(new Puck(pos.x, pos.y, 0, SensedType.RED_PUCK, world, debugDraw));
		*/
		
		robots.add(new Robot(0, 0, 0, this, allowRobotDisplay, false));
	}
	
	public void createPuck(SensedType puckType, float minX, float maxX) {
		// Select random positions until we find one that lies sufficiently
		// far from other robots.
		Vec2 pos;
		float closestRobotDist = 0;
		do {
			pos = enclosure.getRandomInside(Puck.MIN_BOUNDARY_DISTANCE);
			if (pos.x < minX || pos.x > maxX)
				continue;
			
			closestRobotDist = Float.POSITIVE_INFINITY;
			for (Puck p : pucks)
				if (MathUtils.distance(p.body.m_xf.position, pos) < closestRobotDist)
					closestRobotDist = MathUtils.distance(p.body.m_xf.position, pos);		
		} while (closestRobotDist < Puck.WIDTH);

		float randAngle = (float) (2.0f * Math.PI * rng.nextFloat());
		pucks.add(new DotPuck(pos.x, pos.y, randAngle, puckType, world, debugDraw));
	}

	public void createRobot(boolean presetCaches) {
		// Select random positions until we find one that lies sufficiently
		// far from other robots.
		Vec2 pos;
		float closestRobotDist = 0;
		int attempts = 0;
		do {
			pos = enclosure.getRandomInside(Robot.MIN_BOUNDARY_DISTANCE);
			closestRobotDist = Float.POSITIVE_INFINITY;
			for (Robot r : robots)
				if (MathUtils.distance(r.body.m_xf.position, pos) < closestRobotDist)
					closestRobotDist = MathUtils.distance(r.body.m_xf.position, pos);
			attempts++;
		} while (attempts < 1000 && closestRobotDist < 2 * Robot.SRV_LENGTH);
		
		float randAngle = (float) (2.0f * Math.PI * rng.nextFloat());
		
		robots.add(new Robot(pos.x, pos.y, randAngle, this, allowRobotDisplay, presetCaches));
	}

	public void step(boolean allowThinking, boolean allowForwards, boolean allowTurning, boolean forcedMarch, int forcedTurn, boolean showCamera) {
		this.showCamera = showCamera;
		Experiment e = ExperimentManager.getCurrent();
		int puckShiftStep = e.getProperty("Arena.puckShiftStep", -1, null);
		int puckShuffleStep = e.getProperty("Arena.puckShuffleStep", -1, null);
		
		if (puckShiftStep != -1 && stepCount % puckShiftStep == 0)
			shiftPucksThroughCentre();
		if (puckShuffleStep != -1 && stepCount % puckShuffleStep == 0)
			shufflePucks();
		
		// For experiments with object distribution
		//distributionStep(e);
		
		for (Robot r : robots) {
			r.sense();
			if (allowThinking) {
				r.think(stepCount);
				r.move(allowForwards, allowTurning);
			}
		}

		if (forcedMarch) {
			int dir = forcedTurn;
			for (Robot r : robots) {
				r.forcedMarch(dir);
				dir *= -1;
			}
		}
		
		// Count the number of robots carrying pucks
		/*
		int nCarriers = 0;
		for (Robot r : robots)
			if (r.simSuite.getLocalMap().isCarrying())
				nCarriers++;
		carryingStats.push(nCarriers / (double) robots.size());
		*/
		
		// Store all robot and puck positions for later analysis.
		if (ExperimentManager.isActive() && stepCount % Arena.STORAGE_INTERVAL == 0) {
			String code = ExperimentManager.getCurrent().getStringCodeWithoutSeed();
			int expIndex = ExperimentManager.getCurrent().getIndex();
			String base = ExperimentManager.getOutputDir() + SLASH + 
						code + SLASH +
						expIndex + SLASH + 
						"step" + Settings.getStepCountString(stepCount);

			PoseList robotPoses = new PoseList();
			for (Robot r : robots)
				robotPoses.add(r.body.m_xf.position.x, r.body.m_xf.position.y, r.body.getAngle());
			robotPoses.save(base + "_robots.txt");

			for (int k=0; k<SensedType.NPUCK_COLOURS; k++) {
				PositionList puckPositions = new PositionList();
				for (Puck p : pucks)
					if (SensedType.getPuckIndex(p.getPuckType()) == k)
						puckPositions.add(p.body.m_xf.position.x, p.body.m_xf.position.y);
				if (puckPositions.size() > 0)
					puckPositions.save(base + "_" + SensedType.getPuckColorName(k) + "_pucks.txt");
			}
			
			// Save carryingStats.
			/*
			DataTable table = new DataTable(Integer.class,  // stepCount 
 										    Double.class, 	// mean
										    Double.class);	// std.
			table.add(stepCount, carryingStats.getMean(), carryingStats.getStandardDeviation());
			DataTableUtils.storeTable(base + "_carrying.txt", table);
			*/
						
			/** Turning this off, since we can generate the necessary .svg afterwards.
			// Save pictures of the arena as .svg files.
			int flags = SVGArenaPainter.DRAW_BOUNDARY | SVGArenaPainter.DRAW_PUCKS;
			new SVGArenaPainter(base + "_pucks.svg", this, flags, stepCount);
			new SVGArenaPainter(base + "_robots.svg", this, flags | SVGArenaPainter.DRAW_ROBOTS, stepCount);
//			boolean drawHomes = robots.get(0).simSuite.getLocalizerUpdater(SensedType.getPuckColorName(0)) != null;
boolean drawHomes = true;
			if (drawHomes)
				new SVGArenaPainter(base + "_homes.svg", this, flags | SVGArenaPainter.DRAW_HOMES, stepCount);
			 */			
		}

		stepCount++;
	}

	// For experiments with object distribution
	/*
	private void distributionStep(Experiment e) {
		DistributionTaskManager bb = DistributionTaskManager.getInstance();
		int nPuckTypes = bb.getDestinations().length;
		
		// Addition of new pucks at the source.
		float puckSourceX = bb.getSource().x;
		float puckSourceY = bb.getSource().y;
		if (rng.nextFloat() < DistributionTaskManager.ADDITION_PROBABILITY) {
			SensedType puckType = SensedType.getPuckType(rng.nextInt(nPuckTypes));
			puckSourceX += (0.5 - rng.nextFloat());
			puckSourceY += (0.5 - rng.nextFloat());
			pucks.add(new DotPuck(puckSourceX, puckSourceY, 0, puckType, world, debugDraw));
			bb.incrementAddedCount(SensedType.getPuckIndex(puckType));
		}
		
		// Removal of pucks arriving at destinations.
		Vec2[] destinations = bb.getDestinations();
		Iterator<Puck> it = pucks.iterator();
		while (it.hasNext()) {
			Puck puck = it.next();
			for (int k=0; k<destinations.length; k++) {
				Vec2 dest = destinations[k];
				if ((SensedType.getPuckIndex(puck.getPuckType()) == k) &&
					MathUtils.distance(dest, puck.body.getPosition()) < 2.5)
				{
					world.destroyBody(puck.body);
					it.remove();
					bb.incrementRemovedCount(SensedType.getPuckIndex(puck.getPuckType()));
					break;
				}
			}
		}
		
	}
	*/
	
	/**
	 * Just maintain the same robot forces and torques as set in the last call to step.
	 */
	public void coast(boolean allowThinking, boolean allowForwards, boolean allowTurning) {
		for (Robot r : robots) {
			if (allowThinking) {
				r.move(allowForwards, allowTurning);
			}
		}
	}

	private Vec2 getRandomPuckPos() {
		float closestRobotDist = 0;
		Vec2 pos;
		do {
			pos = enclosure.getRandomInside(Puck.MIN_BOUNDARY_DISTANCE);
			closestRobotDist = Float.POSITIVE_INFINITY;
			for (Robot robot : robots)
				if (MathUtils.distance(robot.body.m_xf.position, pos) < closestRobotDist)
					closestRobotDist = MathUtils.distance(robot.body.m_xf.position, pos);		
		} while (closestRobotDist < Puck.WIDTH);
		return pos;
	}
	
	// Determine the centroid of all puck positions and shift the pucks by
	// a fixed amount along the negative of this vector --- moving them to 
	// the other side of the world origin.
	private void shiftPucksThroughCentre() {
		Vec2 centroid = new Vec2();
		for (Puck puck : pucks)
			centroid.addLocal(puck.body.getPosition());
		centroid.normalize();
		centroid.mulLocal(-100);
		
		shiftPucks(centroid);
	}
	
	// Find new random positions for all pucks.
	private void shufflePucks() {
		for (Puck puck : pucks)
			puck.body.setTransform(getRandomPuckPos(), 0);
	}

	// Shift all pucks by the given translation.  If this would result in a puck being
	// outside the arena or penetrating a robot, then find a new random position
	// for that puck.
	private void shiftPucks(Vec2 translation) {
		for (Puck puck : pucks) {
			Vec2 pos = puck.body.m_xf.position.add(translation);
			if (!enclosure.inFreeSpace(pos, Puck.MIN_BOUNDARY_DISTANCE)) {
				// Position is outside arena, go random!
				puck.body.setTransform(getRandomPuckPos(), 0);
			} else {
				// Check for distance to closest robot
				float closestRobotDist = Float.POSITIVE_INFINITY;
				for (Robot robot : robots)
					if (MathUtils.distance(robot.body.m_xf.position, pos) < closestRobotDist)
						closestRobotDist = MathUtils.distance(robot.body.m_xf.position, pos);
				
				if (closestRobotDist < Puck.WIDTH)
					// Position is too close to a robot.  Go random!
					puck.body.setTransform(getRandomPuckPos(), 0);
				else
					puck.body.setTransform(pos, 0);
			}
		}
	}
	
	public void draw() {
		// Draw any additional information associated with each body.
		for (Body b = world.getBodyList(); b != null; b = b.getNext()) {
			Object userData = b.getUserData();
			if (userData != null && userData instanceof Entity)
				((Entity) userData).draw();
		}
		
		
		// For experiments with object distribution
		/*
		DistributionTaskManager bb = DistributionTaskManager.getInstance();
		String bbStatus = "Added / Removed: " + bb.getTotalAdded() + " / " + bb.getTotalRemoved(); 
		debugDraw.drawString(0, 200, bbStatus, Color3f.WHITE);
		*/
	}
	
	public World getWorld() {
		return world;
	}
	
	public DebugDraw getDebugDraw() {
		return debugDraw;
	}

	public int getStepCount() {
		return stepCount;
	}

	public void dispose() {
		for (Robot r : robots)
			r.dispose();
	}

	public Enclosure getEnclosure() {
		return enclosure;
	}

}
