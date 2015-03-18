package launcher.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXB;

import launcher.beans.xml.XmlComponentBean;
import launcher.beans.xml.XmlLauncherConfigBean;
import launcher.beans.xml.XmlPackageBean;

public class RemoteLauncherTest implements Runnable {

	private String localPath = null;
	private String remotePath = null;
	

	
	public RemoteLauncherTest(String remotePath) {
		File remoteFile = new File(remotePath);
		File localFile = new File(System.getProperty("user.dir"), remoteFile.getName());
		
		this.remotePath = remotePath;
		this.localPath = localFile.getAbsolutePath();
	}
	
	public RemoteLauncherTest(String localPath, String remotePath) {
		super();
		this.localPath = localPath;
		this.remotePath = remotePath;
	}

	public static void main(String[] args) {

		Thread th = new Thread(new Runnable() {
			@Override
			public void run() {
				try (ServerSocket server = new ServerSocket(2345)) {
					System.out.println("SERVER BOUND TO 2345");
					while (true) {
						try (Socket socket = server.accept()) {
							try (BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {
								File file = new File("C:\\DEPLOYMENT\\public\\web-package.xml");
								if (file.exists()) {
									out.write(	("HTTP/1.1 200 OK \r\n"+
												"Content-Type: text/plain\r\n"+
												"Content-Length: "+file.length()+"\r\n"+
												"Connection: close\r\n\r\n")
											.getBytes());
									Files.copy(file.toPath(), out);
								} else {
									String msg = "File not found";
									out.write(	("HTTP/1.1 404 Not Found \r\n"+
												"Content-Type: text/plain\r\n"+
												"Content-Length: "+msg.length()+"\r\n"+
												"Connection: close\r\n\r\n")
											.getBytes());
									out.write(msg.getBytes());
								}
							}
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		th.start();
		
	}

	private static XmlLauncherConfigBean downloadRemotePackage(String remotePath) {
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
			XmlPackageBean remotePackage = remoteConfigBean.getPackageByName("TEST");
			System.out.println("Remote package selected: '" + remotePackage.name + "'");
			System.out.println("  " + remotePackage.components.size() + " component(s)");
			
			// 3) Check if we have a local package declaration with the same name. Yes? -> 4; No? -> 5
			XmlPackageBean localPackage = localConfigBean.getPackageByName(remotePackage.name);
			List<XmlComponentBean> toDownload = new ArrayList<>();
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
				toDownload.addAll(remotePackage.components);
			} else {
				
				// 4) Compare 
				System.out.println("Compare remote <-> local ...");
				for (XmlComponentBean remoteComponentBean : remotePackage.components) {
					XmlComponentBean localComponentBean = localPackage.getComponent(remoteComponentBean);					
					if (localComponentBean == null) {
						System.out.println("No local component for '" + remoteComponentBean.source + "'");
						toDownload.add(remoteComponentBean);
						localPackage.components.add(remoteComponentBean);
					} else {
						System.out.println("Compare '" + remoteComponentBean.source + "': remote(" + remoteComponentBean.version + ") <-> local(" + localComponentBean.version + ")"  );
						if (remoteComponentBean.compare(localComponentBean) > 0) {
							System.out.println("  Component added to download queue");
							toDownload.add(remoteComponentBean);
							localComponentBean.version = remoteComponentBean.version;
						} else {
							// Check if the physical file is available, if not download it no matter what version!
							File localFile = new File(remoteComponentBean.target);
							if (!localFile.exists())
								toDownload.add(remoteComponentBean);
						}
					}
				}
				
			}
			
			// 5) Download
			System.out.println("Download: ");
			for (XmlComponentBean dl : toDownload) {
				System.out.printf("SOURCE   = '%s'%n", dl.source);
				System.out.printf("TARGET   = '%s'%n", dl.target);
				System.out.printf("COMPARE  = '%s'%n", dl.compare);
				System.out.printf("REQUIRED = '%s'%n", dl.required);
				System.out.printf("VERSION  = '%s'%n", dl.version);
				try {
					File targetFile = new File(dl.target);
					URL url = new URL(dl.source);
					System.out.printf("Copy from '%s' to '%s'%n", url.toString(), targetFile.getAbsolutePath());
					try (BufferedInputStream in = new BufferedInputStream(url.openStream())) {
						Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
				} catch (Exception ex) {
					System.err.println("Can not download '" + dl.source + "' to '" + dl.target + "'");
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
