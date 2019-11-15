package bikesharing.gui;

import java.io.File;

import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainController {
	
	private Stage currentStage;
	
	public void setSession(Stage stage) {
		this.currentStage = stage;
	}
	
	@FXML
	private void companyLogin(ActionEvent event) {
		System.err.println("login as company...");
	//	StageUtils.replace(this, event, "/gui/LoginCompany.fxml");
	}
	
	@FXML
	private void userLogin(ActionEvent event) {
		System.err.println("loggin in as a user...");
	//	StageUtils.replace(this, event, "/gui/LoginUser.fxml");
	}

	@FXML
	private void userSignup(ActionEvent event){
		System.err.println("signing in as a user...");
	//	StageUtils.replace(this, event, "/gui/signup.fxml");
	}
	
	@FXML
	private void login(ActionEvent event) {
	
		FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
		fileChooser.getExtensionFilters().add(extFilter);
		File file = fileChooser.showOpenDialog(this.currentStage);
		System.out.println(file);	
		
	}
	
}
