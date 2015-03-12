package launcher.beans;

import java.io.File;

/**
 * v_* files
 * 
 * @author SolidLeon
 *
 */
public class ComponentBean {
	private File source;
	private File target;
	private File compare;
	private boolean required = true;
	private boolean download = true;
	
	public boolean isDownload() {
		return download;
	}

	public void setDownload(boolean download) {
		if (!required)
			this.download = download;
	}

	public boolean isRequired() {
		return required;
	}
	
	public void setRequired(boolean required) {
		this.required = required;
	}
	
	public File getCompare() {
		return compare;
	}

	public void setCompare(File compare) {
		this.compare = compare;
	}

	public String getName() {
		return source == null ? "UNKNOWN" : source.getName();
	}

	public File getSource() {
		return source;
	}

	public void setSource(File source) {
		this.source = source;
	}

	public File getTarget() {
		return target;
	}

	public void setTarget(File target) {
		this.target = target;
	}
	
	@Override
	public String toString() {
		return String.format("Component [name='%s', source='%s', target='%s', compare='%s']",
				getName(), source, target, compare);
	}

}