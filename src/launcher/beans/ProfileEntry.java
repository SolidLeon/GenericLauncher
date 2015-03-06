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
	
}
