package launcher.controller;

import launcher.Logging;

public class UpdateController {

	private Logging logging;
	private XmlLauncherController local;
	private XmlLauncherController remote;
	
	public UpdateController(Logging logging) {
		this.logging = logging;
	}
	
	public void prepare(String localPath, String remotePath) {
		local = new XmlLauncherController(logging, localPath);
		remote = new XmlLauncherController(logging, remotePath);
	}
	
	
}
