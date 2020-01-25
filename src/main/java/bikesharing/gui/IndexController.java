package bikesharing.gui;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.bson.Document;

import bikesharing.DatabaseManager;
import bikesharing.FileManager;
import bikesharing.User;
import bikesharing.gui.FilteredResult.Type;
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

	@FXML private Label leftChartLabel;
	@FXML private Label rightChartLabel;

	@FXML private ChoiceBox<Type> choiceStat;
	@FXML private ChoiceBox<String> choiceCity;
	@FXML private ChoiceBox<String> choiceStation;
	@FXML private ChoiceBox<String> choiceYear;
	@FXML private TextField choiceWeek;
	@FXML private Button filterButton;

	/* Generic status indicator for the application */
	@FXML private Label status;
	@FXML private ProgressIndicator progressIndicator;
	
	/* User profile tab */
	@FXML private Label welcomeLabel;
	@FXML private TextField newPassword;

	/* Various constants and variables */
	private File currentFile;
	private User user;

	private DatabaseManager dm;

	public void init(User user) {
		/* WBP init */
		dm = DatabaseManager.getInstance();
		setSession(user);
		
		/* GUI init */
		setUpCitySelector();
		setUpYearSelector();
		initTable();
		choiceCity.setOnAction((event) -> {
			this.citySelected();
		});

		choiceStat.getItems().addAll(Type.values());
		choiceStat.setOnAction((event) -> {
			this.statSelected();
		});
		statSelected();
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
		
		for (String city : cities) {
			citySelector.getItems().add(city);
			choiceCity.getItems().add(city);
		}
	}
	
	private void setUpYearSelector() {
		List<Integer> years = dm.getYears();
		choiceYear.getItems().clear();
		
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
	}

	@FXML
	private void promoteUser(ActionEvent event) {
		status.setText("");
		User user = tableView.getSelectionModel().getSelectedItem();
		if(user == null) {
			status.setText("Please select an employee");
			return;
		}

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
				return dm.insertBatch(data) && dm.updateStationRedundancy();
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
	
	private FilteredResult filterBackend(Type type) {
		System.err.println("[D] filter backend crunching for your " + type + "...");
		/* data structs that will be returned to the caller */
		List<Document> gender_list = null;
		List<Document> trips_list = null;
		String caption = null;

		/* get input from filter controls from GUI */
		String city, station;
		city = choiceCity.getValue();
		station = choiceStation.getValue();
		int year, week;
		try {
			year = Integer.parseInt(choiceYear.getValue());
		} catch (NumberFormatException e) {
			year = 0;
		}
		try {
			week = Integer.parseInt(choiceWeek.getText());
		} catch (NumberFormatException e) {
			week = 0;
		}

		/* perform proper database call depending on type of stat */
		switch (type) {
		case GLOBAL:
			caption = "Global Trips in the various cities";
			break;
		case YEAR_ONLY:
			gender_list = dm.tripsPerGender(year, "trip");
			trips_list = dm.tripsForEachCity(year, "trip");
			caption = "Trips for the year " + year + " in various cities";
			break;
		case CITY_SHOW_MONTH:
			gender_list = dm.tripsPerGender(city, "trip");
			trips_list = dm.tripsForACity(city, "trip");
			caption = "Trips during the various months of years in " + city;
			break;
		case CITY_SHOW_YEAR:
			trips_list = dm.tripsForCityDuringYears(city);
			caption = "Trips during the various years in " + city;
			break;
		case CITY_AND_YEAR:
			gender_list = dm.tripsPerGender(city, year, "trip");
			trips_list = dm.tripsPerCityYear(city, year, "trip");
			caption = "Trips during the various months of year " + year + " in " + city;
			break;
		case STATION_AND_WEEK:
			trips_list = dm.tripsPerStationWeek(choiceCity.getValue(), choiceStation.getValue(), year, week);
			caption = "Trips during week " + week + " of year " + year + " at station " + station + " in " + city;
			break;
		}

		System.err.println("[D] backend is now giving back results...");

		FilteredResult r = new FilteredResult();
		r.setGender_list(gender_list);
		r.setTrips_list(trips_list);
		r.setType(type);
		r.setCaption(caption);

		return r;
	}

	@FXML
	private void filter(ActionEvent event) {
		status.setText("Please wait, be patient and have faith: the database is now crunching for you..." + "☺");
		progressIndicator.setProgress(-1.0);

		Type type = choiceStat.getValue();

		System.err.println("[D] chosen filter is " + type);

		Task<FilteredResult> task = new Task<FilteredResult>() {
			@Override
			public FilteredResult call() {
				return filterBackend(type);
			}
		};

		task.setOnSucceeded(e -> {
			System.err.println("[D] receiving data from backend...");
			// clear old data chart
			barChart.getData().clear();
			pieChart.getData().clear();

			FilteredResult result = task.getValue();

			switch (result.getType()) {
				case GLOBAL:
					initChart();
					initPieChart();
					break;
				case YEAR_ONLY:
					populatePieChart(result.getGender_list());
					populateBarChartPerCity(result.getTrips_list());
					break;
				case CITY_SHOW_MONTH:
					populatePieChart(result.getGender_list());
					populateBarChartPerMonth(result.getTrips_list());
					break;
				case CITY_SHOW_YEAR:
					populateBarChartPerYear(result.getTrips_list());
					break;
				case CITY_AND_YEAR:
					populatePieChart(result.getGender_list());
					populateBarChartPerMonth(result.getTrips_list());
					break;
				case STATION_AND_WEEK:
					populateBarChartPerWeek(result.getTrips_list());
					break;
			}

			progressIndicator.setProgress(1.0);
			leftChartLabel.setText(result.getCaption());
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
	
	private void populateBarChartPerWeek(List<Document> data) {
		final String dowNames[] = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };

		for (int i = 1; i <= 7; i++) {
			XYChart.Series<String, Integer> series1 = new XYChart.Series<String, Integer>();
			series1.setName(Integer.toString(i));
			series1.getData().clear();

			for (Document document : data) {

				int dow = document.getInteger("_id");
				int trips = document.getInteger("count");

				if (dow == i)
					series1.getData().add(new XYChart.Data<String, Integer>(dowNames[dow - 1], trips));

			}

			if (series1.getData().isEmpty() == true) {
				series1.getData().add(new XYChart.Data<String, Integer>(Integer.toString(i), 0));
			}

			barChart.getData().add(series1);
		}

	}

	private void populateBarChartPerYear(List<Document> data) {
		int base_year = Integer.MAX_VALUE;

		for (Document document : data) {
			int year = document.getInteger("_id");
			if (year < base_year)
				base_year = year;
		}

		for (int i = base_year; i < base_year + data.size(); ++i) {
			XYChart.Series<String, Integer> series1 = new XYChart.Series<String, Integer>();
			series1.setName(Integer.toString(i));
			series1.getData().clear();

			for (Document document : data) {
				int year = document.getInteger("_id");
				int count = document.getInteger("count");

				if (year == i)
					series1.getData().add(new XYChart.Data<String, Integer>(Integer.toString(i), count));
			}

			if (series1.getData().isEmpty() == true) {
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
	
	@FXML
	private void citySelected() {
		choiceStation.getItems().clear();
		
		progressIndicator.setProgress(-1.0);
		status.setText("Loading bike stations for " + choiceCity.getValue());
		
		Task<List<String>> task = new Task<List<String>>() {
			@Override
			public List<String> call() {
				return dm.getStationsForCity(choiceCity.getValue());
			}
		};

		task.setOnSucceeded(e -> {
			List<String> stations = task.getValue();
			if (stations == null) {
				status.setText("There are no bike stations in " + choiceCity.getValue());
				return;
			}

			for (String station : stations) {
				choiceStation.getItems().add(station);
			}
			
			progressIndicator.setProgress(1.0);
			status.setText("Loaded bike stations for " + choiceCity.getValue());
		});

		new Thread(task).start();

	}

	@FXML
	private void statSelected() {
		/* enables/disables controls based on selected desired stats */
		if (choiceStat.getValue() == null) {
			choiceCity.setDisable(true);
			choiceStation.setDisable(true);
			choiceYear.setDisable(true);
			choiceWeek.setDisable(true);
			filterButton.setDisable(true);
			return;
		}

		filterButton.setDisable(false);

		switch (choiceStat.getValue()) {
		case GLOBAL:
			choiceCity.setDisable(true);
			choiceStation.setDisable(true);
			choiceYear.setDisable(true);
			choiceWeek.setDisable(true);
			break;
		case CITY_SHOW_MONTH:
		case CITY_SHOW_YEAR:
			choiceCity.setDisable(false);
			choiceStation.setDisable(true);
			choiceYear.setDisable(true);
			choiceWeek.setDisable(true);
			break;
		case YEAR_ONLY:
			choiceCity.setDisable(true);
			choiceStation.setDisable(true);
			choiceYear.setDisable(false);
			choiceWeek.setDisable(true);
			break;
		case CITY_AND_YEAR:
			choiceCity.setDisable(false);
			choiceStation.setDisable(true);
			choiceYear.setDisable(false);
			choiceWeek.setDisable(true);
			break;
		case STATION_AND_WEEK:
			choiceCity.setDisable(false);
			choiceStation.setDisable(false);
			choiceYear.setDisable(false);
			choiceWeek.setDisable(false);
			break;
		}

	}

}
