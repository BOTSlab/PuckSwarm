package sensors;

import java.util.ArrayList;

import arena.Grid;

public class BlobFinderForGrids {
	Grid grid;
	Grid labels;

	// Quantities which are shared between recursive calls.
	int label;
	int blobArea, blobX0, blobX1, blobY0, blobY1;
	
	public BlobFinderForGrids(Grid grid) {
		this.grid = grid;
		labels = new Grid(grid.width, grid.height);
	}
	
	public ArrayList<Blob> getBlobs() {
		labels.setAll(0);
		
		// Go through all image pixels and launch a recursive labelling process
		// on those which are unlabelled and have the right type.
		ArrayList<Blob> blobs = new ArrayList<Blob>();
		label = 1;
		for (int i=0; i<grid.width; i++)
			for (int j=0; j<grid.height; j++)
				if (grid.data[i][j] != 0 && labels.data[i][j] == 0) {
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
		blobX0 = Integer.MAX_VALUE;
		blobX1 = -Integer.MAX_VALUE;
		blobY0 = Integer.MAX_VALUE;
		blobY1 = -Integer.MAX_VALUE;
		recursiveBlobber(i, j);
		return new Blob(blobX0, blobX1, blobY0, blobY1, blobArea);
	}
	
	private void recursiveBlobber(int i, int j) {
	    if (i < 0 || i >= grid.width || j < 0 || j >= grid.height)
	    	// (i, j) is out of bounds.
	        return;
	    else if (grid.data[i][j] == 0 || labels.data[i][j] != 0)
	    	// Pixel is zero or has already been labelled.
	        return;
	    else {
	    	// The two base cases above handle invalid pixels or pixels that do
	    	// not belong to this Blob.  Therefore, we know that (i, j) does
	    	// belong.  Update the blob statistics.
	    	blobArea++;
	    	if (i < blobX0) blobX0 = i;
	    	if (i > blobX1) blobX1 = i;
	    	if (j < blobY0) blobY0 = j;
	    	if (j > blobY1) blobY1 = j;
	    	
	        labels.data[i][j] = label;

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
}
