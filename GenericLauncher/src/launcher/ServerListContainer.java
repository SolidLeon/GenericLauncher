package launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

public class ServerListContainer implements Runnable {

	private Logging logging;
	private List<ServerListEntry> serverList = new ArrayList<>();
	private ServerListEntry selected;
	
	
	public ServerListContainer(Logging logging) {
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
		readServerList(serverList, new File("serverlist.txt"));
		if (serverList.isEmpty()) {
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
			selected = (ServerListEntry) serverSelectionObject;
		}

		logging.logInfo("Selected server '" + serverSelectionObject + "'");
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
		} catch (IOException e) {
			logging.printException(e); // markusmannel@gmail.com 20150224
										// Utilize our exception-to-file
										// mechanism
		}
	}
}
