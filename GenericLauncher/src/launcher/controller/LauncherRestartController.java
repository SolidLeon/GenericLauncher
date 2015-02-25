package launcher.controller;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import launcher.Logging;
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
		boolean bootstrapUpdated = bootstrapModified < new File("bootstrap.jar").lastModified();
		if (bootstrapUpdated) {
			logging.logDebug("Bootstrap update! Restart!");
			JOptionPane.showMessageDialog(null, "Bootstrap updated!", "Launcher", JOptionPane.INFORMATION_MESSAGE);
			try {
				Runtime.getRuntime().exec("java -jar bootstrap.jar");
				exit(0, "Bootstrap update! Restart!");
			} catch (IOException e) {
				logging.printException(e);
			}
		} else if (new File("launcher_new.jar").exists()) {
			logging.logDebug("Launcher update! Restart!");
			JOptionPane.showMessageDialog(null, "Launcher updated!", "Launcher", JOptionPane.INFORMATION_MESSAGE);
			try {
				Runtime.getRuntime().exec("java -jar bootstrap.jar");
				exit(0, "Launcher update! Restart!");
			} catch (IOException e) {
				logging.printException(e);
			}
		} else {
			if (activePackageBean == null) {
				logging.logDebug("no package selected!");
				return;
			}
			// NO UPDATE REQUIRES A RESTART -> EXECUTE 'POST_COMMAND' IN
			// 'POST_CWD'
			if (activePackageBean.getPostCommand() != null) {
				try {
					logging.logInfo("Execute '"
							+ activePackageBean.getPostCommand() + "' ...");
					Runtime.getRuntime().exec(activePackageBean.getPostCommand(),
							null, activePackageBean.getPostCWD());
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
