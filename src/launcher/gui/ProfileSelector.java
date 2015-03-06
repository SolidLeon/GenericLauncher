package launcher.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import launcher.beans.ComponentBean;
import launcher.beans.PackageBean;
import launcher.beans.ServerBean;
import launcher.controller.ComponentController;
import launcher.controller.PackageController;
import launcher.controller.ServerListController;

public class ProfileSelector extends JPanel {

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
	
	public ProfileSelector() {
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
		
	}
	
	private void addSource() {
		TreePath[] selectedPaths = sourceServerLists.getSelectionPaths();
		if (selectedPaths != null && selectedPaths.length > 0) {
			DefaultTreeModel model = (DefaultTreeModel) targetProfileEntries.getModel();
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
			
			for (int i = 0; i < selectedPaths.length; i++) {
				TreePath selected = selectedPaths[i];

				boolean skipRoot = false;
				DefaultMutableTreeNode node = root;
				for (Object path : selected.getPath()) {
					if (!skipRoot) {
						skipRoot = true;
						continue;
					}
					
					DefaultMutableTreeNode sub = new DefaultMutableTreeNode(((DefaultMutableTreeNode)path).getUserObject());
					if (root.isNodeDescendant(sub)) {
						node = sub;
						continue;
					}
					node.add(sub);
					node = sub;
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
