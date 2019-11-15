package bikesharing;

import bikesharing.gui.MainController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	public static void main(String[] args) {
		System.out.println("Helloh");
		launch();
	}

	@Override
	public void start(Stage stage) throws Exception {
		FXMLLoader fxml = new FXMLLoader();
		Parent root = fxml.load(getClass().getResource("/gui/index.fxml").openStream());
		MainController controller = (MainController) fxml.getController();
		controller.setSession(stage);
		
		Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();		
	}

}
