package bikesharing.gui;

import java.util.List;

import org.bson.Document;

public class FilteredResult {
	List<Document> gender_list = null;
	List<Document> trips_list = null;
	int populateType = 0;

	public List<Document> getGender_list() {
		return gender_list;
	}

	public void setGender_list(List<Document> gender_list) {
		this.gender_list = gender_list;
	}

	public List<Document> getTrips_list() {
		return trips_list;
	}

	public void setTrips_list(List<Document> trips_list) {
		this.trips_list = trips_list;
	}

	public int getPopulateType() {
		return populateType;
	}

	public void setPopulateType(int populateType) {
		this.populateType = populateType;
	}

}
