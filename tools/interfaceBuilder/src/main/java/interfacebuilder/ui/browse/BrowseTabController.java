package interfacebuilder.ui.browse;

import com.ahli.galaxy.ui.UIAnchorSide;
import com.ahli.galaxy.ui.UIAnimation;
import com.ahli.galaxy.ui.UIAttribute;
import com.ahli.galaxy.ui.UIController;
import com.ahli.galaxy.ui.UIFrame;
import com.ahli.galaxy.ui.UIState;
import com.ahli.galaxy.ui.UIStateGroup;
import com.ahli.galaxy.ui.UITemplate;
import com.ahli.galaxy.ui.abstracts.UIElement;
import com.ahli.galaxy.ui.interfaces.UICatalog;
import com.ahli.util.Pair;
import gnu.trove.map.hash.THashMap;
import interfacebuilder.ui.settings.Updateable;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BrowseTabController implements Updateable {
	private static final Logger logger = LogManager.getLogger();
	private static final String GAME_UI = "GameUI";
	
	@FXML
	private TableView<Map.Entry<String, String>> tableView;
	@FXML
	private TableColumn<Map.Entry<String, String>, String> columnAttributes;
	@FXML
	private TableColumn<Map.Entry<String, String>, String> columnValues;
	@FXML
	private TreeView<UIElement> frameTree;
	@FXML
	private ComboBox<String> fileDropdown;
	private AutoCompletionBinding<String> fileDropdownAutoCompleteBinding;
	@FXML
	private ComboBox<String> templateDropdown;
	private AutoCompletionBinding<String> templateDropdownAutoCompleteBinding;
	private Map<String, UITemplate> templateMap;
	
	private UICatalog uiCatalog;
	
	/**
	 * Automatically called by FxmlLoader
	 */
	public void initialize() {
		templateMap = new THashMap<>();
		fileDropdown.setOnAction(actionEvent -> Platform.runLater(() -> {
			final String selectedFile = fileDropdown.getSelectionModel().getSelectedItem();
			updateTemplateDropdown(selectedFile);
		}));
		templateDropdown.setOnAction(actionEvent -> Platform.runLater(() -> {
			final String selectedTemplateRootElem = templateDropdown.getSelectionModel().getSelectedItem();
			final UITemplate template = templateMap.get(selectedTemplateRootElem);
			createTree(template);
		}));
		frameTree.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> showInTableView(newValue));
		
		columnAttributes.setCellValueFactory(new Callback<>() {
			@Override
			public ObservableValue<String> call(
					final TableColumn.CellDataFeatures<Map.Entry<String, String>, String> p) {
				return new SimpleObjectProperty<>(p.getValue().getKey());
			}
		});
		columnValues.setCellValueFactory(new Callback<>() {
			@Override
			public ObservableValue<String> call(
					final TableColumn.CellDataFeatures<Map.Entry<String, String>, String> p) {
				return new SimpleObjectProperty<>(p.getValue().getValue());
			}
		});
		columnAttributes.prefWidthProperty().bind(tableView.widthProperty().divide(3));
		columnValues.prefWidthProperty().bind(tableView.widthProperty().divide(1.5));
		columnAttributes.sortTypeProperty().set(TableColumn.SortType.ASCENDING);
		tableView.getSortOrder().add(columnAttributes);
	}
	
	private void showInTableView(final TreeItem<UIElement> selected) {
		if (selected == null) {
			tableView.getItems().clear();
		} else {
			final UIElement el = selected.getValue();
			final Map<String, String> map = new HashMap<>(); // TODO maybe use something different that holds entries
			if (el instanceof UIFrame) {
				final UIFrame elem = (UIFrame) el;
				UIAnchorSide side = UIAnchorSide.TOP;
				map.put("Anchor-Top", elem.getAnchorRelative(side) + " - " + elem.getAnchorPos(side) + " - " +
						elem.getAnchorOffset(side));
				side = UIAnchorSide.LEFT;
				map.put("Anchor-Left", elem.getAnchorRelative(side) + " - " + elem.getAnchorPos(side) + " - " +
						elem.getAnchorOffset(side));
				side = UIAnchorSide.BOTTOM;
				map.put("Anchor-Bottom", elem.getAnchorRelative(side) + " - " + elem.getAnchorPos(side) + " - " +
						elem.getAnchorOffset(side));
				side = UIAnchorSide.RIGHT;
				map.put("Anchor-Right", elem.getAnchorRelative(side) + " - " + elem.getAnchorPos(side) + " - " +
						elem.getAnchorOffset(side));
				for (final UIAttribute attr : elem.getAttributes()) {
					map.put(attr.getName(), prettyPrint(attr));
				}
			} else if (el instanceof UIStateGroup) {
				final UIStateGroup elem = (UIStateGroup) el;
				final String dfltState = elem.getDefaultState();
				if (dfltState != null) {
					map.put("DefaultState", dfltState);
				}
				final String statePrefix = "State ";
				int i = 0;
				for (final UIState state : elem.getStates()) {
					i++;
					map.put(statePrefix + i, state.getName());
				}
			} else if (el instanceof UIAnimation) {
				final UIAnimation elem = (UIAnimation) el;
				final UIAttribute driver = elem.getDriver();
				if (driver != null) {
					map.put("Driver", prettyPrint(driver));
				}
				int i = 0;
				final String eventPrefix = "Event ";
				for (Pair<String, UIAttribute> event : elem.getEvents()) {
					i++;
					map.put(eventPrefix + i, prettyPrint(event.getValue()));
				}
			} else if (el instanceof UIController) {
				final UIController elem = (UIController) el;
				int i = 0;
				final String keyPrefix = "Key ";
				for (final UIAttribute attr : elem.getKeys()) {
					i++;
					map.put(keyPrefix + i, prettyPrint(attr));
				}
			} else if (el instanceof UIState) {
				final UIState elem = (UIState) el;
				int i = 0;
				final String whenPrefix = "When ";
				for (final UIAttribute attr : elem.getWhens()) {
					i++;
					map.put(whenPrefix + i, prettyPrint(attr));
				}
				i = 0;
				final String actionPrefix = "Action ";
				for (final UIAttribute attr : elem.getActions()) {
					i++;
					map.put(actionPrefix + i, prettyPrint(attr));
				}
			}
			tableView.getItems().setAll(map.entrySet());
			tableView.sort();
		}
	}
	
	private String prettyPrint(final UIAttribute attr) {
		return prettyPrintAttributeStringList(attr.getKeyValues());
	}
	
	private String prettyPrintAttributeStringList(final List<String> attributes) {
		if (attributes.size() == 2 && attributes.get(0).equals("val")) {
			return attributes.get(1);
		}
		StringBuilder str = new StringBuilder();
		str.append(attributes.get(0)).append('=').append(attributes.get(1));
		final String separator = ", ";
		for (int i = 2, len = attributes.size(); i < len; i += 2) {
			str.append(separator).append(attributes.get(i)).append('=').append(attributes.get(i + 1));
		}
		return str.toString();
	}
	
	
	/**
	 * @param template
	 */
	private void createTree(final UITemplate template) {
		if (template != null) {
			final UIElement rootElement = template.getElement();
			final TreeItem<UIElement> treeItem = new TreeItem<>(rootElement);
			frameTree.setRoot(treeItem);
			final ObservableList<TreeItem<UIElement>> treeItemChildren = frameTree.getRoot().getChildren();
			for (final UIElement child : rootElement.getChildren()) {
				createTree(child, treeItemChildren);
			}
			treeItem.expandedProperty().setValue(true);
		} else {
			frameTree.setRoot(null);
		}
		frameTree.getSelectionModel().select(0);
	}
	
	/**
	 * @param element
	 * @param parentsChildren
	 */
	private void createTree(final UIElement element, final ObservableList<TreeItem<UIElement>> parentsChildren) {
		final TreeItem<UIElement> treeItem = new TreeItem<>(element);
		parentsChildren.add(treeItem);
		final ObservableList<TreeItem<UIElement>> treeItemChildren = treeItem.getChildren();
		for (final UIElement child : element.getChildren()) {
			createTree(child, treeItemChildren);
		}
		treeItem.expandedProperty().setValue(true);
	}
	
	/**
	 * @param uiCatalog
	 */
	public void setData(final UICatalog uiCatalog) {
		this.uiCatalog = uiCatalog;
		updateDropdowns();
	}
	
	/**
	 *
	 */
	private void updateDropdowns() {
		if (uiCatalog != null) {
			final Set<String> fileNamesSet = new HashSet<>();
			for (final UITemplate template : uiCatalog.getTemplates()) {
				fileNamesSet.add(template.getFileName());
			}
			final ObservableList<String> fileNames = FXCollections.observableList(new ArrayList<>(fileNamesSet));
			fileNames.sort(null);
			fileDropdown.setItems(fileNames);
			final String firstSelection = fileNamesSet.contains(GAME_UI) ? GAME_UI : fileNames.get(0);
			logger.info("first selection {}", firstSelection);
			fileDropdown.setValue(firstSelection);
			if (fileDropdownAutoCompleteBinding != null) {
				fileDropdownAutoCompleteBinding.dispose();
			}
			fileDropdownAutoCompleteBinding =
					TextFields.bindAutoCompletion(fileDropdown.getEditor(), fileDropdown.getItems());
			fileDropdownAutoCompleteBinding.setMaxWidth(fileDropdown.getWidth());
			fileDropdownAutoCompleteBinding.setMinWidth(fileDropdown.getWidth());
			fileDropdownAutoCompleteBinding.setPrefWidth(fileDropdown.getWidth());
		}
	}
	
	@Override
	public void update() {
		// nothing to do
	}
	
	/**
	 * @param fileName
	 */
	private void updateTemplateDropdown(final String fileName) {
		if (uiCatalog != null) {
			templateMap.clear();
			final List<String> templatesOfFile = new ArrayList<>();
			String firstSelection = null;
			String name;
			for (final UITemplate template : uiCatalog.getTemplates()) {
				if (template.getFileName().equals(fileName)) {
					name = template.getElement().getName();
					templatesOfFile.add(name);
					templateMap.put(name, template);
					if (name.equals(GAME_UI)) {
						firstSelection = name;
					}
				}
			}
			final ObservableList<String> elementNames = FXCollections.observableList(new ArrayList<>(templatesOfFile));
			templateDropdown.setItems(elementNames);
			if (templateDropdownAutoCompleteBinding != null) {
				templateDropdownAutoCompleteBinding.dispose();
			}
			templateDropdownAutoCompleteBinding =
					TextFields.bindAutoCompletion(templateDropdown.getEditor(), templateDropdown.getItems());
			templateDropdownAutoCompleteBinding.setMaxWidth(templateDropdown.getWidth());
			templateDropdownAutoCompleteBinding.setMinWidth(templateDropdown.getWidth());
			templateDropdownAutoCompleteBinding.setPrefWidth(templateDropdown.getWidth());
			if (firstSelection != null) {
				templateDropdown.setValue(firstSelection);
			} else {
				templateDropdown.selectionModelProperty().get().selectFirst();
			}
		}
	}
}
