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
			logging.log(LogLevel.INFO, "Get launcher configuration from '" + remotePath + "'");
			result = (XmlLauncherConfigBean) JAXB.unmarshal(remotePath, XmlLauncherConfigBean.class);
		} catch (Exception ex) {
			//ex.printStackTrace();
			logging.log(LogLevel.ERROR, "Can not get launcher configuration from '" + remotePath + "'");
		}
		return result;
	}
	
	@Override
	public void run() {
		List<UpdateBean> toDownload = new ArrayList<>();
		
		// 1) Download remote package declaration
		XmlLauncherConfigBean remoteConfigBean = downloadRemotePackage(remotePath);
		XmlLauncherConfigBean localConfigBean = downloadRemotePackage(localPath);
		if (remoteConfigBean != null) {
			if (localConfigBean == null) {
				localConfigBean = new XmlLauncherConfigBean();
				localConfigBean.basePath = remoteConfigBean.basePath;
				localConfigBean.packages = new ArrayList<>();
			}
			
			// 2) Select package
			XmlPackageBean remotePackage = getPackageFromInterface(remoteConfigBean);
			if (remotePackage != null) {
				logging.log(LogLevel.INFO, "Remote package selected: '" + remotePackage.name + "'");
				logging.log(LogLevel.INFO, "  " + remotePackage.components.size() + " component(s)");
				
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
					
					logging.log(LogLevel.INFO, "No local package, download all components");
					for (XmlComponentBean remoteComponent : remotePackage.components) {
						UpdateBean bean = new UpdateBean();
						bean.remote = remoteComponent;
						bean.local = new XmlComponentBean(remoteComponent);
						bean.local.version = null;
						bean.download = true;
						toDownload.add(bean);
					}
				} else {
					
					// 4) Compare 
					logging.log(LogLevel.INFO, "Compare remote <-> local ...");
					for (XmlComponentBean remoteComponentBean : remotePackage.components) {
						XmlComponentBean localComponentBean = localPackage.getComponent(remoteComponentBean);					
						if (localComponentBean == null) {
							logging.log(LogLevel.INFO, "No local component for '" + remoteComponentBean.source + "'");
							UpdateBean bean = new UpdateBean();
							bean.remote = remoteComponentBean;
							bean.local = new XmlComponentBean(remoteComponentBean);
							bean.local.version = null;
							bean.download = true;
							toDownload.add(bean);
							localPackage.components.add(remoteComponentBean);
						} else {
							logging.log(LogLevel.INFO, "Compare '" + remoteComponentBean.source + "': remote(" + remoteComponentBean.version + ") <-> local(" + localComponentBean.version + ")"  );
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
									bean.local = new XmlComponentBean(remoteComponentBean);
									bean.local.version = null;
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
						if (download(dl.source, dl.target)) {
							bean.local.version = bean.remote.version;
						}
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

	private boolean download(String source, String target) {
		boolean ret = false;
		
		createDirectories(target);
		
		if (!downloadURL(source, target)) {
			logging.log(LogLevel.INFO, "Can not download '" + source + "' to '" + target + "', try file copy ...");
			File sourceFile = new File(source);
			File targetFile = new File(target);
			if (!downloadFile(sourceFile, targetFile)) {
				logging.log(LogLevel.INFO, "Can not copy '" + sourceFile.getAbsolutePath() + "' to '" + targetFile.getAbsolutePath() + "'");
			} else {
				ret = true;
				logging.log(LogLevel.INFO, "OK!");
			}
		} else {
			ret = true;
			logging.log(LogLevel.INFO, "OK!");
		}
		return ret;
	}

	private void createDirectories(String target) {
		File targetFile = new File(target);
		targetFile = targetFile.getParentFile();
		targetFile.mkdirs();
	}

	private boolean downloadFile(File sourceFile, File targetFile) {
		try {
			logging.log(LogLevel.INFO, String.format("Copy from '%s' to '%s'", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath()));
			Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception ex2) {
			return false;
		}
		return true;
	}

	private boolean downloadURL(String source, String target) {
		try {
			File targetFile = new File(target);
			URL url = new URL(source);
			logging.log(LogLevel.INFO, String.format("Download from '%s' to '%s'", url.toString(), targetFile.getAbsolutePath()));
			try (BufferedInputStream in = new BufferedInputStream(url.openStream())) {
				Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception ex) {
			return false;
		}
		return true;
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
