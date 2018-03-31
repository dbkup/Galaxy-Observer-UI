package application.ui.settings;

import application.InterfaceBuilderApp;
import application.config.ConfigService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public abstract class SettingsAutoSaveController implements Updateable {
	
	@FXML
	protected Label savedAnimLabel;
	
	@Autowired
	protected ConfigService configService;
	
	protected InterfaceBuilderApp app;
	private boolean lastSaveFailed = false;
	
	/**
	 * Automatically called by FxmlLoader
	 */
	public void initialize() {
		app = InterfaceBuilderApp.getInstance();
		initSavingLabel();
	}
	
	/**
	 *
	 */
	private void initSavingLabel() {
		savedAnimLabel.setOpacity(0);
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
		icon.setFill(Color.LIMEGREEN);
		savedAnimLabel.setTextFill(Color.LIMEGREEN);
		savedAnimLabel.setGraphic(icon);
	}
	
	@Override
	public void update() {
		savedAnimLabel.setOpacity(0);
	}
	
	/**
	 * Returns the Window that contains this UI.
	 *
	 * @return
	 */
	protected Window getWindow() {
		return savedAnimLabel.sceneProperty().getValue().getWindow();
	}
	
	/**
	 * Persists the settings.ini file.
	 */
	public void persistSettingsIni() {
		try {
			configService.getIniSettings().writeSettingsToFile();
			if (lastSaveFailed) {
				lastSaveFailed = false;
				initSavingLabel();
			}
		} catch (IOException e) {
			savedAnimLabel.setText("Saving failed!");
			FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE);
			icon.setFill(Color.RED);
			savedAnimLabel.setTextFill(Color.RED);
			savedAnimLabel.setGraphic(icon);
			lastSaveFailed = true;
		}
		// label animation
		FadeTransition trans = new FadeTransition(Duration.millis(2500), savedAnimLabel);
		trans.setFromValue(0.0);
		trans.setToValue(1.0);
		trans.setAutoReverse(true);
		trans.setCycleCount(2);
		trans.play();
	}
}