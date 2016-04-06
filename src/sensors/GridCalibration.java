package sensors;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import javax.imageio.ImageIO;

/**
 * This is the new style of grid-based calibration to be used on the SRV-1's.
 * Generation of the calibration data starts with an image of a set of pucks
 * at known positions.  Its convenient (but not necessary) to arrange these 
 * pucks in a grid.  A pair of scripts, 'gridCalibrationPhase1.py' and 
 * 'gridCalibrationPhase2.py' are then used to pick out the puck centres and
 * interpolate in-between pucks.  This yields the .csv files that are read here.
 * We also require 'gripperBody.png' and 'gripperHole.png' which are image
 * masks that indicate the gripper's body and hole, respectively.
 * 
 * @author av
 */
public class GridCalibration implements Calibration {
	
	private int nHolePixels;
	
	private CalibDataPerPixel[][] calibData;

	
	// Coordinates of the camera w.r.t. the robot.  We assume the robot's
	// coordinate frame is coincident with the camera's.
	private static final float CAMERA_X = 0;
	private static final float CAMERA_Y = 0;

	// Dimensions of the images produced.
	private int imageWidth, imageHeight;
	
	/// The constants below are set to an effectively infinite value to disable these
	/// checks for maximum distance in LocalMap.  These checks were put into place
	/// originally for what is now OCamCalibration which provided calibration for a 
	/// very broad area (too broad, in fact).  With GridCalibration only the pre-defined
	/// grid area of the image is actually sensed, so these limits are not necessary.

	// The maximum distance (squared) to incorporate into the LocalMap
	public static float MAX_SENSED_DISTANCE_SQD = Float.MAX_VALUE;

	// The maximum distance (squared) of feasible clusters
	public static float MAX_CLUSTER_DISTANCE_SQD = Float.MAX_VALUE;

	/**
	 * Construct from the calibration files stored in the given directory.
	 */
	public GridCalibration(String dir, int imageWidth, int imageHeight) {
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;

		calibData = new CalibDataPerPixel[imageWidth][imageHeight];

		String resCode = imageWidth + "x" + imageHeight;

		try {
			Scanner scanner = new Scanner(
					new File(dir + File.separatorChar + "phase2_" + resCode  + ".csv"));
			scanner.useDelimiter(System.getProperty("line.separator"));
			
			// Skip the first line which contains the header.
			scanner.next();
			
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
				pixelCalib.Xr = lineScanner.nextFloat() + CAMERA_X;
				pixelCalib.Yr = lineScanner.nextFloat() + CAMERA_Y;

				lineScanner.close();
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// Read the 'gripperBody' and 'gripperHole' images.
		try {
			BufferedImage gripperBody = ImageIO.read(new File(dir + File.separatorChar + "gripperBody_" + resCode + ".png"));
			BufferedImage gripperHole = ImageIO.read(new File(dir + File.separatorChar + "gripperHole_" + resCode + ".png"));
			
			for (int i=0; i<imageWidth; i++)
				for (int j=0; j<imageHeight; j++) {
					CalibDataPerPixel pixelCalib = calibData[i][j];
					if (pixelCalib == null)
						continue;
					
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
		return imageWidth;
	}

	@Override
	public int getImageHeight() {
		return imageHeight;
	}

	@Override
	public CalibDataPerPixel getCalibData(int i, int j) {
		return calibData[i][j];
	}

	@Override
	public int getNHolePixels() {
		return nHolePixels;
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
