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
	
	@Override
	public String toString() {
		return name;
	}
	
}
