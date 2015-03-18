package launcher.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXB;

import launcher.Logging;
import launcher.Logging.LogLevel;
import launcher.beans.UpdateBean;
import launcher.beans.xml.XmlComponentBean;
import launcher.beans.xml.XmlLauncherConfigBean;
import launcher.beans.xml.XmlPackageBean;

public class UpdateController implements Runnable {
	
	private Logging logging;
	private IUpdateListener listener;
	
	private String localPath = null;
	private String remotePath = null;
	
	public UpdateController(IUpdateListener listener, String remotePath) {
		this.listener = listener;
		File remoteFile = new File(remotePath);
		File localFile = new File(System.getProperty("user.dir"), remoteFile.getName());
		
		this.remotePath = remotePath;
		this.localPath = localFile.getAbsolutePath();
	}
	
	public void setLogging(Logging logging) {
		this.logging = logging;
	}
	
	private XmlLauncherConfigBean downloadRemotePackage(String remotePath) {
		XmlLauncherConfigBean result = null;
		try {
			result = (XmlLauncherConfigBean) JAXB.unmarshal(remotePath, XmlLauncherConfigBean.class);
		} catch (Exception ex) {
			//ex.printStackTrace();
			System.err.println("Can not download from '" + remotePath + "'");
		}
		return result;
	}
	
	@Override
	public void run() {
		List<UpdateBean> toDownload = new ArrayList<>();
		
		// 1) Download remote package declaration
		XmlLauncherConfigBean remoteConfigBean = downloadRemotePackage(remotePath);
		XmlLauncherConfigBean localConfigBean = downloadRemotePackage(localPath);
		if (remoteConfigBean == null) {
			System.err.println("Could not download remote launcher configuration.");
		} else {
			if (localConfigBean == null) {
				localConfigBean = new XmlLauncherConfigBean();
				localConfigBean.basePath = remoteConfigBean.basePath;
				localConfigBean.packages = new ArrayList<>();
			}
			
			// 2) Select package
			XmlPackageBean remotePackage = getPackageFromInterface(remoteConfigBean);
			if (remotePackage != null) {
				System.out.println("Remote package selected: '" + remotePackage.name + "'");
				System.out.println("  " + remotePackage.components.size() + " component(s)");
				
				// 3) Check if we have a local package declaration with the same name. Yes? -> 4; No? -> 5
				XmlPackageBean localPackage = localConfigBean.getPackageByName(remotePackage.name);
				if (localPackage == null) {
					localPackage = new XmlPackageBean();
					localPackage.basePath = remotePackage.basePath;
					localPackage.name = remotePackage.name;
					localPackage.depends = remotePackage.depends;
					localPackage.postCommand = remotePackage.postCommand;
					localPackage.postCwd = remotePackage.postCwd;
					localPackage.components = new ArrayList<>();
					localPackage.components.addAll(remotePackage.components);
					localConfigBean.packages.add(localPackage);
					
					System.out.println("No local package, download all components");
					for (XmlComponentBean remoteComponent : remotePackage.components) {
						UpdateBean bean = new UpdateBean();
						bean.remote = remoteComponent;
						bean.local = null;
						bean.download = true;
						toDownload.add(bean);
					}
				} else {
					
					// 4) Compare 
					System.out.println("Compare remote <-> local ...");
					for (XmlComponentBean remoteComponentBean : remotePackage.components) {
						XmlComponentBean localComponentBean = localPackage.getComponent(remoteComponentBean);					
						if (localComponentBean == null) {
							System.out.println("No local component for '" + remoteComponentBean.source + "'");
							UpdateBean bean = new UpdateBean();
							bean.remote = remoteComponentBean;
							bean.local = null;
							bean.download = true;
							toDownload.add(bean);
							localPackage.components.add(remoteComponentBean);
						} else {
							System.out.println("Compare '" + remoteComponentBean.source + "': remote(" + remoteComponentBean.version + ") <-> local(" + localComponentBean.version + ")"  );
							if (remoteComponentBean.compare(localComponentBean) > 0) {
								System.out.println("  Component added to download queue");
								UpdateBean bean = new UpdateBean();
								bean.local = localComponentBean;
								bean.remote = remoteComponentBean;
								toDownload.add(bean);
							} else {
								// Check if the physical file is available, if not download it no matter what version!
								File localFile = new File(remoteComponentBean.target);
								if (!localFile.exists()) {
									UpdateBean bean = new UpdateBean();
									bean.local = null;
									bean.remote = remoteComponentBean;
									toDownload.add(bean);
								}
							}
						}
					}
					
				}
			}
			if (preDownload(toDownload)) {
				// 5) Download
				logging.log(LogLevel.INFO,"Download");
				for (UpdateBean bean : toDownload) {
					if (bean.download) {
						XmlComponentBean dl = bean.remote;
						logging.log(LogLevel.INFO,"Download ...");
						logging.log(LogLevel.INFO, String.format("SOURCE   = '%s'", dl.source));
						logging.log(LogLevel.INFO, String.format("TARGET   = '%s'", dl.target));
						logging.log(LogLevel.INFO, String.format("COMPARE  = '%s'", dl.compare));
						logging.log(LogLevel.INFO, String.format("REQUIRED = '%s'", dl.required));
						logging.log(LogLevel.INFO, String.format("VERSION  = '%s'", dl.version));
						try {
							File targetFile = new File(dl.target);
							URL url = new URL(dl.source);
							logging.log(LogLevel.INFO, String.format("Copy from '%s' to '%s'", url.toString(), targetFile.getAbsolutePath()));
							try (BufferedInputStream in = new BufferedInputStream(url.openStream())) {
								Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
							}
						} catch (Exception ex) {
							logging.log(LogLevel.INFO, "Can not download '" + dl.source + "' to '" + dl.target + "', try file copy ...");
							File sourceFile = new File(dl.source);
							File targetFile = new File(dl.target);
							try {
								Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
							} catch (Exception ex2) {
								logging.log(LogLevel.INFO, "Can not copy '" + sourceFile.getAbsolutePath() + "' to '" + targetFile.getAbsolutePath() + "'");
							}
						}
						bean.local.version = bean.remote.version;
					}
				}
				File saveFile = new File(localPath);
				System.out.println("Save package to '" + saveFile.getAbsolutePath() + "'");
				try {
					JAXB.marshal(localConfigBean, saveFile);
				} catch (Exception ex) {
					System.out.println("Can not write XML");
					ex.printStackTrace();
				}
			}
		}
	}

	/**
	 * 
	 * @param toDownload
	 * @return true -> ok, false -> error; cancel process!
	 */
	private boolean preDownload(List<UpdateBean> toDownload) {
		if (listener != null) 
			return listener.preDownload(toDownload);
		return false;
	}

	/**
	 * 
	 * @param remoteConfigBean
	 * @return null -> cancel!
	 */
	private XmlPackageBean getPackageFromInterface(XmlLauncherConfigBean remoteConfigBean) {
		if (listener != null)
			return listener.selectPackage(remoteConfigBean);
		return null;
	}
	
}
