package launcher.controller;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import launcher.Logging;
import launcher.Logging.LogLevel;
import launcher.beans.PackageBean;

public class LauncherRestartController implements Runnable {

	private Logging logging;
	private long bootstrapModified;
	private PackageBean activePackageBean;
	
	public LauncherRestartController(Logging logging) {
		super();
		this.logging = logging;
		bootstrapModified = new File("bootstrap.jar").lastModified();
	}

	public void setActivePackageBean(PackageBean activePackageBean) {
		this.activePackageBean = activePackageBean;
	}
	
	@Override
	public void run() {
		if (!bootstrapUpdated())
			if (!launcherUpdated())
				noUpdateExecPostCmd();
	}
	
	private void noUpdateExecPostCmd() {
		if (activePackageBean == null) {
			logging.logDebug("no commands executed: no package selected!");
			if (logging.isShwoStatusMessages()) JOptionPane.showMessageDialog(null, "Launcher finished.", "Launcher", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		// NO UPDATE REQUIRES A RESTART -> EXECUTE 'POST_COMMAND' IN
		// 'POST_CWD'
		if (activePackageBean.getPostCommand() != null) {
			if (logging.isShwoStatusMessages()) JOptionPane.showMessageDialog(null, "Launcher finished, execute post command '" + activePackageBean.getPostCommand() + "'.", "Launcher", JOptionPane.INFORMATION_MESSAGE);
			
			logging.log(LogLevel.INFO, "On launch execute '"+ activePackageBean.getPostCommand() + "'");
			logging.getStatusListener().setStatusCompletedExecCommandOnExit(() -> {
				try {
					logging.logInfo("Execute '"+ activePackageBean.getPostCommand() + "' ...");
					Runtime.getRuntime().exec(activePackageBean.getPostCommand(), null, activePackageBean.getPostCWD());
				} catch (IOException e) {
					logging.printException(e);
				}
			});
		} else {
			if (logging.isShwoStatusMessages()) JOptionPane.showMessageDialog(null, "Launcher finished, no post command.", "Launcher", JOptionPane.INFORMATION_MESSAGE);
			logging.getStatusListener().setStatusCompletedExecCommandOnExit(() -> exit(0, "Launcher finished!"));
		}
	}

	private boolean launcherUpdated() {
		boolean launcherUpdated = new File("launcher_new.jar").exists();
		if (launcherUpdated) {
			logging.logDebug("Launcher update! Restart!");
			if (logging.isShwoStatusMessages()) JOptionPane.showMessageDialog(null, "Launcher updated!", "Launcher", JOptionPane.INFORMATION_MESSAGE);
	
			logging.log(LogLevel.INFO, "On launch execute 'java -jar bootstrap.jar'");
			logging.getStatusListener().setStatusCompletedExecCommandOnExit(() -> {
				try {
					Runtime.getRuntime().exec("java -jar bootstrap.jar");
					exit(0, "Launcher update! Restart!");
				} catch (IOException e) {
					logging.printException(e);
				}
			});
		}
		return launcherUpdated;
	}

	private boolean bootstrapUpdated() {
		boolean bootstrapUpdated = bootstrapModified < new File("bootstrap.jar").lastModified();
		if (bootstrapUpdated) {
			logging.logDebug("Bootstrap update! Restart!");
			if (logging.isShwoStatusMessages()) JOptionPane.showMessageDialog(null, "Bootstrap updated!", "Launcher", JOptionPane.INFORMATION_MESSAGE);
	
			logging.log(LogLevel.INFO, "On launch execute 'java -jar bootstrap.jar'");
			logging.getStatusListener().setStatusCompletedExecCommandOnExit(() -> {
				try {
					Runtime.getRuntime().exec("java -jar bootstrap.jar");
					exit(0, "Bootstrap update! Restart!");
				} catch (IOException e) {
					logging.printException(e);
				}
			});
		}
		return bootstrapUpdated;
	}

	private void exit(int exitCode, String msg) {
		// TODO Auto-generated method stub
		logging.logInfo(msg);
		logging.close();
		System.exit(exitCode);
	}
}
