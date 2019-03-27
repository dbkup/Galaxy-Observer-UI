// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package interfacebuilder.base_ui;

import cascexplorerconfigedit.editor.CascExplorerConfigFileEditor;
import com.ahli.galaxy.game.GameData;
import com.ahli.galaxy.game.def.SC2GameDef;
import com.ahli.galaxy.game.def.abstracts.GameDef;
import com.ahli.galaxy.ui.UICatalogImpl;
import com.ahli.galaxy.ui.interfaces.UICatalog;
import com.esotericsoftware.kryo.Kryo;
import interfacebuilder.InterfaceBuilderApp;
import interfacebuilder.compress.GameService;
import interfacebuilder.config.ConfigService;
import interfacebuilder.integration.FileService;
import interfacebuilder.integration.SettingsIniInterface;
import interfacebuilder.integration.kryo.KryoGameInfo;
import interfacebuilder.integration.kryo.KryoService;
import interfacebuilder.projects.enums.Game;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.boris.pecoff4j.PE;
import org.boris.pecoff4j.ResourceDirectory;
import org.boris.pecoff4j.ResourceEntry;
import org.boris.pecoff4j.constant.ResourceType;
import org.boris.pecoff4j.io.PEParser;
import org.boris.pecoff4j.io.ResourceParser;
import org.boris.pecoff4j.resources.StringFileInfo;
import org.boris.pecoff4j.resources.StringTable;
import org.boris.pecoff4j.resources.VersionInfo;
import org.boris.pecoff4j.util.ResourceHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class BaseUiService {
	private static final String UNKNOWN_GAME_EXCEPTION = "Unknown Game";
	private static final Logger logger = LogManager.getLogger(BaseUiService.class);
	private static final String META_FILE_NAME = ".meta";
	
	private final InterfaceBuilderApp app = InterfaceBuilderApp.getInstance();
	
	@Autowired
	private ConfigService configService;
	@Autowired
	private GameService gameService;
	@Autowired
	private FileService fileService;
	@Autowired
	private DiscCacheService discCacheService;
	@Autowired
	private KryoService kryoService;
	
	/**
	 * Checks if the specified game's baseUI is older than the game files.
	 *
	 * @param game
	 * @param usePtr
	 * @return true, if outdated
	 */
	public boolean isOutdated(final Game game, final boolean usePtr) throws IOException {
		final GameDef gameDef = gameService.getGameDef(game);
		final File gameBaseUI = new File(configService.getBaseUiPath(gameDef));
		
		if (!gameBaseUI.exists() || fileService.isEmptyDirectory(gameBaseUI)) {
			return true;
		}
		
		final File baseUiMetaFileDir = new File(configService.getBaseUiPath(gameDef));
		final KryoGameInfo baseUiInfo;
		try {
			baseUiInfo = readMetaFile(baseUiMetaFileDir);
		} catch (final IOException e) {
			final String msg = "Failed to read Game Info from extracted base UI.";
			logger.warn(msg);
			logger.trace(msg, e);
			return true;
		}
		final int[] versionBaseUi = baseUiInfo.getVersion();
		final int[] versionExe = getVersion(gameDef, usePtr);
		boolean isUpToDate = true;
		for (int i = 0; i < versionExe.length && isUpToDate; ++i) {
			isUpToDate = versionExe[i] <= versionBaseUi[i];
		}
		if (logger.isTraceEnabled()) {
			logger.trace(
					"Exe version check - exe=" + Arrays.toString(versionExe) + " - " + Arrays.toString(versionBaseUi) +
							" - upToDate=" + isUpToDate);
		}
		
		return !isUpToDate;
	}
	
	private KryoGameInfo readMetaFile(final File directory) throws IOException {
		final Path path = Paths.get(directory.getAbsolutePath(), META_FILE_NAME);
		final Kryo kryo = kryoService.getKryoForBaseUiMetaFile();
		final List<Class<? extends Object>> payloadClasses = new ArrayList<>();
		payloadClasses.add(KryoGameInfo.class);
		return (KryoGameInfo) kryoService.get(path, payloadClasses, kryo).get(0);
	}
	
	public int[] getVersion(final GameDef gameDef, final boolean isPtr) {
		final int[] versions = new int[4];
		final Path path = Paths.get(gameService.getGameDirPath(gameDef, isPtr), gameDef.getSupportDirectoryX64(),
				gameDef.getSwitcherExeNameX64());
		try {
			final PE pe = PEParser.parse(path.toFile());
			final ResourceDirectory rd = pe.getImageData().getResourceTable();
			
			final ResourceEntry[] entries = ResourceHelper.findResources(rd, ResourceType.VERSION_INFO);
			
			for (final ResourceEntry entry : entries) {
				final byte[] data = entry.getData();
				final VersionInfo version = ResourceParser.readVersionInfo(data);
				
				final StringFileInfo strings = version.getStringFileInfo();
				final StringTable table = strings.getTable(0);
				for (int j = 0; j < table.getCount(); j++) {
					final String key = table.getString(j).getKey();
					if ("FileVersion".equals(key)) {
						final String value = table.getString(j).getValue();
						logger.trace("found FileVersion={}", () -> value);
						
						final String[] parts = value.split("\\.");
						for (int k = 0; k < 4; k++) {
							versions[k] = Integer.parseInt(parts[k]);
						}
						return versions;
					}
				}
			}
		} catch (final IOException e) {
			logger.error("Error attempting to parse FileVersion from game's exe: ", e);
		}
		return versions;
	}
	
	/**
	 * Returns the game path.
	 *
	 * @param game
	 * @param usePtr
	 * @return
	 */
	private String getGamePath(final Game game, final boolean usePtr) {
		final SettingsIniInterface iniSettings = configService.getIniSettings();
		final String gamePath;
		if (game.equals(Game.SC2)) {
			gamePath = iniSettings.getSc2Path();
		} else {
			if (game.equals(Game.HEROES)) {
				gamePath = usePtr ? iniSettings.getHeroesPtrPath() : iniSettings.getHeroesPath();
			} else {
				throw new InvalidParameterException(UNKNOWN_GAME_EXCEPTION);
			}
		}
		return gamePath;
	}
	
	/**
	 * Creates Tasks that will extract the base UI for a specified game.
	 *
	 * @param game
	 * @param usePtr
	 */
	public void extract(final Game game, final boolean usePtr) {
		logger.info(String.format("Extracting baseUI for %s", game.toString()));
		prepareCascExplorerConfig(game, usePtr);
		
		final ThreadPoolExecutor executor = InterfaceBuilderApp.getInstance().getExecutor();
		final GameDef gameDef = gameService.getGameDef(game);
		final File destination = new File(configService.getBaseUiPath(gameDef));
		
		try {
			if (!destination.exists() && !destination.mkdirs()) {
				logger.error(String.format("Directory %s could not be created.", destination));
				return;
			}
			fileService.cleanDirectory(destination);
			discCacheService.remove(gameDef.getName(), usePtr);
		} catch (final IOException e) {
			logger.error(String.format("Directory %s could not be cleaned.", destination), e);
			return;
		}
		
		final File extractorExe = configService.getCascExtractorConsoleExeFile();
		final String[] queryMasks = getQueryMasks(game);
		for (final String mask : queryMasks) {
			final Runnable task = () -> {
				try {
					if (extract(extractorExe, mask, destination)) {
						Thread.sleep(50);
						if (extract(extractorExe, mask, destination)) {
							logger.warn(
									"Extraction failed due to a file access. Try closing the Battle.net App, if it is running and this fails to extract all files.");
						}
					}
				} catch (final IOException e) {
					logger.error("Extracting files from CASC via CascExtractor failed.", e);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			};
			executor.execute(task);
		}
		
		final int[] version = getVersion(gameDef, usePtr);
		try {
			writeToMetaFile(destination, gameDef.getName(), version, usePtr);
		} catch (final IOException e) {
			logger.error("Failed to write metafile: ", e);
		}
	}
	
	/**
	 * Edits the config file of the CASCexplorer.
	 *
	 * @param game
	 * @param usePtr
	 */
	private void prepareCascExplorerConfig(final Game game, final boolean usePtr) {
		final File configFile = configService.getCascExtractorConfigFile();
		final String storagePath;
		final String onlineMode = "False";
		final String product;
		final String locale = "enUS";
		switch (game) {
			case SC2:
				storagePath = configService.getIniSettings().getSc2Path();
				product = "sc2";
				break;
			case HEROES:
				if (usePtr) {
					storagePath = configService.getIniSettings().getHeroesPtrPath();
				} else {
					storagePath = configService.getIniSettings().getHeroesPath();
				}
				product = "heroes";
				break;
			default:
				throw new InvalidParameterException(UNKNOWN_GAME_EXCEPTION);
		}
		CascExplorerConfigFileEditor.write(configFile, storagePath, onlineMode, product, locale);
	}
	
	private String[] getQueryMasks(final Game game) {
		switch (game) {
			case SC2:
				return new String[] { "*.SC2Layout", "*Assets.txt", "*.SC2Style" };
			case HEROES:
				return new String[] { "*.stormlayout", "*assets.txt", "*.stormstyle" };
			default:
				throw new InvalidParameterException(UNKNOWN_GAME_EXCEPTION);
		}
	}
	
	/**
	 * @param extractorExe
	 * @param mask
	 * @param destination
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private boolean extract(final File extractorExe, final String mask, final File destination)
			throws IOException, InterruptedException {
		final ProcessBuilder pb =
				new ProcessBuilder(extractorExe.getAbsolutePath(), mask, destination + File.separator, "enUS", "None");
		// put error and normal output into the same stream
		pb.redirectErrorStream(true);
		
		boolean retry = false;
		final Process process = pb.start();
		// empty output buffers and print to console
		try (final InputStream is = process.getInputStream()) {
			do {
				Thread.sleep(50);
				final String log = IOUtils.toString(is, Charset.defaultCharset());
				if (log.contains("Unhandled Exception: System.IO.IOException: The process cannot access the file")) {
					retry = true;
				} else {
					logger.info(log);
				}
			} while (process.isAlive());
		}
		return retry;
	}
	
	private void writeToMetaFile(final File directory, final String gameName, final int[] version, final boolean isPtr)
			throws IOException {
		final Path path = Paths.get(directory.getAbsolutePath(), META_FILE_NAME);
		final KryoGameInfo metaInfo = new KryoGameInfo(version, gameName, isPtr);
		final List<Object> payload = new ArrayList<>();
		payload.add(metaInfo);
		final Kryo kryo = kryoService.getKryoForBaseUiMetaFile();
		kryoService.put(path, payload, kryo);
	}
	
	/**
	 * Parses the baseUI of the specified game in its own thread. Afterwards, a specified followupTask is executed. The
	 * parsing of the baseUI is synchronized.
	 *
	 * @param game
	 * 		game whose default UI is parsed
	 * @param followupTask
	 */
	public void parseBaseUI(final GameData game, final Runnable followupTask) {
		// create tasks for the worker pool
		app.getExecutor().execute(() -> {
			// lock per game
			synchronized (game.getGameDef().getName()) {
				UICatalog uiCatalog = game.getUiCatalog();
				final String gameName = game.getGameDef().getName();
				if (uiCatalog != null) {
					if (logger.isTraceEnabled()) {
						logger.trace("Aborting parsing baseUI for '" + gameName + "' as was already parsed.");
					}
				} else {
					final long startTime = System.currentTimeMillis();
					logger.info("Loading baseUI for " + gameName);
					boolean needToParseAgain = true;
					final boolean isPtr = !(game.getGameDef() instanceof SC2GameDef) &&
							configService.getIniSettings().isHeroesPtrActive();
					try {
						if (cacheIsUpToDateCheckException(game.getGameDef(), isPtr)) {
							// load from cache
							uiCatalog = discCacheService.getCachedBaseUi(gameName, isPtr).getCatalog();
							game.setUiCatalog(uiCatalog);
							needToParseAgain = false;
							if (logger.isTraceEnabled()) {
								logger.trace("Loaded baseUI for '" + gameName + "' from cache");
							}
						}
					} catch (final IOException e) {
						logger.warn("ERROR: loading cached base UI failed.", e);
					}
					if (needToParseAgain) {
						// parse baseUI
						uiCatalog = new UICatalogImpl();
						app.printInfoLogMessageToGeneral("Starting to parse base " + gameName + " UI.");
						app.addThreadLoggerTab(Thread.currentThread().getName(),
								game.getGameDef().getNameHandle() + "UI", true);
						final String gameDir = configService.getBaseUiPath(game.getGameDef()) + File.separator +
								game.getGameDef().getModsSubDirectory();
						try {
							final WildcardFileFilter fileFilter =
									new WildcardFileFilter("descindex.*layout", IOCase.INSENSITIVE);
							for (final String modOrDir : game.getGameDef().getCoreModsOrDirectories()) {
								
								final File directory = new File(gameDir + File.separator + modOrDir);
								if (!directory.exists() || !directory.isDirectory()) {
									throw new IOException("BaseUI out of date.");
								}
								
								final Collection<File> descIndexFiles =
										FileUtils.listFiles(directory, fileFilter, TrueFileFilter.INSTANCE);
								logger.info("number of descIndexFiles found: " + descIndexFiles.size());
								
								for (final File descIndexFile : descIndexFiles) {
									logger.info("parsing descIndexFile '" + descIndexFile.getPath() + "'");
									uiCatalog.processDescIndex(descIndexFile, game.getGameDef().getDefaultRaceId(),
											game.getGameDef().getDefaultConsoleSkinId());
								}
							}
							game.setUiCatalog(uiCatalog);
						} catch (final SAXException | IOException | ParserConfigurationException e) {
							logger.error("ERROR parsing base UI catalog for '" + gameName + "'.", e);
						} catch (final InterruptedException e) {
							Thread.currentThread().interrupt();
						} finally {
							uiCatalog.clearParser();
						}
						final String msg = "Finished parsing base UI for " + gameName + ".";
						logger.info(msg);
						app.printInfoLogMessageToGeneral(msg);
						try {
							discCacheService.put(uiCatalog, gameName, isPtr, getVersion(game.getGameDef(), isPtr));
						} catch (final IOException e) {
							logger.error("ERROR when creating cache file of UI", e);
						}
					}
					final long executionTime = (System.currentTimeMillis() - startTime);
					logger.info("Loading BaseUI for '" + gameName + "' took " + executionTime + "ms.");
				}
			}
			addTaskToExecutor(followupTask);
		});
	}
	
	public boolean cacheIsUpToDateCheckException(final GameDef gameDef, final boolean usePtr) {
		try {
			return cacheIsUpToDate(gameDef, usePtr);
		} catch (final NoSuchFileException e) {
			logger.trace("No cache exists for " + gameDef.getName());
		} catch (final IOException e) {
			logger.info("Failed to check cache status of " + gameDef.getName() + ":", e);
		}
		return false;
	}
	
	/**
	 * Adds a task to the executor.
	 *
	 * @param followupTask
	 */
	private void addTaskToExecutor(final Runnable followupTask) {
		if (followupTask != null) {
			app.getExecutor().execute(followupTask);
		}
	}
	
	public boolean cacheIsUpToDate(final GameDef gameDef, final boolean usePtr) throws IOException {
		final File baseUiMetaFileDir = new File(configService.getBaseUiPath(gameDef));
		final File cacheFile = discCacheService.getCacheFile(gameDef.getName(), usePtr);
		return cacheIsUpToDate(cacheFile.toPath(), baseUiMetaFileDir);
	}
	
	/**
	 * @param cacheFile
	 * @param metaFileDir
	 * @return
	 */
	public boolean cacheIsUpToDate(final Path cacheFile, final File metaFileDir) throws IOException {
		final KryoGameInfo baseUiInfo = readMetaFile(metaFileDir);
		final KryoGameInfo cacheInfo = discCacheService.getCachedBaseUi(cacheFile).getGameInfo();
		
		final int[] versionCache = cacheInfo.getVersion();
		final int[] versionBaseUi = baseUiInfo.getVersion();
		
		boolean isUpToDate = true;
		for (int i = 0; i < versionCache.length && isUpToDate; ++i) {
			isUpToDate = versionCache[0] == versionBaseUi[0];
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Cache and baseUI versions match: " + isUpToDate);
		}
		return isUpToDate;
	}
	
}
