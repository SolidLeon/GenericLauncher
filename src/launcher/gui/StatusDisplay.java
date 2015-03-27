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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import launcher.beans.UpdateBean;
import launcher.beans.xml.XmlLauncherConfigBean;
import launcher.beans.xml.XmlPackageBean;
import launcher.beans.xml.XmlServerList;
import launcher.controller.IUpdateListener;
import launcher.controller.UpdateController;
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
	private JCheckBox closeCheckbox;
	private JButton launchButton;
	private JButton startButton;
	/** Runnable object to 'run' close button is pressed and before disposing */
	private Runnable exitRunner;
	private Logging logging;
	
	private String selectedServer;
	
	private JFileChooser serverlistFileChooser;
	// Initialize with default server list
	private String serverListPath = "serverlist.xml";
	
	public StatusDisplay() {
		// if the default serverlist exists initialize the serverlist file chooser with it, otherwise use the CWD
		serverlistFileChooser = new JFileChooser(System.getProperty("user.dir"));
		
		serverlistFileChooser.setDialogTitle("Select serverlist ...");
		serverlistFileChooser.setFileFilter(new FileNameExtensionFilter("ServerList XML", "xml"));
		serverlistFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		
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
		text = new JTextPane() {
			@Override
			public boolean getScrollableTracksViewportWidth() {
				return getUI().getPreferredSize(this).width <= getParent().getSize().width;
			}
		};
		text.setEditable(false);
		text.setFont(new Font("Monospaced", Font.PLAIN, 12));
		DefaultCaret caret = (DefaultCaret)text.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		add(new JScrollPane(text), BorderLayout.CENTER);
		closeCheckbox = new JCheckBox("Close on launch");
		launchButton = new JButton("Launch");
		launchButton.setEnabled(false);
		launchButton.addActionListener(e -> new RunWorker().execute());
		startButton = new JButton("Start");
		startButton.addActionListener(e -> run());
		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(startButton);
		JPanel closePanel = new JPanel(new BorderLayout());
		closePanel.add(closeCheckbox, BorderLayout.EAST);
		closePanel.add(launchButton, BorderLayout.CENTER);
		buttonPanel.add(closePanel);
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
				if (startButton.isEnabled()) {
					dispose();
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
		launchButton.setEnabled(exitRunner != null);
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
				if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(StatusDisplay.this, "Execute '" + launchButton.getToolTipText() + "'?", "Launcher", JOptionPane.YES_NO_OPTION))
					exitRunner.run();
			}
			return null;
		}
		
		@Override
		protected void done() {
			if (closeCheckbox.isSelected())
				dispose();
		}
	}
	
	private void run() {
		
		launchButton.setEnabled(false);
		
		logging.getStatusListener().setOverallProgress(0, 0, 4);
		
		IUpdateListener listener = new IUpdateListener() {
			@Override
			public XmlPackageBean selectPackage(XmlLauncherConfigBean remoteConfigBean) {
				Object[] options = remoteConfigBean.packages.toArray();
				return (XmlPackageBean) JOptionPane.showInputDialog(StatusDisplay.this, "Select package", "Launcher", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			}
			
			@Override
			public boolean preDownload(List<UpdateBean> toDownload) {
				if (toDownload != null && toDownload.size() > 0) {
					PreviewDialog previewDialog = new PreviewDialog(StatusDisplay.this, toDownload);
					previewDialog.setVisible(true);
					
					PreviewResult previewResult = previewDialog.getPreviewResult();
					if (previewResult == PreviewResult.OK) {
					} else {
						logging.log(LogLevel.INFO, "User cancelled preview");
						return false;
					}
				}
				return true;
			}
			
			@Override
			public void postUpdate(boolean restart, String logInfo, Runnable runner) {
				logging.log(LogLevel.INFO, "Post update runner set to " + logInfo);
				if (restart) {
					logging.log(LogLevel.INFO, "Restart required!");
					startButton.setEnabled(false);
				}
				launchButton.setToolTipText(logInfo);
				setStatusCompletedExecCommandOnExit(runner);
			}
		};
		
		
		
		int rc = serverlistFileChooser.showOpenDialog(this);
		if (rc == JFileChooser.CANCEL_OPTION) {
			serverListPath = JOptionPane.showInputDialog("Server list xml URL:");
			if (serverListPath == null) {
				logging.log(LogLevel.INFO, "User cancelled!");
			} else {
				readServerList(listener);
			}
		} else if (rc == JFileChooser.APPROVE_OPTION) {
			serverListPath = serverlistFileChooser.getSelectedFile().getAbsolutePath();
			readServerList(listener);
		}
		setStatusCompleted(); //// SolidLeon #4 20150227 - we set the overall status so even if the user cancels the end-state is completed
		logging.log(LogLevel.FINE, "Done!");
	}

	private void readServerList(IUpdateListener listener) {
		logging.log(LogLevel.INFO, "Read server list from '" + serverListPath + "' ...");
		XmlServerList serverList = null;
		try {
			serverList = JAXB.unmarshal(serverListPath, XmlServerList.class);
		} catch (Exception ex) {
			logging.printException(ex);
		}
		if (serverList == null) {
			logging.log(LogLevel.ERROR, "Invalid serverlist xml!");
		} else {
			if (serverList.entries.isEmpty()) {
				logging.log(LogLevel.ERROR, "No servers specified in '" + serverListPath + "'!");
			} else {
				if (selectedServer == null) selectedServer = serverList.entries.get(0);
				if (serverList.entries.size() > 1) {
					selectedServer = (String) JOptionPane.showInputDialog(this, "Select a server", "Launcher", JOptionPane.QUESTION_MESSAGE, null, serverList.entries.toArray(), selectedServer);
				}
				if (selectedServer != null) {
					logging.log(LogLevel.INFO, "Selected server '" + selectedServer + "' ('" + selectedServer + "')");
					UpdateController con = new UpdateController(listener, selectedServer);
					con.setLogging(logging);
					con.run();
				} else {
					logging.log(LogLevel.ERROR, "No server selected!");
				}
			}
		}
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
