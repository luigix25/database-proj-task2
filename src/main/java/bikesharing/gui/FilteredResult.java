package bikesharing.gui;

import java.util.List;

import org.bson.Document;

public class FilteredResult {

	enum Type { /* In java everything is a class, even an enum! O_O */
		GLOBAL("Global"),
		CITY_SHOW_MONTH("by City, show months"), CITY_SHOW_YEAR("by City, show years"),
		YEAR_ONLY("by Year"),
		CITY_AND_YEAR("by City and Year"),
		STATION_AND_WEEK("by Station and Week of Year");
		
		private String label;
		Type(String label) {
			this.label = label;
		}		
		public String toString() {
			return label;
		}
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
