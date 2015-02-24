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

		// SERVER LIST BEGIN
		ServerListContainer serverListContainer = new ServerListContainer(logging);
		serverListContainer.run();
		// SERVER LIST END

		// LAUNCHER CONFIGS (*.cfg) BEGIN
		List<File> launcherConfigList = getLauncherConfigList((serverListContainer.getSelected()).getBasePath());
		if (launcherConfigList.isEmpty()) {
			exit(3, "No launcher configurations found!");
		}

		logging.logInfo(launcherConfigList.size()
				+ " launcher configuration(s) loaded!");
		Object selection = launcherConfigList.get(0);
		if (launcherConfigList.size() > 1) {
			logging.logDebug("User selects configuration...");
			selection = JOptionPane.showInputDialog(null,
					"Select a configuration:", "Launcher "
							+ Launcher.class.getPackage()
									.getImplementationVersion(),
					JOptionPane.QUESTION_MESSAGE, null,
					launcherConfigList.toArray(), launcherConfigList.get(0));
			if (selection == null) {
				exit(0, "No configuration selected!");
			}
		}
		logging.logInfo("Selected launcher config '" + selection + "'");
		LauncherConfig launcherConfig = readLauncherConfig((File) selection);
		logging.logDebug("Launcher Config     '" + ((File) selection).getName()
				+ "'");
		logging.logDebug("  BASE PATH=        '"
				+ launcherConfig.getBasePath().getAbsolutePath() + "'");
		logging.logDebug("  POST COMMAND=     '"
				+ launcherConfig.getPostCommand() + "'");
		logging.logDebug("  POST CWD=         '"
				+ launcherConfig.getPostCWD().getAbsolutePath() + "'");
		logging.logDebug("  LOG LEVEL=        '" + launcherConfig.getLogLevel()
				+ "'");
		for (File f : launcherConfig.getDownloadConfigs())
			logging.logDebug("  DOWNLOAD CONFIG=  '" + f.getAbsolutePath()
					+ "'");
		// LAUNCHER CONFIGS (*.cfg) END

		DownloadConfigContainer downloadConfigContainer = new DownloadConfigContainer(logging, launcherConfig);
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
			if (launcherConfig.getPostCommand() != null) {
				try {
					logging.logInfo("Execute '"
							+ launcherConfig.getPostCommand() + "' ...");
					Runtime.getRuntime().exec(launcherConfig.getPostCommand(),
							null, launcherConfig.getPostCWD());
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


	/**
	 * Scans for launcher configurations (*.cfg files)
	 * 
	 * @param file
	 *            - a directory containing cfg files
	 * @return a list containing all cfg files withing 'file'
	 */
	private List<File> getLauncherConfigList(File file) {
		logging.logDebug("Load launcher configurations from '"
				+ file.getAbsolutePath() + "'");
		if (!file.isDirectory()) {
			logging.logDebug("This is not a directory!");
			logging.logDebug(file.getAbsolutePath());
			return null;
		}
		List<File> configurationList = new ArrayList<>();
		for (File f : file.listFiles()) {
			if (f.getName().endsWith(".cfg")) {
				logging.logDebug("  ADD '" + f.getAbsolutePath() + "'");
				configurationList.add(f);
			}
		}
		logging.logDebug("DONE!");
		return configurationList;
	}

	/**
	 * Reads the launcher configuration file (cfg) and creates a new
	 * {@link LauncherConfig}
	 * 
	 * @param launcherConfigFile
	 * @return
	 */
	private LauncherConfig readLauncherConfig(File launcherConfigFile) {
		LauncherConfig cfg = new LauncherConfig();

		try {
			List<String> lines = Files
					.readAllLines(launcherConfigFile.toPath());
			List<File> downloadConfigList = new ArrayList<>();

			for (String line : lines) {
				if (line.startsWith("BASE_PATH=")) {
					String sBasePath = line.substring("BASE_PATH=".length());
					cfg.setBasePath(new File(sBasePath));
				} else if (line.startsWith("POST_COMMAND=")) {
					String sPostCommand = line.substring("POST_COMMAND="
							.length());
					cfg.setPostCommand(sPostCommand);
				} else if (line.startsWith("POST_CWD=")) {
					String sPostCWD = line.substring("POST_CWD=".length());
					cfg.setPostCWD(new File(sPostCWD));
				} else if (line.startsWith("LOG_LEVEL=")) {
					String sLogLevel = line.substring("LOG_LEVEL=".length());
					cfg.setLogLevel(LogLevel.valueOf(sLogLevel));
				} else if (line.startsWith("DOWNLOAD_CONFIG=")) {
					String sDownloadConfig = line.substring("DOWNLOAD_CONFIG="
							.length());
					downloadConfigList.add(new File(sDownloadConfig));
				}
			}

			// Download configs post-setup
			cfg.setDownloadConfigs(downloadConfigList);
			// we need to adjust relative paths and combine them with the
			// basePath (remote path)
			// Detailed: The path is relative on the remote machine, but new
			// File(path) would create a relative path
			// on the local machine, so we would not be able to access the right
			// file.
			// So we use the remote base path to create a new File pointing to
			// the remote
			for (int i = 0; i < downloadConfigList.size(); i++) {
				File f = downloadConfigList.get(i);
				if (!f.isAbsolute()) {
					f = new File(cfg.getBasePath(), f.getPath());
					downloadConfigList.set(i, f);
				}
			}
			// If the config does not provide a 'POST_CWD' we set it to this
			// jars CWD.
			if (cfg.getPostCWD() == null) {
				cfg.setPostCWD(new File(System.getProperty("user.dir")));
			}

			// Set default log level
			if (cfg.getLogLevel() == null)
				cfg.setLogLevel(LogLevel.DEBUG);

		} catch (IOException e) {
			logging.printException(e);
			return null;
		}

		return cfg;
	}
}