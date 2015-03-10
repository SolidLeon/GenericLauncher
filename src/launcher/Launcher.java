package launcher;

import java.util.Date;

import javax.swing.SwingUtilities;

import launcher.Logging.LogLevel;
import launcher.controller.ComponentController;
import launcher.controller.LauncherRestartController;
import launcher.controller.PackageController;
import launcher.controller.ServerListController;
import launcher.gui.PreviewDialog;
import launcher.gui.StatusDisplay;
import launcher.gui.PreviewDialog.PreviewResult;

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