package analysis;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;

import arena.Arena;
import arena.Settings;

public class CallPostPainter {

	public static void main(String[] args) {

		if (args.length != 5) {
			System.err
					.println("Five arguments required: dirName (String), code (String), index (int), stepCount (int), drawRobots (boolean)");
			System.exit(-1);
		}
		String dirName = args[0];
		String code = args[1];
		int index = Integer.valueOf(args[2]);
		int stepCount = Integer.valueOf(args[3]);
		boolean drawRobots = Boolean.valueOf(args[4]);
		String stepCountStr = Settings.getStepCountString(stepCount);
		
		String baseFilename = dirName + code + "__" + index + "_step" + stepCountStr;

		// Create an Arena, just to get access to its enclosure. This represents
		// a potential problem: The current implementation of Arena may use a different
		// enclosure than for the results we are currently reading.
		Arena arena = new Arena(new World(new Vec2(0, 0), false), null, false);

		String outputFile = baseFilename + ".svg";
		new PostPainter(dirName, code, index, stepCount, drawRobots, outputFile, arena);
	}
}
