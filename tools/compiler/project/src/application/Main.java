package application;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.JFileChooser;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

/**
 * 
 * @author Ahli
 *
 */
public class Main extends Application {
	private MpqInterface mpqi;
	private DescIndexData descIndex = new DescIndexData(this);
	private boolean namespaceHeroes = true;
	private String documentsPath = new JFileChooser().getFileSystemView().getDefaultDirectory().toString();
	private File basePath = null;
	public final static String errorLine = "#########################################";

	private TextArea txtArea = null;

	private boolean encounteredError = false;

	@Override
	/**
	 * Called when the App is initializing.
	 */
	public void start(Stage primaryStage) {
		try {
			clearErrorEncounter();
			
			BorderPane root = new BorderPane();
			primaryStage.setTitle("Compiling Interfaces...");

			txtArea = new TextArea();
			txtArea.setText("Initializing App...");

			root.setCenter(txtArea);
			Scene scene = new Scene(root, 1200, 400);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();

			basePath = null;
			// basePath = new
			// File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			basePath = getJarDir(Main.class);
			System.out.println("basePath: " + basePath);
			System.out.println("documentsPath: " + documentsPath);
			addLogMessage("basePath: " + basePath);
			addLogMessage("documentsPath: " + documentsPath);
			mpqi = new MpqInterface();
			initMpqInterface(mpqi);

			File dir = new File(basePath.getAbsolutePath() + File.separator + "heroes");
			namespaceHeroes = true;
			if (dir.exists() && dir.isDirectory()) {

				File[] directoryListing = dir.listFiles();
				if (directoryListing != null) {
					for (File child : directoryListing) {
						if (child.isDirectory()) {
							buildFile(child, true);
						}
					}
				}
			}
			dir = new File(basePath.getAbsolutePath() + File.separator + "sc2");
			namespaceHeroes = false;
			if (dir.exists() && dir.isDirectory()) {

				File[] directoryListing = dir.listFiles();
				if (directoryListing != null) {
					for (File child : directoryListing) {
						if (child.isDirectory()) {
							buildFile(child, false);
						}
					}
				}
			}

			primaryStage.setTitle("Compiling Interfaces... done.");
			addLogMessage("All done.");
			
			
			if(!hasEncounteredError() && !primaryStage.isFocused()){
				// close after 5 seconds
				PauseTransition delay = new PauseTransition(Duration.seconds(5));
				delay.setOnFinished(event -> primaryStage.close());
				delay.play();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Entry point of the App
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Add a message to the message log.
	 * 
	 * @param msg
	 */
	public void addLogMessage(String msg) {
		txtArea.setText(txtArea.getText() + "\n" + msg);
	}

	/**
	 * Builds MPQ Archive File.
	 * @param file
	 * @param isHeroes
	 */
	private void buildFile(File file, boolean isHeroes) {

		String buildPath = documentsPath + File.separator;
		if (isHeroes) {
			buildPath += "Heroes of the Storm";
		} else {
			buildPath += "StarCraft II";
		}
		buildPath += File.separator + "Interfaces";

		// file.getAbsolutePath();

		// get and create cache
		File cache = new File(mpqi.getMpqCachePath());
		cache.mkdirs();
		mpqi.clearCacheExtractedMpq();

		// put files into cache
		try {
			copyFolder(file, cache);
		} catch (IOException e) {
			e.printStackTrace();
			reportErrorEncounter();
			addLogMessage(errorLine);
			addLogMessage("ERROR unable to copy directory:\n    " + e + "\n    " + e.getMessage() + "\n    "
					+ e.getLocalizedMessage() + "\n    " + e.getCause());
			addLogMessage(errorLine);
		}

		// do stuff
		System.out.println("retrieving componentList");
		File componentListFile = mpqi.getComponentListFile();
		System.out.println("retrieving descIndex - set path and clear");
		try {
			descIndex.setDescIndexPathAndClear(ComponentsListReader.getDescIndexPath(componentListFile));
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			reportErrorEncounter();
			addLogMessage(errorLine);
			addLogMessage("ERROR unable to read DescIndex path:\n    " + e + "\n    " + e.getMessage() + "\n    "
					+ e.getLocalizedMessage() + "\n    " + e.getCause());
			addLogMessage(errorLine);
		}

		System.out.println("retrieving descIndex - get cached file");
		File descIndexFile = mpqi.getCachedFile(descIndex.getDescIndexIntPath());
		System.out.println("adding layouts from descIndexFile: " + descIndexFile.getAbsolutePath());
		try {
			descIndex.addLayoutIntPath(DescIndexReader.getLayoutPathList(descIndexFile));
		} catch (SAXException | ParserConfigurationException | IOException e) {
			e.printStackTrace();
			reportErrorEncounter();
			addLogMessage(errorLine);
			addLogMessage("ERROR unable to read Layout paths:\n    " + e + "\n    " + e.getMessage() + "\n    "
					+ e.getLocalizedMessage() + "\n    " + e.getCause());
			addLogMessage(errorLine);
		}

		addLogMessage("Compiling... " + file.getName());

		// perform checks/improvements on code
		compile();

		addLogMessage("Building... " + file.getName());

		try {
			boolean protectMPQ = isHeroes ? true : false;
			mpqi.buildMpq(buildPath, file.getName(), protectMPQ);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			reportErrorEncounter();
			addLogMessage(errorLine);
			addLogMessage("ERROR unable to construct final Interface file:\n    " + e + "\n    " + e.getMessage()
					+ "\n    " + e.getLocalizedMessage() + "\n    " + e.getCause());
			addLogMessage(errorLine);
		}
	}

	/**
	 * Returns the Mpq Interface.
	 * 
	 * @return
	 */
	public MpqInterface getMpqInterface() {
		return mpqi;
	}

	/**
	 * Is called when the App is closing.
	 */
	public void stop() {
		mpqi.clearCacheExtractedMpq();
	}

	/**
	 * Initializes the MPQ Interface.
	 * 
	 * @param mpqi
	 */
	private void initMpqInterface(MpqInterface mpqi) {

		mpqi.setMpqEditorPath(basePath.getParent() + File.separator + "tools" + File.separator + "plugins"
				+ File.separator + "mpq" + File.separator + "MPQEditor.exe");

	}

	/**
	 * Returns true, if it belongs to Heroes of the Storm, false otherwise.
	 * 
	 * @return
	 */
	public boolean isHeroesFile() {
		return namespaceHeroes;
	}

	/**
	 * Returns the DescIndex data of the opened document.
	 * 
	 * @return
	 */
	public DescIndexData getDescIndexData() {
		return descIndex;
	}

	/**
	 * Compiles and updates the data in the cache.
	 */
	public void compile() {
		try {
			// manage order of layout files in DescIndex
			descIndex.orderLayoutFiles();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			addLogMessage(errorLine);
			reportErrorEncounter();
			addLogMessage("ERROR compiling:\n    " + e + "\n    " + e.getMessage() + "\n    " + e.getLocalizedMessage()
					+ "\n    " + e.getCause());
			addLogMessage(errorLine);
		}
		descIndex.persistDescIndexFile();
	}

	/**
	 * Copies a folder.
	 * 
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	private static void copyFolder(File source, File target) throws IOException {
		if (source.isDirectory()) {
			// create folder if not existing
			if (!target.exists()) {
				target.mkdir();
			}

			// copy all contained files recursively
			for (String file : source.list()) {
				File srcFile = new File(source, file);
				File destFile = new File(target, file);
				// Recursive function call
				copyFolder(srcFile, destFile);
			}
		} else {
			// copy the file
			Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * from stackoverflow because why doesn't java have this functionality? It's
	 * not like nobody would need that or it is trivial to create...
	 * 
	 * @param aclass
	 * @return
	 */
	public static File getJarDir(Class<Main> aclass) {
		URL url;
		String extURL; // url.toExternalForm();

		// get an url
		try {
			url = aclass.getProtectionDomain().getCodeSource().getLocation();
			// url is in one of two forms
			// ./build/classes/ NetBeans test
			// jardir/JarName.jar froma jar
		} catch (SecurityException ex) {
			url = aclass.getResource(aclass.getSimpleName() + ".class");
			// url is in one of two forms, both ending
			// "/com/physpics/tools/ui/PropNode.class"
			// file:/U:/Fred/java/Tools/UI/build/classes
			// jar:file:/U:/Fred/java/Tools/UI/dist/UI.jar!
		}

		// convert to external form
		extURL = url.toExternalForm();

		// prune for various cases
		if (extURL.endsWith(".jar")) // from getCodeSource
			extURL = extURL.substring(0, extURL.lastIndexOf("/"));
		else { // from getResource
			String suffix = "/" + (aclass.getName()).replace(".", "/") + ".class";
			extURL = extURL.replace(suffix, "");
			if (extURL.startsWith("jar:") && extURL.endsWith(".jar!"))
				extURL = extURL.substring(4, extURL.lastIndexOf("/"));
		}

		// convert back to url
		try {
			url = new URL(extURL);
		} catch (MalformedURLException mux) {
			// leave url unchanged; probably does not happen
		}

		// convert url to File
		try {
			return new File(url.toURI());
		} catch (URISyntaxException ex) {
			return new File(url.getPath());
		}
	}

	public boolean hasEncounteredError() {
		return encounteredError;
	}

	public void reportErrorEncounter() {
		this.encounteredError = true;
	}

	private void clearErrorEncounter() {
		this.encounteredError = false;
	}
}