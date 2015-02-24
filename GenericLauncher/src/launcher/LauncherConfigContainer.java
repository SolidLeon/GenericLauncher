package launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import launcher.Logging.LogLevel;
import launcher.beans.LauncherConfig;
import launcher.beans.ServerListEntry;

public class LauncherConfigContainer implements Runnable {

	private Logging logging;
	private ServerListEntry selectedServer;
	private LauncherConfig selectedLauncherConfig;
	
	public LauncherConfigContainer(Logging logging,
			ServerListEntry selectedServer) {
		super();
		this.logging = logging;
		this.selectedServer = selectedServer;
	}
	
	public LauncherConfig getSelectedLauncherConfig() {
		return selectedLauncherConfig;
	}

	@Override
	public void run() {
		List<File> launcherConfigList = getLauncherConfigList(selectedServer.getBasePath());
		if (launcherConfigList.isEmpty()) {
			return; //No launcher configurations found!
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
				return; //No configuration selected, or user cancelled
			}
			selectedLauncherConfig = (LauncherConfig) selection;
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
