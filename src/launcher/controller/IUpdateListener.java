package launcher.controller;

import java.util.List;

import launcher.beans.UpdateBean;
import launcher.beans.xml.XmlLauncherConfigBean;
import launcher.beans.xml.XmlPackageBean;

public interface IUpdateListener {

	boolean preDownload(List<UpdateBean> toDownload);

	XmlPackageBean selectPackage(XmlLauncherConfigBean remoteConfigBean);

	void postUpdate(Runnable runner);

}
