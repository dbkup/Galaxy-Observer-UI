package com.ahli.mpq;

import com.ahli.mpq.i18n.Messages;
import com.ahli.mpq.mpqeditor.MpqEditorCompression;
import com.ahli.mpq.mpqeditor.MpqEditorSettingsInterface;
import com.ahli.util.DeepCopyable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Implements a MpqInterface for Ladislav Zezula's MpqEditor.exe.
 *
 * @author Ahli
 */
public class MpqEditorInterface implements MpqInterface, DeepCopyable {
	private final static char q = '\"';
	private static final Logger logger = LogManager.getLogger();
	private final MpqEditorSettingsInterface settings;
	private String mpqEditor = "plugins" + File.separator + "mpq" + File.separator + "MPQEditor.exe";
	//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private String mpqCachePath;
	
	/**
	 * Constructor.
	 *
	 * @param mpqCachePath2
	 * 		path of the directory that will temporarily contain the extracted mpq
	 */
	public MpqEditorInterface(final String mpqCachePath2) {
		mpqCachePath = mpqCachePath2;
		settings = new MpqEditorSettingsInterface();
	}
	
	/**
	 * Returns the file count in a folder including all subfolders.
	 *
	 * @param file
	 * @return
	 */
	public static int getFileCountInFolder(final File file) {
		final File[] files = file.listFiles();
		int count = 0;
		if (files != null) {
			for (final File f : files) {
				if (f.isDirectory()) {
					count += getFileCountInFolder(f);
				} else {
					count++;
				}
			}
		}
		return count;
	}
	
	/**
	 * @param f
	 * @return
	 */
	public static boolean deleteDir(final File f) {
		if (f.isDirectory()) {
			final File[] content = f.listFiles();
			if (content != null) {
				for (int i = 0; i < content.length; i++) {
					if (!deleteDir(content[i])) {
						return false;
					}
				}
			}
		}
		final boolean result = f.delete();
		if (!result) {
			logger.error("Deleting file/folder " + f.getPath() + " failed."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}
	
	/**
	 * Returns the cache path as String.
	 *
	 * @return cache path as String
	 */
	public String getMpqCachePath() {
		return mpqCachePath;
	}
	
	/**
	 * Sets the cache path.
	 *
	 * @param mpqCachePath2
	 * 		new cache path as String
	 */
	public void setMpqCachePath(final String mpqCachePath2) {
		mpqCachePath = mpqCachePath2;
	}
	
	/**
	 * Returns a cloned instance of this.
	 */
	@Override
	public Object deepCopy() {
		final MpqEditorInterface clone = new MpqEditorInterface(mpqCachePath);
		clone.setMpqEditorPath(mpqEditor);
		return clone;
	}
	
	/**
	 * Sets the MpqEditor path.
	 *
	 * @param editorPath
	 * 		new editor path as String
	 */
	public void setMpqEditorPath(final String editorPath) {
		mpqEditor = editorPath;
	}
	
	/**
	 * Build MPQ from cache.
	 *
	 * @param buildPath
	 * @param buildFileName
	 * @param compressXml
	 * @param compressMpq
	 * @param buildUnprotectedToo
	 * 		if protectMPQ, then this controls if an unprotected version is build, too
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws MpqException
	 */
	public void buildMpq(final String buildPath, final String buildFileName, final boolean compressXml,
			final MpqEditorCompression compressMpq, final boolean buildUnprotectedToo)
			throws IOException, InterruptedException, MpqException {
		final String absolutePath = buildPath + File.separator + buildFileName;
		buildMpq(absolutePath, compressXml, compressMpq, buildUnprotectedToo);
	}
	
	/**
	 * Build MPQ from cache.
	 *
	 * @param absolutePath
	 * @param compressXml
	 * @param compressMpq
	 * @param buildBoth
	 * 		if compressXml, then this controls if an unprotected version is build, too
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws MpqException
	 */
	public void buildMpq(final String absolutePath, final boolean compressXml, final MpqEditorCompression compressMpq,
			final boolean buildBoth) throws IOException, InterruptedException, MpqException {
		// add 2 to be sure to have enough space for listfile and attributes
		final int fileCount = 2 + getFileCountInFolder(new File(mpqCachePath));
		
		// create parent directory
		final File targetFile = new File(absolutePath);
		final File parentFolder = targetFile.getParentFile();
		if (!parentFolder.exists() && !parentFolder.mkdirs()) {
			final String msg = "ERROR: Could not create path " + parentFolder.getAbsolutePath(); //$NON-NLS-1$
			logger.error(msg);
			throw new MpqException(String.format(Messages.getString("MpqInterface.CouldNotCreatePath"), //$NON-NLS-1$
					parentFolder.getAbsolutePath()));
		}
		
		if (compressXml) {
			
			if (buildBoth) {
				// special unprotected file path
				final String unprotectedAbsolutePath = getPathWithSuffix(absolutePath, "_unprtctd"); //$NON-NLS-1$
				
				// make way for unprotected file
				final File fup = new File(unprotectedAbsolutePath);
				if (fup.exists() && fup.isFile()) {
					if (!fup.delete()) {
						final String msg = "ERROR: Could not delete file " + unprotectedAbsolutePath; //$NON-NLS-1$
						logger.error(msg);
						throw new MpqException(String.format(Messages.getString("MpqInterface.CouldNotOverwriteFile"),
								absolutePath)); //$NON-NLS-1$
					}
				}
				
				// build unprotected file
				newMpq(unprotectedAbsolutePath, fileCount);
				addToMpq(unprotectedAbsolutePath, mpqCachePath, ""); //$NON-NLS-1$
				compactMpq(unprotectedAbsolutePath);
			}
			
			// make way for protected file
			final File f = new File(absolutePath);
			if (f.exists() && f.isFile()) {
				if (!f.delete()) {
					final String msg = "ERROR: Could not delete file " + absolutePath; //$NON-NLS-1$
					logger.error(msg);
					throw new MpqException(String.format(Messages.getString("MpqInterface.CouldNotOverwriteFile"),
							absolutePath)); //$NON-NLS-1$
				}
			}
			
			////////////////////////
			// PROTECTION MEASURES
			////////////////////////
			// doesn't work
			// deleteListFile(absolutePathTMP);
			// doesn't work
			// renameFileInMpq(absolutePathTMP, "_Ahli.StormLayout",
			//////////////////////// "_QQQ.StormLayout");
			// renameFileInMpq(absolutePathTMP, "(listfile)", "QQQ");
			// doesn't work
			// writeWrongFileAmount(absolutePathTMP, absolutePath);
			
			// extra compression
			try {
				XmlCompressor.processCache(mpqCachePath, 1);
			} catch (ParserConfigurationException | SAXException e) {
				e.printStackTrace();
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			
			// build protected file
			newMpq(absolutePath, fileCount);
			addToMpq(absolutePath, mpqCachePath, ""); //$NON-NLS-1$
			compactMpq(absolutePath);
			
		} else {
			// NO PROTECTION OPTION
			
			// make way for file
			if (targetFile.exists() && targetFile.isFile()) {
				if (!targetFile.delete()) {
					final String msg = "ERROR: Could not delete file " + absolutePath; //$NON-NLS-1$
					logger.error(msg);
					throw new MpqException(String.format(Messages.getString("MpqInterface.CouldNotOverwriteFile"),
							absolutePath)); //$NON-NLS-1$
				}
			}
			
			// build unprotected file
			newMpq(absolutePath, fileCount);
			addToMpq(absolutePath, mpqCachePath, ""); //$NON-NLS-1$
			compactMpq(absolutePath);
			
		}
		
	}
	
	/**
	 * Returns path with suffix via changing file name.
	 *
	 * @param absolutePath
	 * @return
	 */
	private String getPathWithSuffix(final String absolutePath, final String suffix) {
		final int i = absolutePath.lastIndexOf('.');
		return absolutePath.substring(0, i) + suffix + absolutePath.substring(i);
	}
	
	/**
	 * @param mpqSourcePath
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws MpqException
	 */
	public void extractEntireMPQ(final String mpqSourcePath) throws InterruptedException, IOException, MpqException {
		logger.trace("mpqCachePath: {}", () -> mpqCachePath); //$NON-NLS-1$
		logger.trace("mpqSourcePath: {}", () -> mpqSourcePath); //$NON-NLS-1$
		
		clearCacheExtractedMpq();
		
		extractFromMpq(mpqSourcePath, "*", mpqCachePath, true); //$NON-NLS-1$
		
		clearCacheListFile();
		clearCacheAttributesFile();
		
		if (getFileCountInFolder(new File(mpqCachePath)) <= 0) {
			throw new MpqException(
					String.format(Messages.getString("MpqInterface.NoFilesExtracted"), mpqSourcePath)); //$NON-NLS-1$
		}
	}
	
	/**
	 * Removes the "(listfile)" file from the cache.
	 *
	 * @return whether the cache's list file was deleted
	 */
	private boolean clearCacheListFile() {
		final File f = new File(mpqCachePath + File.separator + "(listfile)"); //$NON-NLS-1$
		return !f.exists() || f.isDirectory() || f.delete();
	}
	
	/**
	 * Removes the "(attributes)" file from the cache.
	 *
	 * @return whether the cache's attribute file was deleted
	 */
	private boolean clearCacheAttributesFile() {
		final File f = new File(mpqCachePath + File.separator + "(attributes)"); //$NON-NLS-1$
		return !f.exists() || f.isDirectory() || f.delete();
	}
	
	/**
	 * Removes all files in the cache.
	 */
	public boolean clearCacheExtractedMpq() {
		final File f = new File(mpqCachePath);
		if (f.exists()) {
			if (deleteDir(f)) {
				if (logger.isTraceEnabled()) {
					logger.trace("clearing Cache succeeded"); //$NON-NLS-1$
				}
				return true;
			} else {
				if (logger.isTraceEnabled()) {
					logger.error("clearing Cache FAILED"); //$NON-NLS-1$
				}
				return false;
			}
		} else {
			return true;
		}
	}
	
	/**
	 * @param mpqPath
	 * @param maxFileCount
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws MpqException
	 */
	public void newMpq(final String mpqPath, final int maxFileCount)
			throws InterruptedException, IOException, MpqException {
		if (!verifyMpqEditor()) {
			throw new MpqException(
					String.format(Messages.getString("MpqInterface.MPQEditorNotFound"), mpqEditor)); //$NON-NLS-1$
		}
		
		final String cmd = "cmd /C " + q + q + mpqEditor + q + " n " + q + mpqPath + q + " " + maxFileCount +
				q; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		logger.trace("executing: {}", () -> cmd); //$NON-NLS-1$
		Runtime.getRuntime().exec(cmd).waitFor();
		if (logger.isTraceEnabled()) {
			logger.trace("execution finished"); //$NON-NLS-1$
		}
	}
	
	/**
	 * @param mpqPath
	 * @param sourceFilePath
	 * @param targetName
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws MpqException
	 */
	public void addToMpq(final String mpqPath, final String sourceFilePath, final String targetName)
			throws InterruptedException, IOException, MpqException {
		if (!verifyMpqEditor()) {
			throw new MpqException(
					String.format(Messages.getString("MpqInterface.MPQEditorNotFound"), mpqEditor)); //$NON-NLS-1$
		}
		final String cmd = "cmd /C " + q + q + mpqEditor + q + " a " + q + mpqPath + q + " " + q + sourceFilePath + q
				//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ " " //$NON-NLS-1$
				+ q + targetName + q + " /r" + q; //$NON-NLS-1$
		logger.trace("executing: {}", () -> cmd); //$NON-NLS-1$
		Runtime.getRuntime().exec(cmd).waitFor();
		if (logger.isTraceEnabled()) {
			logger.trace("execution finished"); //$NON-NLS-1$
		}
	}
	
	/**
	 * @param mpqPath
	 * @param fileName
	 * @param targetPath
	 * @param inclSubFolders
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws MpqException
	 */
	public void extractFromMpq(final String mpqPath, final String fileName, final String targetPath,
			final boolean inclSubFolders) throws InterruptedException, IOException, MpqException {
		if (!verifyMpqEditor()) {
			throw new MpqException(
					String.format(Messages.getString("MpqInterface.MPQEditorNotFound"), mpqEditor)); //$NON-NLS-1$
		}
		final String cmd = "cmd /C " + q + q + mpqEditor + q + " e " + q + mpqPath + q + " " + q + fileName + q + " "
				//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ q + targetPath + q + (inclSubFolders ? " /fp" : "") + q; //$NON-NLS-1$ //$NON-NLS-2$
		logger.trace("executing: {}", () -> cmd); //$NON-NLS-1$
		Runtime.getRuntime().exec(cmd).waitFor();
		if (logger.isTraceEnabled()) {
			logger.trace("execution finished"); //$NON-NLS-1$
		}
		
		// // MONITOR https://github.com/inwc3/JMPQ3 if it can handle sc2 files
		// someday to potentially replace MpqEditor.exe
		// File mpq = new File(mpqPath);
		// logger.trace("Extract from Mpq: "+mpq.getAbsolutePath());
		// JMpqEditor editor = new JMpqEditor(mpq, MPQOpenOption.FORCE_V0);
		// File cache = new File(mpqCachePath);
		// cache.mkdirs();
		// logger.trace("Extract into: "+cache.getAbsolutePath());
		// editor.extractAllFiles(cache);
	}
	
	/**
	 * @param mpqPath
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws MpqException
	 */
	public void compactMpq(final String mpqPath) throws InterruptedException, IOException, MpqException {
		if (!verifyMpqEditor()) {
			throw new MpqException(
					String.format(Messages.getString("MpqInterface.MPQEditorNotFound"), mpqEditor)); //$NON-NLS-1$
		}
		final String cmd =
				"cmd /C " + q + q + mpqEditor + q + " compact " + q + mpqPath + q + q; //$NON-NLS-1$ //$NON-NLS-2$
		logger.trace("executing: {}", () -> cmd); //$NON-NLS-1$
		Runtime.getRuntime().exec(cmd).waitFor();
		if (logger.isTraceEnabled()) {
			logger.trace("execution finished"); //$NON-NLS-1$
		}
		
	}
	
	/**
	 * @param mpqPath
	 * @param scriptPath
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws MpqException
	 */
	public void scriptMpq(final String mpqPath, final String scriptPath)
			throws InterruptedException, IOException, MpqException {
		if (!verifyMpqEditor()) {
			throw new MpqException(
					String.format(Messages.getString("MpqInterface.MPQEditorNotFound"), mpqEditor)); //$NON-NLS-1$
		}
		final String cmd = "cmd /C " + q + q + mpqEditor + q + " s " + q + mpqPath + q + " " + q + scriptPath + q +
				q; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.trace("executing: {}", () -> cmd); //$NON-NLS-1$
		Runtime.getRuntime().exec(cmd).waitFor();
		if (logger.isTraceEnabled()) {
			logger.trace("execution finished"); //$NON-NLS-1$
		}
	}
	
	/**
	 * Verifies the existence of the MPQEditor.exe at the stored file path.
	 *
	 * @return
	 */
	private boolean verifyMpqEditor() {
		final File f = new File(mpqEditor);
		return f.exists() && f.isFile();
	}
	
	/**
	 * @param mpqPath
	 * @param filePath
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws MpqException
	 */
	public void deleteFileInMpq(final String mpqPath, final String filePath)
			throws InterruptedException, IOException, MpqException {
		if (!verifyMpqEditor()) {
			throw new MpqException(
					String.format(Messages.getString("MpqInterface.MPQEditorNotFound"), mpqEditor)); //$NON-NLS-1$
		}
		final String cmd = "cmd /C " + q + q + mpqEditor + q + " d " + q + mpqPath + q + " " + q + filePath + q +
				q; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.trace("executing: {}", () -> cmd); //$NON-NLS-1$
		Runtime.getRuntime().exec(cmd).waitFor();
		if (logger.isTraceEnabled()) {
			logger.trace("execution finished"); //$NON-NLS-1$
		}
	}
	
	/**
	 * @param mpqPath
	 * @param oldfilePath
	 * @param newFilePath
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws MpqException
	 */
	public void renameFileInMpq(final String mpqPath, final String oldfilePath, final String newFilePath)
			throws InterruptedException, IOException, MpqException {
		if (!verifyMpqEditor()) {
			throw new MpqException(
					String.format(Messages.getString("MpqInterface.MPQEditorNotFound"), mpqEditor)); //$NON-NLS-1$
		}
		final String cmd = "cmd /C " + q + q + mpqEditor + q + " r " + q + mpqPath + q + " " + q + oldfilePath + q
				//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ " " + q //$NON-NLS-1$
				+ newFilePath + q + q;
		logger.trace("executing: {}", () -> cmd); //$NON-NLS-1$
		Runtime.getRuntime().exec(cmd).waitFor();
		if (logger.isTraceEnabled()) {
			logger.trace("execution finished"); //$NON-NLS-1$
		}
	}
	
	/**
	 * Returns a file from the cache with the specified internal path.
	 *
	 * @param intPath
	 * @return
	 */
	@Override
	public File getFileFromMpq(final String intPath) {
		return new File(mpqCachePath + "//" + intPath); //$NON-NLS-1$
	}
	
	/**
	 * Returns the componentList file.
	 *
	 * @return
	 */
	public File getComponentListFile() {
		File f = new File(mpqCachePath + File.separator + "ComponentList.StormComponents"); //$NON-NLS-1$
		if (!f.exists() || f.isDirectory()) {
			f = new File(mpqCachePath + File.separator + "ComponentList.SC2Components"); //$NON-NLS-1$
			if (!f.exists() || f.isDirectory()) {
				return null;
			}
		}
		return f;
	}
	
	/**
	 *
	 */
	@Override
	public boolean isHeroesMpq() throws MpqException {
		File f = new File(mpqCachePath + File.separator + "ComponentList.StormComponents"); //$NON-NLS-1$
		if (!f.exists() || f.isDirectory()) {
			logger.trace("file not found in archive: " + f.getAbsolutePath()); //$NON-NLS-1$
			f = new File(mpqCachePath + File.separator + "ComponentList.SC2Components"); //$NON-NLS-1$
			if (!f.exists() || f.isDirectory()) {
				logger.error("ERROR: archive has no ComponentList file."); //$NON-NLS-1$
				throw new MpqException("ERROR: Cannot identify if file belongs to Heroes or SC2."); //$NON-NLS-1$
			}
			return false;
		}
		return true;
	}
	
	@Override
	public File getCache() {
		return new File(mpqCachePath);
	}
	
	@Override
	public void setCache(final File cache) {
		mpqCachePath = cache.getPath();
	}
	
	/**
	 * @throws IOException
	 */
	public void backUpSettings() throws IOException {
		settings.saveOriginalSettingFiles();
	}
	
	/**
	 *
	 */
	public void restoreSettings() throws IOException {
		settings.restoreOriginalSettingFiles();
	}
	
	public void applySetting(final int saveMode) {
		settings.applySettings(saveMode);
	}
	
}
