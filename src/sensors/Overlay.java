package sensors;

import java.awt.Graphics2D;

public interface Overlay {
	/**
	 * Paint the overlay using the given graphics object
	 * @param g2d		  The graphics object;  The image has already been
	 * 					  drawn by this object
	 * @param cellWidth   The width of each cell in the image display
	 * @param cellHeight  The width of each cell in the image display
	 * @param flipVertical If the panel is drawn upside-down
	 */
	void paint(Graphics2D g2d, int cellWidth, int cellHeight, boolean flipVertical);
}
