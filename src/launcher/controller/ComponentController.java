package launcher.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import launcher.Logging;
import launcher.Logging.LogLevel;
import launcher.beans.ComponentBean;
import launcher.beans.PackageBean;

public class ComponentController implements Runnable {

	
	private Logging logging;
	private PackageBean packageBean;
	private List<ComponentBean> resultComponentList = new LinkedList<>();
	
	public ComponentController(Logging logging,
			PackageBean launcherConfig) {
		super();
		this.logging = logging;
		this.packageBean = launcherConfig;
	}
	
	public List<ComponentBean> getResultComponentList() {
		return resultComponentList;
	}

	@Override
	public void run() {
		if (packageBean == null) {
			logging.logDebug("packageBean == null");
			return;
		}
		if (packageBean.getComponentFiles() == null || packageBean.getComponentFiles().isEmpty()) {
			logging.logDebug("package does not contain any components!");
			return;
		}
			
		readComponentBeans(resultComponentList, packageBean,
				packageBean.getComponentFiles());	
	}

	private void readComponentBeans(List<ComponentBean> remoteConfigs,
			PackageBean launcherConfig, List<File> dir) {

		logging.getStatusListener().setCurrentProgress(0, 0, dir.size(), "Read components ...");
		logging.getStatusListener().addOverallProgress(1);
		
		for (File remoteConfigFile : dir) {
			logging.getStatusListener().setCurrentProgress(logging.getStatusListener().getCurrentProgress() + 1);
			ComponentBean componentBean = readComponentBean(launcherConfig.getBasePath(), remoteConfigFile);
			
			if (componentBean.getSource().isDirectory()) {
				addComponentBeansRecursivly(remoteConfigs, componentBean.getSource(),componentBean.getTarget(), componentBean.getSource());
			} else {
				remoteConfigs.add(componentBean);
				logging.log(LogLevel.INFO, "COMPONENT  '" + componentBean.getName() + "'");
				logging.log(LogLevel.CONFIG, "  SOURCE=  '" + componentBean.getSource().getAbsolutePath() + "'");
				logging.log(LogLevel.CONFIG, "  TARGET=  '" + componentBean.getTarget().getAbsolutePath() + "'");
				logging.log(LogLevel.CONFIG, "  COMPARE= '" + (componentBean.getCompare() == null ? "None" : componentBean.getCompare().getAbsolutePath()) + "'");
			}
		}
		logging.getStatusListener().setCurrentProgressToMax();
		logging.logDebug("DONE");
	}


	private void addComponentBeansRecursivly(
			List<ComponentBean> remoteConfigs, File basePath, File target,
			File source) {
		if (source.isFile()) {
			ComponentBean cfg = new ComponentBean();
			cfg.setSource(source);
			cfg.setTarget(new File(target, source.getAbsolutePath().substring(basePath.getAbsolutePath().length())));
			cfg.setName(source.getName() + UUID.randomUUID().toString());
			logging.log(LogLevel.INFO, "COMPONENT  '" + cfg.getName() + "'");
			logging.log(LogLevel.CONFIG, "  SOURCE=  '" + cfg.getSource().getAbsolutePath()+ "'");
			logging.log(LogLevel.CONFIG, "  TARGET=  '" + cfg.getTarget().getAbsolutePath()+ "'");
			logging.log(LogLevel.CONFIG, "  COMPARE= '"+ (cfg.getCompare() == null ? "None" : cfg.getCompare().getAbsolutePath()) + "'");
			remoteConfigs.add(cfg);
		} else {
			for (File ff : source.listFiles())
				addComponentBeansRecursivly(remoteConfigs, basePath, target,
						ff);
		}
	}


	/**
	 * Parse remote file and create a new {@link ComponentBean}
	 * 
	 * @param basePath
	 * @param remoteFile
	 * @return
	 */
	private ComponentBean readComponentBean(File basePath,
			File remoteFile) {
		ComponentBean cfg = new ComponentBean();
		cfg.setName(remoteFile.getName());

		try {
			logging.logDebug("Read component bean from '" + remoteFile.toPath() + "'");
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
				}
			}

		} catch (IOException e) {
			logging.printException(e);
			return null;
		}

		return cfg;
	}

}
