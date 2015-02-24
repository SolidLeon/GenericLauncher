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

public class Launcher implements Runnable {

	private Logging logging;
	
	public static void main(String[] args) {
		new Thread(new Launcher()).start();
	}

	@Override
	public void run() {

		logging = new Logging();
		
		logging.setup();

		// SETUP logging BEGIN
		// SETUP logging END

		long bootstrapModified = new File("bootstrap.jar").lastModified();

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

		ServerListContainer serverListContainer = new ServerListContainer(logging);
		serverListContainer.run();

		LauncherConfigContainer launcherConfigContainer = new LauncherConfigContainer(logging, serverListContainer.getSelected());
		launcherConfigContainer.run();

		DownloadConfigContainer downloadConfigContainer = new DownloadConfigContainer(logging, launcherConfigContainer.getSelectedLauncherConfig());
		downloadConfigContainer.run();

		Downloader downloader = new Downloader(logging, downloadConfigContainer.getRemoteConfigs());
		downloader.run();

		// CHECK IF SOMETHING WAS UPDATED THAT REQUIRES A LAUNCHER RESTART
		boolean bootstrapUpdated = bootstrapModified < new File("bootstrap.jar")
				.lastModified();
		if (bootstrapUpdated) {
			logging.logDebug("Bootstrap update! Restart!");
			try {
				Runtime.getRuntime().exec("java -jar bootstrap.jar");
				exit(0, "Bootstrap update! Restart!");
			} catch (IOException e) {
				logging.printException(e);
			}
		} else if (new File("launcher_new.jar").exists()) {
			logging.logDebug("Launcher update! Restart!");
			try {
				Runtime.getRuntime().exec("java -jar bootstrap.jar");
				exit(0, "Launcher update! Restart!");
			} catch (IOException e) {
				logging.printException(e);
			}
		} else {
			// NO UPDATE REQUIRES A RESTART -> EXECUTE 'POST_COMMAND' IN
			// 'POST_CWD'
			if (launcherConfigContainer.getSelectedLauncherConfig().getPostCommand() != null) {
				try {
					logging.logInfo("Execute '"
							+ launcherConfigContainer.getSelectedLauncherConfig().getPostCommand() + "' ...");
					Runtime.getRuntime().exec(launcherConfigContainer.getSelectedLauncherConfig().getPostCommand(),
							null, launcherConfigContainer.getSelectedLauncherConfig().getPostCWD());
				} catch (IOException e) {
					logging.printException(e);
				}
			}

			exit(0, "Launcher finished!");
		}
	}
	
	private void exit(int exitCode, String msg) {
		// TODO Auto-generated method stub
		logging.logInfo(msg);
		logging.close();
		System.exit(exitCode);
	}


}