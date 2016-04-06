package controllers;

public interface KickingController extends Controller {
	/**
	 * @return A boolean indicating whether a kick should be initiated.
	 */
	public boolean getKick();
}
