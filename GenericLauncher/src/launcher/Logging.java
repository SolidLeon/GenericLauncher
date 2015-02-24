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
	private static PrintStream ps;
	/** Logging SimpleDateFormat */
	private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	/** LogLevel */
	public static enum LogLevel {
		DEBUG,
		INFO
	};
	/** Current LogLevel */
	private static LogLevel logLevel = LogLevel.DEBUG;
	
	public static void setup() {
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
				Logging.printException(e);
			}
		});
	}
	/**
	 * Prints the passed exception to an unique error file
	 * @param e
	 */
	public static void printException(Throwable e) {
		try (PrintStream exout = new PrintStream("ERROR_" + UUID.randomUUID().toString() + ".txt")) {
			e.printStackTrace(exout);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}
	public static void close() {
		if (ps != null) {
			ps.close();
		}
	}

	public static void logInfo(String s) {
		log(LogLevel.INFO, s);
	}
	public static void logDebug(String s) {
		log(LogLevel.DEBUG, s);
	}
	public static void log(LogLevel logLevel, String s) {
		if (logLevel == logLevel) {
			System.out.println("["+sdf.format(new Date())+" "+logLevel.name()+"]: " + s);
		}
	}
	public static void logEmptyLine(){
		System.out.println();
	}
}
