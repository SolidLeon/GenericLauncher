package launcher.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import launcher.Launcher;
import launcher.Logging;
import launcher.Logging.LogLevel;
import launcher.beans.PackageBean;
import launcher.beans.ServerListEntry;

public class PackageController implements Runnable {

	private Logging logging;
	private ServerListEntry selectedServer;
	private PackageBean selectedPackageBean;
	
	public PackageController(Logging logging,
			ServerListEntry selectedServer) {
		super();
		this.logging = logging;
		this.selectedServer = selectedServer;
	}
	
	public PackageBean getSelectedLauncherConfig() {
		return selectedPackageBean;
	}

	@Override
	public void run() {
		if (selectedServer == null)
			return;
		List<File> packageBeanList = getPackageBeanList(selectedServer.getBasePath());
		if (packageBeanList.isEmpty()) {
			return; //No launcher configurations found!
		}

		logging.logInfo(packageBeanList.size()
				+ " packages(s) loaded!");
		Object selection = packageBeanList.get(0);
		if (packageBeanList.size() > 1) {
			logging.logDebug("User selects package ...");
			selection = JOptionPane.showInputDialog(null,
					"Select a package:", "Launcher "
							+ Launcher.class.getPackage()
									.getImplementationVersion(),
					JOptionPane.QUESTION_MESSAGE, null,
					packageBeanList.toArray(), packageBeanList.get(0));
			if (selection == null) {
				return; //No configuration selected, or user cancelled
			}
		}
		logging.logInfo("Selected package '" + selection + "'");
		selectedPackageBean = readPackageBean((File) selection);
		logging.logDebug("Package             '" + ((File) selection).getName()
				+ "'");
		logging.logDebug("  BASE PATH=        '"
				+ selectedPackageBean.getBasePath().getAbsolutePath() + "'");
		logging.logDebug("  POST COMMAND=     '"
				+ selectedPackageBean.getPostCommand() + "'");
		logging.logDebug("  POST CWD=         '"
				+ selectedPackageBean.getPostCWD().getAbsolutePath() + "'");
		logging.logDebug("  LOG LEVEL=        '" + selectedPackageBean.getLogLevel()
				+ "'");
		for (File f : selectedPackageBean.getComponentFiles())
			logging.logDebug("  COMPONENT=        '" + f.getAbsolutePath()
					+ "'");
	}
	
	/**
	 * Scans for launcher configurations (*.cfg files)
	 * 
	 * @param file
	 *            - a directory containing cfg files
	 * @return a list containing all cfg files withing 'file'
	 */
	private List<File> getPackageBeanList(File file) {
		logging.logDebug("Load package(s) from '"
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
	 * {@link PackageBean}
	 * 
	 * @param packageBeanFile
	 * @return
	 */
	private PackageBean readPackageBean(File packageBeanFile) {
		PackageBean cfg = new PackageBean();

		try {
			List<String> lines = Files
					.readAllLines(packageBeanFile.toPath());
			List<File> componentFileList = new ArrayList<>();

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
					String sComponentFile = line.substring("DOWNLOAD_CONFIG="
							.length());
					componentFileList.add(new File(sComponentFile));
				}
			}

			// Download configs post-setup
			cfg.setComponentFiles(componentFileList);
			// we need to adjust relative paths and combine them with the
			// basePath (remote path)
			// Detailed: The path is relative on the remote machine, but new
			// File(path) would create a relative path
			// on the local machine, so we would not be able to access the right
			// file.
			// So we use the remote base path to create a new File pointing to
			// the remote
			for (int i = 0; i < componentFileList.size(); i++) {
				File f = componentFileList.get(i);
				if (!f.isAbsolute()) {
					f = new File(cfg.getBasePath(), f.getPath());
					componentFileList.set(i, f);
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
