package sensors;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class StringOverlay implements Overlay {

	// Image coordinates of display position.
	int x, y;
	
	String str;
	
	Color color;
	
	public StringOverlay(int x, int y, String str, Color color) {
		this.x = x;
		this.y = y;
		this.str = str;
		this.color = color;
	}

	@Override
	public void paint(Graphics2D g2d, int cellWidth, int cellHeight, boolean flipVertical) {
		g2d.setColor(color);
		if (flipVertical) {
			// Create a little image within which to draw the string so that
			// it appears upright.
			FontMetrics m = g2d.getFontMetrics();
			Rectangle2D rect = m.getStringBounds(str, g2d);
			int w = (int) rect.getWidth();
			int h = (int) rect.getHeight();
			BufferedImage image = new BufferedImage(w, h,
					  								BufferedImage.TYPE_INT_RGB);
			
			Graphics2D littleG = (Graphics2D) image.createGraphics();
        	littleG.translate(0, h);
			littleG.scale(1, -1);
        	littleG.setColor(color);
        	littleG.setBackground(Color.white);
        	littleG.clearRect(0, 0, w, h);
			littleG.drawString(str, 0, h);
			
			g2d.drawImage(image, cellWidth*x, cellHeight*y, null);
			littleG.dispose();
			
		} else
			g2d.drawString(str, cellWidth*x, cellHeight*y);
	}
}
