package bikesharing;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainGUI extends Application {

	public void main(String[] args) {
		launch();	
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		FXMLLoader fxml = new FXMLLoader();
		
		//Loads the first Interface we need
		Parent root = fxml.load(getClass().getResource("/gui/login.fxml").openStream());

		Scene scene = new Scene(root);
        stage.setScene(scene);
		stage.setTitle("WBP Bike Sharing System");
		//shows the view
        stage.show();
	}
	
	
}
