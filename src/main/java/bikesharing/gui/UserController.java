package bikesharing.gui;

import bikesharing.MongoDatabaseManager;
import bikesharing.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class UserController {
	@FXML private TextField name;
	@FXML private TextField surname;
	@FXML private Label status;
	
	private IndexController indexCtrl;
	
	MongoDatabaseManager dm;
	
	public void init(IndexController indexCtrl) {
		this.indexCtrl = indexCtrl;
	}
	
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
		
		User user = new User();
		user.setName(name.getText());
		user.setSurname(surname.getText());
		user.setUsername(name.getText().toLowerCase() + surname.getText().toLowerCase());
		user.setStatus("S");
		user.setPassword("pwd");
		
		dm = MongoDatabaseManager.getInstance();
		if (!dm.insertUser(user)) {
			status.setText("Database error: impossible to insert a new user");
		} else {
			indexCtrl.loadUsers();
			
			Alert alert = new Alert(AlertType.INFORMATION);
	        alert.setTitle("Success");
	        alert.setHeaderText("Successfully done!");
	        alert.showAndWait();
		}
	}
}
