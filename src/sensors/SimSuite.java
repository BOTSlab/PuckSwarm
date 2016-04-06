package sensors;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import localmap.LocalMap;
import localmap.LocalMapImagePanel;

import org.jbox2d.dynamics.Body;

import arena.Arena;
import experiment.ExperimentManager;

/**
 * Represents the set of available sensors and common sensor-derived data
 * sources available to each simulated robot.  Also provides some visualization 
 * for the sensors.
 */
public class SimSuite implements Sensor, Suite {
//	OCamCamera camera;	
	GridCamera camera;	
	LocalMap localMap;
	APS aps;
	
	HashMap<String, LocalizerUpdater> localizerUpdaters = new HashMap<String, LocalizerUpdater>();
	
	JFrame suiteFrame;
	
	// Panels for drawing the image and LocalMap image.
	STImagePanel cameraPanel;
	LocalMapImagePanel mapPanel;

	// Name and body of the corresponding robot.
	String name;
	Body body;
	
	Arena arena;
	boolean enableDisplay;
	
	// Used for positioning image display JFrames.
	public static int jframeY = 25;
	
	public SimSuite(final String name, Body body, Arena arena, boolean enableDisplay) {
		this.name = name;
		this.body = body;
		this.arena = arena;
		this.enableDisplay = enableDisplay;
		
		//camera = new OCamCamera(body, arena);
		camera = new GridCamera(body, arena);
		Calibration calib = camera.getImage().getCalibration();
		
		localMap = new LocalMap(calib);
		
		aps = new SimAPS(body);

		this.enableDisplay = !ExperimentManager.isActive();
//if (!name.equalsIgnoreCase("R0"))
//this.enableDisplay = false;

		if (enableDisplay) {
			int imageWidth = calib.getImageWidth();
			int imageHeight = calib.getImageWidth();
			cameraPanel = new STImagePanel(imageWidth, imageHeight, false);	
			mapPanel = new LocalMapImagePanel(localMap.getWidth(), localMap.getHeight(), calib);	
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					suiteFrame = new JFrame(name);
					suiteFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					suiteFrame.getContentPane().setLayout(
							new BoxLayout(suiteFrame.getContentPane(), BoxLayout.Y_AXIS));
					
					// We wrap an additional panel around 'cameraPanel' so that it
					// is not stretched to the same size as 'mapPanel'.
					//JPanel wrappingPanel = new JPanel();
					//wrappingPanel.add(cameraPanel);
					//suiteFrame.getContentPane().add(wrappingPanel);
					suiteFrame.getContentPane().add(cameraPanel);
					suiteFrame.getContentPane().add(mapPanel);
					
					suiteFrame.pack();
					suiteFrame.setVisible(true);
					suiteFrame.setLocation(1595, jframeY);
					jframeY += suiteFrame.getHeight();
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see sensors.Suite#sense()
	 */
	@Override
	public void sense() {
		for (LocalizerUpdater lu : localizerUpdaters.values())
			lu.update();
		camera.sense();
		localMap.update(camera.getImage());
	}

	/* (non-Javadoc)
	 * @see sensors.Suite#draw()
	 */
	@Override
	public void draw() {
		for (LocalizerUpdater lu : localizerUpdaters.values())
			lu.draw();
		camera.draw();

		if (!arena.showCamera)
			return;
		cameraPanel.setImage(camera.getImage());
		cameraPanel.repaint();
		mapPanel.setMap(localMap);
		mapPanel.repaint();
	}

	
	/* (non-Javadoc)
	 * @see sensors.Suite#dispose()
	 */
	@Override
	public void dispose() {
		if (enableDisplay)
			suiteFrame.dispose();
	}

	/* (non-Javadoc)
	 * @see sensors.Suite#getCameraImage()
	 */
	@Override
	public STCameraImage getCameraImage() {
		return camera.getImage();
	}

	/* (non-Javadoc)
	 * @see sensors.Suite#getLocalMap()
	 */
	@Override
	public LocalMap getLocalMap() {
		return localMap;
	}

	@Override
	public void addLocalizerUpdater(LocalizerUpdater lu, String identifier) {
		lu.init(body.getTransform(), arena);
		localizerUpdaters.put(identifier, lu);
	}
	
	public LocalizerUpdater getLocalizerUpdater(String identifier) {
		return localizerUpdaters.get(identifier);
	}

	@Override
	public APS getAPS() {
		return aps;
	}

	@Override
	public String getRobotName() {
		return name;
	}

	@Override
	public int getStorageInterval() {
		return Arena.STORAGE_INTERVAL;
	}
}
