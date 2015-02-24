package launcher.beans;

import java.io.File;
import java.util.List;

import launcher.Logging;
import launcher.Logging.LogLevel;

/**
 * *.cfg files
 * 
 * @author Markus
 *
 */
public class PackageBean {
	private File basePath;
	private String postCommand;
	private File postCWD;
	private LogLevel logLevel;
	private List<File> componentFiles;

	public List<File> getComponentFiles() {
		return componentFiles;
	}

	public void setComponentFiles(List<File> componentFiles) {
		this.componentFiles = componentFiles;
	}

	public LogLevel getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(LogLevel logLevel) {
		this.logLevel = logLevel;
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

}