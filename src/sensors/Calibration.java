package sensors;



public interface Calibration {

	public float getCameraXr();
	public float getCameraYr();
	public int getImageWidth();
	public int getImageHeight();

	public static class CalibDataPerPixel {
		// Intersection of the pixel ray with the ground plane.
		public float Xr, Yr;

		// Indicates that the pixel is part of the gripper's body and hole,
		// respectively.
		public boolean gripperBody, gripperHole;
	}
	
	public CalibDataPerPixel getCalibData(int i, int j);
	
	/** Number of pixels in the gripper hole. */
	public int getNHolePixels();
	
	public float getMaxSensedDistanceSqd();
	
	public float getMaxClusterDistanceSqd();
}
