package launcher.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import launcher.Downloader;
import launcher.Launcher;
import launcher.Logging;
import launcher.Logging.LogLevel;
import launcher.controller.ComponentController;
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
		
		int start = text.getCaretPosition();
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
		closeButton.setEnabled(false);
		
		logging.getStatusListener().setOverallProgress(0, 0, 4);
		
		logging.getStatusListener().setCurrentProgress(0, 0, 0, "Initialize launcher restart ...");
		LauncherRestartController launcherRestartController = new LauncherRestartController(logging);

		ServerListController serverListController = new ServerListController(logging);
		serverListController.run();

		PackageController packageController = new PackageController(logging, serverListController.getSelected());
		packageController.run();
		launcherRestartController.setActivePackageBean(packageController.getSelectedPackageBean());

		ComponentController componentController = new ComponentController(logging, packageController.getSelectedPackageBean());
		componentController.run();
		
		PreviewDialog previewDialog = new PreviewDialog(this, componentController.getResultComponentList());
		previewDialog.setVisible(true);
		
		PreviewResult previewResult = previewDialog.getPreviewResult();
		if (previewResult == PreviewResult.OK) {
			Downloader downloader = new Downloader(logging, componentController.getResultComponentList());
			downloader.run();
	
			// CHECK IF SOMETHING WAS UPDATED THAT REQUIRES A LAUNCHER RESTART
			launcherRestartController.run();
		} else {
			logging.log(LogLevel.INFO, "User cancelled preview");
		}
		setStatusCompleted(); //// SolidLeon #4 20150227 - we set the overall status so even if the user cancels the end-state is completed
		logging.log(LogLevel.FINE, "Done!");
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
