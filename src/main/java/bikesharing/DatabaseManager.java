package bikesharing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.codehaus.jackson.map.*;

import com.mongodb.client.*;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;

import static com.mongodb.client.model.Filters.*;

public class DatabaseManager {

	private static DatabaseManager instance = null;
	
	private final ReplicaHost[] hosts = {
		new ReplicaHost("172.16.0.58"),
		new ReplicaHost("172.16.0.59"),
		new ReplicaHost("172.16.0.61")
	};
	
	private String mongoURL;
	 
	// private final String mongoURL = "mongodb://dutcher.robinodds.it:27018,dutcher.robinodds.it:27019,dutcher.robinodds.it:27020/?replicaSet=BSSReplica";
		
	private final String databaseName = "ducange";
	
	private MongoClient mongoClient;
	private MongoDatabase database;
	
	private DatabaseManager() {
		/* prepare URL for connection */
		mongoURL = "mongodb://";
		for (int i = 0; i < hosts.length; ++i) {
			mongoURL += hosts[i].getHostname() + ":" + Integer.toString(hosts[i].getPort());
			if (i != (hosts.length - 1)) mongoURL += ",";
		}
		mongoURL += "/?replicaSet=BSSReplica";
		System.err.println("[D] connecting to URL: " + mongoURL);
		
		/* actually connect */
		mongoClient = MongoClients.create(mongoURL);
		database = mongoClient.getDatabase(databaseName);
	}
	
	public static DatabaseManager getInstance() {
		if(instance == null) {
			instance = new DatabaseManager();
		}
		
		return instance;
	}
	
	public static void close() {
		if(instance == null)
			return;
		instance.mongoClient.close();
	}
	
	public User login(String username, String password) {
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
		if (data == null)
			return false;
		
		List<Document> documents = new ArrayList<Document>();
		for(String json : data) {
			Document doc;
			try {
				doc = Document.parse(json);
			} catch(Exception e) {
				//e.printStackTrace();
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
		
		return fixDate(collectionName);
		
		//return true;
	}
		
	
	private boolean fixDate(String collectionName) {
		
		MongoCollection<Document> collection = database.getCollection(collectionName);
		
		//"time.timestamp_end": {$not: {$type:9}}  filters all the non-date fields

		Document filter = new Document("time.timestamp_end",new Document("$not",new Document("$type",9)));
		

		
		//Updates all the fields inside the collection, so that they use the Date format, instead of String, parsing it using the specified format
		
		List<Bson> pipeline = Arrays.asList(
				Aggregates.addFields(
						new Field<Document>("time",
								new Document("timestamp_start",new Document("$dateFromString",new Document("dateString", "$time.timestamp_start").append("format", "%Y-%m-%d %H:%M:%S")))
								.append("timestamp_end",new Document("$dateFromString",new Document("dateString", "$time.timestamp_end").append("format", "%Y-%m-%d %H:%M:%S")))
								)
					
				)
		);
		
		try {
			collection.updateMany(filter, pipeline);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
		
	}
	
	public List<Document> tripsForEachCity(String collectionName){
		List<Bson> project = new ArrayList<Bson>();
		project.add(Projections.excludeId());
		project.add(Projections.include("trips"));
		project.add(new Document("city","$_id"));
		
		List<Bson> pipeline = Arrays.asList(
				Aggregates.group("$city", Accumulators.sum("trips", 1)),
				Aggregates.project(Projections.fields(project))		
		);
		
		MongoCollection<Document> collection = database.getCollection(collectionName);
		
		MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
		
		ArrayList<Document> result = new ArrayList<Document>();
		
		while(cursor.hasNext()) {
			result.add(cursor.next());
		}
		
		cursor.close();
		System.out.println(result.toString());
		
		return result;
		
	}
	
	public List<Document> tripsPerCityYear(String city,int year,String collectionName){
		
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

		MongoCollection<Document> collection = database.getCollection(collectionName);
		
		MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
		
		ArrayList<Document> result = new ArrayList<Document>();
		
		while(cursor.hasNext()) {
			result.add(cursor.next());
		}
		
		cursor.close();
		System.out.println(result.toString());
		
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
		
		MongoCollection<Document> collection = database.getCollection(collectionName);
		MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
		ArrayList<Document> result = new ArrayList<Document>();
		
		while(cursor.hasNext()) {
			result.add(cursor.next());
		}
		
		cursor.close();
		System.out.println(result.toString());
		
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
	

}
