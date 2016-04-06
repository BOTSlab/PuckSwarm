package arena;

import javax.swing.JOptionPane;

/**
 * MessageBoard is a singleton that can be used by any class to post messages
 * that will be displayed to the user at the end of each iteration.
 */
public class MessageBoard {

	private StringBuffer messageBuffer = new StringBuffer();
	
	private static MessageBoard instance;
	
	private MessageBoard() {
	}
	
	public static MessageBoard getMessageBoard() {
		if (instance == null)
			instance = new MessageBoard();
		return instance;
	}
	
	public void post(String msg) {
		messageBuffer.append(msg);
		//messageBuffer.append("\n");
	}
	
	/**
	 * Display the messages received since the last call to a 'Display' method
	 * using a pop-up dialog box.
	 */
	public void popUpDisplay() {
		if (messageBuffer.length() > 0) {
			JOptionPane.showMessageDialog(null, messageBuffer.toString(), "", JOptionPane.ERROR_MESSAGE);
			messageBuffer.delete(0, messageBuffer.length());
		}
	}

	/**
	 * Display to the console the messages received since the last call to 
	 * a 'Display' method
	 */
	public void consoleDisplay() {
		if (messageBuffer.length() > 0) {
			System.out.println(messageBuffer.toString());
			messageBuffer.delete(0, messageBuffer.length());
		}
	}
}
