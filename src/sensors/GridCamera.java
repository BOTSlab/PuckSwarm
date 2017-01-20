package sensors;

import java.io.File;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

import sensors.Calibration.CalibDataPerPixel;
import sensors.OCamCalibration.ExtraDataPerPixel;
import arena.Arena;
import arena.Settings;

/**
 * Camera for simulation, based on GridCalibration.
 * 
 * @author av
 */
public class GridCamera implements Sensor {
	
	Arena arena;

	public static final char SLASH = File.separatorChar;
	//public static final String CALIB_DIR = System.getProperty("user.home") + SLASH + "av_work" + SLASH + "data" + SLASH + "srv1" + SLASH + "grid_calib";
	public static final String CALIB_DIR = "data" + SLASH + "srv1" + SLASH + "grid_calib";
	private GridCalibration calib = new GridCalibration(CALIB_DIR + SLASH + "192.168.0.114", 160, 120);
	
	private int imageWidth = calib.getImageWidth();
	private int imageHeight = calib.getImageHeight();

	private PointSensor[][] pSensors = new PointSensor[imageWidth][imageHeight];

	private STCameraImage image = new STCameraImage(calib);

	public GridCamera(Body robotBody, Arena arena) {
		this.arena = arena;
		
		// Create PointSensors. Leave invisible areas as null.
		for (int y = 0; y < imageHeight; y++)
			for (int x = 0; x < imageWidth; x++) {
				CalibDataPerPixel pixelData = calib.getCalibData(x, y);
				if (pixelData != null) {
					pSensors[x][y] = new PointSensor(
							new Vec2(pixelData.Xr, pixelData.Yr), robotBody, arena);
				}
			}		
	}

	public void sense() {
		// Reset all image pixels to NOTHING.
		for (int yp = 0; yp < imageHeight; yp++) {
			for (int xp = 0; xp < imageWidth; xp++) {
				image.pixels[xp][yp] = SensedType.NOTHING;
			}
		}

		for (int yp = 0; yp < imageHeight; yp++) {
			for (int xp = 0; xp < imageWidth; xp++) {
				CalibDataPerPixel pixelCalib = calib.getCalibData(xp, yp);
				
				if (pixelCalib == null || pixelCalib.gripperBody)
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
						//image.pixels[xp][yp] = SensedType.HIDDEN;
						continue;
					}

					// Unlike OCamCamera, we cannot really simulate 3D
				}
			}
		}
		
		//image.preprocess();
	}

	public STCameraImage getImage() {
		return image;
	}
	
	public void draw() {
		if (arena.showCamera) {
			// Draw the PointSensors
			for (int yp = 0; yp < imageHeight; yp+=5) {
				for (int xp = 0; xp < imageWidth; xp+=5) {
					CalibDataPerPixel pixelCalib = calib.getCalibData(xp, yp);
					if (pixelCalib != null && pSensors[xp][yp] != null) {
						pSensors[xp][yp].draw();
					}
				}
			}
		}
	}
}
