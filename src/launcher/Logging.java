package launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.swing.JOptionPane;

import launcher.gui.IStatusListener;

/**
 * 
 * Modified: SolidLeon #4 20150227 
 * 
 * @author SolidLeon
 *
 */
public class Logging {


	/** Logging PrintStream */
	private PrintStream ps;
	/** SolidLeon #4 20150227 OutputStream provided by IStatusListener */
	private PrintStream displayTextStream;
	
	/** Logging SimpleDateFormat */
	private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	/** LogLevel */
	public enum LogLevel {
		DEBUG,
		INFO
	};
	
	/** SolidLeon #4 20150227  Status listener used to show progress */
	private IStatusListener statusListener;
	
	/** SolidLeon #4 20150227  If true message boxes will be shown */
	private boolean showStatusMessages;
	
	public Logging(IStatusListener statusListener) {
		this.statusListener = statusListener;
		
		statusListener.setCurrentProgress(0, 0, 3, "Initialize logging ...");
		try {
			ps = new PrintStream(new File(getLogsDirectory(), "log_" + UUID.randomUUID().toString() + ".txt"));
		} catch (FileNotFoundException e) {
			printException(e);
		}
		
		displayTextStream = new PrintStream(statusListener.getOutputStream());
		
		statusListener.setCurrentProgress(statusListener.getCurrentProgress() + 1);
		
		System.setOut(ps);
		statusListener.setCurrentProgress(statusListener.getCurrentProgress() + 1);
		
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> printException(e));
		statusListener.setCurrentProgress(statusListener.getCurrentProgress() + 1);
	}
	
	public void setShowStatusMessages(boolean showStatusMessages) {
		this.showStatusMessages = showStatusMessages;
	}
	
	public boolean isShwoStatusMessages() {
		return showStatusMessages;
	}
	
	public IStatusListener getStatusListener() {
		return statusListener;
	}
	
	/**
	 * Returns an File object pointing to the 'logs' directory.
	 * It also checks if the directory exists and if not creates it.
	 * @return File pointing to 'logs' directory
	 */
	private File getLogsDirectory() {
		File logsFile = new File("logs");
		if (!logsFile.exists()) logsFile.mkdir();
		return logsFile;
	}
	
	/**
	 * Prints the passed exception to an unique error file
	 * @param e
	 */
	public void printException(Throwable e) {
		File exceptionFile = new File(getLogsDirectory(), "ERROR_" + UUID.randomUUID().toString() + ".txt");
		try (PrintStream exout = new PrintStream(exceptionFile)) {
			e.printStackTrace(exout);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} finally {
			logDebug("Exit due to exception, see: '" + exceptionFile.getAbsolutePath() + "'");
			e.printStackTrace(ps);
			statusListener.setStatusCompletedExecCommandOnExit(null);
			JOptionPane.showMessageDialog(null, "Error see '" + exceptionFile.getAbsolutePath() + "'", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(99);
		}
	}
	
	public void close() {
		if (ps != null) {
			ps.close();
		}
	}

	public void logInfo(String s) {
		log(LogLevel.INFO, s);
	}
	public void logDebug(String s) {
		log(LogLevel.DEBUG, s);
	}
	public void log(LogLevel logLevel, String s) {
		System.out.printf("[%s %5s]:  %s%n", sdf.format(new Date()), logLevel.name(), s);
		displayTextStream.printf("[%s %5s]:  %s%n", sdf.format(new Date()), logLevel.name(), s);
		
	}
	public void logEmptyLine(){
		System.out.println();
		displayTextStream.println();
	}
}
