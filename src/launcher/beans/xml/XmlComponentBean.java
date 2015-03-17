package launcher.beans.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="component")
public class XmlComponentBean {

	@XmlAttribute(name="source", required=true)
	public String source;
	
	@XmlAttribute(name="target", required=true)
	public String target;
	
	@XmlAttribute(name="compare", required=false)
	public String compare;

	@XmlAttribute(name="required", required=false)
	public boolean required = true;
	
	@XmlAttribute(name="version", required=false)
	public String version;

	/**
	 * 
	 * @param other
	 * @return 	1: This is newer
	 * 			0: Both are on same version
	 * 		   -1: This is older
	 */
	public int compare(XmlComponentBean other) {
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
			otherMinor = Integer.parseInt(otherVersion[0]);
			otherMajor = Integer.parseInt(otherVersion[1]);
		} catch (Exception ex) {
			return 1;// Invalid other.version
		}
		try {
			localMinor = Integer.parseInt(localVersion[0]);
			localMajor = Integer.parseInt(localVersion[1]);
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
