package launcher.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import launcher.Logging;
import launcher.Logging.LogLevel;
import launcher.beans.ServerBean;

public class ServerListController implements Runnable {

	private Logging logging;
	private List<ServerBean> serverList = new ArrayList<>();
	private ServerBean selected;
	
	
	public ServerListController(Logging logging) {
		super();
		this.logging = logging;
	}
	
	public ServerBean getSelected() {
		return selected;
	}

	public List<ServerBean> getServerList() {
		return serverList;
	}
	
	@Override
	public void run() {
		JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));
		jfc.setFileFilter(new FileNameExtensionFilter("Server List", "txt"));
		jfc.setMultiSelectionEnabled(false);
		
		File selectedServerList = null;
		
		while (selectedServerList == null) {
			int rc = jfc.showOpenDialog(null); 
			if (rc == JFileChooser.APPROVE_OPTION) {
				selectedServerList = jfc.getSelectedFile();
				if (logging != null) logging.logDebug("User selected serverlist file '" + selectedServerList.getAbsolutePath() + "'");
				if (!selectedServerList.exists() || selectedServerList.isDirectory()) {
					if (logging != null) logging.logDebug("Server list file is invalid!");
					JOptionPane.showMessageDialog(jfc, "Invalid server list selected!");
					selectedServerList = null;
				}
			} else if (rc == JFileChooser.CANCEL_OPTION) {
				if (logging != null) logging.logDebug("User cancelled server list selection");
				return;
			}
		}
		
		readServerList(serverList, selectedServerList);
		if (serverList.isEmpty()) {
			if (logging != null) logging.logDebug("no server found");
			return; //No server found
		}

		if (logging != null) logging.logInfo(serverList.size() + " server loaded!");
		Object serverSelectionObject = serverList.get(0);
		if (serverList.size() > 1) {
			
			serverSelectionObject = JOptionPane.showInputDialog(null,
					"Select a server you want to connect to: ", "Launcher",
					JOptionPane.QUESTION_MESSAGE, null, serverList.toArray(),
					serverList.get(0));
			if (serverSelectionObject == null) {
				return; //No server selected or user cancelled
			}
		}
		selected = (ServerBean) serverSelectionObject;

		if (logging != null) logging.logInfo("Selected server '" + selected + "'");
	}

	/**
	 * Reads a list of NAME=SERVER_PATH from a text file
	 * 
	 * @param serverList
	 * @param file
	 */
	public void readServerList(List<ServerBean> serverList,
			File file) {
		try {
			if (logging != null) logging.logDebug("Read server list from '" + file.getAbsolutePath() + "'");

			List<String> lines = Files.readAllLines(file.toPath());
			if (logging != null) logging.getStatusListener().setCurrentProgress(0, 0, lines.size(), "Read server list from '" + file.getAbsolutePath() + "' ...");
			if (logging != null) logging.getStatusListener().addOverallProgress(1);
			for (String line : lines) {
				if (logging != null) logging.getStatusListener().setCurrentProgress(logging.getStatusListener().getCurrentProgress() + 1);
				int idx = line.indexOf('=');
				if (idx == -1)
					continue;
				String name = line.substring(0, idx).trim();
				String basePath = line.substring(idx + 1).trim();

				ServerBean entry = new ServerBean();
				entry.setName(name);
				entry.setBasePath(new File(basePath));

				serverList.add(entry);
			}
			if (logging != null) logging.getStatusListener().setCurrentProgressToMax(); //set to max
			
			if (logging != null) logging.logDebug(String.format("Server list '%s' contains %d entries", file.getName(), serverList.size()));
			if (logging != null) 
				for (ServerBean entry : serverList)
					logging.log(LogLevel.CONFIG, String.format("  %-30s  '%s'", "'" + entry.getName() + "'", entry.getBasePath()));
			
		} catch (IOException e) {
			if (logging != null) logging.printException(e); // markusmannel@gmail.com 20150224
										// Utilize our exception-to-file
										// mechanism
		}
	}
}
