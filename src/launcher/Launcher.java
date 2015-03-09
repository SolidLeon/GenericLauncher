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
		
		logging = new Logging(statusDisplay);
		logBasicInfo();
		
		logging.getStatusListener().setOverallProgress(0, 0, 4);
		
		logging.getStatusListener().setCurrentProgress(0, 0, 0, "Initialize launcher restart ...");
		LauncherRestartController launcherRestartController = new LauncherRestartController(logging);

		ServerListController serverListController = new ServerListController(logging);
		serverListController.run();

		PackageController packageController = new PackageController(logging, serverListController.getSelected());
		packageController.run();
		launcherRestartController.setActivePackageBean(packageController.getSelectedPackageBean());

		ComponentController componentController = new ComponentController(logging, packageController.getSelectedPackageBean());
		componentController.run();
		
		PreviewDialog previewDialog = new PreviewDialog(statusDisplay, componentController.getResultComponentList());
		previewDialog.setVisible(true);
		
		PreviewResult previewResult = previewDialog.getPreviewResult();
		if (previewResult == PreviewResult.OK) {
			Downloader downloader = new Downloader(logging, componentController.getResultComponentList());
			downloader.run();
	
			// CHECK IF SOMETHING WAS UPDATED THAT REQUIRES A LAUNCHER RESTART
			launcherRestartController.run();
		} else {
			logging.log(LogLevel.INFO, "User cancelled preview");
		}
		statusDisplay.setStatusCompleted(); //// SolidLeon #4 20150227 - we set the overall status so even if the user cancels the end-state is completed
		logging.log(LogLevel.FINE, "Done!");
	}

	private void logBasicInfo() {
		logging.logInfo("Launcher "
				+ Launcher.class.getPackage().getImplementationVersion()
				+ " started");
		logging.logInfo("Current Time is " + new Date().toString());
		logging.logInfo("System.getProperty('os.name') == '"
				+ System.getProperty("os.name") + "'");
		logging.logInfo("System.getProperty('os.version') == '"
				+ System.getProperty("os.version") + "'");
		logging.logInfo("System.getProperty('os.arch') == '"
				+ System.getProperty("os.arch") + "'");
		logging.logInfo("System.getProperty('java.version') == '"
				+ System.getProperty("java.version") + "'");
		logging.logInfo("System.getProperty('java.vendor') == '"
				+ System.getProperty("java.vendor") + "'");
		logging.logInfo("System.getProperty('java.home') == '"
				+ System.getProperty("java.home") + "'");
		logging.logInfo("System.getProperty('sun.arch.data.model') == '"
				+ System.getProperty("sun.arch.data.model") + "'");
		logging.logInfo("System.getProperty('user.dir') == " + "'" + System.getProperty("user.dir") + "'");
		logging.logEmptyLine();
	}
	


}