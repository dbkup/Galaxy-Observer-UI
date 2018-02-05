package com.ahli.galaxy.ui;

import com.ahli.util.DeepCopyable;

import java.io.Serializable;

/**
 * @author Ahli
 */
public class UITemplate implements Serializable, DeepCopyable {
	/**
	 *
	 */
	private static final long serialVersionUID = 7686203678975623860L;
	
	// private static final Logger logger = LogManager.getLogger();
	
	private String fileName;
	private UIElement element;
	private boolean isLocked;
	
	/**
	 * @param fileName
	 * @param element
	 */
	public UITemplate(final String fileName, final UIElement element) {
		this.fileName = fileName;
		this.element = element;
	}
	
	/**
	 * Returns a deep clone of this.
	 */
	@Override
	public Object deepCopy() {
		final UITemplate clone = new UITemplate(fileName, (UIElement) element.deepCopy());
		clone.isLocked = isLocked;
		return clone;
	}
	
	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * @param fileName
	 * 		the fileName to set
	 */
	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}
	
	/**
	 * @return the element
	 */
	public UIElement getElement() {
		return element;
	}
	
	/**
	 * @param element
	 * 		the element to set
	 */
	public void setElement(final UIElement element) {
		this.element = element;
	}
	
	/**
	 * @return the isLocked
	 */
	public boolean isLocked() {
		return isLocked;
	}
	
	/**
	 * @param isLocked
	 * 		the isLocked to set
	 */
	public void setLocked(final boolean isLocked) {
		this.isLocked = isLocked;
	}
	
	/**
	 * @param path
	 * @return
	 */
	public UIElement receiveFrameFromPath(final String path) {
		// logger.trace("receive Frame from path: " + path);
		// logger.trace("template's element name: " + element.getName());
		final String curName = UIElement.getLeftPathLevel(path);
		// logger.trace("searched name: " + curName);
		if (curName != null) {
			if (curName.equalsIgnoreCase(element.getName())) {
				final String newPath = UIElement.removeLeftPathLevel(path);
				// logger.trace("match! new Path: " + newPath);
				
				return element.receiveFrameFromPath(newPath);
			}
		}
		// logger.trace("did not find template: " + path);
		return null;
	}
	
	@Override
	public String toString() {
		return "<Template fileName='" + fileName + "' elementName='" + element.getName() + "'>";
	}
}
