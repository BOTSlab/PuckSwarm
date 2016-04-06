package sensors;

import java.util.ArrayList;

/**
 * A 2-D matrix of SensedType values.
 */
public class STImage {
	public int width;
	public int height;
	public SensedType[][] pixels;
	
	ArrayList<Overlay> overlays = new ArrayList<Overlay>();

	public STImage(int width, int height) {
		this.width = width;
		this.height = height;
		pixels = new SensedType[width][height];
		for (int j=0; j<height; j++)
			for (int i=0; i<width; i++)
				pixels[i][j] = SensedType.NOTHING;
	}

	@Override
	public Object clone() {
		STImage copy = new STImage(width, height);
		for (int j=0; j<height; j++)
			for (int i=0; i<width; i++)
				copy.pixels[i][j] = pixels[i][j];
		
		copy.overlays.addAll(overlays);
		overlays.clear();
		
		return copy;
	}
	
	/**
	 * Add an overlay that will be displayed when the image is next drawn (it
	 * will then be automatically removed).
	 */	
	public void addOverlay(Overlay overlay) {
		overlays.add(overlay);
	}

	public void setAll(SensedType type) {
		for (int j=0; j<height; j++)
			for (int i=0; i<width; i++)
				pixels[i][j] = type;
	}
}
