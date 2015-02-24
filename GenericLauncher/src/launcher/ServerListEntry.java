package launcher;

import java.io.File;

/**
 * serverlist.txt entries
 * 
 * @author Markus
 *
 */
class ServerListEntry {
	private String name;
	private File basePath;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public File getBasePath() {
		return basePath;
	}

	public void setBasePath(File basePath) {
		this.basePath = basePath;
	}

	@Override
	public String toString() {
		return name + " (" + basePath.getAbsolutePath() + ")";
	}

}