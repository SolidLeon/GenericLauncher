package launcher.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

/**
 * SolidLeon #4 20150227 
 * 
 * Simple display for showing current progress
 * @author SolidLeon
 *
 */
public class StatusDisplay extends JFrame implements IStatusListener, ActionListener {

	private JProgressBar overallProgress;
	private JProgressBar currentProgress;
	private JTextArea text;
	/** OutputStream redirecting output to JTextArea 'text' */
	private OutputStream textOut;
	private JButton closeButton;
	/** Runnable object to 'run' close button is pressed and before disposing */
	private Runnable exitRunner;
	
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
		text = new JTextArea();
		DefaultCaret caret = (DefaultCaret)text.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		add(new JScrollPane(text), BorderLayout.CENTER);
		closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		add(closeButton, BorderLayout.SOUTH);
		
		textOut = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				text.append(String.valueOf((char) b));
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
		
		closeButton.setEnabled(false);
	}
	
	@Override
	public OutputStream getOutputStream() {
		return textOut;
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
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == closeButton) {
			if (exitRunner != null)
				exitRunner.run();
			dispose();
		}
	}
	
	/**
	 * - Sets a runner being executed after close button has been pressed.
	 * - Enabled the close button
	 * - Sets a fancy text for the close button
	 * - Sets overall progress to max
	 * - Sets current progress max to 100 (for cases where the max is 0)
	 * - Sets current progress to max
	 * - Sets 'Done!' as curren progress text
	 */
	@Override
	public void setStatusCompletedExecCommandOnExit(Runnable runner) {
		this.exitRunner = runner;
		closeButton.setEnabled(true);
		closeButton.setText("Completed! Close me!");
		overallProgress.setValue(overallProgress.getMaximum());
		currentProgress.setMaximum(100);
		setCurrentProgressToMax();
		setCurrentProgress("Done!");
	}
	
}
