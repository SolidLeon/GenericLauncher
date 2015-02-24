package launcher.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import launcher.Logging;
import launcher.beans.DownloadConfig;
import launcher.beans.LauncherConfig;

public class DownloadConfigController implements Runnable {

	
	private Logging logging;
	private LauncherConfig launcherConfig;
	private List<DownloadConfig> remoteConfigs = new LinkedList<>();
	
	public DownloadConfigController(Logging logging,
			LauncherConfig launcherConfig) {
		super();
		this.logging = logging;
		this.launcherConfig = launcherConfig;
	}
	
	public List<DownloadConfig> getRemoteConfigs() {
		return remoteConfigs;
	}

	@Override
	public void run() {
		

		if (launcherConfig.getDownloadConfigs().isEmpty())
			readDownloadConfigs(remoteConfigs, launcherConfig,
					Arrays.asList(launcherConfig.getBasePath().listFiles()));
		else
			readDownloadConfigs(remoteConfigs, launcherConfig,
					launcherConfig.getDownloadConfigs());	
	}

	private void readDownloadConfigs(List<DownloadConfig> remoteConfigs,
			LauncherConfig launcherConfig, List<File> dir) {
		for (File remoteConfigFile : dir) {
			DownloadConfig cfg = readDownloadConfig(
					launcherConfig.getBasePath(), remoteConfigFile);
			if (cfg.getSource().isDirectory()) {
				addDownloadConfigsRecursivly(remoteConfigs, cfg.getSource(),
						cfg.getTarget(), cfg.getSource());
			} else {
				remoteConfigs.add(cfg);
				logging.logDebug("Download Config '" + cfg.getName() + "'");
				logging.logDebug("  SOURCE=  '"
						+ cfg.getSource().getAbsolutePath() + "'");
				logging.logDebug("  TARGET=  '"
						+ cfg.getTarget().getAbsolutePath() + "'");
				logging.logDebug("  COMPARE= '"
						+ (cfg.getCompare() == null ? "None" : cfg.getCompare()
								.getAbsolutePath()) + "'");
			}
		}
		logging.logDebug("DONE");
	}

	private void addDownloadConfigsRecursivly(
			List<DownloadConfig> remoteConfigs, File basePath, File target,
			File source) {
		if (source.isFile()) {
			DownloadConfig cfg = new DownloadConfig();
			cfg.setSource(source);
			cfg.setTarget(new File(target, source.getAbsolutePath().substring(
					basePath.getAbsolutePath().length())));
			cfg.setName(source.getName() + UUID.randomUUID().toString());
			logging.logDebug("Download Config '" + cfg.getName() + "'");
			logging.logDebug("  SOURCE=  '" + cfg.getSource().getAbsolutePath()
					+ "'");
			logging.logDebug("  TARGET=  '" + cfg.getTarget().getAbsolutePath()
					+ "'");
			logging.logDebug("  COMPARE= '"
					+ (cfg.getCompare() == null ? "None" : cfg.getCompare()
							.getAbsolutePath()) + "'");
			remoteConfigs.add(cfg);
		} else {
			for (File ff : source.listFiles())
				addDownloadConfigsRecursivly(remoteConfigs, basePath, target,
						ff);
		}
	}


	/**
	 * Parse remote file and create a new {@link DownloadConfig}
	 * 
	 * @param basePath
	 * @param remoteFile
	 * @return
	 */
	private DownloadConfig readDownloadConfig(File basePath,
			File remoteFile) {
		DownloadConfig cfg = new DownloadConfig();
		cfg.setName(remoteFile.getName());

		try {
			List<String> lines = Files.readAllLines(remoteFile.toPath());

			for (String line : lines) {
				if (line.startsWith("SOURCE=")) {
					String sSource = line.substring("SOURCE=".length());
					File sourceFile = new File(sSource);
					if (!sourceFile.isAbsolute())
						sourceFile = new File(basePath, sSource);
					cfg.setSource(sourceFile);
				} else if (line.startsWith("TARGET=")) {
					String sTarget = line.substring("TARGET=".length());
					File targetFile = new File(sTarget);
					cfg.setTarget(targetFile);
				} else if (line.startsWith("COMPARE=")) {
					String sCompare = line.substring("COMPARE=".length());
					File compareFile = new File(sCompare);
					cfg.setCompare(compareFile);
				} else if (line.startsWith("VERSION=")) {
					cfg.setVersion(Integer.valueOf(line.substring("VERSION="
							.length())));
				}
			}

		} catch (IOException e) {
			logging.printException(e);
			return null;
		}

		return cfg;
	}

}
