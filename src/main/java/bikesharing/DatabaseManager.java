package bikesharing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.*;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Projections;

import static com.mongodb.client.model.Filters.*;

public class DatabaseManager {

	private static DatabaseManager instance = null;
	
	//private final String mongoURL = "mongodb://localhost:27017";
	private final String hostname = "dutcher.robinodds.it";
	private final String username = "root";
	private final String password = "prova";

	private final String portNumber = "27017";
	private final String mongoURL = "mongodb://"+username+":"+password+"@"+hostname+":"+portNumber+"/?authSource=admin";
	
	
	private final String databaseName = "ducange";
	
	private MongoClient mongoClient;
	private MongoDatabase database;
	
	private DatabaseManager() {
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
	
	public User login(String id, String password) {
		Document d = database.getCollection("user").find(and(eq("id", id), eq("password", password))).first();
		if (d != null) {
			User user = new User();
			user.setId(d.getString("id"));
			user.setName(d.getString("name"));
			user.setSurname(d.getString("surname"));
			user.setStatus(d.getString("status"));
			return user;
		}
		return null;
	}
	
	public boolean insertDocument(String data, String collectionName) {
		Document document = Document.parse(data);
		try {
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
}
