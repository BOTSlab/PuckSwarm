package arena;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A GridPanel displays a Grid.
 */
public class GridPanel extends JPanel implements MouseMotionListener, MouseListener {
	
	String title;
	
	Grid grid;
	
	// Bitwise combination of flags and the boolean values that are encoded.
	int flags;
	boolean flipVertical, drawNegative;
	
	// Position selected by the user for highlighting (-1 means none selected).
	int selectedI = -1, selectedJ;
	
	DrawPanel drawPanel;
	JLabel label;

	public static int DEFAULT_SCALING = 2;
	
	// Bitwise flags that can be AND'ed and passed to the GridPanel constructor.
	public static int FLIP_VERTICAL = 1;
	public static int DRAW_NEGATIVE = 2;
	
	/// A DrawPanel paints the actual Grid.
	class DrawPanel extends JPanel {
		int cw, ch;
		
	    public void paintComponent(Graphics g) {
	        super.paintComponent(g);
	        
	        if (grid == null)
	        	return;

	        Graphics2D g2d = (Graphics2D) g;
	        if (flipVertical) {
	        	g2d.scale(1, -1);
	        	g2d.translate(0, -getHeight());
	        }

	        cw = getWidth() / grid.width;
			ch = getHeight() / grid.height;
			
			// Determine the maximum value in the grid.
			float max = 0;
			for (int i=0; i<grid.width; i++)
				for (int j=0; j<grid.height; j++)
					if (grid.data[i][j] > max)
						max = grid.data[i][j];

			// Now draw all grid entries
			for (int i=0; i<grid.width; i++)
				for (int j=0; j<grid.height; j++) {
					float value = grid.data[i][j];
					if (max > 0)
						value /= max;
					if (drawNegative)
						value = 1 - value;
					if (value >= 0 && value <= 1)
						g2d.setColor(new Color(value, value, value));
					else
						// It can occur that value is outside of [0, 1] if setGridCopy
						// is called in the middle of a call to paintComponent.
						g2d.setColor(new Color(1, 0, 0));
					g2d.fillRect(i*cw, j*ch, cw, ch);
				}
			
			// Highlight selected cell.
			if (selectedI != -1) {
				g2d.setColor(Color.yellow);
				g2d.drawRect(selectedI*cw - 1, selectedJ*ch - 1, cw + 1, ch + 1);				
			}
	    }
	}
	
	public GridPanel(String title, int gridWidth, int gridHeight, int flags) {
		this.title = title;
		this.flags = flags;
		
		flipVertical = (flags & FLIP_VERTICAL) != 0;
		drawNegative = (flags & DRAW_NEGATIVE) != 0;
		
		label = new JLabel(title);
		drawPanel = new DrawPanel();
		drawPanel.setMinimumSize(new Dimension(gridWidth, gridHeight));
		drawPanel.setPreferredSize(new Dimension(DEFAULT_SCALING*gridWidth, 
									   			 DEFAULT_SCALING*gridHeight));		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(label);
		add(drawPanel);
		
		drawPanel.addMouseListener(this);
		drawPanel.addMouseMotionListener(this);
	}
   
    /**
     * Use the given grid to display on the panel.  To prevent problem should
     * the given grid be modified while drawing we clone this grid so that
     * the grid passed in remains untouched.
     */
    public void takeCloneForDisplay(Grid grid) {
    	takeForDisplay((Grid) grid.clone());
    }

    /**
     * Use the given grid to display on the panel.  Note that we do not make
     * a copy of the grid.  It can create problems if the grid is modified while
     * we are drawing, in this case you should use 'takeCloneForDisplay'.
     */
    public void takeForDisplay(Grid grid) {
    	this.grid = grid;
    	
    	if (selectedI != -1)
    		label.setText(title + ": (" + selectedI + ", " + selectedJ + ") = " + grid.data[selectedI][selectedJ]);
    	
    }

	@Override
	public void mouseDragged(MouseEvent e) {
		if (e.getComponent() == drawPanel) {
			int i = e.getX() / drawPanel.cw;
			int j = e.getY() / drawPanel.ch;
			if (flipVertical)
				j = grid.height - j - 1;
			if (i >= 0 && i < grid.width)
				selectedI = i;
			if (j >= 0 && j < grid.height)
				selectedJ = j;
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		mouseDragged(arg0);
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}
}