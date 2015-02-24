package launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarInputStream;

import javax.swing.JOptionPane;


public class Downloader {

	/** Logging PrintStream */
	private static PrintStream ps;
	/** Logging SimpleDateFormat */
	private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	/** LogLevel */
	private static enum LogLevel {
		DEBUG,
		INFO
	};
	/** Current LogLevel */
	private static LogLevel logLevel = LogLevel.DEBUG;
	
	
	public static void main(String[] args) {
		
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				printException(e);
			}
		});
		
		try {
			File logsFile = new File("logs");
			if (!logsFile.exists()) logsFile.mkdir();
			ps = new PrintStream(new File(logsFile, "log_" + UUID.randomUUID().toString() + ".txt"));
		} catch (FileNotFoundException e) {
			printException(e);
		}
		
		System.setOut(ps);
		
		long bootstrapModified = new File("bootstrap.jar").lastModified();
		
		logInfo("Launcher " + Downloader.class.getPackage().getImplementationVersion() + " started");
		logInfo("Current Time is " + new Date().toString());
		logInfo("System.getProperty('os.name') == '" + System.getProperty("os.name") + "'");
		logInfo("System.getProperty('os.version') == '" + System.getProperty("os.version") + "'");
		logInfo("System.getProperty('os.arch') == '" + System.getProperty("os.arch") + "'");
		logInfo("System.getProperty('java.version') == '" + System.getProperty("java.version") + "'");
		logInfo("System.getProperty('java.vendor') == '" + System.getProperty("java.vendor") + "'");
		logInfo("System.getProperty('sun.arch.data.model') == '" + System.getProperty("sun.arch.data.model") + "'");
		logEmptyLine();
		
		
		// SERVER LIST BEGIN
		List<ServerListEntry> serverList = new ArrayList<>();
		readServerList(serverList, new File("serverlist.txt"));
		if (serverList.isEmpty()) {
			logInfo("No server found!");
			ps.close();
			System.exit(0);
		}
		
		logInfo(serverList.size() + " server loaded!");
		Object serverSelectionObject = serverList.get(0);
		if (serverList.size() > 1) {
			serverSelectionObject = JOptionPane.showInputDialog(null, "Select a server you want to connect to: ", "Launcher", JOptionPane.QUESTION_MESSAGE, null, serverList.toArray(), serverList.get(0));
			if (serverSelectionObject == null) {
				logInfo("No configuration selected!");
				ps.close();
				System.exit(0);
			}
		}
		
		logInfo("Selected server '" + serverSelectionObject + "'");
		// SERVER LIST END
		
		// LAUNCHER CONFIGS (*.cfg) BEGIN
		List<File> launcherConfigList = getLauncherConfigList(((ServerListEntry)serverSelectionObject).basePath);
		if (launcherConfigList.isEmpty()) {
			logInfo("No launcher configurations found!");
			ps.close();
			System.exit(0);
		}
		
		logInfo(launcherConfigList.size() + " launcher configuration(s) loaded!");
		Object selection = launcherConfigList.get(0);
		if (launcherConfigList.size() > 1) {
			logDebug("User selects configuration...");
			selection = JOptionPane.showInputDialog(null, "Select a configuration:", "Launcher " + 
					Downloader.class.getPackage().getImplementationVersion(), 
					JOptionPane.QUESTION_MESSAGE, null, 
					launcherConfigList.toArray(), launcherConfigList.get(0));
			if (selection == null) {
				logInfo("No configuration selected!");
				ps.close();
				System.exit(0);
			}
		}
		logInfo("Selected launcher config '" + selection + "'");
		LauncherConfig launcherConfig = readLauncherConfig((File)selection);
		logDebug("Launcher Config     '" + ((File)selection).getName() + "'");
		logDebug("  BASE PATH=        '" + launcherConfig.getBasePath().getAbsolutePath() + "'");
		logDebug("  POST COMMAND=     '" + launcherConfig.getPostCommand() + "'");
		logDebug("  POST CWD=         '" + launcherConfig.getPostCWD().getAbsolutePath() + "'");
		logDebug("  LOG LEVEL=        '" + launcherConfig.getLogLevel() + "'");
		// LAUNCHER CONFIGS (*.cfg) END
		
		// DOWNLOAD CONFIGS (v_*) BEGIN
		for (File f : launcherConfig.getDownloadConfigs())
			logDebug("  DOWNLOAD CONFIG=  '" + f.getAbsolutePath() + "'");
		if (launcherConfig.getLogLevel() != null) {
			Downloader.logLevel = launcherConfig.getLogLevel();
			logDebug("Set log level to " + Downloader.logLevel);
		}
		
		List<DownloadConfig> remoteConfigs = new LinkedList<>();
		
		if (launcherConfig.getDownloadConfigs().isEmpty())
			readDownloadConfigs(remoteConfigs, launcherConfig, Arrays.asList(launcherConfig.getBasePath().listFiles()));
		else
			readDownloadConfigs(remoteConfigs, launcherConfig, launcherConfig.getDownloadConfigs());
		// DOWNLOAD CONFIGS (v_*) END
		
		// ACTUAL DOWNLOAD BEGIN
		logEmptyLine();
		logDebug("BEGIN DOWNLOAD");
		for (DownloadConfig cfg : remoteConfigs) {
			logDebug("CHECK DOWNLOAD '" + cfg.name + "'");
			File sourceComparisonFile = cfg.compare != null ? cfg.compare : cfg.target;
			logDebug("  COMPARE= '" + sourceComparisonFile.getAbsolutePath() + "'");
			if (cfg.source.lastModified() > sourceComparisonFile.lastModified())
				download(cfg);
			else  {
				logDebug("  SKIP ALREADY UPDATE");
				logDebug("  [" + cfg.getVersion() + "] " + 
				"'" +  cfg.getSource().getAbsolutePath() + "'" + " -> " + 
						"'" + cfg.getTarget().getAbsolutePath() + "'");
			}
		}
		logDebug("DONE!");
		// ACTUAL DOWNLOAD END
		
		// CHECK IF SOMETHING WAS UPDATED THAT REQUIRES A LAUNCHER RESTART
		boolean bootstrapUpdated = bootstrapModified < new File("bootstrap.jar").lastModified();
		if (bootstrapUpdated) {
			logDebug("Bootstrap update! Restart!");
			try {
				ps.close();
				Runtime.getRuntime().exec("java -jar bootstrap.jar");
				System.exit(0);
			} catch (IOException e) {
				printException(e);
			}
		} else if (new File("launcher_new.jar").exists()) {
			logDebug("Launcher update! Restart!");
			try {
				ps.close();
				Runtime.getRuntime().exec("java -jar bootstrap.jar");
				System.exit(0);
			} catch (IOException e) {
				printException(e);
			}
		} else {
			// NO UPDATE REQUIRES A RESTART -> EXECUTE 'POST_COMMAND' IN 'POST_CWD'
			if (launcherConfig.getPostCommand() != null) {
				try {
					logInfo("Execute '" + launcherConfig.getPostCommand() + "' ...");
					Runtime.getRuntime().exec(launcherConfig.getPostCommand(), null, launcherConfig.getPostCWD());
				} catch (IOException e) {
					printException(e);
				}
			}
			
			ps.close();
			System.exit(0);
		}
		
	}

	/**
	 * Reads a list of NAME=SERVER_PATH from a text file 
	 * @param serverList
	 * @param file
	 */
	private static void readServerList(List<ServerListEntry> serverList,
			File file) {
		try {
			for (String line : Files.readAllLines(file.toPath())) {
				int idx = line.indexOf('=');
				if (idx == -1) continue;
				String name = line.substring(0, idx).trim();
				String basePath = line.substring(idx + 1).trim();
				
				ServerListEntry entry = new ServerListEntry();
				entry.setName(name);
				entry.setBasePath(new File(basePath));
				
				serverList.add(entry);
			}
		} catch (IOException e) {
			printException(e); // markusmannel@gmail.com 20150224 Utilize our exception-to-file mechanism
		}
	}

	/**
	 * Prints the passed exception to an unique error file
	 * @param e
	 */
	protected static void printException(Throwable e) {
		try (PrintStream exout = new PrintStream("ERROR_" + UUID.randomUUID().toString() + ".txt")) {
			e.printStackTrace(exout);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Scans for launcher configurations (*.cfg files)
	 * @param file - a directory containing cfg files
	 * @return a list containing all cfg files withing 'file'
	 */
	private static List<File> getLauncherConfigList(File file) {
		logDebug("Load launcher configurations from '" + file.getAbsolutePath() + "'");
		if (!file.isDirectory()) {
			logDebug("This is not a directory!");
			logDebug(file.getAbsolutePath());
			return null;
		}
		List<File> configurationList = new ArrayList<>();
		for (File f : file.listFiles()) {
			if (f.getName().endsWith(".cfg")) { 
				logDebug("  ADD '" + f.getAbsolutePath() + "'");
				configurationList.add(f);
			}
		}
		logDebug("DONE!");
		return configurationList;
	}

	/**
	 * Reads the launcher configuration file (cfg) and creates a new
	 * {@link LauncherConfig} 
	 * @param launcherConfigFile
	 * @return
	 */
	private static LauncherConfig readLauncherConfig(File launcherConfigFile) {
		LauncherConfig cfg = new LauncherConfig();
		
		try {
			List<String> lines = Files.readAllLines(launcherConfigFile.toPath());
			List<File> downloadConfigList = new ArrayList<>();
			
			for (String line : lines) {
				if (line.startsWith("BASE_PATH=")) {
					String sBasePath = line.substring("BASE_PATH=".length());
					cfg.setBasePath(new File(sBasePath));
				} else if (line.startsWith("POST_COMMAND=")) {
					String sPostCommand = line.substring("POST_COMMAND=".length());
					cfg.setPostCommand(sPostCommand);
				}  else if (line.startsWith("POST_CWD=")) {
					String sPostCWD = line.substring("POST_CWD=".length());
					cfg.setPostCWD(new File(sPostCWD));
				} else if (line.startsWith("LOG_LEVEL=")) {
					String sLogLevel = line.substring("LOG_LEVEL=".length());
					cfg.setLogLevel(LogLevel.valueOf(sLogLevel));
				} else if (line.startsWith("DOWNLOAD_CONFIG=")) {
					String sDownloadConfig = line.substring("DOWNLOAD_CONFIG=".length());
					downloadConfigList.add(new File(sDownloadConfig));
				}
			}

			// Download configs post-setup
			cfg.setDownloadConfigs(downloadConfigList);
			//   we need to adjust relative paths and combine them with the basePath (remote path)
			//   Detailed: The path is relative on the remote machine, but new File(path) would create a relative path
			//			   on the local machine, so we would not be able to access the right file.
			//			   So we use the remote base path to create a new File pointing to the remote
			for (int i = 0; i < downloadConfigList.size(); i++) {
				File f = downloadConfigList.get(i);
				if (!f.isAbsolute()) {
					f = new File(cfg.getBasePath(), f.getPath());
					downloadConfigList.set(i, f);
				}
			}
			// If the config does not provide a 'POST_CWD' we set it to this jars CWD.
			if (cfg.getPostCWD() == null) {
				cfg.setPostCWD(new File(System.getProperty("user.dir")));
			}
			
		} catch (IOException e) {
			printException(e);
			return null;
		}
		
		return cfg;
	}
	


	private static void readDownloadConfigs(List<DownloadConfig> remoteConfigs,
			LauncherConfig launcherConfig,
			List<File> dir) {
		for (File remoteConfigFile : dir) {
			DownloadConfig cfg = readDownloadConfig(launcherConfig.getBasePath(), remoteConfigFile);
			if (cfg.getSource().isDirectory()) {
				addDownloadConfigsRecursivly(remoteConfigs, cfg.getSource(), cfg.getTarget(), cfg.getSource());
			} else {
				remoteConfigs.add(cfg);
				logDebug("Download Config '" + cfg.getName() + "'");
				logDebug("  SOURCE=  '" + cfg.getSource().getAbsolutePath() + "'");
				logDebug("  TARGET=  '" + cfg.getTarget().getAbsolutePath() + "'");
				logDebug("  COMPARE= '" + (cfg.getCompare() == null ? "None" : cfg.getCompare().getAbsolutePath()) + "'");
			}
		}
		logDebug("DONE");
	}
	


	private static void addDownloadConfigsRecursivly(
			List<DownloadConfig> remoteConfigs, File basePath, File target, File source) {
		if (source.isFile()) {
			DownloadConfig cfg = new DownloadConfig();
			cfg.setSource(source);
			cfg.setTarget( new File(target, source.getAbsolutePath().substring(basePath.getAbsolutePath().length())) );
			cfg.setName(source.getName() + UUID.randomUUID().toString());
			logDebug("Download Config '" + cfg.getName() + "'");
			logDebug("  SOURCE=  '" + cfg.getSource().getAbsolutePath() + "'");
			logDebug("  TARGET=  '" + cfg.getTarget().getAbsolutePath() + "'");
			logDebug("  COMPARE= '" + (cfg.getCompare() == null ? "None" : cfg.getCompare().getAbsolutePath()) + "'");
			remoteConfigs.add(cfg);
		} else {
			for (File ff : source.listFiles())
				addDownloadConfigsRecursivly(remoteConfigs, basePath, target, ff);
		}
	}

	/**
	 * Copy files using {@link Files#copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption...)}
	 * @param cfg
	 */
	private static void download(DownloadConfig cfg) {
		try {
			logInfo("  DOWNLOADING ...");
			logInfo("  [" + cfg.getVersion() + "] " +
			"'" + cfg.getSource().getAbsolutePath() + "'" + " -> " + 
			"'" + cfg.getTarget().getAbsolutePath() + "'");
			cfg.getTarget().mkdirs();
			Files.copy(cfg.getSource().toPath(), cfg.getTarget().toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			printException(e);
		}
	}

	private static void logInfo(String s) {
		log(LogLevel.INFO, s);
	}
	private static void logDebug(String s) {
		log(LogLevel.DEBUG, s);
	}
	private static void log(LogLevel logLevel, String s) {
		if (logLevel == Downloader.logLevel) {
			System.out.println("["+sdf.format(new Date())+" "+logLevel.name()+"]: " + s);
		}
	}
	private static void logEmptyLine(){
		System.out.println();
	}

	/**
	 * Parse remote file and create a new {@link DownloadConfig}
	 * @param basePath
	 * @param remoteFile
	 * @return
	 */
	private static DownloadConfig readDownloadConfig(File basePath, File remoteFile) {
		DownloadConfig cfg = new DownloadConfig();
		cfg.setName(remoteFile.getName());
		
		try {
			List<String> lines = Files.readAllLines(remoteFile.toPath());
			
			for (String line : lines) {
				if (line.startsWith("SOURCE=")) {
					String sSource = line.substring("SOURCE=".length());
					File sourceFile = new File(sSource);
					if (!sourceFile.isAbsolute())
						sourceFile = new File(basePath, sSource);
					cfg.setSource(sourceFile);
				} else if (line.startsWith("TARGET=")) {
					String sTarget = line.substring("TARGET=".length());
					File targetFile = new File(sTarget);
					cfg.setTarget(targetFile);
				} else if (line.startsWith("COMPARE=")) {
					String sCompare = line.substring("COMPARE=".length());
					File compareFile = new File(sCompare);
					cfg.setCompare(compareFile);
				} else if (line.startsWith("VERSION=")) {
					cfg.setVersion(Integer.valueOf(line.substring("VERSION=".length())));
				}
			}
			
			
		} catch (IOException e) {
			printException(e);
			return null;
		}
		
		return cfg;
	}


	/**
	 * v_* files
	 * @author Markus
	 *
	 */
	static class DownloadConfig {
		private String name;
		private File source;
		private File target;
		private File compare;
		private int version;
		
		
		
		public File getCompare() {
			return compare;
		}
		public void setCompare(File compare) {
			this.compare = compare;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public File getSource() {
			return source;
		}
		public void setSource(File source) {
			this.source = source;
		}
		public File getTarget() {
			return target;
		}
		public void setTarget(File target) {
			this.target = target;
		}
		public int getVersion() {
			return version;
		}
		public void setVersion(int version) {
			this.version = version;
		}
	}
	
	/**
	 * *.cfg files
	 * @author Markus
	 *
	 */
	static class LauncherConfig {
		private File basePath;
		private String postCommand;
		private File postCWD;
		private LogLevel logLevel;
		private List<File> downloadConfigs;
		
		public List<File> getDownloadConfigs() {
			return downloadConfigs;
		}

		public void setDownloadConfigs(List<File> downloadConfigs) {
			this.downloadConfigs = downloadConfigs;
		}

		public LogLevel getLogLevel() {
			return logLevel;
		}

		public void setLogLevel(LogLevel logLevel) {
			this.logLevel = logLevel;
		}

		public File getPostCWD() {
			return postCWD;
		}

		public void setPostCWD(File postCWD) {
			this.postCWD = postCWD;
		}

		public String getPostCommand() {
			return postCommand;
		}

		public void setPostCommand(String postCommand) {
			this.postCommand = postCommand;
		}

		public File getBasePath() {
			return basePath;
		}

		public void setBasePath(File basePath) {
			this.basePath = basePath;
		}
		
	}
	
	/**
	 * serverlist.txt entries
	 * @author Markus
	 *
	 */
	static class ServerListEntry {
		private String name;
		private File basePath;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public File getBasePath() {
			return basePath;
		}
		public void setBasePath(File basePath) {
			this.basePath = basePath;
		}
		
		@Override
		public String toString() {
			return name + " (" + basePath.getAbsolutePath() + ")";
		}
		
	}
}
