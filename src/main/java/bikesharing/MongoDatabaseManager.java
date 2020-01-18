package bikesharing;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;

public class MongoDatabaseManager {

	private static MongoDatabaseManager instance = null;
		
	private final ServerAddress[] hosts = {
		new ServerAddress("172.16.0.58",27017),
		new ServerAddress("172.16.0.59",27017),
		new ServerAddress("172.16.0.61",27017)
	};
	
	private String mongoURL;
		
	private final String databaseName = "ducange";
	
	private MongoClient mongoClient;
	private MongoDatabase database;
	
	private MongoDatabaseManager() {
		/* prepare URL for connection */
		mongoURL = "mongodb://";
		for (int i = 0; i < hosts.length; ++i) {
			mongoURL += hosts[i].getHost() + ":" + Integer.toString(hosts[i].getPort());
			if (i != (hosts.length - 1)) mongoURL += ",";
		}
		mongoURL += "/?replicaSet=BSSReplica";
		System.err.println("[D] connecting to URL: " + mongoURL);
		
		/* actually connect */
		mongoClient = MongoClients.create(mongoURL);
		database = mongoClient.getDatabase(databaseName);
	}
	
	public static MongoDatabaseManager getInstance() {
		if(instance == null) {
			instance = new MongoDatabaseManager();
		}
		
		return instance;
	}
	
	public static void close() {
		if(instance == null)
			return;
		instance.mongoClient.close();
	}
	
	public final ServerAddress[] getHosts() {
		return this.hosts;
	}

	public User login(String username, String password) {
		//Can't use secondary, because i need consistency
		
		Document d = database.getCollection("user").find(and(eq("username", username), eq("password", password))).first();
		if (d != null) {
			User user = new User();
			user.setName(d.getString("name"));
			user.setSurname(d.getString("surname"));
			user.setUsername(d.getString("username"));
			user.setStatus(d.getString("status"));
			return user;
		}
		return null;
	}
	
	public boolean insertDocument(String data, String collectionName) {
		try {
			Document document = Document.parse(data);
			database.getCollection(collectionName).insertOne(document);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean insertBatch(List<String>data, String collectionName) {
		if (data == null) {
			System.err.println("No Data");
			return false;
		}
			
		List<Document> documents = new ArrayList<Document>();
		for(String json : data) {
			Document doc;
			try {
				doc = Document.parse(json);
				
				Document time = (Document)doc.get("time");
				
				String ts_start = time.getString("timestamp_start");
				String ts_end = time.getString("timestamp_end");
				
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		        
		        LocalDateTime localDate_start = LocalDateTime.parse(ts_start,formatter);
		        LocalDateTime localDate_end = LocalDateTime.parse(ts_end,formatter);       
		        
		        time.put("timestamp_start", localDate_start);
		        time.put("timestamp_end", localDate_end);

		        doc.put("time", time);
		      		        
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
			documents.add(doc);
		}
		
		try {
			database.getCollection(collectionName).insertMany(documents);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
				
		return true;
	}
		
	//AnyTime
	public List<Document> tripsForEachCity(String collectionName){
		List<Bson> project = new ArrayList<Bson>();
		project.add(Projections.excludeId());
		project.add(Projections.include("trips"));
		project.add(new Document("city","$_id"));
		
		List<Bson> pipeline = Arrays.asList(
				Aggregates.group("$city", Accumulators.sum("trips", 1)),
				Aggregates.project(Projections.fields(project))		
		);
		
		MongoCollection<Document> collection = database.getCollection(collectionName).withReadPreference(ReadPreference.secondaryPreferred());
		
		MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
		
		ArrayList<Document> result = new ArrayList<Document>();
		
		while(cursor.hasNext()) {
			result.add(cursor.next());
		}
		
		cursor.close();
		
		return result;
		
	}
	
	//For a Given Year
	public List<Document> tripsForEachCity(int year, String collectionName){
		List<Bson> project = new ArrayList<Bson>();
		project.add(Projections.excludeId());
		project.add(Projections.include("trips"));
		project.add(new Document("city","$_id"));
		
		ArrayList<Field<?>> addYear = new ArrayList<Field<?>>();
		addYear.add(new Field<Document>("year",new Document("$year","$time.timestamp_start")));
		
		List<Bson> pipeline = Arrays.asList(
				Aggregates.addFields(addYear),
				Aggregates.match(new Document("year",year)),
				Aggregates.group("$city", Accumulators.sum("trips", 1)),
				Aggregates.project(Projections.fields(project))		
		);
		
		MongoCollection<Document> collection = database.getCollection(collectionName).withReadPreference(ReadPreference.secondaryPreferred());
		
		MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
		
		ArrayList<Document> result = new ArrayList<Document>();
		
		while(cursor.hasNext()) {
			result.add(cursor.next());
		}
		
		cursor.close();
		
		return result;
		
	}
	
	//Trips for a given city
	// TODD: can be optimized!!!
	public List<Document> tripsForACity(String city, String collectionName){
		List<Bson> project = new ArrayList<Bson>();
		project.add(Projections.excludeId());
		project.add(Projections.include("city"));
		project.add(Projections.include("month"));
		project.add(Projections.include("year"));
		project.add(Projections.include("trips"));

		ArrayList<Field<?>> addDates = new ArrayList<Field<?>>();
		addDates.add(new Field<Document>("year",new Document("$year","$time.timestamp_start")));
		addDates.add(new Field<Document>("month",new Document("$month","$time.timestamp_start")));
		
		List<Bson> pipeline = Arrays.asList(
				//Filter by City
				Aggregates.match(new Document("city",city)),
				Aggregates.addFields(addDates),
				//Group by Month
				Aggregates.group("$month", Accumulators.sum("trips", 1),
						//Otherwise these fields will be lost
						new BsonField("year", new Document("$first","$year")),
						new BsonField("city", new Document("$first","$city")),
						new BsonField("month", new Document("$first","$month"))
				),
				//Sort ASC by month
				Aggregates.sort(new Document("month",1))
		);

		MongoCollection<Document> collection = database.getCollection(collectionName).withReadPreference(ReadPreference.secondaryPreferred());
		
		MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
		
		ArrayList<Document> result = new ArrayList<Document>();
		
		while(cursor.hasNext()) {
			result.add(cursor.next());
		}
		
		cursor.close();
		
		return result;
		
	}
	
	public List<Document> tripsPerCityYear(String city,int year,String collectionName){		//on a monthly basis
		
		List<Bson> project = new ArrayList<Bson>();
		project.add(Projections.excludeId());
		project.add(Projections.include("city"));
		project.add(Projections.include("month"));
		project.add(Projections.include("year"));
		project.add(Projections.include("trips"));

		ArrayList<Field<?>> addDates = new ArrayList<Field<?>>();
		addDates.add(new Field<Document>("year",new Document("$year","$time.timestamp_start")));
		addDates.add(new Field<Document>("month",new Document("$month","$time.timestamp_start")));
		
		List<Bson> pipeline = Arrays.asList(
				//Filter by City
				Aggregates.match(new Document("city",city)),
				Aggregates.addFields(addDates),
				//Filter by Year
				Aggregates.match(new Document("year",year)),
				//Group by Month
				Aggregates.group("$month", Accumulators.sum("trips", 1),
						//Otherwise these fields will be lost
						new BsonField("year", new Document("$first","$year")),
						new BsonField("city", new Document("$first","$city")),
						new BsonField("month", new Document("$first","$month"))
				),
				//Sort ASC by month
				Aggregates.sort(new Document("month",1))
		);

		MongoCollection<Document> collection = database.getCollection(collectionName).withReadPreference(ReadPreference.secondaryPreferred());
		
		MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
		
		ArrayList<Document> result = new ArrayList<Document>();
		
		while(cursor.hasNext()) {
			result.add(cursor.next());
		}
		
		cursor.close();
		
		return result;
		
	}
	
	public List<Document> tripsPerGender(String city, int year, String collectionName){
		List<Bson> projections = new ArrayList<Bson>();
		projections.add(Projections.excludeId());
		projections.add(Projections.include("count"));
		projections.add(new Document("gender","$_id"));
		
		List<Bson> pipeline = Arrays.asList(
				Aggregates.match(Filters.eq("city", city)),
				Aggregates.addFields(new Field<Document>("year",new Document("$year","$time.timestamp_start"))),
				Aggregates.match(Filters.eq("year", year)),
				Aggregates.group("$rider.gender", Accumulators.sum("count", 1)),
				Aggregates.project(Projections.fields(projections))
		);
		
		MongoCollection<Document> collection = database.getCollection(collectionName).withReadPreference(ReadPreference.secondaryPreferred());
		MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
		ArrayList<Document> result = new ArrayList<Document>();
		
		while(cursor.hasNext()) {
			result.add(cursor.next());
		}
		
		cursor.close();
		
		return result;
	}
	
	public List<Document> tripsPerGender(String city, String collectionName){
		List<Bson> projections = new ArrayList<Bson>();
		projections.add(Projections.excludeId());
		projections.add(Projections.include("count"));
		projections.add(new Document("gender","$_id"));
		
		List<Bson> pipeline = Arrays.asList(
				Aggregates.match(Filters.eq("city", city)),
				Aggregates.group("$rider.gender", Accumulators.sum("count", 1)),
				Aggregates.project(Projections.fields(projections))
		);
		
		MongoCollection<Document> collection = database.getCollection(collectionName).withReadPreference(ReadPreference.secondaryPreferred());
		MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
		ArrayList<Document> result = new ArrayList<Document>();
		
		while(cursor.hasNext()) {
			result.add(cursor.next());
		}
		
		cursor.close();
		
		return result;
	}
	
	public List<Document> tripsPerGender(int year, String collectionName){
		List<Bson> projections = new ArrayList<Bson>();
		projections.add(Projections.excludeId());
		projections.add(Projections.include("count"));
		projections.add(new Document("gender","$_id"));
		
		List<Bson> pipeline = Arrays.asList(
				Aggregates.addFields(new Field<Document>("year",new Document("$year","$time.timestamp_start"))),
				Aggregates.match(Filters.eq("year", year)),
				Aggregates.group("$rider.gender", Accumulators.sum("count", 1)),
				Aggregates.project(Projections.fields(projections))
		);
		
		MongoCollection<Document> collection = database.getCollection(collectionName).withReadPreference(ReadPreference.secondaryPreferred());
		MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
		ArrayList<Document> result = new ArrayList<Document>();
		
		while(cursor.hasNext()) {
			result.add(cursor.next());
		}
		
		cursor.close();
		
		return result;
	}
	
	public List<Document> tripsPerGender(String collectionName){
		List<Bson> projections = new ArrayList<Bson>();
		projections.add(Projections.excludeId());
		projections.add(Projections.include("count"));
		projections.add(new Document("gender","$_id"));
		
		List<Bson> pipeline = Arrays.asList(
				Aggregates.group("$rider.gender", Accumulators.sum("count", 1)),
				Aggregates.project(Projections.fields(projections))
		);
		
		MongoCollection<Document> collection = database.getCollection(collectionName).withReadPreference(ReadPreference.secondaryPreferred());
		MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
		ArrayList<Document> result = new ArrayList<Document>();
		
		while(cursor.hasNext()) {
			result.add(cursor.next());
		}
		
		cursor.close();
		
		return result;
	}
	
	public List<User> getAllUsers(){
		
		List<User> users = new ArrayList<User>();
		MongoCursor<Document> cursor = database.getCollection("user").find().iterator();
		while(cursor.hasNext()) {
				Document document = cursor.next();
				User user = new User();
				user.setName((String)document.get("name"));
				user.setSurname((String)document.get("surname"));
				user.setStatus((String)document.get("status"));
				user.setUsername((String)document.get("username"));
				users.add(user);
				
		}
		
		return users;
	}
	
	public int deleteTrips(String city, LocalDate fromDate, LocalDate toDate) {
		Bson filter = and(eq("city", city), gte("time.timestamp_start", fromDate), lte("time.timestamp_start", toDate));
		DeleteResult result = database.getCollection("trip").deleteMany(filter);
		return (int)result.getDeletedCount();
	}
	
	public List<String> getCities(){
		List<String> cities = new ArrayList<String>();
		
		MongoCursor<String> cursor = database.getCollection("trip").distinct("city", String.class).iterator();
		while(cursor.hasNext()) {
			cities.add(cursor.next());
		}
		
		return cities;
	}
	
	public List<Integer> getYears(){
		List<Integer> years = new ArrayList<Integer>();
		
		ArrayList<Field<?>> addYear = new ArrayList<Field<?>>();
		addYear.add(new Field<Document>("year",new Document("$year","$time.timestamp_start")));
		
		List<Bson> pipeline = Arrays.asList(
				//Add Year Field
				Aggregates.addFields(addYear),
				//Group by Year
				Aggregates.group("$year",
						//Renames the _id to year
						new BsonField("year", new Document("$first","$year"))
				),
				//Sort ASC by year
				Aggregates.sort(new Document("year",1))
		);
		
		
		MongoCollection<Document> collection = database.getCollection("trip").withReadPreference(ReadPreference.secondaryPreferred());
		
		MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
		
		
		while(cursor.hasNext()) {
			years.add((Integer) cursor.next().get("year"));
		}
		
		cursor.close();
		
		
		return years;
	}
	
	public boolean insertUser(User user) {
		
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		try {
			String json = ow.writeValueAsString(user);
			return insertDocument(json,"user");
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} 
				
	}
	
	public boolean promoteUser(User user) {
		Document filter = new Document("username",user.getUsername());
		String currentLevel = (String)user.getStatus();
		String newLevel = null;
		
		if(currentLevel.equals("A")) {
			return false;
		} else if(currentLevel.equals("S")) {
			System.out.println("Promoting to Collaborator");
			newLevel = "C";
		} else if(currentLevel.equals("C")) {
			return false; 
		}
		
		List<Bson> pipeline = Arrays.asList(
				Aggregates.addFields(new Field<String>("status",newLevel))
		);
		
		try {
			database.getCollection("user").updateOne(filter, pipeline);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
		
	}
	
	public boolean demoteUser(User user) {
		Document filter = new Document("username",user.getUsername());
		String currentLevel = (String)user.getStatus();
		String newLevel = null;
		
		if(currentLevel.equals("A")) {
			return false;
		} else if(currentLevel.equals("C")) {
			System.out.println("Demoting to Collaborator");
			newLevel = "S";
		} else if(currentLevel.equals("S")) {
			return false; 
		}
		
		List<Bson> pipeline = Arrays.asList(
				Aggregates.addFields(new Field<String>("status",newLevel))
		);
		
		try {
			database.getCollection("user").updateOne(filter, pipeline);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean fire(User user) {
		
		Bson filter = eq("username",user.getUsername());
		String currentLevel = (String)user.getStatus();
		
		if(currentLevel.equals("A")) {
			System.out.println("Firing is not permitted");
			return false;
		
			}
		else {
			DeleteResult result = database.getCollection("user").deleteOne(filter);
			if( (int)result.getDeletedCount()==0)
				return false;	
		}
		
		return true;
	}

	public boolean changePassword(User user, String newPassword) {
		Document filter = new Document("username", user.getUsername());

		List<Bson> pipeline = Arrays.asList(Aggregates.addFields(new Field<String>("password", newPassword)));
		
		try {
			database.getCollection("user").updateOne(filter, pipeline);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}	
	

}
