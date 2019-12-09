package bikesharing.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;
import javax.swing.JOptionPane;

import bikesharing.DatabaseManager;
import bikesharing.FileManager;
import bikesharing.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class IndexController {
	@FXML private TabPane tabPane;
	@FXML private Tab manageDatasetTab;
	@FXML private Tab employeesTab;
	@FXML private Tab statisticsTab;
	
	/* Manage Dataset */
	/* Insert new Tips */
	@FXML private Button chooseButton;
	@FXML private Button loadButton;
	@FXML private Label path;
	@FXML private Label loadStatus;
	/* Delete Trips */
	@FXML private MenuButton city;
	@FXML private DatePicker fromDate;
	@FXML private DatePicker toDate;
	@FXML private Label deleteStatus;
	
	private String tripsCollection = "members";
	
	private File currentFile;
	private User user;
	
	public void setSession(User user) {
		this.user = user;
		// TODO -- remove
		System.out.println(this.user.getStatus());
		if (this.user.getStatus().equals("S")) {
			tabPane.getTabs().remove(manageDatasetTab);
			tabPane.getTabs().remove(employeesTab);
		}
	}
	
	@FXML
	private void choose(ActionEvent event) {
		Stage stage = StageUtils.getStage(event);
		FileChooser fileChooser = new FileChooser();
		// TODO -- remove this lines?
		//FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
		//fileChooser.getExtensionFilters().add(extFilter);
		currentFile = fileChooser.showOpenDialog(stage);
		path.setText(currentFile.getPath());
	}
	
	@FXML
	private void load() {
		loadStatus.setText("");
		
		if (currentFile == null) {
			loadStatus.setText("Please choose the file to load.");
			return;
		}
		
		/* TODO -- check the mime type: unfortunately it is based on the file extension 
		try {
			System.out.println(currentFile.toURI().toURL().openConnection().getContentType());
			if (!currentFile.toURI().toURL().openConnection().getContentType().equals("text/plain")) {
				loadStatus.setText("Please choose a text file.");
				return;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
			
		FileManager fm = new FileManager(this.currentFile.toURI());
		List<String>data = fm.readLines();
		DatabaseManager dm = DatabaseManager.getInstance();
		if (!dm.insertBatch(data, tripsCollection)) {
			JOptionPane.showMessageDialog(null, "Error");
			loadStatus.setText("Impossible to load this file. Check the format.");
		}
		else
			JOptionPane.showMessageDialog(null, "Success");
	}
	
	@FXML
	private void delete() {
		deleteStatus.setText("");
		
		if (fromDate.getValue() == null) {
			deleteStatus.setText("Please enter the start date.");
			return;
		}
		
		if (toDate.getValue() == null) {
			deleteStatus.setText("Please enter the end date.");
			return;
		}
		
		// TODO -- check the city selection
	}
}
