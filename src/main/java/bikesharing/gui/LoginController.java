package bikesharing.gui;

import bikesharing.DatabaseManager;
import bikesharing.User;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class LoginController {
	@FXML private TextField username;
	@FXML private TextField password;
	@FXML private Text status;

	@FXML
    private void login(ActionEvent event){
		status.setText("");
		
	    if (username.getText().isEmpty()){
	        status.setText("Please enter your ID");
	        return;
        }

        if (password.getText().isEmpty()){
            status.setText("Please enter your password");
            return;
        }
        
		status.setText("Wait a moment...");

        DatabaseManager dm = DatabaseManager.getInstance();
        
        Task<User> task = new Task<User>() {
        	@Override
			public User call() {
				System.err.println("[I] Login...");
        		return dm.login(username.getText(), password.getText());
        	}
		};
		task.setOnSucceeded(e -> {
			System.err.println("[I] Login ok");
			User user = task.getValue();

			if (user != null) {
				IndexController ctrl = (IndexController) StageUtils.replace(this, event, "/gui/index.fxml");
				ctrl.init(user);
			} else {
				status.setText("Invalid credentials");
			}
		});
    }
}
