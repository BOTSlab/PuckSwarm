package arena;

import java.awt.Color;
import java.awt.Dimension;
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
import org.jbox2d.dynamics.Fixture;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import experiment.ExperimentManager;

import sensors.LocalizerUpdater;
import sensors.SensedType;

public class SVGArenaPainter {
	
	public static final int DRAW_BOUNDARY = 1;
	public static final int DRAW_PUCKS = 2; 
	public static final int DRAW_ROBOTS = 4;
	public static final int DRAW_ROBOT_LABELS = 8;
	public static final int DRAW_HOMES = 16;
	
	Arena arena;
	int drawFlags, stepCount;
	SVGGraphics2D g;
	
	Vec2[] robotVertices = {
			new Vec2(-7.7f, -5.7f), // Bottom left (on page)
			new Vec2(7.2f, -5.7f),
			new Vec2(11.5f, -3.4f),
			new Vec2(12.5f, -2.7f),
			new Vec2(13.2f, -1.4f),
			new Vec2(13.6f, 0f),
			new Vec2(13.2f, 1.4f),
			new Vec2(12.5f, 2.7f),
			new Vec2(11.5f, 3.4f),
			new Vec2(7.2f, 5.7f),
			new Vec2(-7.7f, 5.7f),
			new Vec2(-8.7f, 0)
	};

    public SVGArenaPainter(String filename, Arena arena, int drawFlags, int stepCount) {
    	this.arena = arena;
    	this.drawFlags = drawFlags;
    	this.stepCount = stepCount;
    	
        // Get a DOMImplementation.
        DOMImplementation domImpl =
            GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);

        // Create an instance of the SVG Generator.
        g = new SVGGraphics2D(document);

        paint();
        
        // Finally, stream out SVG to the standard output using
        // UTF-8 encoding.
        boolean useCSS = true; // we want to use CSS style attributes
        Writer out;
		try {
			out = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8");
	        g.stream(out, useCSS);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	public void paint() {
		float width = arena.enclosure.getWidth();
		float height = arena.enclosure.getHeight();

		g.translate(width / 2.0 + 0.5, height / 2.0 + 0.5);
		g.setSVGCanvasSize(new Dimension((int) width + 1, (int) height + 1));
		g.scale(1, -1);

		if ((drawFlags & DRAW_BOUNDARY) != 0) {
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

		if ((drawFlags & DRAW_PUCKS) != 0) {
			float w = Puck.WIDTH;
			for (Puck p : arena.pucks) {
				Vec2 pos = p.body.m_xf.position;
				g.setColor(p.puckType.color);
				Ellipse2D.Float circle = new Ellipse2D.Float(pos.x - w / 2,
						pos.y - w / 2, w, w);
				g.fill(circle);
			}
		}

		if ((drawFlags & DRAW_ROBOTS) != 0) {
			g.setFont(g.getFont().deriveFont(5f));
			for (Robot r : arena.robots) {
				g.setColor(r.color);
				paintRobot(r.body.getTransform());
			}
		}

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


		if ((drawFlags & DRAW_HOMES) != 0) {
			float w = 1.5f * Puck.WIDTH;
			for (Robot r : arena.robots) {
				// Read the homes file for this robot.
				String code = ExperimentManager.getCurrent().getStringCode();
				String base = ExperimentManager.getOutputDir() + File.separatorChar + code
							 + "_step" + String.format("%07d", stepCount);
				String filename = base + "_" + r.name + "_homes.txt";
				PositionList homes = PositionList.load(filename);
				
				for (int k = 0; k < SensedType.NPUCK_COLOURS; k++) {
					String colorName = SensedType.getPuckColorName(k);
					Vec2 pos = homes.get(k);
					if (pos.x == 0 && pos.y == 0)
						// (0, 0) acts as a non-existent value here.
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