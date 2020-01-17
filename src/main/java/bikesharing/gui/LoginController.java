package bikesharing.gui;

import java.net.InetAddress;

import com.mongodb.ServerAddress;

import bikesharing.DatabaseManager;
import bikesharing.User;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class LoginController {
	@FXML private TextField username;
	@FXML private TextField password;
	@FXML private Text status;

	@FXML
    private void login(ActionEvent event){
		status.setText("please wait a moment");

		DatabaseManager dm = DatabaseManager.getInstance();
		
		if (username.getText().isEmpty()) {
	        status.setText("Please enter your ID");
	        return;
        }

        if (password.getText().isEmpty()){
            status.setText("Please enter your password");
            return;
        }

		Task<Object> task = new Task<Object>() {
			@Override
			public Object call() {
				for (ServerAddress host : dm.getHosts()) {
					try {
						InetAddress inet = InetAddress.getByName(host.getHost());
						if (!inet.isReachable(5000)) {
							return false;
						}
					} catch (Exception ex) {
						return false;
					}
				}

				return dm.login(username.getText(), password.getText());
			}
		};
        
		task.setOnSucceeded(e -> {
			Object result = task.getValue();

			if (result == null) {
				status.setText("Invalid credentials");
				return;
			} else if (result instanceof Boolean) {
				if ((Boolean) (result) == false) {
					status.setText("server is not reachable");
					Alert alert = new Alert(AlertType.WARNING);
					alert.setTitle("Warning");
					alert.setHeaderText("Server connection");
					alert.setContentText("Server is not reachable.\nPlease check your Internet and VPN connection");
					alert.showAndWait();
					return;
				}
			} else if (result instanceof User) {
				User user = (User) result;
				IndexController ctrl = (IndexController) StageUtils.replace(this, event, "/gui/index.fxml");
				ctrl.init(user);
			}
		});

		new Thread(task).start();

    }
}
