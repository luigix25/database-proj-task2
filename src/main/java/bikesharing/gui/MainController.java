package bikesharing.gui;

import java.io.File;
import java.util.List;

import bikesharing.*;

import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainController {
	
	@FXML Button loadButton;
	
	private File currentFile;
	private Stage currentStage;
	
	
	
	
	public void setSession(Stage stage) {
		this.currentStage = stage;
		loadButton.setDisable(true);
	}
	
	
	@FXML
	private void login(ActionEvent event) {
	
		FileChooser fileChooser = new FileChooser();
		//FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
		//fileChooser.getExtensionFilters().add(extFilter);
		File file = fileChooser.showOpenDialog(this.currentStage);
		if(file != null)
			this.loadButton.setDisable(false);
		
		currentFile = file;
		//System.out.println(file);	
		
	}
	
	@FXML
	private void load(ActionEvent event) {
	
		FileManager fm = new FileManager(this.currentFile.toURI());
		List<String>data = fm.readLines();
		DatabaseManager dm = DatabaseManager.getInstance();
		dm.insertBatch(data, "members");
		
		//System.out.println(this.currentFile.toURI());	
		
	}
	
}
