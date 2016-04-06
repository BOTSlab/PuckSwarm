package controllers;

import localmap.LocalMap;
import localmap.VFHPlus;
import arena.Robot;
import sensors.Camera;
import sensors.STCameraImage;
import sensors.SensedType;
import sensors.StoredCalibration;
import utils.AngleUtils;

public class BehaviourUtils {
	
	public static final float CARRY_THRESHOLD = 0.8f;

	public static final float MAX_TURN_ANGLE = (float) (Math.PI/2);
	public static final float MAX_TURN_ANGLE_SQD = MAX_TURN_ANGLE * MAX_TURN_ANGLE;
	
	public static boolean isCarrying(STCameraImage image, SensedType puckType) {
		// The image region used to sense whether a puck is currently being carried.
		int CARRY_X0 = image.getCalibration().getMeridian() - 10;
		int CARRY_X1 = image.getCalibration().getMeridian() + 10;
		int CARRY_Y0 = 100;
		int CARRY_Y1 = 110;
		int CARRY_AREA = (CARRY_X1 - CARRY_X0) * (CARRY_Y1 - CARRY_Y0);

		// Determine if we are carrying or not.
		float sum = 0;
		for (int i=CARRY_X0; i<=CARRY_X1; i++)
			for (int j=CARRY_Y0; j<=CARRY_Y1; j++)
				if (image.pixels[i][j] == puckType)
					sum++;
		sum /= CARRY_AREA;
		return sum > CARRY_THRESHOLD;
	}
	
	/**
	 * Determine from the given image and calibration data whether the robot
	 * should turn to avoid an obstacle (WALL or ROBOT). If wall is true then
	 * the obstacle must be WALL, otherwise it must be a ROBOT. If passClosely
	 * is true then the robot turns away from the obstacle only if it is within
	 * the avoidance zone on both sides. This tends to cause the robot to pass
	 * by the object within a constant distance defined by the avoidance zone.
	 * 
	 * If avoidPucksAsWalls is true then pucks are treated as walls.
	 * If avoidPucksAsRobots is true then pucks are treated as robots.
	 * 
	 * @return 1 for CCW turn, 1 for CW turn, and 0 for no turn required.
	 */
	public static int getTurnSign(STCameraImage image, boolean wall, boolean passClosely,
								  boolean avoidPucksAsWalls, boolean avoidPucksAsRobots) {
		int leftSum = 0, rightSum = 0;

		if (wall) {
			if (avoidPucksAsWalls) {
				// Treat pucks and walls the same --- avoid both.
				for (int i = 0; i < image.width; i++)
					for (int j = 0; j < image.height; j++) {
						if (image.getCalibration().getCalibData(i, j).active) {
							if (image.pixels[i][j] == SensedType.WALL ||
								SensedType.isPuckType(image.pixels[i][j])) {
								if (image.getCalibration().getCalibData(i, j).avoidWallLeft)
									leftSum++;
								if (image.getCalibration().getCalibData(i, j).avoidWallRight)
									rightSum++;
							}
						}
					}
			} else {
				for (int i = 0; i < image.width; i++)
					for (int j = 0; j < image.height; j++) {
						if (image.getCalibration().getCalibData(i, j).active) {
							if (image.pixels[i][j] == SensedType.WALL) {
								if (image.getCalibration().getCalibData(i, j).avoidWallLeft)
									leftSum++;
								if (image.getCalibration().getCalibData(i, j).avoidWallRight)
									rightSum++;
							}
						}
					}
			}
		} else {
			if (avoidPucksAsRobots) {
				for (int i = 0; i < image.width; i++)
					for (int j = 0; j < image.height; j++) {
						if (image.getCalibration().getCalibData(i, j).active) {
							if (image.pixels[i][j] == SensedType.ROBOT ||
								SensedType.isPuckType(image.pixels[i][j])) {
								if (image.getCalibration().getCalibData(i, j).avoidOtherRobotLeft)
									leftSum++;
								if (image.getCalibration().getCalibData(i, j).avoidOtherRobotRight)
									rightSum++;
							}
						}
					}
			} else {
				for (int i = 0; i < image.width; i++)
					for (int j = 0; j < image.height; j++) {
						if (image.getCalibration().getCalibData(i, j).active) {
							if (image.pixels[i][j] == SensedType.ROBOT) {
								if (image.getCalibration().getCalibData(i, j).avoidOtherRobotLeft)
									leftSum++;
								if (image.getCalibration().getCalibData(i, j).avoidOtherRobotRight)
									rightSum++;
							}
						}
					}
			}
		}

		int ts = 0;
		if (passClosely) {
			// Turn to avoid only if the obstacle is present on both sides.
			// This delays the turn so that the obstacle is passed at
			// a constant distance, defined by the avoidance circle.
			if (leftSum > 0 && rightSum > 0) {
				if (leftSum < rightSum)
					ts = 1;
				if (rightSum < leftSum)
					ts = -1;
				// If leftSum == rightSum keep going straight.
			}
		} else {
			// If the obstacle is present on either side, turn away from it.
			if (leftSum < rightSum)
				ts = 1;
			if (rightSum < leftSum)
				ts = -1;
		}
		return ts;
	}	

	/**
	 * As above with 'avoidPucksAsWalls' and 'avoidPucksAsRobots' false.
	 */
	public static int getTurnSign(STCameraImage image, boolean wall, boolean passClosely) {
		return getTurnSign(image, wall, passClosely, false, false);
	}

}
