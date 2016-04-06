package sensors;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import javax.imageio.ImageIO;

/**
 * This was the original calibration for the SRV-1's using The OCamCalib
 * MATLAB toolbox, then processed further by my script srvCalibration.m to
 * yield basic calibration information (relationship between pixel and robot
 * coordinates) plus a bunch of extra information used in the generation of
 * synthetic images and for various behaviours.
 * 
 * As of November 2012 it is still useful for the simulation, but the basic
 * calibration information is not sufficiently accurate for the actual robots!
 * So I am moving to GridCalibration for the actual robots.  This hangs around
 * to support the simulation.
 * 
 * @author av
 *
 */
public class OCamCalibration implements Calibration {

	public static class ExtraDataPerPixel {
		// Indicates that the pixel is inactive because it lies outside the
		// circle imaged by the fisheye lens.
		public boolean masked;
		
		// The height in pixels for a robot or puck imaged at the current
		// position. This height is incorporated by setting this number
		// of pixels above the current one to the same value. The image
		// must be rendered from the top down for this to work.
		public int robotHeight, puckHeight;

		// Whether or not this pixel lies within the avoidance zone for WALL or
		// for other robots.
		public boolean avoidWallLeft, avoidWallRight, avoidOtherRobotLeft,
				avoidOtherRobotRight;
		
		// Whether or not this pixel represents part of the environment that the
		// robot's gripper cannot reach when turning along a circular trajectory.
		public boolean unreachable;
		
		// Size of the ground plane area imaged by this pixel (calculated from
		// the (Xr, Yr) values of current and neighbouring pixels).
		public float groundArea;		
	}

	
	// This is the index of the image column which seems to best represent the
	// meridian of the image
	private int meridian;
	
	private int nActivePixels, nAvoidWallPixels, nAvoidOtherRobotPixels,
					nHolePixels, gripperHeight;
	
	private CalibDataPerPixel[][] calibData = new CalibDataPerPixel[IMAGE_WIDTH][IMAGE_HEIGHT];

	private ExtraDataPerPixel[][] extraData = new ExtraDataPerPixel[IMAGE_WIDTH][IMAGE_HEIGHT];

	// Coordinates of the camera w.r.t. the robot.
	private static final float CAMERA_X = 6.5f;
	private static final float CAMERA_Y = 0;

	// Dimensions of the images produced.
	private static final int IMAGE_WIDTH = 160;
	private static final int IMAGE_HEIGHT = 120;
	
	// The maximum distance (squared) to incorporate into the LocalMap
	public static int MAX_SENSED_DISTANCE_SQD = (int) Math.pow(100, 2);

	// The maximum distance (squared) of feasible clusters
	public static int MAX_CLUSTER_DISTANCE_SQD = (int) Math.pow(40, 2);

	/**
	 * Construct from the calibration files stored in the given directory.
	 */
	public OCamCalibration(String dir) {

		// Reading the data from 'srvCalibration.txt' into calibData. This
		// file specifies the intersection point of each pixel ray with the
		// ground plane. Each line of the file gives pixel coordinates (xp, yp),
		// coordinates of the ground plane intersection point in the
		// 2D robot reference frame (Xr, Yr), and the height in pixels for a
		// robot or puck imaged at (xp, yp).
		try {
			Scanner scanner = new Scanner(
					new File(dir + File.separatorChar + "srvCalibration.txt"));
			scanner.useDelimiter(System.getProperty("line.separator"));
			while (scanner.hasNext()) {
				String s = scanner.next();
				if (s.charAt(0) == '#')
					// Comment line.
					continue;
				Scanner lineScanner = new Scanner(s);
				lineScanner.useDelimiter("\\s*,\\s*");

				int xp = lineScanner.nextInt();
				int yp = lineScanner.nextInt();
				CalibDataPerPixel pixelCalib = calibData[xp][yp] = new CalibDataPerPixel();
				ExtraDataPerPixel pixelExtra = extraData[xp][yp] = new ExtraDataPerPixel();
				pixelCalib.Xr = lineScanner.nextFloat() + CAMERA_X;
				pixelCalib.Yr = lineScanner.nextFloat() + CAMERA_Y;
				pixelExtra.robotHeight = lineScanner.nextInt();
				pixelExtra.puckHeight = lineScanner.nextInt();
//meta.robotHeight = 0;
//meta.puckHeight = 0;
				pixelExtra.avoidWallLeft = lineScanner.nextInt() == 1;
				pixelExtra.avoidWallRight = lineScanner.nextInt() == 1;
				pixelExtra.avoidOtherRobotLeft = lineScanner.nextInt() == 1;
				pixelExtra.avoidOtherRobotRight = lineScanner.nextInt() == 1;
				pixelExtra.unreachable = lineScanner.nextInt() == 1;
				pixelExtra.groundArea = lineScanner.nextFloat();

				lineScanner.close();
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// Read the 'mask', 'gripperBody', and 'gripperHole' images.
		try {
			BufferedImage mask = ImageIO.read(new File(dir + File.separatorChar + "mask.png"));
			BufferedImage gripperBody = ImageIO.read(new File(dir + File.separatorChar + "gripperBody.png"));
			BufferedImage gripperHole = ImageIO.read(new File(dir + File.separatorChar + "gripperHole.png"));
			
			for (int i=0; i<IMAGE_WIDTH; i++)
				for (int j=0; j<IMAGE_HEIGHT; j++) {
					CalibDataPerPixel pixelCalib = calibData[i][j];
					ExtraDataPerPixel pixelExtra = extraData[i][j];
					if (pixelCalib == null)
						continue;
					
					int maskRGB = mask.getRGB(i, j);
					if ((maskRGB & 255) != 0) {
						pixelExtra.masked = true;
					}
					
					int gripperRGB = gripperBody.getRGB(i, j);
					if ((gripperRGB & 255) != 0)
						pixelCalib.gripperBody = true;
					
					int carryRegionRGB = gripperHole.getRGB(i, j);
					if ((carryRegionRGB & 255) != 0) {
						pixelCalib.gripperHole = true;
						nHolePixels++;
					}
				}
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		// Choose the meridian as the image column where Yr goes from positive
		// to negative.  We assume it is vertical (it appears to be) and therefore
		// just search for this point along the image row 3/4 down from the top.
		int j = 3 * IMAGE_HEIGHT / 4;
		int i = 0;
		for (; i<IMAGE_WIDTH && calibData[i][j].Yr > 0; i++)
			;
		meridian = i;		

		// Determine number of active pixels and number of avoidWall and avoidOtherRobot
		// pixels.
		nActivePixels = 0;
		nAvoidWallPixels = 0;
		nAvoidOtherRobotPixels = 0;
		for (j = 0; j < IMAGE_HEIGHT; j++)
			for (i = 0; i < IMAGE_WIDTH; i++)
				if (extraData[i][j] != null) {
					nActivePixels++;
					if (extraData[i][j].avoidWallLeft || extraData[i][j].avoidWallRight)
						nAvoidWallPixels++;
					if (extraData[i][j].avoidOtherRobotLeft || extraData[i][j].avoidOtherRobotRight)
						nAvoidOtherRobotPixels++;
				}
		
		// Determine the height of the gripper in rows.
		gripperHeight = 0;
		for (j = 0; j < IMAGE_HEIGHT; j++)
			for (i = 0; i < IMAGE_WIDTH; i++)
				if (calibData[i][j] != null && calibData[i][j].gripperBody) {
					gripperHeight++;
					break;
				}
	}

	@Override
	public float getCameraXr() {
		return CAMERA_X;
	}

	@Override
	public float getCameraYr() {
		return CAMERA_Y;
	}

	@Override
	public int getImageWidth() {
		return IMAGE_WIDTH;
	}

	@Override
	public int getImageHeight() {
		return IMAGE_HEIGHT;
	}

	@Override
	public CalibDataPerPixel getCalibData(int i, int j) {
		return calibData[i][j];
	}

	public ExtraDataPerPixel getExtraData(int i, int j) {
		return extraData[i][j];
	}

	public int getMeridian() {
		return meridian;
	}

	public int getNActivePixels() {
		return nActivePixels;
	}

	public int getNAvoidWallPixels() {
		return nAvoidWallPixels;
	}

	public int getNAvoidOtherRobotPixels() {
		return nAvoidOtherRobotPixels;
	}

	@Override
	public int getNHolePixels() {
		return nHolePixels;
	}

	public int getGripperHeight() {
		return gripperHeight;
	}

	@Override
	public float getMaxSensedDistanceSqd() {
		return MAX_SENSED_DISTANCE_SQD;
	}

	@Override
	public float getMaxClusterDistanceSqd() {
		return MAX_CLUSTER_DISTANCE_SQD;
	}
}
