package bikesharing.gui;

import bikesharing.DatabaseManager;
import bikesharing.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;

public class UserController {
	@FXML private TextField name;
	@FXML private TextField surname;
	@FXML private Label status;
	
	private IndexController indexCtrl;
	
	DatabaseManager dm;
	
	public void init(IndexController indexCtrl) {
		this.indexCtrl = indexCtrl;
	}
	
	
	//This function handle the usert insertion action on the GUI
	@FXML
	private void insertUser() {
		status.setText("");
		
		if (name.getText().isEmpty()) {
			status.setText("Please enter the name");
			return;
		}
		
		if (surname.getText().isEmpty()) {
			status.setText("Please enter the surname");
			return;
		}
		
		//Creates user instance
		User user = new User();
		user.setName(name.getText());
		user.setSurname(surname.getText());
		user.setUsername(name.getText().toLowerCase() + surname.getText().toLowerCase());
		user.setStatus("S");
		user.setPassword("pwd");
		
		//Gets the db instance
		dm = DatabaseManager.getInstance();
		if (!dm.insertUser(user)) {
			status.setText("Database error: impossible to insert a new user");
		} else {
			//Realods the table
			indexCtrl.loadUsers();
			//Show successful message
			Alert alert = new Alert(AlertType.INFORMATION);
	        alert.setTitle("Success");
	        alert.setHeaderText("Successfully done!");
	        alert.showAndWait();
		}
	}
}
