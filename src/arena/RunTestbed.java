package arena;

import javax.swing.JFrame;

import org.jbox2d.testbed.framework.TestList;
import org.jbox2d.testbed.framework.TestbedFrame;
import org.jbox2d.testbed.framework.TestbedModel;
import org.jbox2d.testbed.framework.TestbedPanel;
import org.jbox2d.testbed.framework.TestbedSetting;
import org.jbox2d.testbed.framework.TestbedSettings;
import org.jbox2d.testbed.framework.j2d.TestPanelJ2D;

public class RunTestbed {

	public static void main(String[] args) {

		TestbedModel model = new TestbedModel();
		
		// Customize the testbed
		TestbedSettings settings = model.getSettings();
		settings.getSetting("Hz").value = 14;
		settings.getSetting("Draw Stats").enabled = false;
		settings.addSetting(new TestbedSetting("Activate Experiments", TestbedSetting.SettingType.DRAWING, false));
		settings.addSetting(new TestbedSetting("Allow Thinking", TestbedSetting.SettingType.DRAWING, true));
		settings.addSetting(new TestbedSetting("Allow Forwards", TestbedSetting.SettingType.DRAWING, true));
		settings.addSetting(new TestbedSetting("Allow Turning", TestbedSetting.SettingType.DRAWING, true));
		settings.addSetting(new TestbedSetting("Forced March", TestbedSetting.SettingType.DRAWING, false));
		settings.addSetting(new TestbedSetting("Forced Turn", TestbedSetting.SettingType.DRAWING, false));
		settings.addSetting(new TestbedSetting("Pause on Messages", TestbedSetting.SettingType.DRAWING, false));
		settings.addSetting(new TestbedSetting("Show Cameras", TestbedSetting.SettingType.DRAWING, true));

		TestbedPanel panel = new TestPanelJ2D(model);

		// Add tests
		PuckSwarmTest test = new PuckSwarmTest();
		model.addCategory("My Tests");
		model.addTest(test);
		TestList.populateModel(model);    // Populate with the default tests

		JFrame frame = new TestbedFrame(model, panel);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocation(0, 0);
		frame.setSize(1550, 850);
	}
}