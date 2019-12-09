package bikesharing.gui;

import bikesharing.DatabaseManager;
import bikesharing.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class LoginController {
	@FXML private TextField id;
	@FXML private TextField password;
	@FXML private Text status;

	@FXML
    private void login(ActionEvent event){
		status.setText("");
		
	    if (id.getText().isEmpty()){
	        status.setText("Please enter your ID");
	        return;
        }

        if (password.getText().isEmpty()){
            status.setText("Please enter your password");
            return;
        }
        
        DatabaseManager dm = DatabaseManager.getInstance();
        User user = dm.login(id.getText(), password.getText());
        if (user != null) {
        	IndexController ctrl = (IndexController) StageUtils.replace(this, event, "/gui/index.fxml");
        	ctrl.setSession(user);
        }
        else
        	status.setText("Invalid credentials");
    }
}
