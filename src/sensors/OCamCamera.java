package sensors;

import java.io.File;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

import sensors.Calibration.CalibDataPerPixel;
import sensors.OCamCalibration.ExtraDataPerPixel;
import arena.Arena;
import arena.Settings;

/**
 * Camera for simulation, based on OCamCalibration.
 * 
 * @author av
 */
public class OCamCamera implements Sensor {

	public static final char SLASH = File.separatorChar;
	public static final String CALIB_DIR = System.getProperty("user.home") + SLASH + "work" + SLASH + "data" + SLASH + "srv1" + SLASH + "ocam_calib";
	private OCamCalibration calib = new OCamCalibration(CALIB_DIR + SLASH + "192.168.0.111");
	
	private int imageWidth = calib.getImageWidth();
	private int imageHeight = calib.getImageHeight();

	private PointSensor[][] pSensors = new PointSensor[imageWidth][imageHeight];

	private STCameraImage image = new STCameraImage(calib);

	public OCamCamera(Body robotBody, Arena arena) {
		// Create PointSensors. Leave invisible areas as null.
		for (int y = 0; y < imageHeight; y++)
			for (int x = 0; x < imageWidth; x++) {
				CalibDataPerPixel pixelData = calib.getCalibData(x, y);
				if (pixelData != null) {
					pSensors[x][y] = new PointSensor(
							new Vec2(pixelData.Xr, pixelData.Yr), robotBody, arena);
				}
			}
		
// Modified the creation of PointSensors to incorporate a sensing along a
// a line segment for distant PointSensors.  Intended to prevent distant pucks
// from winking in and out of existence.
		/*
		for (int y = 1; y < imageHeight; y++)
			for (int x = 0; x < imageWidth; x++) {
				CalibDataPerPixel pixelData = calib.getCalibData(x, y);
				ExtraDataPerPixel pixelExtra = calib.getExtraData(x, y);
				
				if (pixelData == null || pixelExtra.masked)
					continue;
				
				CalibDataPerPixel metaFurther = calib.getCalibData(x, y-1);
				if (metaFurther != null) {
					pSensors[x][y] = new PointSensor(
							new Vec2(pixelData.Xr, pixelData.Yr), robotBody, arena,
							new Vec2(metaFurther.Xr, metaFurther.Yr));
				} else { 
					pSensors[x][y] = new PointSensor(
							new Vec2(pixelData.Xr, pixelData.Yr), robotBody, arena);					
				}
				
			}
		*/
	}

	public void sense() {
		// Reset all image pixels to WALL.
		for (int yp = 0; yp < imageHeight; yp++) {
			for (int xp = 0; xp < imageWidth; xp++) {
				image.pixels[xp][yp] = SensedType.NOTHING;
			}
		}

		for (int yp = 0; yp < imageHeight; yp++) {
			for (int xp = 0; xp < imageWidth; xp++) {
				CalibDataPerPixel pixelCalib = calib.getCalibData(xp, yp);
				ExtraDataPerPixel pixelExtra = calib.getExtraData(xp, yp);
				
				if (pixelCalib == null || pixelExtra.masked || pixelCalib.gripperBody)
					image.pixels[xp][yp] = SensedType.HIDDEN;
				else {
					PointSensor sensor = pSensors[xp][yp];
					if (sensor == null)
						continue;
					
					sensor.sense();
					SensedType type = sensor.getSensedType();
					image.pixels[xp][yp] = type;
					
					if (pixelCalib.gripperHole && !SensedType.isPuckType(type)) {
						// Only pucks can be sensed in the gripper.
						image.pixels[xp][yp] = SensedType.HIDDEN;
						continue;
					}

					// Fill in the appropriate number of pixels above if this
					// is a robot (or wall) or puck to simulate 3D perspective.
					if ((type == SensedType.ROBOT || type == SensedType.WALL) 
							&& pixelExtra.robotHeight > 0) {
						for (int dy = yp - 1; yp - dy <= pixelExtra.robotHeight
								&& dy >= 0; dy--)
							image.pixels[xp][dy] = type;
					} else if (SensedType.isPuckType(type) && pixelExtra.puckHeight > 0) {
						for (int dy = yp - 1; yp - dy <= pixelExtra.puckHeight
								&& dy >= 0; dy--)
							image.pixels[xp][dy] = type;
					}
				}
			}
		}
		
		//image.preprocess();
	}

	public STCameraImage getImage() {
		return image;
	}
	
	public void draw() {		
		// Draw the PointSensors
		/*
		for (int yp = 0; yp < IMAGE_HEIGHT; yp++) {
			for (int xp = 0; xp < IMAGE_WIDTH; xp++) {
				CalibDataPerPixel calib = calibData[xp][yp];
				if (calib.active && pSensors[xp][yp] != null) {
					pSensors[xp][yp].draw();
				}
			}
		}
		*/
	}
}
