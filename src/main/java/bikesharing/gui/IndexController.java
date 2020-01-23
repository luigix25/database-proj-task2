package bikesharing.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.bson.Document;

import bikesharing.DatabaseManager;
import bikesharing.FileManager;
import bikesharing.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class IndexController {
	@FXML private TabPane tabPane;
	@FXML private Tab manageDatasetTab;
	@FXML private Tab employeesTab;
	@FXML private Tab statisticsTab;

	/* Manage Dataset */
	/* Insert new Trips */
	@FXML private Button chooseButton;
	@FXML private Button loadButton;
	
	/* Delete Trips */
	@FXML private ChoiceBox<String> citySelector;
	@FXML private DatePicker fromDate;
	@FXML private DatePicker toDate;
	@FXML private Label deleteStatus;

	/* Statistics Tab Elements */
	@FXML private TableView<User> tableView;
	@FXML private TableColumn<User,String> columnName;
	@FXML private TableColumn<User,String> columnSurname;
	@FXML private TableColumn<User,String> columnStatus;
	@FXML private TableColumn<User, String> columnUsername;

	@FXML private BarChart<String, Integer> barChart;
	@FXML private PieChart pieChart;

	@FXML
	private Label leftChartLabel;
	@FXML
	private Label rightChartLabel;

	@FXML
	private ChoiceBox<String> choiceCity;
	@FXML
	private ChoiceBox<String> choiceYear;

	/* Generic status indicator for the application */
	@FXML private Label status;
	@FXML
	private ProgressIndicator progressIndicator;
	
	/* User profile tab */
	@FXML
	private Label welcomeLabel;
	@FXML
	private TextField newPassword;

	/* Various constants and variables */
	private final String tripsCollection = "trip";

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
		initPieChart();
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

		welcomeLabel.setText("Welcome " + user.getName() + " " + user.getSurname());

	}

	private void initTable() {

		columnName.setCellValueFactory(new PropertyValueFactory<User, String>("name"));
		columnSurname.setCellValueFactory(new PropertyValueFactory<User, String>("surname"));
		columnStatus.setCellValueFactory(new PropertyValueFactory<User, String>("status"));
		columnUsername.setCellValueFactory(new PropertyValueFactory<User, String>("username"));
		loadUsers();
	}

	private void initChart() {

        barChart.getData().clear();
		List<Document> data = dm.tripsForEachCity("trip");

        XYChart.Series<String,Integer> series1 = new XYChart.Series<String, Integer>();
        series1.setName("Global Trips");

        for(Document document : data) {
            series1.getData().add(new XYChart.Data<String, Integer>((String)document.get("city"), (Integer)document.get("trips")));

        }
        barChart.getData().add(series1);
	}
	
	private void initPieChart() {
		List<Document> data = dm.tripsPerGender("trip");
		populatePieChart(data);
		rightChartLabel.setText("Riders by sex");
	}
	
	private void populatePieChart(List<Document> data) {
		ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
		for (Document document : data) {
			String gender = (document.getString("gender") == null) ? "U" : (document.getString("gender"));
			pieChartData.add(new PieChart.Data(gender, Double.valueOf(document.getInteger("count"))));
		}
		
		pieChart.setData(pieChartData);
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

		currentFile = fileChooser.showOpenDialog(stage);
		if (currentFile == null)
			return;

		Path path = Paths.get(currentFile.getPath());
		status.setText("Selected " + path);

		try {
			if (!Files.probeContentType(path).equals("text/json")) {
				status.setText("invalid file");
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Invalid MIME type for file");
				alert.setContentText("File is not text/json");
				alert.showAndWait();
			}
		} catch (IOException e) {
			System.err.println("[E] some error occurred");
			e.printStackTrace();
		}
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
		if (currentFile == null) {
			status.setText("Please choose the file to load.");
			return;
		}

		FileManager fm = new FileManager(this.currentFile.toURI());
		List<String> data;

		try {
			data = fm.readLines();
		} catch (Exception e) {
			return;
		}
        
		progressIndicator.setProgress(-1.0);
		status.setText("Loading dataset. This operation could take some minutes. Please wait...");
        
        Task<Boolean> task = new Task<Boolean>() {
        	@Override
        	public Boolean call() {
        		return dm.insertBatch(data, tripsCollection);
        	}
        };
        
        task.setOnSucceeded(e -> {
        	Boolean result = task.getValue();

        	if (result) {
				progressIndicator.setProgress(1.0);
        		setUpCitySelector();
        	
	        	Alert alert = new Alert(AlertType.INFORMATION);
	            alert.setTitle("Success");
	            alert.setHeaderText("Task succeded");
	            alert.setHeaderText("Dataset has been successfully imported in database");
	           
	            alert.showAndWait();
				status.setText("");
        	}
        	else {
				progressIndicator.setProgress(0.0);
        		
        		Alert alert = new Alert(AlertType.ERROR);
        		alert.setTitle("Error");
        		alert.setHeaderText("Task failed");
        		alert.setContentText("An error occurred while importing the dataset");
        		
	            alert.showAndWait();
				status.setText("");

        	}
        });
        
		new Thread(task).start();
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
        //to clear selected date after deleting
        fromDate.setValue(null);
        toDate.setValue(null);
       
	}
	
	private FilteredResult filterBackend() {
		List<Document> gender_list = null;
		List<Document> trips_list = null;
		int populateType = 0;

		String year_string = choiceYear.getValue();

		String caption;

		// global statistics
		if (choiceCity.getValue().equals("All") && year_string.equals("All")) {
			return null;
		}
		// Year Only
		else if (choiceCity.getValue().equals("All") && !year_string.equals("All")) {
			gender_list = dm.tripsPerGender(Integer.parseInt(year_string), "trip");
			trips_list = dm.tripsForEachCity(Integer.parseInt(year_string), "trip");
			populateType = 1;
			caption = "Trips for the year " + year_string + " in various cities";
		}
		// City Only
		else if (!choiceCity.getValue().equals("All") && year_string.equals("All")) {
			gender_list = dm.tripsPerGender(choiceCity.getValue(), "trip");
			trips_list = dm.tripsForACity(choiceCity.getValue(), "trip");
			caption = "Trips during the various months of years in " + choiceCity.getValue();
		}
		// Both city and year filter
		else {
			gender_list = dm.tripsPerGender(choiceCity.getValue(), Integer.parseInt(year_string), "trip");
			trips_list = dm.tripsPerCityYear(choiceCity.getValue(), Integer.parseInt(year_string), "trip");
			caption = "Trips during the various months of year " + year_string + " in " + choiceCity.getValue();
		}

		FilteredResult r = new FilteredResult();
		r.setGender_list(gender_list);
		r.setTrips_list(trips_list);
		r.setPopulateType(populateType);
		r.setCaption(caption);

		return r;
	}

	@FXML
	private void filter(ActionEvent event) {
		status.setText("Please wait a moment...");
		progressIndicator.setProgress(-1.0);

		// clear old data chart
		barChart.getData().clear();
		pieChart.getData().clear();
		
		Task<FilteredResult> task = new Task<FilteredResult>() {
			@Override
			public FilteredResult call() {
				return filterBackend();
			}
		};

		task.setOnSucceeded(e -> {
			FilteredResult result = task.getValue();
			if (result != null) {
				if (result.getGender_list() != null)
					populatePieChart(result.getGender_list());

				if (result.getTrips_list() != null) {
					if (result.getPopulateType() == 0)
						populateBarChartPerMonth(result.getTrips_list());
					else
						populateBarChartPerCity(result.getTrips_list());
				}
			} else {
				initChart();
				initPieChart();
			}

			progressIndicator.setProgress(1.0);
			leftChartLabel.setText((result != null) ? result.getCaption() : "Global Trips in the various cities");
			status.setText("Ready.");

		});

		new Thread(task).start();
	}
	
	private void populateBarChartPerMonth(List<Document> data) {
		
		for(int i=1;i<=12;i++) {
			XYChart.Series<String,Integer> series1 = new XYChart.Series<String, Integer>();
		    series1.setName(Integer.toString(i));
		    series1.getData().clear();
		    
		    for(Document document : data) {

			    int month = document.getInteger("month");
			    int trips = (document.getInteger("trips"));
			    
		    	if(month == i)
		    		series1.getData().add(new XYChart.Data<String, Integer>(Integer.toString(month), trips));

	        }
		    
		    if(series1.getData().isEmpty() == true) {
	    		series1.getData().add(new XYChart.Data<String, Integer>(Integer.toString(i), 0));
		    }
		    
	        barChart.getData().add(series1);	
		}
		
	}
	
	private void populateBarChartPerCity(List<Document>data) {
		
		barChart.getData().clear();

        XYChart.Series<String,Integer> series1 = new XYChart.Series<String, Integer>();
        series1.setName("Global Trips");

        for(Document document : data) {
            series1.getData().add(new XYChart.Data<String, Integer>((String)document.get("city"), (Integer)document.get("trips")));

        }

        barChart.getData().add(series1);

	}

	@FXML
	private void changePassword() {
		if (newPassword.getText().equals("")) {
			status.setText("Empty password is not allowed");
			return;
		}
		if (dm.changePassword(this.user, newPassword.getText())) {
			status.setText("Password changed!");
		} else {
			status.setText("An error occurred. Try again.");
		}
	}
	
}
