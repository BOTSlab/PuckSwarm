package experiment;

public interface PropertiesListener {
	/**
	 * Called to indicate that one of the properties of the implementing class
	 * (don't know which one) has been updated. 
	 */
	public void propertiesUpdated();
}
