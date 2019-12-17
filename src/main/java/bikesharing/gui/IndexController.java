package bikesharing.gui;

import java.io.File;
import java.util.*;

import org.bson.Document;

import bikesharing.DatabaseManager;
import bikesharing.FileManager;
import bikesharing.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class IndexController {
	@FXML private TabPane tabPane;
	@FXML private Tab manageDatasetTab;
	@FXML private Tab employeesTab;
	@FXML private Tab statisticsTab;

	/* Manage Dataset */
	/* Insert new Tips */
	@FXML private Button chooseButton;
	@FXML private Button loadButton;
	@FXML private Label path;
	@FXML private Label loadStatus;
	/* Delete Trips */
	@FXML private ChoiceBox<String> citySelector;
	@FXML private DatePicker fromDate;
	@FXML private DatePicker toDate;
	@FXML private Label deleteStatus;

	@FXML private TableView<User> tableView;
	@FXML private TableColumn<User,String> columnName;
	@FXML private TableColumn<User,String> columnSurname;
	@FXML private TableColumn<User,String> columnStatus;
	@FXML private TableColumn<User, String> columnUsername;

	@FXML private BarChart<String, Integer> barChart;
	@FXML private Label status;


	@FXML private ChoiceBox<String> choiceCity;
	@FXML private ChoiceBox<String> choiceYear;

	

	private String tripsCollection = "trip";

	private File currentFile;
	private User user;

	private DatabaseManager dm;

	public void init(User user) {
		dm = DatabaseManager.getInstance();
		setSession(user);
		
		setUpCitySelector();
		setUpYearSelector();
		
		initTable();
		initChart();
		
				
	}

	private void setSession(User user) {
		this.user = user;

		if (this.user.getStatus().equals("C")) {
			tabPane.getTabs().remove(employeesTab);
		}

		if (this.user.getStatus().equals("S")) {
			tabPane.getTabs().remove(manageDatasetTab);
			tabPane.getTabs().remove(employeesTab);
		}

	}

	private void initTable() {

		columnName.setCellValueFactory(new PropertyValueFactory<User, String>("name"));
		columnSurname.setCellValueFactory(new PropertyValueFactory<User, String>("surname"));
		columnStatus.setCellValueFactory(new PropertyValueFactory<User, String>("status"));
		columnUsername.setCellValueFactory(new PropertyValueFactory<User, String>("username"));
		loadUsers();


	}

	private void initChart() {

		List<Document> data = DatabaseManager.getInstance().tripsForEachCity("trip");
        barChart.getData().clear();

        XYChart.Series<String,Integer> series1 = new XYChart.Series<String, Integer>();
        series1.setName("Global Trips");

        for(Document document : data) {
            series1.getData().add(new XYChart.Data<String, Integer>((String)document.get("city"), (Integer)document.get("trips")));

        }

        barChart.getData().add(series1);

	}
	
	public void loadUsers() {
		List<User> users = DatabaseManager.getInstance().getAllUsers();
		tableView.getItems().setAll(users);


	}

	private void setUpCitySelector() {
		List<String> cities = dm.getCities();
		citySelector.getItems().clear();
		choiceCity.getItems().clear();
		
		choiceCity.getItems().add("All");
		choiceCity.setValue("All");
		
		for (String city : cities) {
			citySelector.getItems().add(city);
			choiceCity.getItems().add(city);
		}
	}
	
	private void setUpYearSelector() {
		List<Integer> years = dm.getYears();
		choiceYear.getItems().clear();
		
		choiceYear.getItems().add("All");
		choiceYear.setValue("All");
		
		for (Integer year : years) {
			choiceYear.getItems().add(year.toString());
		}
	}

	@FXML
	private void choose(ActionEvent event) {
		Stage stage = StageUtils.getStage(event);
		FileChooser fileChooser = new FileChooser();
		// TODO -- remove this lines?
		//FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
		//fileChooser.getExtensionFilters().add(extFilter);
		currentFile = fileChooser.showOpenDialog(stage);
		if(currentFile != null)
			path.setText(currentFile.getPath());
	}

	@FXML
	private void promoteUser(ActionEvent event) {
		status.setText("");
		User user = tableView.getSelectionModel().getSelectedItem();
		if(user == null) {
			status.setText("Please select an employee");
			return;
		}

		System.out.println(user);

		if (!dm.promoteUser(user))
			status.setText("Cannot perform this action on this employee");
		else
			loadUsers();

	}

	@FXML
	private void demoteUser(ActionEvent event) {
		status.setText("");
		User user = tableView.getSelectionModel().getSelectedItem();
		if(user == null) {
			status.setText("Please select an employee");
			return;
		}

		System.out.println(user);

		if (!dm.demoteUser(user))
			status.setText("Cannot perform this action on this employee");
		else
			loadUsers();

	}

	@FXML
	private void hireUser() {
		status.setText("");
		UserController ctrl = (UserController) StageUtils.open(this, null, "/gui/user.fxml");
		ctrl.init(this);
	}

	@FXML
	private void fire(ActionEvent event) {
		User user = tableView.getSelectionModel().getSelectedItem();
		status.setText("");


		if(user == null) {
			status.setText("Please select an employee");
			return;
		}

		Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirm Fire");
        alert.setHeaderText("Are you sure to fire " + user.getName() + " " + user.getSurname() + "?");
        alert.setContentText("Confirm?");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.get() == ButtonType.OK) {
			System.out.println(user);

			if(dm.fire(user))
				System.out.print("fired successfully:)");
			else
				status.setText("fire is not permitted");
        }

		loadUsers();

	}

	@FXML
	private void load() {
		loadStatus.setText("");

		if (currentFile == null) {
			loadStatus.setText("Please choose the file to load.");
			return;
		}

		FileManager fm = new FileManager(this.currentFile.toURI());
		List<String> data;

		try {
			data = fm.readLines();
		} catch (Exception e) {
			return;
		}

		if (!dm.insertBatch(data, tripsCollection)) {
			loadStatus.setText("Impossible to load this file. Check the format.");
			return;
		}
		
		setUpCitySelector();

		Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Dataset successfully imported");
        alert.showAndWait();
	}

	@FXML
	private void delete() {
		deleteStatus.setText("");

		if (citySelector.getValue() == null) {
			deleteStatus.setText("Please select the city.");
			return;
		}

		if (fromDate.getValue() == null) {
			deleteStatus.setText("Please enter the start date.");
			return;
		}

		if (toDate.getValue() == null) {
			deleteStatus.setText("Please enter the end date.");
			return;
		}

		int deletedTrips = dm.deleteTrips(citySelector.getValue().toString(), fromDate.getValue(), toDate.getValue());
		if (deletedTrips == 0) {
			deleteStatus.setText("0 documents have been deleted.");
			return;
		}
		
		setUpCitySelector();

		Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Success: " + deletedTrips + " document(s) have been deleted");
        alert.showAndWait();
	}

	@FXML
	private void filter(ActionEvent event) {
		
		if(choiceCity.getValue().equals("All")) {				// I don't want to filter
			initChart();
			return;
		}
		
		String year_string = choiceYear.getValue();
		if(year_string.equals("All")) {
			//TODO: gestire errore
			return;
		}
		
		int year = Integer.parseInt(year_string);
		String city = choiceCity.getValue();
		
		List<Document> trips_list = dm.tripsPerCityYear(city, year, "trip");

		barChart.getData().clear();
		
		for(int i=1;i<=12;i++) {
			XYChart.Series<String,Integer> series1 = new XYChart.Series<String, Integer>();
		    series1.setName(Integer.toString(i));
		    
		    
		    for(Document document : trips_list) {

			    int month = document.getInteger("month");
			    int trips = (document.getInteger("trips"));
			    
		    	if(month == i)
		    		series1.getData().add(new XYChart.Data<String, Integer>(Integer.toString(month), trips));

	        }
		    
	        barChart.getData().add(series1);

		    
			
		}
		
		/*for(Document document : trips) {
            series1.getData().add(new XYChart.Data<String, Integer>((String)document.get("city"), (Integer)document.get("trips")));

        }*/
        

		
		
	}
	
}
