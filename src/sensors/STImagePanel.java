package sensors;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * An STImagePanel displays an STImage.  
 */
public class STImagePanel extends JPanel {
	
	protected STImage image;
	
	boolean flipVertical;
	
	// Width and height of each on-screen pixel.
	protected int cw, ch;
	
	public static int DEFAULT_SCALING = 2;

	public STImagePanel(int imageWidth, int imageHeight, boolean flipVertical,
			int scale) {
		this.flipVertical = flipVertical;
		setMinimumSize(new Dimension(imageWidth, imageHeight));
		setPreferredSize(new Dimension(scale*imageWidth, 
									   scale*imageHeight));
	}
	
	public STImagePanel(int imageWidth, int imageHeight, boolean flipVertical) {
		this(imageWidth, imageHeight, flipVertical, DEFAULT_SCALING);
	}
	
    public void setImage(final STImage inputImage) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
		    		if (image == null)
		    			image = new STImage(inputImage.width, inputImage.height);
		    		for (int i=0; i<image.width; i++)
		    			for (int j=0; j<image.height; j++)
		    				image.pixels[i][j] = inputImage.pixels[i][j];
			}
		});
    }
        
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
                
        if (image == null)
			return;
        
        Graphics2D g2d = (Graphics2D) g;
        //AffineTransform transformBeforeFlip = g2d.getTransform();
        if (flipVertical) {
	        	g2d.scale(1, -1);
	        	g2d.translate(0, -getHeight());
        }
        
		cw = getWidth() / image.width;
		ch = getHeight() / image.height;

		// Now fill in all grid entries
		for (int i=0; i<image.width; i++)
			for (int j=0; j<image.height; j++) {
				SensedType value = image.pixels[i][j];
				g2d.setColor(value.color);
				g2d.fillRect(i*cw, j*ch, cw, ch);
			}
		
		// Draw overlays
		for (Overlay o : image.overlays)
			o.paint(g2d, cw, ch, flipVertical);
//		image.overlays.clear();
    }  
}