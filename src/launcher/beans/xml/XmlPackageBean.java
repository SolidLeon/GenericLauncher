package launcher.beans.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="package")
public class XmlPackageBean {
	@XmlID
	@XmlAttribute(name="name", required=true)
	public String name;
	
	@XmlAttribute(name="postCwd", required=false)
	public String postCwd;
	
	@XmlAttribute(name="postCommand", required=false)
	public String postCommand;

	@XmlAttribute(name="depends", required=false)
	@XmlIDREF
	public XmlPackageBean depends;
	
	@XmlAttribute(name="basePath", required=false)
	public File basePath;

	@XmlElement(name="component")
	public List<XmlComponentBean> components = new ArrayList<>();
	
	@XmlAttribute(name="version", required=false)
	public String version;
	
	@Override
	public String toString() {
		return name;
	}

	public XmlComponentBean getComponent(XmlComponentBean other) {
		
		if (other != null && other.source != null && other.target != null)
			for (XmlComponentBean component : components)
				if (component != null &&
					component.source != null &&
					component.target != null && 
					component.source.equals(other.source) && component.target.equals(other.target))
					return component;
			
		
		return null;
	}
	/**
	 * Same version compare as in XmlComponentBean, keep them synchronized!!!
	 * @param other
	 * @return 	1: This is newer
	 * 			0: Both are on same version
	 * 		   -1: This is older
	 */
	public int compare(XmlPackageBean other) {
		if (other == null) 								return 1;
		if (other.version == null && version == null) 	return 0;
		if (other.version == null) 						return 1;
		if (version == null) 							return -1;
		String[] localVersion = version.split("\\.");
		String[] otherVersion = other.version.split("\\.");
		
		if (otherVersion.length != 2) 					return 1; 	// Invalid other.version
		if (localVersion.length != 2)					return -1; 	// Invalid this.version
		int localMinor = 0;
		int localMajor = 0;
		int otherMinor = 0;
		int otherMajor = 0;
		try {
			otherMajor = Integer.parseInt(otherVersion[0]);
			otherMinor = Integer.parseInt(otherVersion[1]);
		} catch (Exception ex) {
			return 1;// Invalid other.version
		}
		try {
			localMajor = Integer.parseInt(localVersion[0]);
			localMinor = Integer.parseInt(localVersion[1]);
		} catch (Exception ex) {
			return -1;// Invalid this.version
		}
		if (localMajor < otherMajor) return -1;
		if (localMajor > otherMajor) return 1;
		if (localMinor < otherMinor) return -1;
		if (localMinor > otherMinor) return 1;
		return 0;
	}
	
}
