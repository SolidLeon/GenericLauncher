package launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import launcher.beans.ComponentBean;

public class Downloader {

	private Logging logging;
	private List<ComponentBean> componentList;
	
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
		logging.logDebug("DONE!");
	}

	/**
	 * Copy files using
	 * {@link Files#copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption...)}
	 * 
	 * @param component
	 */
	private void download(ComponentBean component) {
		try {
			logging.logInfo("  DOWNLOADING COMPONENT ...");
			logging.logInfo("  '" + component.getSource().getAbsolutePath() + "'" + " -> " + "'"
					+ component.getTarget().getAbsolutePath() + "'");
			component.getTarget().mkdirs();
			logging.getStatusListener().setCurrentProgress("Download '" + component.getSource().getAbsolutePath() + "' -> '" + component.getTarget().getAbsolutePath() + "'");
			Files.copy(component.getSource().toPath(), component.getTarget().toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logging.printException(e);
		}
	}
}
