package com.ahli.hotkeyUi.application.ui;

import com.ahli.hotkeyUi.application.model.ValueDef;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A table cell for ValueDef model data to reset its value to the default value.
 *
 * @author Ahli
 */
public class ResetDefaultButtonTableCell extends TableCell<ValueDef, Boolean> {
	private static final Logger logger = LogManager.getLogger();
	
	private final Button cellButton = new Button();
	
	/**
	 * @param text
	 */
	public ResetDefaultButtonTableCell(final String text) {
		this();
		cellButton.setText(text);
	}
	
	/**
	 * Constructor.
	 */
	public ResetDefaultButtonTableCell() {
		cellButton.setOnAction(new EventHandler<>() {
			@Override
			public void handle(final ActionEvent t) {
				logger.trace("reset table item clicked");
				final ValueDef data = getTableRow().getItem();
				data.setValue(data.getDefaultValue());
			}
		});
	}
	
	// Display button if the row is not empty
	@Override
	protected void updateItem(final Boolean t, final boolean empty) {
		super.updateItem(t, empty);
		if (!empty) {
			setGraphic(cellButton);
		} else {
			setGraphic(null);
		}
	}
}
