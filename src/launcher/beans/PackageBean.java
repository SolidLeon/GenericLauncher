package launcher.beans;

import java.io.File;
import java.util.List;

import launcher.Logging;
import launcher.Logging.LogLevel;

/**
 * *.cfg files
 * 
 * @author SolidLeon
 *
 */
public class PackageBean {
	private File basePath;
	private String postCommand;
	private File postCWD;
	private List<File> componentFiles;

	public List<File> getComponentFiles() {
		return componentFiles;
	}

	public void setComponentFiles(List<File> componentFiles) {
		this.componentFiles = componentFiles;
	}

	public File getPostCWD() {
		return postCWD;
	}

	public void setPostCWD(File postCWD) {
		this.postCWD = postCWD;
	}

	public String getPostCommand() {
		return postCommand;
	}

	public void setPostCommand(String postCommand) {
		this.postCommand = postCommand;
	}

	public File getBasePath() {
		return basePath;
	}

	public void setBasePath(File basePath) {
		this.basePath = basePath;
	}

	@Override
	public String toString() {
		return String.format("Package [BasePath='%s', PostCmd='%s', PostCWD='%s', Components=%d]",
				basePath, postCommand, postCWD, componentFiles == null ? 0 : componentFiles.size());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((basePath == null) ? 0 : basePath.hashCode());
		result = prime * result
				+ ((componentFiles == null) ? 0 : componentFiles.hashCode());
		result = prime * result + ((postCWD == null) ? 0 : postCWD.hashCode());
		result = prime * result
				+ ((postCommand == null) ? 0 : postCommand.hashCode());
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
		PackageBean other = (PackageBean) obj;
		if (basePath == null) {
			if (other.basePath != null)
				return false;
		} else if (!basePath.equals(other.basePath))
			return false;
		if (componentFiles == null) {
			if (other.componentFiles != null)
				return false;
		} else if (!componentFiles.equals(other.componentFiles))
			return false;
		if (postCWD == null) {
			if (other.postCWD != null)
				return false;
		} else if (!postCWD.equals(other.postCWD))
			return false;
		if (postCommand == null) {
			if (other.postCommand != null)
				return false;
		} else if (!postCommand.equals(other.postCommand))
			return false;
		return true;
	}
	
	
}