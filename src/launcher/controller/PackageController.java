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
import launcher.beans.ServerBean;

public class PackageController implements Runnable {

	private Logging logging;
	private ServerBean selectedServer;
	private PackageBean selectedPackageBean;
	
	public PackageController(Logging logging,
			ServerBean selectedServer) {
		super();
		this.logging = logging;
		this.selectedServer = selectedServer;
	}
	
	public PackageBean getSelectedPackageBean() {
		return selectedPackageBean;
	}

	@Override
	public void run() {
		if (selectedServer == null) {
			if (logging != null) logging.logDebug("selectedServer == null");
			return;
		}
		List<File> packageBeanList = getPackageBeanList(selectedServer.getBasePath());
		if (packageBeanList.isEmpty()) {
			if (logging != null) logging.logDebug("no packages loaded!");
			return; //No launcher configurations found!
		}

		if (logging != null) logging.logInfo(packageBeanList.size() + " packages(s) loaded!");
		Object selection = packageBeanList.get(0);
		if (packageBeanList.size() > 1) {
			if (logging != null) logging.logDebug("User selects package ...");
			selection = JOptionPane.showInputDialog(null,
													"Select a package:", "Launcher " + Launcher.class.getPackage() .getImplementationVersion(),
													JOptionPane.QUESTION_MESSAGE, null,
													packageBeanList.toArray(), packageBeanList.get(0));
			if (selection == null) {
				if (logging != null) logging.logDebug("selection == null");
				return; //No configuration selected, or user cancelled
			}
		}
		if (logging != null) logging.logInfo("Selected package '" + selection + "'");
		selectedPackageBean = readPackageBean((File) selection);
		if (logging != null) logging.logDebug("Package             '" + ((File) selection).getName() + "'");
		if (logging != null) logging.log(LogLevel.CONFIG, "  BASE PATH=        '" + selectedPackageBean.getBasePath().getAbsolutePath() + "'");
		if (logging != null) logging.log(LogLevel.CONFIG, "  POST COMMAND=     '" + selectedPackageBean.getPostCommand() + "'");
		if (logging != null) logging.log(LogLevel.CONFIG, "  POST CWD=         '" + selectedPackageBean.getPostCWD().getAbsolutePath() + "'");
		if (selectedPackageBean.getComponentFiles().isEmpty()) {
			if (logging != null) logging.logDebug("  No components specified by this package!");
		} else {
			for (File f : selectedPackageBean.getComponentFiles())
				if (logging != null) logging.log(LogLevel.CONFIG, "  COMPONENT=        '" + f.getAbsolutePath() + "'");
		}
	}
	
	/**
	 * Scans for launcher configurations (*.cfg files)
	 * 
	 * @param file
	 *            - a directory containing cfg files
	 * @return a list containing all cfg files withing 'file'
	 */
	public List<File> getPackageBeanList(File file) {
		if (logging != null) logging.logDebug("Load package(s) from '" + file.getAbsolutePath() + "'");
		if (!file.isDirectory()) {
			if (logging != null) logging.logDebug("This is not a directory!");
			if (logging != null) logging.logDebug(file.getAbsolutePath());
			return null;
		}
		List<File> configurationList = new ArrayList<>();
		if (logging != null) logging.getStatusListener().setCurrentProgress(0, 0, file.listFiles().length, "Collect packages ...");
		for (File f : file.listFiles()) {
			if (logging != null) logging.getStatusListener().setCurrentProgress(1 + logging.getStatusListener().getCurrentProgress());
			if (f.getName().endsWith(".cfg")) {
				if (logging != null) logging.log(LogLevel.CONFIG, "  ADD '" + f.getAbsolutePath() + "'");
				configurationList.add(f);
			}
		}
		if (logging != null) logging.getStatusListener().setCurrentProgressToMax();
		if (logging != null) logging.logDebug("DONE!");
		return configurationList;
	}

	/**
	 * Reads the launcher configuration file (cfg) and creates a new
	 * {@link PackageBean}
	 * 
	 * @param packageBeanFile
	 * @return
	 */
	public PackageBean readPackageBean(File packageBeanFile) {
		PackageBean packageBean = new PackageBean();

		try {
			List<String> lines = Files.readAllLines(packageBeanFile.toPath());
			List<File> componentFileList = new ArrayList<>();

			if (logging != null) logging.getStatusListener().setCurrentProgress(0, 0, lines.size(), "Parse package file ...");
			if (logging != null) logging.getStatusListener().addOverallProgress(1);
			for (String line : lines) {
				if (logging != null) logging.getStatusListener().setCurrentProgress(logging.getStatusListener().getCurrentProgress() + 1);
				if (line.startsWith("BASE_PATH=")) {
					String sBasePath = line.substring("BASE_PATH=".length());
					packageBean.setBasePath(new File(sBasePath));
				} else if (line.startsWith("POST_COMMAND=")) {
					String sPostCommand = line.substring("POST_COMMAND=".length());
					packageBean.setPostCommand(sPostCommand);
				} else if (line.startsWith("POST_CWD=")) {
					String sPostCWD = line.substring("POST_CWD=".length());
					packageBean.setPostCWD(new File(sPostCWD));
				} else if (line.startsWith("COMPONENT=")) {
					String sComponentFile = line.substring("COMPONENT=".length());
					componentFileList.add(new File(sComponentFile));
				}
			}

			// Download configs post-setup
			packageBean.setComponentFiles(componentFileList);
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
					f = new File(packageBean.getBasePath(), f.getPath());
					componentFileList.set(i, f);
				}
			}
			// If the config does not provide a 'POST_CWD' we set it to this
			// jars CWD.
			if (packageBean.getPostCWD() == null) {
				packageBean.setPostCWD(new File(System.getProperty("user.dir")));
			}

		} catch (IOException e) {
			if (logging != null) logging.printException(e);
			return null;
		}

		return packageBean;
	}

}
