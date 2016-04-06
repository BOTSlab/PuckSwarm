package sensors;

import java.awt.Color;

import sensors.Calibration.CalibDataPerPixel;
import sensors.OCamCalibration.ExtraDataPerPixel;
import experiment.ExperimentManager;
import experiment.PropertiesListener;

/**
 * An STImage with associated calibration data from the camera that generated
 * the image.  Additional information is also available after a call to
 * 'preprocess'.
 */
public class STCameraImage extends STImage /* implements PropertiesListener */{
	
	private Calibration calib;
	
	//private SensedType carriedType;

	// Parameters whose values are loaded from the current Experiment...

	//static float CARRY_THRESHOLD;

	public STCameraImage(Calibration calib) {
		super(calib.getImageWidth(), calib.getImageHeight());
		this.calib = calib;
		
		// Initialize this image with HIDDEN values for masked and gripper pixels.
		for (int j=0; j<height; j++)
			for (int i=0; i<width; i++) {
				CalibDataPerPixel pixelCalib = calib.getCalibData(i, j);
				if (pixelCalib == null || pixelCalib.gripperBody)
					pixels[i][j] = SensedType.HIDDEN;
				else
					pixels[i][j] = SensedType.NOTHING;
			}

		//propertiesUpdated();
	}

	/*
	@Override
	public void propertiesUpdated() {
		CARRY_THRESHOLD = ExperimentManager.getCurrent().getProperty("STCameraImage.CARRY_THRESHOLD", 0.5f, this);
	}
	*/
	
	public Calibration getCalibration() {
		return calib;
	}
	
	/**
	 * Preprocess the image so that methods like 'getCarriedType' yield a
	 * correct result.
	 */
	/*
	public void preprocess() {
		
		// Determine if we are carrying a puck and if so, what type.
		carriedType = SensedType.NOTHING;
		for (int k=0; k<SensedType.NPUCK_COLOURS; k++) {
			SensedType puckType = SensedType.getPuckType(k);
			float sum = 0;
			for (int j=0; j<height; j++)
				for (int i=0; i<width; i++) {
					CalibDataPerPixel pixelCalib = calib.getCalibData(i, j);
					if (pixelCalib !=pixelCalib.gripperHole && pixels[i][j] == puckType)
						sum++;
				}
			
			sum /= calib.getNHolePixels();
			if (sum > CARRY_THRESHOLD) {
				carriedType = puckType;
				break;
			}
		}
		
		addOverlay(new StringOverlay(40, 110, "STCameraImage: Carrying: " + carriedType, Color.BLACK));
	}
	*/
	/**
	 * If a puck is being carried, return its type.  If not return NOTHING.
	 * 
	 * @pre preprocess must have been called since the image last changed
	 */
	/*
	public SensedType getCarriedType() {
		return carriedType;
	}
	*/
}