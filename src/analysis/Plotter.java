package analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import utils.FileUtils;
import arena.Settings;
import de.erichseifert.gral.DrawableConstants.Location;
import de.erichseifert.gral.data.Column;
import de.erichseifert.gral.data.DataSeries;
import de.erichseifert.gral.data.DataSource;
import de.erichseifert.gral.data.statistics.Statistics;
import de.erichseifert.gral.io.data.DataReader;
import de.erichseifert.gral.io.data.DataReaderFactory;
import de.erichseifert.gral.plots.Plot;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.axes.AxisRenderer;
import de.erichseifert.gral.plots.colors.QuasiRandomColors;
import de.erichseifert.gral.plots.colors.RainbowColors;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.plots.lines.LineRenderer;
import de.erichseifert.gral.ui.InteractivePanel;
import de.erichseifert.gral.util.Insets2D;

public class Plotter {

	public static String PLOT_DIR;

	// THE FOLLOWING PLOTS THE GIVEN .csv FILES.
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) {

		if (args.length < 2) {
			System.err.println("Arguments required: VERTICAL_AXIS FILES");
			System.exit(-1);
		}
		String verticalAxisName = args[0];
		ArrayList<String> arguments = new ArrayList<String>();
		for (int i = 1; i<args.length; i++)
			arguments.add(args[i]);
		
		Class[] columnClasses = {Double.class, Double.class};
		String[] columnNames = {"Step Count", verticalAxisName};
		
		new Plotter(arguments, "", columnClasses, columnNames, 1, false);
	}
	
	// THE FOLLOWING PLOTS ALL .csv FILES IN THE GIVEN DIRECTORY.
	/*
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) {

		if (args.length != 1) {
			System.err.println("Argument required: PLOT_DIR (String)");
			System.exit(-1);
		}
		PLOT_DIR = args[0];
		
		//Class[] tableFormat = {Double.class, Double.class, Double.class, 
		//		   Double.class, Double.class, Double.class};
		
		
		Class[] columnClasses = {Double.class, Double.class};
		String[] columnNames = {"Step Count", "Percentage Completion"};
		
		new Plotter(".*__avg.csv", PLOT_DIR, columnClasses, columnNames, 1, false);
	}
	*/

	JFrame frame;
	
	// GRAL plotting objects
	XYPlot plot;
	InteractivePanel panel;
		
	/**
	 * Plot the contents of all .csv files in the given directory matching the 
	 * given regular expression string.
	 */
	Plotter(ArrayList<String> matchedFilenames, String dirName, Class<? extends Number>[] columnClasses, String[] columnNames, int column, boolean selectBest) {
		// Go through each matched filename and create the corresponding
		// DataSeries.
		int n = matchedFilenames.size();
		DataSeries series[] = new DataSeries[n];
		DataReaderFactory factory = DataReaderFactory.getInstance();
		DataReader reader = factory.get("text/csv");
		for (int i=0; i<n; i++) {
			String filename = matchedFilenames.get(i);
			System.out.println("Plotter: Reading " + filename);
			DataSource ds = null;
			try {
				ds = reader.read(new FileInputStream(dirName + "/" + filename), columnClasses);
			} catch (Exception e) {
				System.err.println("Plotter: Problem reading: " + filename);
				System.err.println(e);
			}
			String legendName = filename.substring(0, filename.lastIndexOf('.'));

if (filename.contains("VARIANT_1"))
	legendName = "BEECLUST";
else if (filename.contains("ALPHA_0"))
	legendName = "ODOCLUST, alpha = 0";
else if (filename.contains("ALPHA_1"))
	legendName = "ODOCLUST, alpha = 1";
else if (filename.contains("ALPHA_2"))
	legendName = "ODOCLUST, alpha = 2";
else if (filename.contains("ALPHA_5"))
	legendName = "ODOCLUST, alpha = 5";
else if (filename.contains("VARIANT_3"))
	legendName = "ODOCLUST, alpha = 1";
else
	legendName = "???";

			series[i] = new DataSeries(legendName, ds, 0, column);
		}
		
//		sortByMean(series);

		if (selectBest) {
			if (column == 1) {
				// For number of clusters we would like the lowest
				DataSeries top[] = new DataSeries[Math.min(n, 10)];
				for (int i=0; i<top.length; i++)
					top[top.length-i-1] = series[n-i-1];
				series = top;
			} else if (column == 2 || column == 5) {
				// For mean and max cluster size we would like the highest
				DataSeries top[] = new DataSeries[Math.min(n, 25)];
				for (int i=0; i<top.length; i++)
					top[i] = series[i];
				series = top;	
			}
		}

		// Create the plot.
		plot = new XYPlot(series);

		// Assign a different colour to each series.
		//QuasiRandomColors colorMapper = new QuasiRandomColors();
		RainbowColors colorMapper = new RainbowColors();
		for (int i=0; i<series.length; i++) {
			Color color = colorMapper.get((series.length - i - 1)/(float)series.length);
			LineRenderer lineRenderer = new DefaultLineRenderer2D();
			lineRenderer.setSetting(DefaultLineRenderer2D.COLOR, color);

			float lineWidth = 2f;
			float[] dashArray = {i+1};
			
			//if (i==0)
			
			
//dashArray = null;
			
if (series[i].getName().contains("BEECLUST")) {
	lineWidth = 2f;
	dashArray = null;
	lineRenderer.setSetting(DefaultLineRenderer2D.COLOR, Color.red);
		
} else if (series[i].getName().contains("ODOCLUST, alpha = 0")) {
	lineWidth = 4f;
	dashArray = null;
	lineRenderer.setSetting(DefaultLineRenderer2D.COLOR, Color.blue);
} else if (series[i].getName().contains("ODOCLUST, alpha = 1")) {
	lineWidth = 2f;
	dashArray = null;
	lineRenderer.setSetting(DefaultLineRenderer2D.COLOR, Color.blue);
} else if (series[i].getName().contains("ODOCLUST, alpha = 2")) {
	lineWidth = 1f;
	lineRenderer.setSetting(DefaultLineRenderer2D.COLOR, Color.blue);
} else if (series[i].getName().contains("ODOCLUST, alpha = 5")) {
	lineWidth = 1f;
	lineRenderer.setSetting(DefaultLineRenderer2D.COLOR, Color.blue);
}

			
			lineRenderer.setSetting(LineRenderer.STROKE, 
						new BasicStroke(lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1.0f, dashArray, 0));

			plot.setLineRenderer(series[i], lineRenderer);
			plot.setPointRenderer(series[i], null);
		}
		
		String yLabel = columnNames[column];		
		setPlotSettings(series, columnNames[0], yLabel);
		createFrameAndPanel();
	}

	Plotter(String regex, String dirName, Class<? extends Number>[] columnClasses, String[] columnNames, int column, boolean selectBest) {
		// Scan the results directory for matching filenames.  Put the matched
		// ones into matchedFilenames.
		this(FileUtils.getMatchedFilenames(regex, dirName), dirName, columnClasses, columnNames, column, selectBest);
	}

	class MeanComparator implements Comparator<DataSeries> {
		@Override
		public int compare(DataSeries o1, DataSeries o2) {
			double mean1 = o1.getColumn(1).getStatistics(Statistics.MEAN);
			double mean2 = o2.getColumn(1).getStatistics(Statistics.MEAN);
			if (mean1 < mean2)
				return 1;
			else if (mean2 < mean1)
				return -1;
			else
				return 0;
		}
	}
	
	private void sortByMean(DataSeries[] series) {
		Arrays.sort(series, new MeanComparator());
	}

	private void setPlotSettings(DataSeries series[], String xLabel, String yLabel) {
//		plot.setInsets(new Insets2D.Double(20, 60, 60, 40));
plot.setInsets(new Insets2D.Double(20, 80, 60, 40));
		plot.getAxisRenderer(XYPlot.AXIS_X).setSetting(
				AxisRenderer.LABEL, xLabel);
		plot.getAxisRenderer(XYPlot.AXIS_Y).setSetting(
				AxisRenderer.LABEL, yLabel);
		plot.setSetting(XYPlot.LEGEND, true);
		plot.setSetting(Plot.LEGEND_LOCATION, Location.SOUTH_EAST);
		
		// Set the range and number of ticks.
		double xMax = 0;
		double yMax = 0;
		for (DataSeries s : series) {
			if (s.getRowCount() == 0)
				continue;
			Column xCol = s.getColumn(0);
			Column yCol = s.getColumn(1);
			double sxMax = xCol.getStatistics(Statistics.MAX);
			double syMax = yCol.getStatistics(Statistics.MAX);
			xMax = Math.max(xMax, sxMax);
			yMax = Math.max(yMax, syMax);
		}
		
		// Round yMax up to the nearest 20
//		yMax = 20 * Math.ceil(yMax / 20);
//		System.out.println("yMax:" + yMax);
		
		plot.getAxis(XYPlot.AXIS_X).setRange(0, xMax);

// If we're dealing with percentage completion then yMax should be <= 100.  Focus
// on the upper portion
if (yMax >=60 && yMax <= 100)
plot.getAxis(XYPlot.AXIS_Y).setRange(0, 105);
else		
		plot.getAxis(XYPlot.AXIS_Y).setRange(0, yMax);
		plot.getAxisRenderer(XYPlot.AXIS_X).setSetting(
				AxisRenderer.TICKS_SPACING, xMax / 5);
		plot.getAxisRenderer(XYPlot.AXIS_Y).setSetting(
				AxisRenderer.TICKS_SPACING, yMax / 5);
	}
	
	private void createFrameAndPanel() {
		// Create frame to display grid and entropy plot.
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					frame = new JFrame("Plotter");
					frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

					panel = new InteractivePanel(plot);
					frame.getContentPane().add(panel);

					frame.pack();
					frame.setLocation(600, 25);
					frame.setSize(650, 650);
					frame.setVisible(true);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void dispose() {
		frame.dispose();
	}	
}
