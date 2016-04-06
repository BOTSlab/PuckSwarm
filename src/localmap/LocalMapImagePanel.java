package localmap;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jgraph.graph.DefaultEdge;

import sensors.Calibration;
import sensors.STImagePanel;
import sensors.SensedType;
import arena.Puck;

public class LocalMapImagePanel extends STImagePanel {
	LocalMap localMap;
	Calibration calib;

	public static int PUCK_RADIUS = (int) (0.5 * Puck.WIDTH / LocalMap.CELL_SIZE);
	public static int PUCK_DIAM = 2 * PUCK_RADIUS;

	public LocalMapImagePanel(int imageWidth, int imageHeight, Calibration calib) {
		super(imageWidth, imageHeight, true, 4);
		this.calib = calib;
	}

	/**
	 * Use the given LocalMap to display on this panel. To prevent problems
	 * should the map be modified while drawing we clone it.
	 */
	public void setMap(final LocalMap mapIn) {

		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					if (localMap == null)
						localMap = new LocalMap(mapIn);
					else
						localMap.copyFrom(mapIn);

					LocalMapImagePanel.this.image = localMap.occupancy;
				}
			});
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (image == null)
			return;

		drawClusterDistanceThreshold(g);
		//drawReachable(g);
		drawClusters(g);
	}

	private void drawClusterDistanceThreshold(Graphics g) {
		g.setColor(Color.black);
		
		int r = (int) (Math.sqrt(calib.getMaxClusterDistanceSqd()) / LocalMap.CELL_SIZE);
		g.drawOval(cw * (image.width / 2 - r), -ch * r, 2 * r * cw, 2 * r * ch);
	}

	private void drawReachable(Graphics g) {
		// Fill in unreachable positions in yellow.
		g.setColor(Color.yellow);
		for (int i = 0; i < image.width; i++)
			for (int j = 0; j < image.height; j++) {
				Vec2 v = localMap.getGroundPlane(i, j);
				if (!localMap.isReachable(v))
					g.drawLine(i * cw, j * ch, (i + 1) * cw, (j + 1) * ch);
			}
	}

	private void drawClusters(Graphics g) {
		// Draw the pucks as squares.
		for (int k=0; k<SensedType.NPUCK_COLOURS; k++)
			for (Vec2 puckV : localMap.pucks[k]) {
				Point p = localMap.getGridPoint(puckV.x, puckV.y);
				SensedType pt = SensedType.getPuckType(k);
				g.setColor(pt.color);
				g.drawRect((p.x - PUCK_RADIUS) * cw, (p.y - PUCK_RADIUS) * ch,
						PUCK_DIAM * cw, PUCK_DIAM * ch);
				
				if (localMap.carrying && MathUtils.distance(localMap.carriedV, puckV) < 1) {
					g.setColor(Color.BLACK);
					g.drawRect((p.x - PUCK_RADIUS) * cw - 1, (p.y - PUCK_RADIUS) * ch - 1,
							PUCK_DIAM * cw + 2, PUCK_DIAM * ch + 2);
				}
			}
		
		for (Cluster cluster : localMap.clusters) {
			// Draw the pucks involved in this cluster.
			Set<Vec2> nodeSet = cluster.subgraph.vertexSet();
			for (Vec2 nodeV : nodeSet) {
				Point p = localMap.getGridPoint(nodeV.x, nodeV.y);
				SensedType pt = SensedType.getPuckType(cluster.puckType);
				g.setColor(pt.color);
				g.drawOval((p.x - PUCK_RADIUS) * cw, (p.y - PUCK_RADIUS) * ch,
						PUCK_DIAM * cw, PUCK_DIAM * ch);
			}

			// Draw all of the edges of the cluster's subgraph.
			Set<DefaultEdge> edgeSet = cluster.subgraph.edgeSet();
			for (DefaultEdge edge : edgeSet) {
				Vec2 source = cluster.subgraph.getEdgeSource(edge);
				Vec2 target = cluster.subgraph.getEdgeTarget(edge);
				Point p1 = localMap.getGridPoint(source.x, source.y);
				Point p2 = localMap.getGridPoint(target.x, target.y);
				g.setColor(Color.black);
				g.drawLine(p1.x * cw, p1.y * ch, p2.x * cw, p2.y * ch);
			}
		}
	}

}
