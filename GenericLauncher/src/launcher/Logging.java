package launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Logging {


	/** Logging PrintStream */
	private PrintStream ps;
	/** Logging SimpleDateFormat */
	private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	/** LogLevel */
	public enum LogLevel {
		DEBUG,
		INFO
	};
	/** Current LogLevel */
	private LogLevel logLevel = LogLevel.DEBUG;
	
	public void setup() {
		try {
			File logsFile = new File("logs");
			if (!logsFile.exists()) logsFile.mkdir();
			ps = new PrintStream(new File(logsFile, "log_" + UUID.randomUUID().toString() + ".txt"));
		} catch (FileNotFoundException e) {
			printException(e);
		}
		
		System.setOut(ps);
		
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				printException(e);
			}
		});
	}
	/**
	 * Prints the passed exception to an unique error file
	 * @param e
	 */
	public void printException(Throwable e) {
		try (PrintStream exout = new PrintStream("ERROR_" + UUID.randomUUID().toString() + ".txt")) {
			e.printStackTrace(exout);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
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
		if (logLevel == logLevel) {
			System.out.println("["+sdf.format(new Date())+" "+logLevel.name()+"]: " + s);
		}
	}
	public void logEmptyLine(){
		System.out.println();
	}
}
