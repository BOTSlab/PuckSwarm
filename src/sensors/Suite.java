package sensors;

import localmap.LocalMap;

public interface Suite {

	public void sense();

	public void draw();

	public void dispose();

	public STCameraImage getCameraImage();

	public LocalMap getLocalMap();

	public String getRobotName();

	/// There are two different mechanisms for controllers to obtain position information
	/// from a suite.  The first usess 'addLocalizerUpdater' and gives the ability to provide
	/// seperate localizers with different identifiers.  This is more powerful but also harder
	/// to support.  The second uses a single APS (absolute positioning system) which
	/// tells the controller where its robot body is quite directly.  Both methods are
	/// supported by SimSuite, but RealSuite will throw an exception if
	/// 'addLocalizerUpdate' is called.
	
	/**
	 * A controller that wishes to localize with respect to some point in space must create
	 * their own Localizer and add it by calling this method.  That localizer will then be
	 * updated by this Suite.
	 */
	public void addLocalizerUpdater(LocalizerUpdater lu, String identifier);

	/**
	 * Return a reference to the Absolute Positioning System.
	 */
	public APS getAPS();
	
	/**
	 * Get the interval at which the controller should store any relavent state information
	 * for later analysis.
	 */
	 public int getStorageInterval();
}