package bikesharing.gui;

import bikesharing.DatabaseManager;
import bikesharing.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;

public class userController {
	@FXML private TextField name;
	@FXML private TextField surname;
	@FXML private Label status;
	
	DatabaseManager dm;
	
	@FXML
	private void insertUser() {
		if (name.getText().isEmpty()) {
			status.setText("Please enter the name");
			return;
		}
		
		if (surname.getText().isEmpty()) {
			status.setText("Please enter the surname");
			return;
		}
		
		User user = new User();
		user.setName(name.getText());
		user.setSurname(surname.getText());
		
		dm = DatabaseManager.getInstance();
		if (!dm.insertUser()) {
			status.setText("Database error: impossible to insert a new user");
		} else {
			Alert alert = new Alert(AlertType.INFORMATION);
	        alert.setTitle("Success");
	        alert.setHeaderText("Successfully done!");
	        alert.showAndWait();
		}
	}
}
