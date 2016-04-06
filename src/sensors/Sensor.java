package sensors;

public interface Sensor {
	/**
	 * Apply the sensor to extract some information from the world.  Typically
	 * that information will be returned via some 'get' or 'getPercept'
	 * methods.  However, the exact signature of these will be sensor-specific.
	 */
	public void sense();
	
	/**
	 * Draw either the sensor itself or perhaps the percept sensed.
	 */
	public void draw();
}
