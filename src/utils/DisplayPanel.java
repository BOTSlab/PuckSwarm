package utils;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.MemoryImageSource;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Used to display the contents of a 2-D array.
 *
 */
public class DisplayPanel extends JPanel implements ComponentListener {
	
	String title;
	int dataWidth, dataHeight;

	JLabel titleLabel;
	JLabel imageLabel;
	ImageIcon icon;
	
	int[] pixels;
	Image image;
	
	MemoryImageSource source;
	
	public static int DEFAULT_SCALING = 2;

	public DisplayPanel(final String title, final int dataWidth, final int dataHeight) {
		this.title = title;
		this.dataWidth = dataWidth;
		this.dataHeight = dataHeight;
		
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					titleLabel = new JLabel(title);
					imageLabel = new JLabel();
					
					imageLabel.setMinimumSize(new Dimension(dataWidth, dataHeight));
					imageLabel.setPreferredSize(new Dimension(DEFAULT_SCALING*dataWidth, 
												   DEFAULT_SCALING*dataHeight));

					setLayout(new GridLayout(0,1));
					add(titleLabel);
					add(imageLabel);

					icon = new ImageIcon();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		imageLabel.addComponentListener(this);

		/*
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY); 
		int bits[] = new int[] {8};
		ColorModel colorModel = new ComponentColorModel(cs, bits, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
		*/
		
		// A blank pixel array, needed to create the source.
		pixels = new int[dataWidth * dataHeight];
		
		source = new MemoryImageSource(dataWidth, dataHeight, pixels, 0, dataWidth);
		source.setAnimated(true);
		image = imageLabel.createImage(source);
	}
	
	public synchronized void setData(float[][] array) {
		assert array.length == dataWidth && array[0].length == dataHeight;
		
		// Get the minimum and maximum values.
		float min = Float.POSITIVE_INFINITY;
		float max = Float.NEGATIVE_INFINITY;
		for (int i=0; i<dataWidth; i++)
			for (int j=0; j<dataHeight; j++) {
				float v = array[i][j];
				if (v > max)
					max = v;
				if (v < min)
					min = v;
			}
		
		// Modify the array of pixels.
		int index = 0;
		for (int j=0; j<dataHeight; j++)
			for (int i=0; i<dataWidth; i++) {
				byte v = (byte)(255 * (array[i][j] - min) / (max - min));
				pixels[index++] = (255 << 24) |  v;
			}
		
		source.newPixels();
		componentResized(null);
	}

	@Override
	public void componentResized(ComponentEvent e) {
		if (image != null) {
			// Resize image to match current component size.
			Image resizedImage = image.getScaledInstance(imageLabel.getWidth(),
					imageLabel.getHeight(), Image.SCALE_FAST);

			icon.setImage(resizedImage);
			imageLabel.setIcon(icon);
		}
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
	}
}
