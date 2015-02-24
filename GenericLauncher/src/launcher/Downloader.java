package launcher;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.swing.JOptionPane;

import launcher.Logging.LogLevel;


public class Downloader {
	
	
	public static void main(String[] args) {
		
		Logging.setup();
		
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				Logging.printException(e);
			}
		});
		
		// SETUP LOGGING BEGIN
		// SETUP LOGGING END
		
		long bootstrapModified = new File("bootstrap.jar").lastModified();
		
		Logging.logInfo("Launcher " + Downloader.class.getPackage().getImplementationVersion() + " started");
		Logging.logInfo("Current Time is " + new Date().toString());
		Logging.logInfo("System.getProperty('os.name') == '" + System.getProperty("os.name") + "'");
		Logging.logInfo("System.getProperty('os.version') == '" + System.getProperty("os.version") + "'");
		Logging.logInfo("System.getProperty('os.arch') == '" + System.getProperty("os.arch") + "'");
		Logging.logInfo("System.getProperty('java.version') == '" + System.getProperty("java.version") + "'");
		Logging.logInfo("System.getProperty('java.vendor') == '" + System.getProperty("java.vendor") + "'");
		Logging.logInfo("System.getProperty('sun.arch.data.model') == '" + System.getProperty("sun.arch.data.model") + "'");
		Logging.logEmptyLine();
		
		
		// SERVER LIST BEGIN
		List<ServerListEntry> serverList = new ArrayList<>();
		readServerList(serverList, new File("serverlist.txt"));
		if (serverList.isEmpty()) {
			Logging.logInfo("No server found!");
			Logging.close();;
			System.exit(0);
		}
		
		Logging.logInfo(serverList.size() + " server loaded!");
		Object serverSelectionObject = serverList.get(0);
		if (serverList.size() > 1) {
			serverSelectionObject = JOptionPane.showInputDialog(null, "Select a server you want to connect to: ", "Launcher", JOptionPane.QUESTION_MESSAGE, null, serverList.toArray(), serverList.get(0));
			if (serverSelectionObject == null) {
				Logging.logInfo("No configuration selected!");
				Logging.close();;
				System.exit(0);
			}
		}
		
		Logging.logInfo("Selected server '" + serverSelectionObject + "'");
		// SERVER LIST END
		
		// LAUNCHER CONFIGS (*.cfg) BEGIN
		List<File> launcherConfigList = getLauncherConfigList(((ServerListEntry)serverSelectionObject).getBasePath());
		if (launcherConfigList.isEmpty()) {
			Logging.logInfo("No launcher configurations found!");
			Logging.close();
			System.exit(0);
		}
		
		Logging.logInfo(launcherConfigList.size() + " launcher configuration(s) loaded!");
		Object selection = launcherConfigList.get(0);
		if (launcherConfigList.size() > 1) {
			Logging.logDebug("User selects configuration...");
			selection = JOptionPane.showInputDialog(null, "Select a configuration:", "Launcher " + 
					Downloader.class.getPackage().getImplementationVersion(), 
					JOptionPane.QUESTION_MESSAGE, null, 
					launcherConfigList.toArray(), launcherConfigList.get(0));
			if (selection == null) {
				Logging.logInfo("No configuration selected!");
				Logging.close();
				System.exit(0);
			}
		}
		Logging.logInfo("Selected launcher config '" + selection + "'");
		LauncherConfig launcherConfig = readLauncherConfig((File)selection);
		Logging.logDebug("Launcher Config     '" + ((File)selection).getName() + "'");
		Logging.logDebug("  BASE PATH=        '" + launcherConfig.getBasePath().getAbsolutePath() + "'");
		Logging.logDebug("  POST COMMAND=     '" + launcherConfig.getPostCommand() + "'");
		Logging.logDebug("  POST CWD=         '" + launcherConfig.getPostCWD().getAbsolutePath() + "'");
		Logging.logDebug("  LOG LEVEL=        '" + launcherConfig.getLogLevel() + "'");
		// LAUNCHER CONFIGS (*.cfg) END
		
		// DOWNLOAD CONFIGS (v_*) BEGIN
		for (File f : launcherConfig.getDownloadConfigs())
			Logging.logDebug("  DOWNLOAD CONFIG=  '" + f.getAbsolutePath() + "'");
		
		List<DownloadConfig> remoteConfigs = new LinkedList<>();
		
		if (launcherConfig.getDownloadConfigs().isEmpty())
			readDownloadConfigs(remoteConfigs, launcherConfig, Arrays.asList(launcherConfig.getBasePath().listFiles()));
		else
			readDownloadConfigs(remoteConfigs, launcherConfig, launcherConfig.getDownloadConfigs());
		// DOWNLOAD CONFIGS (v_*) END
		
		// ACTUAL DOWNLOAD BEGIN
		Logging.logEmptyLine();
		Logging.logDebug("BEGIN DOWNLOAD");
		for (DownloadConfig cfg : remoteConfigs) {
			Logging.logDebug("CHECK DOWNLOAD '" + cfg.getName() + "'");
			File sourceComparisonFile = cfg.getCompare() != null ? cfg.getCompare() : cfg.getTarget();
			Logging.logDebug("  COMPARE= '" + sourceComparisonFile.getAbsolutePath() + "'");
			if (cfg.getSource().lastModified() > sourceComparisonFile.lastModified())
				download(cfg);
			else  {
				Logging.logDebug("  SKIP ALREADY UPDATE");
				Logging.logDebug("  [" + cfg.getVersion() + "] " + 
				"'" +  cfg.getSource().getAbsolutePath() + "'" + " -> " + 
						"'" + cfg.getTarget().getAbsolutePath() + "'");
			}
		}
		Logging.logDebug("DONE!");
		// ACTUAL DOWNLOAD END
		
		// CHECK IF SOMETHING WAS UPDATED THAT REQUIRES A LAUNCHER RESTART
		boolean bootstrapUpdated = bootstrapModified < new File("bootstrap.jar").lastModified();
		if (bootstrapUpdated) {
			Logging.logDebug("Bootstrap update! Restart!");
			try {
				Logging.close();
				Runtime.getRuntime().exec("java -jar bootstrap.jar");
				System.exit(0);
			} catch (IOException e) {
				Logging.printException(e);
			}
		} else if (new File("launcher_new.jar").exists()) {
			Logging.logDebug("Launcher update! Restart!");
			try {
				Logging.close();
				Runtime.getRuntime().exec("java -jar bootstrap.jar");
				System.exit(0);
			} catch (IOException e) {
				Logging.printException(e);
			}
		} else {
			// NO UPDATE REQUIRES A RESTART -> EXECUTE 'POST_COMMAND' IN 'POST_CWD'
			if (launcherConfig.getPostCommand() != null) {
				try {
					Logging.logInfo("Execute '" + launcherConfig.getPostCommand() + "' ...");
					Runtime.getRuntime().exec(launcherConfig.getPostCommand(), null, launcherConfig.getPostCWD());
				} catch (IOException e) {
					Logging.printException(e);
				}
			}
			
			Logging.close();
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
			Logging.printException(e); // markusmannel@gmail.com 20150224 Utilize our exception-to-file mechanism
		}
	}


	/**
	 * Scans for launcher configurations (*.cfg files)
	 * @param file - a directory containing cfg files
	 * @return a list containing all cfg files withing 'file'
	 */
	private static List<File> getLauncherConfigList(File file) {
		Logging.logDebug("Load launcher configurations from '" + file.getAbsolutePath() + "'");
		if (!file.isDirectory()) {
			Logging.logDebug("This is not a directory!");
			Logging.logDebug(file.getAbsolutePath());
			return null;
		}
		List<File> configurationList = new ArrayList<>();
		for (File f : file.listFiles()) {
			if (f.getName().endsWith(".cfg")) { 
				Logging.logDebug("  ADD '" + f.getAbsolutePath() + "'");
				configurationList.add(f);
			}
		}
		Logging.logDebug("DONE!");
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
			
			// Set default log level
			if (cfg.getLogLevel() == null)
				cfg.setLogLevel(LogLevel.DEBUG);
			
		} catch (IOException e) {
			Logging.printException(e);
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
				Logging.logDebug("Download Config '" + cfg.getName() + "'");
				Logging.logDebug("  SOURCE=  '" + cfg.getSource().getAbsolutePath() + "'");
				Logging.logDebug("  TARGET=  '" + cfg.getTarget().getAbsolutePath() + "'");
				Logging.logDebug("  COMPARE= '" + (cfg.getCompare() == null ? "None" : cfg.getCompare().getAbsolutePath()) + "'");
			}
		}
		Logging.logDebug("DONE");
	}
	


	private static void addDownloadConfigsRecursivly(
			List<DownloadConfig> remoteConfigs, File basePath, File target, File source) {
		if (source.isFile()) {
			DownloadConfig cfg = new DownloadConfig();
			cfg.setSource(source);
			cfg.setTarget( new File(target, source.getAbsolutePath().substring(basePath.getAbsolutePath().length())) );
			cfg.setName(source.getName() + UUID.randomUUID().toString());
			Logging.logDebug("Download Config '" + cfg.getName() + "'");
			Logging.logDebug("  SOURCE=  '" + cfg.getSource().getAbsolutePath() + "'");
			Logging.logDebug("  TARGET=  '" + cfg.getTarget().getAbsolutePath() + "'");
			Logging.logDebug("  COMPARE= '" + (cfg.getCompare() == null ? "None" : cfg.getCompare().getAbsolutePath()) + "'");
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
			Logging.logInfo("  DOWNLOADING ...");
			Logging.logInfo("  [" + cfg.getVersion() + "] " +
			"'" + cfg.getSource().getAbsolutePath() + "'" + " -> " + 
			"'" + cfg.getTarget().getAbsolutePath() + "'");
			cfg.getTarget().mkdirs();
			Files.copy(cfg.getSource().toPath(), cfg.getTarget().toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			Logging.printException(e);
		}
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
			Logging.printException(e);
			return null;
		}
		
		return cfg;
	}

}



/**
 * v_* files
 * @author Markus
 *
 */
class DownloadConfig {
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
class LauncherConfig {
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
class ServerListEntry {
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