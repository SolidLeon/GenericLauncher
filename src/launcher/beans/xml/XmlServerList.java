package launcher.beans.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="serverlist")
public class XmlServerList {

	@XmlElement(name="entry")
	public List<String> entries = new ArrayList<>();
	
}
