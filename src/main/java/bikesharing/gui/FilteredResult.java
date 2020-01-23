package bikesharing.gui;

import java.util.List;

import org.bson.Document;

public class FilteredResult {
	enum Type {
		GLOBAL, CITY_ONLY, YEAR_ONLY, CITY_AND_YEAR, STATION_AND_WEEK
	};

	Type type = null;
	List<Document> gender_list = null;
	List<Document> trips_list = null;
	String caption;

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

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
	public String getCaption() {
		return this.caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

}
