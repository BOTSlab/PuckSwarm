package analysis;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.common.Vec3;
import org.jbox2d.dynamics.Fixture;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import arena.Arena;
import arena.PoseList;
import arena.PositionList;
import arena.Puck;
import arena.Settings;

import experiment.ExperimentManager;

import sensors.LocalizerUpdater;
import sensors.Pose;
import sensors.SensedType;

public class PostPainter {
	
	String dirName, code, baseFilename;
	int index, stepCount;
	boolean drawRobots;
	SVGGraphics2D g;
	
	float shiftX = -3.88f;
	Vec2[] robotVertices = {
			new Vec2(shiftX - 7.7f, -5.7f), // Bottom left (on page)
			new Vec2(shiftX + 7.2f, -5.7f),
			new Vec2(shiftX + 11.5f, -3.4f),
			new Vec2(shiftX + 12.5f, -2.7f),
			new Vec2(shiftX + 13.2f, -1.4f),
			new Vec2(shiftX + 13.6f, 0f),
			new Vec2(shiftX + 13.2f, 1.4f),
			new Vec2(shiftX + 12.5f, 2.7f),
			new Vec2(shiftX + 11.5f, 3.4f),
			new Vec2(shiftX + 7.2f, 5.7f),
			new Vec2(shiftX - 7.7f, 5.7f),
			new Vec2(shiftX - 8.7f, 0)
	};

    public PostPainter(String dirName, String code, int index, int stepCount, 
    		boolean drawRobots, String outputFilename, Arena arena) {
    	this.dirName = dirName;
    	this.code = code;
    	this.index = index;
    	this.stepCount = stepCount;
    	this.drawRobots = drawRobots;
    	
    	baseFilename = dirName + code + "/" + index + "/step" + Settings.getStepCountString(stepCount);    	
    	
        // Get a DOMImplementation.
        DOMImplementation domImpl =
            GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);

        // Create an instance of the SVG Generator.
        g = new SVGGraphics2D(document);

        paintEnclosure(arena);
        paint(arena);
        
        // Finally, stream out SVG to the standard output using
        // UTF-8 encoding.
        boolean useCSS = true; // we want to use CSS style attributes
        Writer out;
		try {
			out = new OutputStreamWriter(new FileOutputStream(outputFilename), "UTF-8");
	        g.stream(out, useCSS);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    /**
     * Given an Arena, paint its enclosure.
     */
	public void paintEnclosure(Arena arena) {
		float width = arena.enclosure.getWidth();
		float height = arena.enclosure.getHeight();

		g.translate(width / 2.0 + 0.5, height / 2.0 + 0.5);
		g.setSVGCanvasSize(new Dimension((int) width + 1, (int) height + 1));
		g.scale(1, -1);

		g.setColor(Color.black);
		for (Fixture f = arena.enclosure.getBody().getFixtureList(); f != null; f = f
				.getNext()) {
			PolygonShape poly = (PolygonShape) f.m_shape;
			Line2D.Float line = new Line2D.Float(poly.m_vertices[0].x,
					poly.m_vertices[0].y, poly.m_vertices[1].x,
					poly.m_vertices[1].y);
			g.draw(line);
		}
	}

	public void paint(Arena arena) {
		float width = arena.enclosure.getWidth();
		float height = arena.enclosure.getHeight();

		// Draw the pucks.
		for (int k=0; k<SensedType.NPUCK_COLOURS; k++) {
			String filename = baseFilename + "_" + SensedType.getPuckColorName(k) + "_pucks.txt";
			PositionList pucks = PositionList.load(filename);
			if (pucks == null)
				continue;
			
			float w = Puck.WIDTH;
			int n = pucks.size();
			for (int i=0; i<n; i++) {
				Vec2 pos = pucks.get(i);
				g.setColor(SensedType.getPuckType(k).color);
				Ellipse2D.Float circle = new Ellipse2D.Float(pos.x - w / 2,
						pos.y - w / 2, w, w);
				g.fill(circle);
			}
		}

		PoseList robots = PoseList.load(baseFilename + "_robots.txt");
		int nRobots = robots.size();
		if (drawRobots) {
			g.setFont(g.getFont().deriveFont(5f));
			for (int i=0; i<nRobots; i++) {
				Pose pose = robots.get(i);
				g.setColor(Color.BLACK);
				Transform rt = new Transform();
				rt.set(new Vec2((float) pose.getX(), (float) pose.getY()), (float) pose.getTheta());
				paintRobot(rt);
			}
		}

		/*
		if ((drawFlags & DRAW_ROBOT_LABELS) != 0) {
			g.setFont(g.getFont().deriveFont(5f));
			for (Robot r : arena.robots) {
				g.setColor(r.color);

				// Draw the robot's infoString.
				g.scale(1, -1);
				g.drawString(r.infoString, 10 + r.body.m_xf.position.x,
						-r.body.m_xf.position.y);
				g.scale(1, -1);
			}
		}
		*/

		// For ECAL 2013 Workshop: Draw the pre-specified cache points for informed
		// individuals.
		/*
		int nTypes = 2;
		float markerRadius = 2.0f * Puck.WIDTH;
		float diagonalLength = (float) Math.hypot(width, height);
		float stepLength = diagonalLength / (nTypes + 1);
		float cos_45 = (float) Math.cos(Math.PI/4f);
		for (int type = 0; type<nTypes; type++) {
			float x = -width/2f + cos_45 * (type + 1) * stepLength;

			Rectangle2D.Float rect = new Rectangle2D.Float(x - markerRadius
					/ 2, x - markerRadius / 2, markerRadius, markerRadius);
			// Outline in black.
			g.setColor(Color.black);
			g.draw(rect);
		}
		*/
		
		// Draw cache points if they exist.
		float w = 1.0f * Puck.WIDTH;
		for (int i=0; i<nRobots; i++) {
			// Read the cache points file for this robot.
			String filename = baseFilename + "_R" + i + "_cachePoints.txt";
			PositionList cachePoints = PositionList.load(filename);
			if (cachePoints == null)
				continue;
			
			for (int k = 0; k < SensedType.NPUCK_COLOURS; k++) {
				String colorName = SensedType.getPuckColorName(k);
				Vec2 pos = cachePoints.get(k);
				if (Float.isNaN(pos.x) || Float.isNaN(pos.y))
					// (NaN, NaN) acts as a non-existent value here.
					continue;

				// Fill a rectangle with the appropriate color.
				g.setColor(SensedType.getPuckType(k).color);
				Rectangle2D.Float rect = new Rectangle2D.Float(pos.x - w
						/ 2, pos.y - w / 2, w, w);
				g.fill(rect);

				// Outline in black.
				g.setColor(Color.black);
				g.draw(rect);
			}
		}
	}

	private void paintRobot(Transform T) {
		Path2D.Float path = new Path2D.Float();
		
		// Initial point.
		Vec2 v0 = robotVertices[0];
		v0 = Transform.mul(T, v0);
		path.moveTo(v0.x, v0.y);

		for (int i=1; i<robotVertices.length; i++) {
			Vec2 v = robotVertices[i];
			v = Transform.mul(T, v);
			path.lineTo(v.x, v.y);
			path.moveTo(v.x, v.y);
		}
		path.lineTo(v0.x, v0.y);
		
		g.draw(path);
	}
}