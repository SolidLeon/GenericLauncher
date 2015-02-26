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
import launcher.beans.ServerListEntry;

public class ServerListController implements Runnable {

	private Logging logging;
	private List<ServerListEntry> serverList = new ArrayList<>();
	private ServerListEntry selected;
	
	
	public ServerListController(Logging logging) {
		super();
		this.logging = logging;
	}
	
	public ServerListEntry getSelected() {
		return selected;
	}

	public List<ServerListEntry> getServerList() {
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
				logging.logDebug("User selected serverlist file '" + selectedServerList.getAbsolutePath() + "'");
				if (!selectedServerList.exists() || selectedServerList.isDirectory()) {
					logging.logDebug("Server list file is invalid!");
					JOptionPane.showMessageDialog(jfc, "Invalid server list selected!");
					selectedServerList = null;
				}
			} else if (rc == JFileChooser.CANCEL_OPTION) {
				logging.logDebug("User cancelled server list selection");
				return;
			}
		}
		
		readServerList(serverList, selectedServerList);
		if (serverList.isEmpty()) {
			logging.logDebug("no server found");
			return; //No server found
		}

		logging.logInfo(serverList.size() + " server loaded!");
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
		selected = (ServerListEntry) serverSelectionObject;

		logging.logInfo("Selected server '" + selected + "'");
	}

	/**
	 * Reads a list of NAME=SERVER_PATH from a text file
	 * 
	 * @param serverList
	 * @param file
	 */
	private void readServerList(List<ServerListEntry> serverList,
			File file) {
		try {
			logging.logDebug("Read server list from '" + file.getAbsolutePath() + "'");
			for (String line : Files.readAllLines(file.toPath())) {
				int idx = line.indexOf('=');
				if (idx == -1)
					continue;
				String name = line.substring(0, idx).trim();
				String basePath = line.substring(idx + 1).trim();

				ServerListEntry entry = new ServerListEntry();
				entry.setName(name);
				entry.setBasePath(new File(basePath));

				serverList.add(entry);
			}
			
			logging.logDebug(String.format("Server list '%s' contains %d entries", file.getName(), serverList.size()));
			for (ServerListEntry entry : serverList)
				logging.logDebug(String.format("  '%-30s'  '%s'", entry.getName(), entry.getBasePath()));
			
		} catch (IOException e) {
			logging.printException(e); // markusmannel@gmail.com 20150224
										// Utilize our exception-to-file
										// mechanism
		}
	}
}
