package launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.swing.JOptionPane;

import launcher.Logging.LogLevel;
import launcher.controller.DownloadConfigController;
import launcher.controller.LauncherConfigController;
import launcher.controller.LauncherRestartController;
import launcher.controller.ServerListController;

public class Launcher implements Runnable {

	private Logging logging;
	
	public static void main(String[] args) {
		new Thread(new Launcher()).start();
	}

	@Override
	public void run() {
		logging = new Logging();
		
		LauncherRestartController launcherRestartChecker = new LauncherRestartController(logging);

		logBasicInfo();

		ServerListController serverListContainer = new ServerListController(logging);
		serverListContainer.run();

		LauncherConfigController launcherConfigContainer = new LauncherConfigController(logging, serverListContainer.getSelected());
		launcherConfigContainer.run();
		launcherRestartChecker.setActiveLauncherConfig(launcherConfigContainer.getSelectedLauncherConfig());

		DownloadConfigController downloadConfigContainer = new DownloadConfigController(logging, launcherConfigContainer.getSelectedLauncherConfig());
		downloadConfigContainer.run();

		Downloader downloader = new Downloader(logging, downloadConfigContainer.getRemoteConfigs());
		downloader.run();

		// CHECK IF SOMETHING WAS UPDATED THAT REQUIRES A LAUNCHER RESTART
		launcherRestartChecker.run();
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
		logging.logInfo("System.getProperty('sun.arch.data.model') == '"
				+ System.getProperty("sun.arch.data.model") + "'");
		logging.logEmptyLine();
	}
	


}