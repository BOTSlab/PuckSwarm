package experiment;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class EditableExperiment extends Experiment implements ActionListener {

	String code;
	JFrame frame;
	JPanel panel;
	HashMap<String,ArrayList<PropertiesListener>> listeners = new HashMap<String,ArrayList<PropertiesListener>>(); 
	
	EditableExperiment(int seed, String outputDir, String code) {
		super(0, outputDir, null);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				frame = new JFrame("EditableExperiment");
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				
				panel = new JPanel();
				panel.setLayout(new GridLayout(0, 2));
				
				frame.add(panel);
				frame.pack();
				frame.setVisible(true);
			}
		});
	}
	
	public String getStringCode() {
		return code + "__" + seed;
	}

	public String getStringCodeWithoutSeed() {
		return code;
	}

	private void incorporateProperty(final String key, final String value, final PropertiesListener listener) {
		if (properties.getProperty(key) == null) {
			// Add the property
			properties.put(key, value);
			
			// Create a text field for editing the property.  Any modifications to
			// the content of the text field will yield a call to 'actionPerformed'
			// below, which will in turn delegate to the listeners.
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					panel.add(new JLabel(key));
					JTextField textField = new JTextField(value);
					textField.setActionCommand(key);
					textField.addActionListener(EditableExperiment.this);
					if (listener == null) {
						textField.setBackground(Color.LIGHT_GRAY);
					}
					panel.add(textField);
					frame.pack();
				}
			});
		}
		
		if (listener == null)
			return;
		
		// Add the listener if it doesn't already exist.
		if (listeners.containsKey(key)) {
			// We have at least one existing listener.
			ArrayList<PropertiesListener> list = listeners.get(key);
			if (!list.contains(listener))
				list.add(listener);
		} else {
			ArrayList<PropertiesListener> list = new ArrayList<PropertiesListener>();
			list.add(listener);
			listeners.put(key, list);
		}
	}

	public int getProperty(String key, int value, PropertiesListener listener) {
		incorporateProperty(key, value + "", listener);
		return super.getProperty(key, value, null);
	}

	public float getProperty(String key, float value, PropertiesListener listener) {		
		incorporateProperty(key, value + "", listener);
		return super.getProperty(key, value, null);
	}

	public boolean getProperty(String key, boolean value, PropertiesListener listener) {		
		incorporateProperty(key, value + "", listener);
		return super.getProperty(key, value, null);
	}

	public String getProperty(String key, String value, PropertiesListener listener) {
		incorporateProperty(key, value, listener);
		return super.getProperty(key, value, null);
	}

	/**
	 * The contents of a JTextField has been modified by the user.  Inform
	 * all listeners for the given 'key' (the key is stored as the ActionCommand
	 * of the JTextField, and hence of the ActionEvent).
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		String key = e.getActionCommand();
		JTextField textField = (JTextField) e.getSource();
		
		// Modify the appropriate property.
		properties.put(key, textField.getText());
		
		// Call all listeners to key.
		ArrayList<PropertiesListener> list = listeners.get(key);
		if (list == null)
			return;
		for (int i=0; i<list.size(); i++)
			list.get(i).propertiesUpdated();
	}	
}
