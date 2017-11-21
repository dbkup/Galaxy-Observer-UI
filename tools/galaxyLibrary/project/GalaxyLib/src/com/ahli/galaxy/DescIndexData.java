package com.ahli.galaxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import com.ahli.mpq.MpqException;
import com.ahli.mpq.MpqInterface;

import javafx.util.Pair;

/**
 * Stores the Data of a Desc Index File.
 * 
 * @author Ahli
 *
 */
public class DescIndexData {
	static Logger LOGGER = LogManager.getLogger(DescIndexData.class);
	
	private String descIndexIntPath = null;
	private ArrayList<Pair<File, String>> fileIntPathList = new ArrayList<>();
	private MpqInterface mpqi;
	
	/**
	 * 
	 * @param mpqi
	 */
	public DescIndexData(MpqInterface mpqi) {
		this.mpqi = mpqi;
		fileIntPathList = new ArrayList<>();
	}
	
	/**
	 * 
	 * @return
	 */
	public String getDescIndexIntPath() {
		return descIndexIntPath;
	}
	
	/**
	 * 
	 * @param descIndexIntPath
	 */
	public void setDescIndexIntPath(String descIndexIntPath) {
		this.descIndexIntPath = descIndexIntPath;
	}
	
	/**
	 * 
	 * @param i
	 * @return
	 */
	public String getLayoutIntPath(int i) {
		return fileIntPathList.get(i).getValue();
	}
	
	/**
	 * 
	 * @param intPath
	 * @throws MpqException
	 */
	public void addLayoutIntPath(String intPath) throws MpqException {
		String intPath2 = intPath;
		File f = mpqi.getFileFromMpq(intPath);
		if (!f.exists()) {
			// add base folder to the path
			intPath2 = (mpqi.isHeroesMpq() ? "Base.StormData" : "Base.SC2Data") + "/" + intPath;
			f = mpqi.getFileFromMpq(intPath2);
		}
		if (!f.exists()) {
			return;
		}
		Pair<File, String> p = new Pair<>(f, intPath2);
		fileIntPathList.add(p);
		LOGGER.debug("added Layout path: " + intPath2);
		LOGGER.debug("added File path: " + f.getAbsolutePath());
	}
	
	/**
	 * 
	 * @param intPath
	 * @return
	 */
	public boolean removeLayoutIntPath(String intPath) {
		for (int i = 0; i < fileIntPathList.size(); i++) {
			Pair<File, String> p = fileIntPathList.get(i);
			if (p.getValue().equals(intPath)) {
				fileIntPathList.remove(i);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 */
	public void clear() {
		fileIntPathList.clear();
	}
	
	/**
	 * 
	 * @param descIndexPath
	 */
	public void setDescIndexPathAndClear(String descIndexPath) {
		clear();
		setDescIndexIntPath(descIndexPath);
	}
	
	/**
	 * 
	 * @param layoutPathList
	 * @throws MpqException
	 */
	public void addLayoutIntPath(Iterable<String> layoutPathList) throws MpqException {
		Iterator<String> it = layoutPathList.iterator();
		while (it.hasNext()) {
			this.addLayoutIntPath(it.next());
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public int getLayoutCount() {
		return fileIntPathList.size();
	}
	
	/**
	 * 
	 * @param intPath
	 * @return
	 */
	public File getLayoutFile(String intPath) {
		for (int i = 0; i < fileIntPathList.size(); i++) {
			Pair<File, String> p = fileIntPathList.get(i);
			if (p.getValue().equals(intPath)) {
				return p.getKey();
			}
		}
		return null;
	}
	
	/**
	 * @throws IOException
	 * 
	 */
	public void persistDescIndexFile() throws IOException {
		File f = mpqi.getFileFromMpq(descIndexIntPath);
		OutputStreamWriter bw = null;
		try {
			bw = new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8");
			bw.write("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\r\n<Desc>\r\n");
			
			for (Pair<File, String> p : fileIntPathList) {
				bw.write("    <Include path=\"");
				bw.write(p.getValue());
				bw.write("\"/>\r\n");
			}
			
			bw.write("</Desc>\r\n");
		} catch (IOException e) {
			LOGGER.error("ERROR writing DescIndex to disc" + e);
			throw e;
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Orders layout files in DescIndex
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public void orderLayoutFiles() throws ParserConfigurationException, SAXException, IOException {
		ArrayList<ArrayList<String>> dependencies = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> ownConstants = new ArrayList<ArrayList<String>>();
		
		// grab dependencies and constant definitions for every layout file
		for (Pair<File, String> pair : fileIntPathList) {
			ArrayList<String> layoutDeps = null;
			ArrayList<String> curConstants = null;
			
			curConstants = LayoutReader.getLayoutsConstantDefinitions(pair.getKey());
			
			// add calculated list of dependencies from layout file
			layoutDeps = LayoutReader.getDependencyLayouts(pair.getKey(), curConstants);
			LOGGER.debug("Dependencies found: " + layoutDeps);
			
			ownConstants.add(curConstants);
			dependencies.add(layoutDeps);
		}
		
		boolean insertOccurred = true;
		for (int counter = 0; insertOccurred && counter < Math.pow(fileIntPathList.size(), 4); counter++) {
			LOGGER.trace("counter=" + counter);
			insertOccurred = false;
			for (int i = 0; i < dependencies.size(); i++) {
				
				ArrayList<String> curLayoutDepList = dependencies.get(i);
				Pair<File, String> pair = fileIntPathList.get(i);
				
				for (int j = 0; j < curLayoutDepList.size(); j++) {
					
					String curDependencyTo = curLayoutDepList.get(j);
					
					if (curDependencyTo.startsWith("#")) {
						LOGGER.trace("DEPENDENCY: " + curDependencyTo);
						while (curDependencyTo.startsWith("#")) {
							curDependencyTo = curDependencyTo.substring(1);
						}
						boolean constantDefinedBefore = false;
						// check if it appears before the template
						if (i > 0) {
							y: for (int i2 = 0; i2 < i; i2++) {
								// Pair<File, String> otherPair =
								// fileIntPathList.get(i2);
								// String fileName =
								// otherPair.getKey().getName();
								// fileName = fileName.substring(0,
								// fileName.lastIndexOf('.'));
								for (String constant : ownConstants.get(i2)) {
									if (constant.equals(curDependencyTo)) {
										constantDefinedBefore = true;
										break y;
									}
								}
							}
						}
						if (!constantDefinedBefore) {
							y: for (int i2 = i + 1; i2 < dependencies.size(); i2++) {
								Pair<File, String> otherPair = fileIntPathList.get(i2);
								String fileName = otherPair.getKey().getName();
								fileName = fileName.substring(0, fileName.lastIndexOf('.'));
								for (String constant : ownConstants.get(i2)) {
									if (constant.equals(curDependencyTo)) {
										LOGGER.trace("checked " + fileIntPathList.get(i).getKey().getName()
												+ " with dependency " + curDependencyTo + " and " + constant + " i=" + i
												+ " j=" + j + " i2=" + i2);
										LOGGER.trace("fileIntPathList:" + fileIntPathList);
										// i's needs to be inserted after i2
										dependencies.remove(i);
										fileIntPathList.remove(i);
										i = i2;
										dependencies.add(i, curLayoutDepList);
										fileIntPathList.add(i, pair);
										constantDefinedBefore = true;
										LOGGER.trace("inserted " + fileIntPathList.get(i).getKey().getName() + " after "
												+ fileName);
										LOGGER.trace("fileIntPathList: " + fileIntPathList);
										break y;
									} else {
										LOGGER.trace("checked " + fileIntPathList.get(i).getKey().getName()
												+ " with dependency " + curDependencyTo + " and " + constant + " i=" + i
												+ " j=" + j + " i2=" + i2);
									}
								}
							}
						}
					} else {
						// templates
					}
				}
				
			}
		}
		
		// change order according to templates
		insertOccurred = true;
		for (int counter = 0; insertOccurred && counter < Math.pow(fileIntPathList.size(), 4); counter++) {
			LOGGER.trace("counter=" + counter);
			insertOccurred = false;
			x: for (int i = 0; i < dependencies.size(); i++) {
				
				ArrayList<String> curLayoutDepList = dependencies.get(i);
				Pair<File, String> pair = fileIntPathList.get(i);
				
				for (int j = 0; j < curLayoutDepList.size(); j++) {
					
					String curDependencyTo = curLayoutDepList.get(j);
					
					if (curDependencyTo.startsWith("#")) {
						// constants
					} else {
						// check if it appears after the template
						for (int i2 = i + 1; i2 < dependencies.size(); i2++) {
							Pair<File, String> otherPair = fileIntPathList.get(i2);
							String fileName = otherPair.getKey().getName();
							fileName = fileName.substring(0, fileName.lastIndexOf('.'));
							
							if (fileName.equals(curDependencyTo)) {
								LOGGER.trace("checked " + fileIntPathList.get(i).getKey().getName()
										+ " with dependency " + curDependencyTo + " and " + fileName + " i=" + i + " j="
										+ j + " i2=" + i2);
								LOGGER.trace("fileIntPathList:" + fileIntPathList);
								// i's needs to be inserted after i2
								dependencies.remove(i);
								fileIntPathList.remove(i);
								i = i2;
								dependencies.add(i, curLayoutDepList);
								fileIntPathList.add(i, pair);
								insertOccurred = true;
								LOGGER.trace(
										"inserted " + fileIntPathList.get(i).getKey().getName() + " after " + fileName);
								LOGGER.trace("fileIntPathList: " + fileIntPathList);
								break x;
							} else {
								LOGGER.trace("checked " + fileIntPathList.get(i).getKey().getName()
										+ " with dependency " + curDependencyTo + " and " + fileName + " i=" + i + " j="
										+ j + " i2=" + i2);
							}
						}
						
					}
				}
				
			}
		}
		
		// 1. layouts durchgehen und Abhaengigkeiten speichern
		// Abhaengigkeit:
		// - template aus anderem layout aus dieser Liste benutzen
		// (template="filename/...")
		// - constant benutzen, die in anderem layout definiert wird und nicht
		// im eigenen (#abc oder ##abc)
		// 2. layouts anordnen, sodass keine Abhaengigkeit mehr vorhanden sind
		
	}
	
}
