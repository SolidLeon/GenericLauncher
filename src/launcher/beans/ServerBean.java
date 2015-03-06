package launcher.beans;

import java.io.File;

/**
 * serverlist.txt entries
 * 
 * @author SolidLeon
 *
 */
public class ServerBean {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((basePath == null) ? 0 : basePath.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerBean other = (ServerBean) obj;
		if (basePath == null) {
			if (other.basePath != null)
				return false;
		} else if (!basePath.equals(other.basePath))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	

}