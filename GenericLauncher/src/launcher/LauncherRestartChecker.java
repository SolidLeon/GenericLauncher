package launcher;

import java.io.File;
import java.io.IOException;

public class LauncherRestartChecker implements Runnable {

	private Logging logging;
	private long bootstrapModified;
	private LauncherConfig activeLauncherConfig;
	
	public LauncherRestartChecker(Logging logging) {
		super();
		this.logging = logging;
		bootstrapModified = new File("bootstrap.jar").lastModified();
	}

	public void setActiveLauncherConfig(LauncherConfig activeLauncherConfig) {
		this.activeLauncherConfig = activeLauncherConfig;
	}
	
	@Override
	public void run() {
		boolean bootstrapUpdated = bootstrapModified < new File("bootstrap.jar").lastModified();
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
			if (activeLauncherConfig.getPostCommand() != null) {
				try {
					logging.logInfo("Execute '"
							+ activeLauncherConfig.getPostCommand() + "' ...");
					Runtime.getRuntime().exec(activeLauncherConfig.getPostCommand(),
							null, activeLauncherConfig.getPostCWD());
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
