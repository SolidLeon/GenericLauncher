package launcher.beans;

public class ProfileEntry {

	/**
	 * FIRST (ROOT) NODE for this profile entry
	 */
	private ServerBean serverListEntry;
	/** 
	 * FIRST SUB NODE
	 */
	private PackageBean packageBean;
	/**
	 * Second sub node
	 * LEAF
	 */
	private ComponentBean component;
	
	
	public ProfileEntry(ServerBean serverListEntry, PackageBean packageBean,
			ComponentBean component) {
		super();
		this.serverListEntry = serverListEntry;
		this.packageBean = packageBean;
		this.component = component;
	}


	public ServerBean getServerListEntry() {
		return serverListEntry;
	}


	public void setServerListEntry(ServerBean serverListEntry) {
		this.serverListEntry = serverListEntry;
	}


	public PackageBean getPackageBean() {
		return packageBean;
	}


	public void setPackageBean(PackageBean packageBean) {
		this.packageBean = packageBean;
	}


	public ComponentBean getComponent() {
		return component;
	}


	public void setComponent(ComponentBean component) {
		this.component = component;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((component == null) ? 0 : component.hashCode());
		result = prime * result
				+ ((packageBean == null) ? 0 : packageBean.hashCode());
		result = prime * result
				+ ((serverListEntry == null) ? 0 : serverListEntry.hashCode());
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
		ProfileEntry other = (ProfileEntry) obj;
		if (component == null) {
			if (other.component != null)
				return false;
		} else if (!component.equals(other.component))
			return false;
		if (packageBean == null) {
			if (other.packageBean != null)
				return false;
		} else if (!packageBean.equals(other.packageBean))
			return false;
		if (serverListEntry == null) {
			if (other.serverListEntry != null)
				return false;
		} else if (!serverListEntry.equals(other.serverListEntry))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("server='%s', package='%s', component='%s'", serverListEntry.getName(), packageBean.getName(), component.getName());
	}
	
	
}
