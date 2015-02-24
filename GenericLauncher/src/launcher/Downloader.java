package launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import launcher.beans.DownloadConfig;

public class Downloader {

	private Logging logging;
	private List<DownloadConfig> remoteConfigs;
	
	public Downloader(Logging logging, List<DownloadConfig> remoteConfigs) {
		super();
		this.logging = logging;
		this.remoteConfigs = remoteConfigs;
	}

	public void run() {
		logging.logEmptyLine();
		logging.logDebug("BEGIN DOWNLOAD");
		for (DownloadConfig cfg : remoteConfigs) {
			logging.logDebug("CHECK DOWNLOAD '" + cfg.getName() + "'");
			File sourceComparisonFile = cfg.getCompare() != null ? cfg
					.getCompare() : cfg.getTarget();
			logging.logDebug("  COMPARE= '"
					+ sourceComparisonFile.getAbsolutePath() + "'");
			if (cfg.getSource().lastModified() > sourceComparisonFile
					.lastModified())
				download(cfg);
			else {
				logging.logDebug("  SKIP ALREADY UPDATE");
				logging.logDebug("  [" + cfg.getVersion() + "] " + "'"
						+ cfg.getSource().getAbsolutePath() + "'" + " -> "
						+ "'" + cfg.getTarget().getAbsolutePath() + "'");
			}
		}
		logging.logDebug("DONE!");
	}

	/**
	 * Copy files using
	 * {@link Files#copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption...)}
	 * 
	 * @param cfg
	 */
	private void download(DownloadConfig cfg) {
		try {
			logging.logInfo("  DOWNLOADING ...");
			logging.logInfo("  [" + cfg.getVersion() + "] " + "'"
					+ cfg.getSource().getAbsolutePath() + "'" + " -> " + "'"
					+ cfg.getTarget().getAbsolutePath() + "'");
			cfg.getTarget().mkdirs();
			Files.copy(cfg.getSource().toPath(), cfg.getTarget().toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logging.printException(e);
		}
	}
}
