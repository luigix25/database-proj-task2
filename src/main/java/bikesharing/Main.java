package bikesharing;

import bikesharing.gui.MainController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	public static void main(String[] args) {
	/*	MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
		MongoDatabase database = mongoClient.getDatabase("ducange");
		MongoCollection<Document> collection = database.getCollection("members");
		MongoCursor<Document> cursor = collection.find().iterator();
		try {
			while (cursor.hasNext()) {
				System.out.println(cursor.next().toJson());
			}
		} finally {
			cursor.close();
		}		
		mongoClient.close();
		*/
		launch();
		DatabaseManager.close();
		
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
