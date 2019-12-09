package bikesharing.gui;

import java.io.File;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class IndexController {
	@FXML private Button chooseButton;
	@FXML private Button loadButton;
	@FXML private Label path;
	
	private File currentFile;
	
	@FXML
	private void choose(ActionEvent event) {
		Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
		FileChooser fileChooser = new FileChooser();
		//FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
		//fileChooser.getExtensionFilters().add(extFilter);
		currentFile = fileChooser.showOpenDialog(stage);
		path.setText(currentFile.getPath());
	}
	
	@FXML
	private void load() {}
}
