package launcher.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import launcher.beans.ComponentBean;
import launcher.beans.PackageBean;
import launcher.beans.ProfileEntry;
import launcher.beans.ServerBean;
import launcher.controller.ComponentController;
import launcher.controller.PackageController;
import launcher.controller.ServerListController;

public class ProfileSelector extends JDialog {

	/**
	 * Contains:
	 * - ROOT
	 * -- ServerListEntry name
	 * --- Package name
	 * ---- Component name
	 */
	private JTree sourceServerLists;
	
	/**
	 * Contains ProfileEntries
	 */
	private JTree targetProfileEntries;

	private JButton addButton;
	private JButton removeButton;
	private JButton addAllButton;
	private JButton removeAllButton;
	private JButton addServerButton;
	private JButton executeButton;
	
	public ProfileSelector(JFrame owner) {
		super(owner, true);
		setLayout(new BorderLayout());

		addButton = new JButton("Add >");
		removeButton = new JButton("< Remove");
		addAllButton = new JButton("Add All >>");
		removeAllButton = new JButton("<< Remove All");
		addServerButton = new JButton("Add server");
		executeButton = new JButton("Execute");
		
		sourceServerLists = new JTree();
		targetProfileEntries = new JTree();
		
		JPanel centerPanel = new JPanel(new GridLayout(1, 3));
		JPanel centerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JPanel centerButtonGridPanel = new JPanel(new GridLayout(5, 1, 8, 8));
		centerButtonPanel.add(centerButtonGridPanel);
		centerButtonGridPanel.add(addAllButton);
		centerButtonGridPanel.add(addButton);
		centerButtonGridPanel.add(removeButton);
		centerButtonGridPanel.add(removeAllButton);
		centerButtonGridPanel.add(addServerButton);
		
		centerPanel.add(new JScrollPane(sourceServerLists));
		centerPanel.add(centerButtonPanel);
		centerPanel.add(new JScrollPane(targetProfileEntries));
		add(centerPanel, BorderLayout.CENTER);
		add(executeButton, BorderLayout.SOUTH);
		

		targetProfileEntries.setRootVisible(false);
		targetProfileEntries.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
		
		sourceServerLists.setRootVisible(false);
		sourceServerLists.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
		
		addServerButton.addActionListener(evt -> addServer());
		addButton.addActionListener(evt -> addSource());
		addAllButton.addActionListener(evt -> addSourceAll());
		removeButton.addActionListener(evt -> removeTarget());
		removeAllButton.addActionListener(evt -> removeAllTarget());
		
		executeButton.addActionListener(evt -> execute());
	}
	
	private void execute() {
		
	}

	private void  removeAllTarget() {
		DefaultTreeModel model = (DefaultTreeModel) targetProfileEntries.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		root.removeAllChildren();
		model.reload();
	}

	private void addSourceAll() {
		DefaultTreeModel model = (DefaultTreeModel) sourceServerLists.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		List<TreePath> paths = new ArrayList<>();
		for (int i = 0; i < root.getChildCount(); i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
			TreePath path = new TreePath(child.getPath());
			paths.add(path);
		}
		sourceServerLists.setSelectionPaths(paths.toArray(new TreePath[0]));
		addSource();
	}

	private void removeTarget() {
		TreePath[] selectedPaths = targetProfileEntries.getSelectionPaths();
		if (selectedPaths != null && selectedPaths.length > 0) {
			DefaultTreeModel model = (DefaultTreeModel) targetProfileEntries.getModel();
			for (int i = 0; i < selectedPaths.length; i++) {
				TreePath selected = selectedPaths[i];
				model.removeNodeFromParent((MutableTreeNode) selected.getLastPathComponent());
			}
			model.reload();
		}
	}

	private void addSource() {
		TreePath[] selectedPaths = sourceServerLists.getSelectionPaths();
		if (selectedPaths != null && selectedPaths.length > 0) {
			DefaultTreeModel model = (DefaultTreeModel) targetProfileEntries.getModel();
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
			
			for (int i = 0; i < selectedPaths.length; i++) {
				TreePath selected = selectedPaths[i];

				ServerBean serverBean = null;
				PackageBean packageBean = null;
				ComponentBean componentBean = null;
				
				Object selObj = ((DefaultMutableTreeNode)selected.getLastPathComponent()).getUserObject();
				if (selObj instanceof ServerBean) {
					serverBean = (ServerBean) selObj;
					//Add all packages
					DefaultMutableTreeNode serverNode = (DefaultMutableTreeNode) selected.getLastPathComponent();
					for (int j = 0; j < serverNode.getChildCount(); j++) {
						DefaultMutableTreeNode packageChild = (DefaultMutableTreeNode) serverNode.getChildAt(j);
						PackageBean childPackage = (PackageBean) packageChild.getUserObject();

						//Add all components
						for (int k = 0; k < packageChild.getChildCount(); k++) {
							DefaultMutableTreeNode componentChild = (DefaultMutableTreeNode) packageChild.getChildAt(k);
							root.add(new DefaultMutableTreeNode(new ProfileEntry(serverBean, childPackage, (ComponentBean)componentChild.getUserObject())));
						}
					}
				} else if (selObj instanceof PackageBean) {
					packageBean = (PackageBean) selObj;
					serverBean = (ServerBean) ((DefaultMutableTreeNode)selected.getParentPath().getLastPathComponent()).getUserObject();
					//Add all components
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) selected.getLastPathComponent();
					for (int j = 0; j < node.getChildCount(); j++) {
						DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(j);
						root.add(new DefaultMutableTreeNode(new ProfileEntry(serverBean, packageBean, (ComponentBean)child.getUserObject())));
					}
				} else if (selObj instanceof ComponentBean) {
					componentBean = (ComponentBean) selObj;
					packageBean = (PackageBean) ((DefaultMutableTreeNode)selected.getParentPath().getLastPathComponent()).getUserObject();
					serverBean = (ServerBean) ((DefaultMutableTreeNode)selected.getParentPath().getParentPath().getLastPathComponent()).getUserObject();
					root.add(new DefaultMutableTreeNode(new ProfileEntry(serverBean, packageBean, componentBean)));
				} else if (selected.equals(sourceServerLists.getModel().getRoot())) {
					// Add all server
					DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) selected.getLastPathComponent();
					for (int l = 0; l < rootNode.getChildCount(); l++) {
						DefaultMutableTreeNode serverNode = (DefaultMutableTreeNode) rootNode.getChildAt(l);
						//Add all packages
						for (int j = 0; j < serverNode.getChildCount(); j++) {
							DefaultMutableTreeNode packageChild = (DefaultMutableTreeNode) serverNode.getChildAt(j);
							PackageBean childPackage = (PackageBean) packageChild.getUserObject();
	
							//Add all components
							for (int k = 0; k < packageChild.getChildCount(); k++) {
								DefaultMutableTreeNode componentChild = (DefaultMutableTreeNode) packageChild.getChildAt(k);
								root.add(new DefaultMutableTreeNode(new ProfileEntry(serverBean, childPackage, (ComponentBean)componentChild.getUserObject())));
							}
						}
					}
				} else {
					System.err.println("Wrong node '" + selObj + "'");
					continue;
				}
				model.reload(root);
			}
		}
	}
	

	private void addServer() {
		JFileChooser jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jfc.setMultiSelectionEnabled(true);
		int rc = jfc.showOpenDialog(this);
		System.out.println("JFC -> " + rc);
		if (rc == JFileChooser.APPROVE_OPTION) {
			System.out.println("Selected files: " + jfc.getSelectedFiles().length);
			Arrays.asList(jfc.getSelectedFiles()).stream().map(e -> e.getAbsolutePath()).forEach(System.out::println);
			ServerListController con = new ServerListController(null);
			List<ServerBean> serverList = new ArrayList<>();
			for (File file : jfc.getSelectedFiles()) {
				System.out.println("READ SERVER LIST '" + file.getAbsolutePath() + "'");
				con.readServerList(serverList, file);
			}
			
			DefaultTreeModel model = (DefaultTreeModel) sourceServerLists.getModel();
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
			for (ServerBean serverBean : serverList) {
				System.out.println("ADD SERVER NODE '" + serverBean.toString() + "'");
				DefaultMutableTreeNode serverNode = new DefaultMutableTreeNode(serverBean);
				root.add(serverNode);
				model.reload(root);
				
				PackageController packageCon = new PackageController(null, serverBean);
				List<File> packageList = packageCon.getPackageBeanFileList(serverBean.getBasePath());
				for (File packageFile : packageList) {
					PackageBean packageBean = packageCon.readPackageBean(packageFile);
					DefaultMutableTreeNode node = new DefaultMutableTreeNode(packageBean);
					
					serverNode.add(node);
					model.reload(serverNode);
					
					
					ComponentController componentCon = new ComponentController(null, packageBean);
					

					List<ComponentBean> componentBeanList = new ArrayList<>();
					componentCon.readComponentBeans(componentBeanList, packageBean,
							packageBean.getComponentFiles());	
					
					for (ComponentBean component : componentBeanList) {
						node.add(new DefaultMutableTreeNode(component));
						model.reload(node);
					}
				}
			}
			model.reload();
		}
	}
	
}
