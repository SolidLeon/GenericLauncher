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