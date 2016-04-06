package sensors;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

import controllers.BehaviourUtils;
import controllers.ColourBackupBehaviour;
import controllers.SeekBehaviour;

public class CameraImagePanel extends STImagePanel {

	public CameraImagePanel(int imageWidth, int imageHeight) {
		super(imageWidth, imageHeight, false);
	}

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
 
        if (image == null)
        	return;
        
//		drawMask(g);
//		drawInactive(g);
//		drawUnreachable(g);
//		drawAvoidZones(g);
//		drawBackupZone(g);
//		drawCarryZone(g);
//		drawDistanceLines(g);
//		drawMeridian(g);
//		drawBlobs(g);
    }  

	private void drawMask(Graphics g) {
		for (int i=0; i<image.width; i++)
			for (int j=0; j<image.height; j++) {
				if (Camera.calibData[i][j].masked) {
					g.setColor(Color.lightGray);
					g.fillRect(i*cw, j*ch, cw, ch);
				}
			}
	}

	private void drawInactive(Graphics g) {
		// Fill inactive pixels with gray.
		for (int i=0; i<image.width; i++)
			for (int j=0; j<image.height; j++) {
				if (!Camera.calibData[i][j].active) {
					g.setColor(Color.lightGray);
					g.fillRect(i*cw, j*ch, cw, ch);
				}
			}
	}

	private void drawUnreachable(Graphics g) {
		// Draw diagonal lines through pixels with unreachable set to 
		// true.
		for (int i=0; i<image.width; i++)
			for (int j=0; j<image.height; j++) {
				if (Camera.calibData[i][j].unreachable) {
					g.setColor(Color.black);
					g.drawLine(i*cw, j*ch, (i+1)*cw - 1, (j+1)*ch - 1);
				}
			}
	}
	
	private void drawAvoidZones(Graphics g) {
		// Draw diagonal lines through pixels with avoidWallLeft, 
		// avoidWallRight, avoidOtherRobotLeft, and avoidOtherRobotRight set to 
		// true.
		for (int i=0; i<image.width; i++)
			for (int j=0; j<image.height; j++) {
				if (Camera.calibData[i][j].avoidOtherRobotLeft) {
					g.setColor(Color.blue);
					g.drawLine(i*cw, j*ch, (i+1)*cw - 1, (j+1)*ch - 1);
				}
				if (Camera.calibData[i][j].avoidOtherRobotRight) {
					g.setColor(Color.green);
					g.drawLine((i+1)*cw - 1, j*ch, i*cw, (j+1)*ch - 1);
				}
				if (Camera.calibData[i][j].avoidWallLeft) {
					g.setColor(Color.red);
					g.drawLine(i*cw, j*ch, (i+1)*cw - 1, (j+1)*ch - 1);
				}
				if (Camera.calibData[i][j].avoidWallRight) {
					g.setColor(Color.magenta);
					g.drawLine((i+1)*cw - 1, j*ch, i*cw, (j+1)*ch - 1);
				}
			}
	}
    
    private void drawBackupZone(Graphics g) {
		// Draw horizontal lines through pixels in the detection zone of
		// BackupBehaviour.
		int x0 = Camera.meridian - ColourBackupBehaviour.REGION_WIDTH / 2;
		int x1 = Camera.meridian + ColourBackupBehaviour.REGION_WIDTH / 2;
		int y0 = ColourBackupBehaviour.BOTTOM_Y
				- ColourBackupBehaviour.REGION_HEIGHT + 1;
		int y1 = ColourBackupBehaviour.BOTTOM_Y;
		for (int i = x0; i <= x1; i++)
			for (int j = y0; j <= y1; j++) {
				g.setColor(Color.blue);
				g.drawLine(i * cw, j * ch, (i + 1) * cw - 1, (j + 1) * ch - 1);
			}
    }

    private void drawCarryZone(Graphics g) {
		// Draw horizontal lines through pixels in the carry detection zone of
		// SeekBehaviour.
		for (int i=0; i<image.width; i++)
			for (int j=0; j<image.height; j++)
				if (i >= BehaviourUtils.CARRY_X0 && i <= BehaviourUtils.CARRY_X1 &&
						j >= BehaviourUtils.CARRY_Y0 && j <= BehaviourUtils.CARRY_Y1) {
					g.setColor(Color.GREEN);
					g.drawLine(i*cw, j*ch, (i+1)*cw - 1, j*ch);
				}
    }
    
    private void drawDistanceLines(Graphics g) {
		paintDistanceLine(g, 10, Color.BLUE);
		paintDistanceLine(g, 15, Color.GREEN);
		paintDistanceLine(g, 20, Color.RED);
		paintDistanceLine(g, 25, Color.GREEN);
		paintDistanceLine(g, 30, Color.BLUE);
		paintDistanceLine(g, 40, Color.RED);
		paintDistanceLine(g, 60, Color.RED);
    }
    
    /**
     * Draw a line through pixels whose ground intersection point (Xr, Yr) is
     * closest to the given distance from the robot.
     */
    private void paintDistanceLine(Graphics g, double distance, Color color) {
		distance *= distance; // square it so we can compare with sqdDistance
		int lastBestJ = -1;
		for (int i=0; i<image.width; i++) {
			// Search through the whole column to find the pixel with the
			// difference with the specified distance.
			int bestJ = -1;
			double smallestDifference = Double.MAX_VALUE;
			for (int j=0; j<image.height; j++) {
				float Xr = Camera.calibData[i][j].Xr;
				float Yr = Camera.calibData[i][j].Yr;
				double sqdDistance = Xr*Xr + Yr*Yr;
				if (Math.abs(sqdDistance - distance) < smallestDifference) {
					smallestDifference = Math.abs(sqdDistance - distance);
					bestJ = j;
				}
			}
			if (i > 0 && Camera.calibData[i][bestJ].active) {
				g.setColor(color);
				g.drawLine((i-1)*cw + cw/2, lastBestJ*ch + ch/2, i*cw + cw/2, bestJ*ch + ch/2);				
			}
			lastBestJ = bestJ;
    	}     	
    }

    private void drawMeridian(Graphics g) {
		// Draw horizontal lines of different colours through pixels in the
		// left (+Xr), vs. those on the right (-Xr).
		for (int i=0; i<image.width; i++)
			for (int j=0; j<image.height; j++) {
				if (!Camera.calibData[i][j].active)
					continue;
				
				if (i < Camera.meridian)
					g.setColor(Color.green);
				else
					g.setColor(Color.red);
				g.drawLine(i*cw, j*ch, (i+1)*cw - 1, j*ch);
			}
	}
    
    private void drawBlobs(Graphics g) {
    	for (int k=0; k<SensedType.NPUCK_COLOURS; k++) {
    		SensedType puckType = SensedType.getPuckType(k);
    		
	    	BlobFinder finder = new BlobFinder((STCameraImage) image, puckType, 0, image.width-1, 0, image.height-24);
	    	ArrayList<Blob> blobs = finder.getBlobs();
	    	finder.filterBlobsNear(blobs, SensedType.ROBOT, SeekBehaviour.TARGET_MIN_DIST_ROBOT);
	    	for (Blob blob : blobs) {
//	    		if (Camera.calibData[blob.getCentreX()][blob.getCentreY()].unreachable)
//	    			continue;
	    		
	    		for (int i=blob.getX0(); i<=blob.getX1(); i++)
	    			for (int j=blob.getY0(); j<=blob.getY1(); j++) {
						g.setColor(Color.BLUE);
						g.drawLine(i*cw, j*ch, (i+1)*cw - 1, (j+1)*ch - 1);
	    			}
	    		int cx = blob.getX1();
	    		int cy = blob.getY1();    		
	    		g.setColor(Color.BLUE);
				//g.drawString("area: " + blob.getArea(), cx*cw, cy*ch);
				g.drawString("" + (int)blob.getGroundArea(), cx*cw - 20, cy*ch + 20);
	    	}
    	}
    }	
}
