package launcher.test;

import java.util.ArrayList;
import java.util.List;

import launcher.Logging;
import launcher.beans.xml.XmlComponentBean;
import launcher.beans.xml.XmlLauncherConfigBean;
import launcher.beans.xml.XmlPackageBean;

public class RemoteLauncherTest {

	public static void main(String[] args) {
		Logging logging = new Logging(null);
		String localPath = "C:\\DEPLOYMENT\\TEST";
		String remotePath = "http://localhost:2345/web-package.xml";

		// 1) Download remote package declaration
		XmlLauncherConfigBean configBean = downloadRemotePackage();
		if (configBean == null) {
			System.err.println("Could not download remote launcher configuration.");
		} else {
			// 2) Select package
			XmlPackageBean remotePackage = null;
			// 3) Check if we have a local package declaration with the same name. Yes? -> 4; No? -> 5
			XmlPackageBean localPackage = configBean.getPackageByName(remotePackage.name);
			List<XmlComponentBean> toDownload = new ArrayList<>();
			if (localPackage == null) {
				toDownload.addAll(remotePackage.components);
			} else {
				// 4) Compare 
				for (XmlComponentBean remoteComponentBean : remotePackage.components) {
					XmlComponentBean localComponentBean = localPackage.getComponent(remoteComponentBean);
					if (localComponentBean == null) {
						toDownload.add(remoteComponentBean);
					} else {
						if (remoteComponentBean.compare(localComponentBean) > 0) {
							toDownload.add(remoteComponentBean);
						}
					}
				}
			}
			// 5) Download
		}
	}

	private static XmlLauncherConfigBean downloadRemotePackage() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
