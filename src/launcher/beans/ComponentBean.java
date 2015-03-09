package launcher.beans;

import java.io.File;

/**
 * v_* files
 * 
 * @author SolidLeon
 *
 */
public class ComponentBean {
	private String name;
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
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((compare == null) ? 0 : compare.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
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
		ComponentBean other = (ComponentBean) obj;
		if (compare == null) {
			if (other.compare != null)
				return false;
		} else if (!compare.equals(other.compare))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	
}