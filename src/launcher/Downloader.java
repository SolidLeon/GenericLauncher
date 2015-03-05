package launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import sun.awt.AWTAccessor.ToolkitAccessor;
import launcher.Logging.LogLevel;
import launcher.beans.ComponentBean;

public class Downloader {

	private Logging logging;
	private List<ComponentBean> componentList;
	private int totalDownloads = 0;
	private long totalDownloadSize = 0L;
	
	public Downloader(Logging logging, List<ComponentBean> componentList) {
		super();
		this.logging = logging;
		this.componentList = componentList;
	}

	public void run() {
		if (componentList == null || componentList.isEmpty()) {
			logging.logDebug("componentList == null");
			return;
		}
		totalDownloads = 0;
		totalDownloadSize = 0L;
		logging.logEmptyLine();
		logging.logDebug("BEGIN DOWNLOAD");
		logging.getStatusListener().setCurrentProgress(0, 0, componentList.size(), "Download ...");
		logging.getStatusListener().addOverallProgress(1);
		for (ComponentBean cfg : componentList) {
			logging.getStatusListener().setCurrentProgress(logging.getStatusListener().getCurrentProgress() + 1);
			logging.logDebug("CHECK DOWNLOAD '" + cfg.getName() + "'");
			File sourceComparisonFile = cfg.getCompare() != null ? cfg.getCompare() : cfg.getTarget();
			logging.logDebug("  COMPARE= '" + sourceComparisonFile.getAbsolutePath() + "'");
			if (cfg.getSource().lastModified() > sourceComparisonFile.lastModified())
				download(cfg);
			else {
				logging.logDebug("  SKIP ALREADY UPDATE");
				logging.logDebug("  '" + cfg.getSource().getAbsolutePath() + "'" + " -> " + "'" + cfg.getTarget().getAbsolutePath() + "'");
			}
		}
		logging.log(LogLevel.FINE, String.format("Downloaded %d file(s) (%.2f kB)!", totalDownloads, (totalDownloadSize / 1024.0)));
	}

	/**
	 * Copy files using
	 * {@link Files#copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption...)}
	 * 
	 * @param component
	 */
	private void download(ComponentBean component) {
		try {
			totalDownloads += 1;
			logging.log(LogLevel.INFO, "  DOWNLOADING COMPONENT ...");
			logging.log(LogLevel.FINE, "  '" + component.getSource().getAbsolutePath() + "'" + " -> " + "'"
					+ component.getTarget().getAbsolutePath() + "'");
			component.getTarget().mkdirs();
			logging.getStatusListener().setCurrentProgress("Download '" + component.getSource().getAbsolutePath() + "' -> '" + component.getTarget().getAbsolutePath() + "'");
			Files.copy(component.getSource().toPath(), component.getTarget().toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			totalDownloadSize += Files.size(component.getTarget().toPath());
		} catch (IOException e) {
			logging.printException(e);
		}
	}
}
