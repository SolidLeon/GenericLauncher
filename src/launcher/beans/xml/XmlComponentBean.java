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

	
}
