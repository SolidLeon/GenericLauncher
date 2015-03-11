package launcher;

import javax.swing.SwingUtilities;

import launcher.gui.StatusDisplay;

public class Launcher implements Runnable {

	private Logging logging;
	private StatusDisplay statusDisplay;
	
	public static void main(String[] args) {
		new Thread(new Launcher()).start();
	}

	@Override
	public void run() {
		
		statusDisplay = new StatusDisplay();
		SwingUtilities.invokeLater(() -> statusDisplay.setVisible(true)); 
	}

	


}