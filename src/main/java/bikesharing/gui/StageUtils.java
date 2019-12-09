package bikesharing.gui;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class StageUtils {
	
	/* get current stage */
	public static Stage getStage(ActionEvent event) {
		return (Stage) ((Node)event.getSource()).getScene().getWindow();
	}
	
	/* replaces current window with a new one */	
	public static Object replace(Object o, ActionEvent event, String filename) {
		Object controller = null;
		
		try {
			FXMLLoader fxml = new FXMLLoader();
						
			Parent root = fxml.load(o.getClass().getResource(filename).openStream());
			controller = (Object) fxml.getController();
			
			Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
			Scene scene = new Scene(root);
			//scene.getStylesheets().add(o.getClass().getResource("/gui/stylesheets/bootstrap3.css").toExternalForm());
	        stage.setScene(scene);
	        stage.show();
		} catch (IOException e) {
			System.err.println("Can not load new FXML " + filename);
			e.printStackTrace();
			System.exit(1);
		}
		
		return controller;
		
	}
	
	/* opens a new window */
	public static Object open(Object o, ActionEvent event, String filename) {
		Object controller = null;
		
		try {
			FXMLLoader fxml = new FXMLLoader();
						
			Parent root = fxml.load(o.getClass().getResource(filename).openStream());
			controller = (Object) fxml.getController();
			
			Stage stage = new Stage();
			Scene scene = new Scene(root);
			scene.getStylesheets().add(o.getClass().getResource("/gui/stylesheets/bootstrap3.css").toExternalForm());
	        stage.setScene(scene);
	        stage.show();
		} catch (IOException e) {
			System.err.println("Can not load new FXML " + filename);
			e.printStackTrace();
			System.exit(1);
		}
		
		return controller;
	}
	
	public static void close(Object o, ActionEvent event) {
		Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
		stage.close();		
	}
	

}
