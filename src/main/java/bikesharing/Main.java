package bikesharing;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	public static void main(String[] args) {
		launch();
		System.out.println("Closing db connection...");
		MongoDatabaseManager.close();
	}

	@Override
	public void start(Stage stage) throws Exception {
		FXMLLoader fxml = new FXMLLoader();
		Parent root = fxml.load(getClass().getResource("/gui/login.fxml").openStream());

		Scene scene = new Scene(root);
        stage.setScene(scene);
		stage.setTitle("WBP Bike Sharing System");
        stage.show();
	}
}
