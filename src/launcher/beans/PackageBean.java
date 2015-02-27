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
}