package launcher.controller;

import javax.xml.bind.JAXB;

import launcher.Logging;
import launcher.Logging.LogLevel;
import launcher.beans.xml.XmlComponentBean;
import launcher.beans.xml.XmlLauncherConfigBean;
import launcher.beans.xml.XmlPackageBean;

public class XmlLauncherController {

	private Logging logging;

	private XmlLauncherConfigBean launcherConfigBean;

	
	public XmlLauncherController(Logging logging, String xmlPath) {
		this.logging = logging;
		read(xmlPath);
	}
	
	private void read(String xmlPath) {
		logging.log(LogLevel.INFO, "Read XML file '" + xmlPath + "' ...");
		launcherConfigBean = (XmlLauncherConfigBean) JAXB.unmarshal(xmlPath, XmlLauncherConfigBean.class);
		logging.log(LogLevel.INFO, "  Done!");
		logging.log(LogLevel.CONFIG,     "BASE_PATH     " + "'" + (launcherConfigBean.basePath == null ? "Inherit" : launcherConfigBean.basePath.getAbsolutePath()) + "'");
		logging.log(LogLevel.CONFIG, launcherConfigBean.packages.size() + " package(s)");
		for (XmlPackageBean pkgBean : launcherConfigBean.packages) {
			logging.log(LogLevel.CONFIG, "-- PACKAGE --");
			logging.log(LogLevel.CONFIG, "NAME          " + "'" + pkgBean.name + "'");
			logging.log(LogLevel.CONFIG, "POST_CWD      " + "'" + pkgBean.postCwd + "'");
			logging.log(LogLevel.CONFIG, "POST_COMMAND  " + "'" + pkgBean.postCommand + "'");
			logging.log(LogLevel.CONFIG, "BASE_PATH     " + "'" + pkgBean.basePath + "'");
			logging.log(LogLevel.CONFIG, "DEPENDS       " + "'" + pkgBean.depends + "'");
			logging.log(LogLevel.CONFIG, pkgBean.components.size() + " component(s)");
			for (XmlComponentBean cm : pkgBean.components) {
				logging.log(LogLevel.CONFIG, "SOURCE        " + "'" + cm.source + "'");
				logging.log(LogLevel.CONFIG, "TARGET        " + "'" + cm.target + "'");
				logging.log(LogLevel.CONFIG, "COMPARE       " + "'" + cm.compare + "'");
				logging.log(LogLevel.CONFIG, "REQUIRED      " + "'" + cm.required + "'");
				logging.log(LogLevel.CONFIG, "VERSION       " + "'" + cm.version + "'");
			}
		}
	}
}
