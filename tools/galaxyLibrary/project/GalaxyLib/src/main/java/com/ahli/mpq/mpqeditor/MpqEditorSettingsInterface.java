package com.ahli.mpq.mpqeditor;

import com.ahli.util.DeepCopyable;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.INIBuilderParameters;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class to manage the settings of Ladik's MpqEditor.
 */
public class MpqEditorSettingsInterface implements DeepCopyable {
	private static final String MPQEDITOR_RULESET_INI = "MPQEditor_Ruleset.ini";
	private static final String CUSTOM_RULE_PROPERTY_KEY = "CustomRules. ";
	private static final String MPQEDITOR_INI = "MPQEditor.ini";
	private static final Logger logger = LogManager.getLogger();
	private static final String APPDATA = "APPDATA";
	private static final String NO_COMPRESSION_CUSTOM_RULE = "0x01000000, 0x00000002, 0xFFFFFFFF";
	private static final String DEFAULT = "Default";
	private final File iniFile;
	private final File rulesetFile;
	private File iniFileBackUp = null;
	private File rulesetFileBackUp = null;
	private boolean backupActive = false;
	private MpqEditorCompressionRule[] customRules = null;
	
	private MpqEditorCompression compression = MpqEditorCompression.BLIZZARD_SC2_HEROES;
	
	public MpqEditorSettingsInterface() {
		iniFile = new File(System.getenv(APPDATA) + File.separator + MPQEDITOR_INI);
		rulesetFile = new File(System.getenv(APPDATA) + File.separator + MPQEDITOR_RULESET_INI);
	}
	
	/**
	 * Restored the original settings files that were backed up.
	 *
	 * @throws IOException
	 */
	public void restoreOriginalSettingFiles() throws IOException {
		if (!backupActive) {
			return;
		}
		
		restoreFileFromBackUp(rulesetFileBackUp, rulesetFile);
		rulesetFileBackUp = null;
		
		restoreFileFromBackUp(iniFileBackUp, iniFile);
		iniFileBackUp = null;
		
		backupActive = false;
	}
	
	/**
	 * Restores a file that was backed up.
	 *
	 * @param backUpFileName
	 * @param originalFileName
	 * @throws IOException
	 */
	private void restoreFileFromBackUp(final File backUpFileName, final File originalFileName) throws IOException {
		if (backUpFileName != null && backUpFileName.exists()) {
			if (originalFileName.exists()) {
				Files.delete(originalFileName.toPath());
			}
			if (!backUpFileName.renameTo(originalFileName)) {
				throw new IOException(
						"Could not restore original via renaming " + backUpFileName.getAbsolutePath() + " to " +
								originalFileName.getName());
			}
		}
	}
	
	/**
	 * Returns the state of file backups.
	 *
	 * @return
	 */
	public boolean isBackupActive() {
		return backupActive;
	}
	
	/**
	 * Returns the currently active compression method.
	 *
	 * @return
	 */
	public MpqEditorCompression getCompression() {
		return compression;
	}
	
	/**
	 * Sets the compression method.
	 *
	 * @param compression
	 */
	public void setCompression(final MpqEditorCompression compression) {
		this.compression = compression;
	}
	
	/**
	 * Applies the compression. Make sure to call <code>restoreOriginalSettingFiles()</code> afterwards to restore these
	 * files.
	 */
	public void applyCompression() throws IOException {
		switch (compression) {
			// custom, blizz & none use custom ruleset
			case CUSTOM:
			case BLIZZARD_SC2_HEROES:
			case NONE:
				backUpOriginalSettingsFiles();
				applyChangesToFiles();
				break;
			
			case SYSTEM_DEFAULT:
				break;
			
			default:
				throw new IOException("unknown compression setting");
		}
	}
	
	/**
	 * Backs up the original settings files
	 *
	 * @throws IOException
	 */
	private void backUpOriginalSettingsFiles() throws IOException {
		if (backupActive) {
			return;
		}
		
		final String directoryPath = iniFile.getParent();
		int i = 0;
		File backupFile;
		
		// ruleset file
		if (rulesetFile.exists()) {
			do {
				backupFile = new File(directoryPath + File.separator + "MPQEditor_Ruleset" + "_" + i + ".tmp");
				i++;
				if (i > 999) {
					throw new IOException("Could not find unique name for MPQEditor_Ruleset.ini's backup copy.");
				}
			} while (backupFile.exists());
			Files.copy(rulesetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			rulesetFileBackUp = backupFile;
		}
		
		// ini file
		if (iniFile.exists()) {
			i = 0;
			do {
				backupFile = new File(directoryPath + File.separator + "MPQEditor" + "_" + i + ".tmp");
				i++;
				if (i > 999) {
					throw new IOException("Could not find unique name for MPQEditor.ini's backup copy.");
				}
			} while (backupFile.exists());
			Files.copy(iniFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			iniFileBackUp = backupFile;
		}
		
		backupActive = true;
	}
	
	/**
	 * Applies the necessary changes to the ini files.
	 */
	private void applyChangesToFiles() throws IOException {
		if (!iniFile.exists()) {
			logger.error(
					"MpqEditor's ini file does not exist. It would be located at '" + iniFile.getAbsolutePath() + "'" +
							". The editor will run with its factory settings.");
			return;
		}
		int gameId = 6;
		try {
			final INIBuilderParameters params = new Parameters().ini().setFile(iniFile).setEncoding("UTF-8");
			final FileBasedConfigurationBuilder<INIConfiguration> b =
					new FileBasedConfigurationBuilder<>(INIConfiguration.class).configure(params);
			final INIConfiguration ini = b.getConfiguration();
			
			final SubnodeConfiguration options = ini.getSection("Options");
			gameId = getGameIdPropertyValue(compression);
			options.setProperty("GameId", gameId);
			b.save();
		} catch (final ConfigurationException e) {
			logger.error("Error while applying custom ruleset usage entry.", e);
		}
		
		if (gameId == 13) {
			writeMpqRuleset();
		}
	}
	
	/**
	 * @param compression
	 * @return
	 */
	private int getGameIdPropertyValue(final MpqEditorCompression compression) {
		switch (compression) {
			case BLIZZARD_SC2_HEROES:
				return 11;
			case NONE:
			case CUSTOM:
			case SYSTEM_DEFAULT:
			default:
				return 13;
		}
	}
	
	/**
	 *
	 */
	private void writeMpqRuleset() throws IOException {
		INIConfiguration ini;
		try {
			final INIBuilderParameters params = new Parameters().ini().setFile(rulesetFile).setEncoding("UTF-8");
			final FileBasedConfigurationBuilder<INIConfiguration> b =
					new FileBasedConfigurationBuilder<>(INIConfiguration.class).configure(params);
			ini = b.getConfiguration();
			ini.clear();
		} catch (final ConfigurationException e) {
			logger.error("Error while editing custom ruleset file.", e);
			ini = new INIConfiguration();
		}
		final SubnodeConfiguration section = ini.getSection("CustomRules");
		section.setProperty("MpqVersion", 3);
		section.setProperty("AttrFlags", 5);
		section.setProperty("SectorSize", 16384);
		section.setProperty("RawChunkSize", 16384);
		
		switch (compression) {
			case CUSTOM:
				if (customRules != null) {
					for (int i = 0, len = customRules.length; i < len; i++) {
						ini.addProperty(CUSTOM_RULE_PROPERTY_KEY, customRules[i].toString());
					}
				} else {
					section.addProperty(DEFAULT, NO_COMPRESSION_CUSTOM_RULE);
				}
				break;
			case NONE:
				section.addProperty(DEFAULT, NO_COMPRESSION_CUSTOM_RULE);
				break;
			case SYSTEM_DEFAULT:
			case BLIZZARD_SC2_HEROES:
			default:
				break;
		}
		
		try (final FileWriter fw = new FileWriter(rulesetFile); final BufferedWriter bw = new BufferedWriter(fw)) {
			ini.write(bw);
		} catch (final ConfigurationException | IOException e) {
			throw new IOException("Could not write '" + rulesetFile.getAbsolutePath() + "'.", e);
		}
		
		// remove custom ruleset line beginnings
		if (compression == MpqEditorCompression.CUSTOM) {
			final List<String> editedLines;
			try (final Stream<String> lineStream = Files.lines(rulesetFile.toPath())) {
				editedLines = lineStream.map(line -> line.replace("  = ", "")).collect(Collectors.toList());
			}
			try (final FileWriter fw = new FileWriter(rulesetFile); final BufferedWriter bw = new BufferedWriter(fw)) {
				Files.write(rulesetFile.toPath(), editedLines);
			}
		}
	}
	
	public MpqEditorCompressionRule[] getCustomRuleSet() {
		return customRules;
	}
	
	/**
	 * @param customRules
	 */
	public void setCustomRules(final MpqEditorCompressionRule[] customRules) {
		this.customRules = customRules;
	}
	
	@Override
	public Object deepCopy() {
		final MpqEditorSettingsInterface clone = new MpqEditorSettingsInterface();
		if (customRules != null) {
			clone.customRules = new MpqEditorCompressionRule[customRules.length];
			for (int i = 0, len = customRules.length; i < len; i++) {
				customRules[i] = customRules[i] == null ? null : (MpqEditorCompressionRule) customRules[i].deepCopy();
			}
		}
		clone.compression = compression;
		clone.iniFileBackUp = iniFileBackUp;
		clone.rulesetFileBackUp = rulesetFileBackUp;
		clone.backupActive = backupActive;
		return clone;
	}
}