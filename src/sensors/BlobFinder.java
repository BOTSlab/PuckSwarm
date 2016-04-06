package sensors;

import java.util.ArrayList;
import java.util.Iterator;

import arena.Grid;

public class BlobFinder {
	
	STCameraImage image;
	Grid labels;
	int xMin, xMax, yMin, yMax;
	SensedType type;
	
	// Quantities which are shared between recursive calls.
	int label;
	int blobArea, blobX0, blobX1, blobY0, blobY1;
//	float blobGroundArea;
	
	public BlobFinder(STCameraImage image, SensedType type, int xMin, int xMax, int yMin, int yMax) {
		this.image = image;
		labels = new Grid(image.width, image.height);
		this.type = type;
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;
	}

	public ArrayList<Blob> getBlobs() {
		labels.setAll(0);
		
		// Go through all image pixels and launch a recursive labelling process
		// on those which are unlabelled and have the right type.
		ArrayList<Blob> blobs = new ArrayList<Blob>();

		label = 1;
		for (int i=xMin; i<=xMax; i++)
			for (int j=yMin; j<yMax; j++)
				if (image.pixels[i][j] == type && labels.data[i][j] == 0) {
					blobs.add( extractBlob(i, j, label) );
					label++;
				}
	
		return blobs;
	}

	/**
	 * Determine the blob which includes pixel (i, j) as one of its
	 * members.
	 */
	private Blob extractBlob(int i, int j, int label) {
		blobArea = 0;
//		blobGroundArea = 0;
		blobX0 = Integer.MAX_VALUE;
		blobX1 = -Integer.MAX_VALUE;
		blobY0 = Integer.MAX_VALUE;
		blobY1 = -Integer.MAX_VALUE;
		recursiveBlobber(i, j);
		return new Blob(blobX0, blobX1, blobY0, blobY1, blobArea/*, blobGroundArea*/);
	}
	
	private void recursiveBlobber(int i, int j) {
	    if (i < xMin || i > xMax || j < yMin || j > yMax)
	    	// (i, j) is out of bounds.
	        return;
	    else if (image.pixels[i][j] != type || labels.data[i][j] != 0)
	    	// Pixel is the wrong type or has already been labelled.
	        return;
	    else {
	    	// The two base cases above handle invalid pixels or pixels that do
	    	// not belong to this Blob.  Therefore, we know that (i, j) does
	    	// belong.  Update the blob statistics.
	    	blobArea++;
//	    	blobGroundArea += image.getCalibration().getCalibData(i, j).groundArea;
	    	if (i < blobX0) blobX0 = i;
	    	if (i > blobX1) blobX1 = i;
	    	if (j < blobY0) blobY0 = j;
	    	if (j > blobY1) blobY1 = j;
	    	
	        labels.data[i][j] = label;

	        // 4-connected
	        /*
	        recursiveBlobber(i  , j-1);
	        recursiveBlobber(i-1, j  );
	        recursiveBlobber(i+1, j  );
	        recursiveBlobber(i  , j+1);
	        */

	        // 8-connected.
	        recursiveBlobber(i-1, j-1);
	        recursiveBlobber(i  , j-1);
	        recursiveBlobber(i+1, j-1);
	        recursiveBlobber(i-1, j  );
	        recursiveBlobber(i+1, j  );
	        recursiveBlobber(i-1, j+1);
	        recursiveBlobber(i  , j+1);
	        recursiveBlobber(i+1, j+1);
	    }
	}

	/**
	 * Applied after getBlobs to remove blobs which are within the given
	 * distance to the given type.
	 */
	public void filterBlobsNear(ArrayList<Blob> blobs, SensedType badType, float distance) {
		// Square the distance so we can just compare to squared distances below.
		distance *= distance;
		for (int i=xMin; i<=xMax; i++)
			for (int j=yMin; j<yMax; j++)
				if (image.pixels[i][j] == badType) {
					// Go through all blobs and remove those that have a squared
					// distance smaller than or equal to distance.
					Iterator<Blob> it = blobs.iterator();
					while (it.hasNext()) {
					   Blob b = it.next();
					   int dx = Math.min(Math.abs(b.getX0() - i), Math.abs(b.getX1() - i));
					   int dy = Math.min(Math.abs(b.getY0() - j), Math.abs(b.getY1() - j));
					   if (dx*dx + dy*dy <= distance)
						   it.remove();
					}

				}
	}
}
