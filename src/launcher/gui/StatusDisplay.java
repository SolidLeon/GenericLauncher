package launcher.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.xml.bind.JAXB;

import launcher.Launcher;
import launcher.Logging;
import launcher.Logging.LogLevel;
import launcher.beans.ComponentBean;
import launcher.beans.PackageBean;
import launcher.beans.xml.XmlComponentBean;
import launcher.beans.xml.XmlLauncherConfigBean;
import launcher.beans.xml.XmlPackageBean;
import launcher.controller.ComponentController;
import launcher.controller.DownloaderController;
import launcher.controller.LauncherRestartController;
import launcher.controller.PackageController;
import launcher.controller.ServerListController;
import launcher.gui.PreviewDialog.PreviewResult;

/**
 * SolidLeon #4 20150227 
 * 
 * Simple display for showing current progress
 * @author SolidLeon
 *
 */
public class StatusDisplay extends JFrame implements IStatusListener {

	
	private JProgressBar overallProgress;
	private JProgressBar currentProgress;
	private JTextPane text;
	/** OutputStream redirecting output to JTextArea 'text' */
	private OutputStream textOut;
	private JButton closeButton;
	private JButton startButton;
	/** Runnable object to 'run' close button is pressed and before disposing */
	private Runnable exitRunner;
	private Logging logging;
	
	private String lastModeSelection = "XML";
	private JFileChooser xmlFileChooser;
	
	public StatusDisplay() {
		setPreferredSize(new Dimension(800, 600));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setLayout(new BorderLayout());
		setTitle("Launcher Status");
		
		overallProgress = new JProgressBar();
		overallProgress.setStringPainted(true);
		currentProgress = new JProgressBar();
		currentProgress.setStringPainted(true);
		
		JPanel progressPanel = new JPanel(new BorderLayout());
		JPanel progressLabelPanel = new JPanel(new GridLayout(2, 1));
		JPanel progressBarPanel = new JPanel(new GridLayout(2, 1));
		progressLabelPanel.add(new JLabel("Overall Progress:"));
		progressLabelPanel.add(new JLabel("Current Progress:"));
		progressBarPanel.add(overallProgress);
		progressBarPanel.add(currentProgress);
		progressPanel.add(progressLabelPanel, BorderLayout.WEST);
		progressPanel.add(progressBarPanel, BorderLayout.CENTER);
		add(progressPanel, BorderLayout.NORTH);
		text = new JTextPane();
		text.setEditable(false);
		text.setFont(new Font("Monospaced", Font.PLAIN, 12));
		DefaultCaret caret = (DefaultCaret)text.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		add(new JScrollPane(text), BorderLayout.CENTER);
		closeButton = new JButton("Close");
		closeButton.addActionListener(e -> new RunWorker().execute());
		startButton = new JButton("Start");
		startButton.addActionListener(e -> run());
		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(startButton);
		buttonPanel.add(closeButton);
		add(buttonPanel, BorderLayout.SOUTH);
		
		textOut = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				try {
					text.getDocument().insertString(text.getDocument().getLength(), String.valueOf((char)b), null);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (closeButton.isEnabled()) {
					closeButton.doClick();
				} else {
					JOptionPane.showMessageDialog(StatusDisplay.this, "Cannot close the window right now!");
				}
			}
		});
		
		pack();
		setLocationRelativeTo(null);
		

		logging = new Logging(this);
		logBasicInfo();
		
	}
	
	public void setProgress(JProgressBar bar, int value, int min, int max, String text) {
		bar.setMinimum(min);
		bar.setMaximum(max);
		bar.setValue(value);
		bar.setString(text);
	}
	
	@Override
	public void setCurrentProgress(int value, int min, int max, String text) {
		setProgress(currentProgress, value, min, max, text);
	}
	
	@Override
	public void setCurrentProgress(int value) {
		currentProgress.setValue(value);
	}
	
	@Override
	public void setCurrentProgressToMax() {
		currentProgress.setValue(currentProgress.getMaximum());
	}
	
	@Override
	public void setCurrentProgress(String text) {
		currentProgress.setString(text);
	}
	
	@Override
	public int getCurrentProgress() {
		return currentProgress.getValue();
	}
	
	@Override
	public void setOverallProgress(int value, int min, int max) {
		setProgress(overallProgress, value, min, max, null);
	}
	
	@Override
	public void setOverallProgress(int value) {
		overallProgress.setValue(value);
	}
	
	@Override
	public void setOverallProgress(String text) {
		overallProgress.setString(text);
	}
	
	@Override
	public int getOverallProgress() {
		return overallProgress.getValue();
	}
	
	@Override
	public void addOverallProgress(int i) {
		setOverallProgress(getOverallProgress() + i);
	}
	
	
	/**
	 * - Sets a runner being executed after close button has been pressed.
	 * - Calls {@link #setStatusCompleted()}
	 */
	@Override
	public void setStatusCompletedExecCommandOnExit(Runnable runner) {
		this.exitRunner = runner;
		setStatusCompleted(); // SolidLeon #4 20150227
	}

	/**
	 * SolidLeon #4 20150227
	 * 
	 * - Enabled the close button
	 * - Sets a fancy text for the close button
	 * - Sets overall progress to max
	 * - Sets current progress max to 100 (for cases where the max is 0)
	 * - Sets current progress to max
	 * - Sets 'Done!' as curren progress text
	 */
	public void setStatusCompleted() {
		closeButton.setEnabled(true);
		closeButton.setText(exitRunner == null ? "Close" : "Launch...");
		overallProgress.setValue(overallProgress.getMaximum());
		currentProgress.setMaximum(100);
		setCurrentProgressToMax();
		setCurrentProgress("Done!");
		
	}
	
	public void appendText(Color fg, Color bg, String str) {
		SimpleAttributeSet aset = new SimpleAttributeSet();
		StyleConstants.setForeground(aset, fg);
		StyleConstants.setBackground(aset, bg);
		
		int start = text.getDocument().getLength();
		int len = text.getText().length();
		StyledDocument sd = (StyledDocument) text.getDocument();
		sd.setCharacterAttributes(start, len, aset, false);
		

		try {
			text.getDocument().insertString(text.getDocument().getLength(), str, aset);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private class RunWorker extends SwingWorker<Void, Void> {
		@Override
		protected Void doInBackground() throws Exception {
			if (exitRunner != null) {
				exitRunner.run();
			}
			return null;
		}
		
		@Override
		protected void done() {
			dispose();
		}
	}
	
	private void run() {
		
		String mode = (String) JOptionPane.showInputDialog(this, "Select mode", "Mode", JOptionPane.QUESTION_MESSAGE, null, new Object[] { "FILE", "XML" }, lastModeSelection);
		if (mode != null) {
			lastModeSelection = mode;
			List<ComponentBean> components = null;
			
			closeButton.setEnabled(false);
			
			logging.getStatusListener().setOverallProgress(0, 0, 4);
			
			logging.getStatusListener().setCurrentProgress(0, 0, 0, "Initialize launcher restart ...");
			LauncherRestartController launcherRestartController = new LauncherRestartController(logging);
	
			if ("FILE".equals(mode)) {
				components = runFile(launcherRestartController);
			} else if ("XML".equals(mode)) {
				components = runXML(launcherRestartController);
			}
	
			if (components != null && !components.isEmpty()) {
				PreviewDialog previewDialog = new PreviewDialog(this, components);
				previewDialog.setVisible(true);
				
				PreviewResult previewResult = previewDialog.getPreviewResult();
				if (previewResult == PreviewResult.OK) {
					DownloaderController downloader = new DownloaderController(logging, components);
					downloader.run();
			
					// CHECK IF SOMETHING WAS UPDATED THAT REQUIRES A LAUNCHER RESTART
					launcherRestartController.run();
				} else {
					logging.log(LogLevel.INFO, "User cancelled preview");
				}
			} else {
				logging.log(LogLevel.INFO, "No components to be updated");
			}
		}
		setStatusCompleted(); //// SolidLeon #4 20150227 - we set the overall status so even if the user cancels the end-state is completed
		logging.log(LogLevel.FINE, "Done!");
	}
	

	private List<ComponentBean> runXML(LauncherRestartController launcherRestartController) {
		logging.log(LogLevel.INFO, "XML mode");
		List<ComponentBean> components = null;
		if (xmlFileChooser == null) {
			xmlFileChooser = new JFileChooser(System.getProperty("user.dir"));
			xmlFileChooser.setFileFilter(new FileNameExtensionFilter("XML Launcher Configuration", "xml"));
			xmlFileChooser.setMultiSelectionEnabled(false);
		}
		int rc = xmlFileChooser.showOpenDialog(this);
		if (rc == JFileChooser.APPROVE_OPTION) {
			components = new ArrayList<>();
			logging.log(LogLevel.INFO, "Read XML file '" + xmlFileChooser.getSelectedFile().getAbsolutePath() + "' ...");
			XmlLauncherConfigBean cfg = (XmlLauncherConfigBean) JAXB.unmarshal(xmlFileChooser.getSelectedFile(), XmlLauncherConfigBean.class);
			logging.log(LogLevel.INFO, "  Done!");
			
			logging.log(LogLevel.CONFIG,     "BASE_PATH     " + "'" + (cfg.basePath == null ? "Inherit" : cfg.basePath.getAbsolutePath()) + "'");
			logging.log(LogLevel.CONFIG, cfg.packages.size() + " package(s)");
			for (XmlPackageBean pkgBean : cfg.packages) {
				logging.log(LogLevel.CONFIG, "-- PACKAGE --");
				logging.log(LogLevel.CONFIG, "NAME          " + "'" + pkgBean.name + "'");
				logging.log(LogLevel.CONFIG, "POST_CWD      " + "'" + pkgBean.postCwd + "'");
				logging.log(LogLevel.CONFIG, "POST_COMMAND  " + "'" + pkgBean.postCommand + "'");
				logging.log(LogLevel.CONFIG, "BASE_PATH     " + "'" + pkgBean.basePath + "'");
				logging.log(LogLevel.CONFIG, "DEPENDS       " + "'" + pkgBean.depends + "'");
				logging.log(LogLevel.CONFIG, pkgBean.components.size() + " component(s)");
				for (XmlComponentBean cm : pkgBean.components) {
					logging.log(LogLevel.CONFIG, "SOURCE        " + "'" + cm.source + "'");
					logging.log(LogLevel.CONFIG, "TARGET        " + "'" + cm.target + "'");
					logging.log(LogLevel.CONFIG, "COMPARE       " + "'" + cm.compare + "'");
					logging.log(LogLevel.CONFIG, "REQUIRED      " + "'" + cm.required + "'");
				}
			}
			
			logging.log(LogLevel.INFO, "User selectes a package...");
			Object sel = JOptionPane.showInputDialog(this, "Select a package", "Launch Package Selection", JOptionPane.QUESTION_MESSAGE, null, cfg.packages.toArray(), cfg.packages.get(0));
			if (sel != null) {
				logging.log(LogLevel.INFO, "User selected '" + sel.toString() + "'");
				
				// First sort the packages by their dependencies 
				// [0] -> top 
				// [1] -> depends on [0]
				List<XmlPackageBean> sorted = new ArrayList<>();
				XmlPackageBean pkg = (XmlPackageBean) sel;
				while (pkg != null) {
					sorted.add(0, pkg);
					pkg = pkg.depends;
				}
				
				for (int i = 0; i < sorted.size(); i++) {
					pkg = sorted.get(i);
					logging.log(LogLevel.INFO, "Add package '" + pkg.name + "'");
					PackageBean pkgBean = new PackageBean();
					pkgBean.setBasePath(pkg.basePath != null ? pkg.basePath : cfg.basePath);
					pkgBean.setPostCommand(pkg.postCommand);
					pkgBean.setPostCWD(pkg.postCwd == null ? null : new File(pkg.postCwd));
					
					// Components...
					logging.log(LogLevel.INFO, "Add package components ...");
					for (XmlComponentBean com : pkg.components) {
						File sourceFile = new File(com.source);
						File targetFile = new File(com.target);
						ComponentBean comp = new ComponentBean();
						comp.setCompare(com.compare == null ? null : new File(com.compare));
						comp.setSource(sourceFile);
						comp.setTarget(targetFile);
						comp.setRequired(com.required);
						
						if (comp.getCompare() != null) {
							if (comp.getSource().lastModified() > comp.getCompare().lastModified()) {
								logging.log(LogLevel.INFO, "Add component '" + com.source + "'");
								components.add(comp);
							} else {
								logging.log(LogLevel.INFO, "Skip component '" + com.source + "'");
							}
						} else {
							if (comp.getSource().lastModified() > comp.getTarget().lastModified()) {
								logging.log(LogLevel.INFO, "Add component '" + com.source + "'");
								components.add(comp);
							} else {
								logging.log(LogLevel.INFO, "Skip component '" + com.source + "'");
							}
						}
					}
					// If we added some components do not add components from depending packages (just update this package)
					if (!components.isEmpty()) {
						launcherRestartController.setActivePackageBean(pkgBean);
						break;
					}
				}
			}
		}
		return components;
	}

	private List<ComponentBean> runFile(LauncherRestartController launcherRestartController) {
		logging.log(LogLevel.INFO, "File mode");
		ServerListController serverListController = new ServerListController(logging);
		serverListController.run();

		PackageController packageController = new PackageController(logging, serverListController.getSelected());
		packageController.run();
		launcherRestartController.setActivePackageBean(packageController.getSelectedPackageBean());

		ComponentController componentController = new ComponentController(logging, packageController.getSelectedPackageBean());
		componentController.run();
		
		return componentController.getResultComponentList();
	}

	private void logBasicInfo() {
		logging.logInfo("Launcher "
				+ Launcher.class.getPackage().getImplementationVersion()
				+ " started");
		logging.logInfo("Current Time is " + new Date().toString());
		logging.logInfo("System.getProperty('os.name') == '"
				+ System.getProperty("os.name") + "'");
		logging.logInfo("System.getProperty('os.version') == '"
				+ System.getProperty("os.version") + "'");
		logging.logInfo("System.getProperty('os.arch') == '"
				+ System.getProperty("os.arch") + "'");
		logging.logInfo("System.getProperty('java.version') == '"
				+ System.getProperty("java.version") + "'");
		logging.logInfo("System.getProperty('java.vendor') == '"
				+ System.getProperty("java.vendor") + "'");
		logging.logInfo("System.getProperty('java.home') == '"
				+ System.getProperty("java.home") + "'");
		logging.logInfo("System.getProperty('sun.arch.data.model') == '"
				+ System.getProperty("sun.arch.data.model") + "'");
		logging.logInfo("System.getProperty('user.dir') == " + "'" + System.getProperty("user.dir") + "'");
		logging.logEmptyLine();
	}
}
