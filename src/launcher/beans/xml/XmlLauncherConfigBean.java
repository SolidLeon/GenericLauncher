package launcher.beans.xml;

import java.io.File;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="launcher")
public class XmlLauncherConfigBean {

	@XmlAttribute(name="basePath", required=false)
	public File basePath;
	
	@XmlElement(name="package", type=XmlPackageBean.class)
	public List<XmlPackageBean> packages;

	public XmlPackageBean getPackageByName(String name) {
		if (name != null)
			for (XmlPackageBean pkg : packages)
				if (name.equals(pkg.name))
					return pkg;
		return null;
	}


	
	
	
}
