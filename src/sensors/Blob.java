package sensors;

public class Blob {
	
	private int x0, x1, y0, y1, area;
//	private float groundArea;
	private int cx, cy;

	public Blob(int blobX0, int blobX1, int blobY0, int blobY1, int blobArea/*, float blobGroundArea*/) {
		this.x0 = blobX0;
		this.x1 = blobX1;
		this.y0 = blobY0;
		this.y1 = blobY1;
		this.area = blobArea;
//		this.groundArea = blobGroundArea;
		this.cx = (x0 + x1) / 2;
		this.cy = (y0 + y1) / 2;
	}

	public int getX0() {
		return x0;
	}

	public int getX1() {
		return x1;
	}

	public int getY0() {
		return y0;
	}

	public int getY1() {
		return y1;
	}
	
	public int getArea() {
		return area;
	}
	
//	public float getGroundArea() {
//		return groundArea;
//	}
	
	public int getCentreX() {
		return cx;
	}

	public int getCentreY() {
		return cy;
	}
}
