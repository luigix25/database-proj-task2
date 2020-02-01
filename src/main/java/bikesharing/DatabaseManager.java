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
import java.util.Collections;
import java.util.List;

import org.bson.BsonArray;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.TransactionBody;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;

public class DatabaseManager {

	private static DatabaseManager instance = null;
		
	private final ServerAddress[] hosts = {
		new ServerAddress("172.16.0.58",27017),
		new ServerAddress("172.16.0.59",27017),
		new ServerAddress("172.16.0.61",27017)
	};
	
	private String mongoURL;
		
	private final String databaseName = "ducange";
	
	private MongoClient mongoClient;
	private MongoDatabase database;
	
	private DatabaseManager() {
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
		} catch(MongoException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean insertBatch(List<String> data) {
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
		
		final ClientSession clientSession = mongoClient.startSession();
				
		ReplaceOptions options = new ReplaceOptions();
		options.upsert(true);
		
		for(Document doc : documents) {
		
			TransactionBody<String> txnBody = new TransactionBody<String>() {
			    public String execute() {
			        MongoCollection<Document> trip = database.getCollection("trip");
			        MongoCollection<Document> station = database.getCollection("station");
			        
			        Document space = (Document)doc.get("space");
	
			        Document station_start = null;
			        Document station_end = null;
			        
			        if(space.containsKey("station_start")) {
			        	station_start = new Document().append("city", doc.getString("city")).append("name", space.getString("station_start"));
			        }
			        
			        if(space.containsKey("station_end")) {
			        	station_end = new Document().append("city", doc.getString("city")).append("name", space.getString("station_end"));
			        }
			        			        
			        //This one will be catched by the caller
			        trip.insertOne(clientSession,doc);
			        
			        if(station_start != null)
			        	station.replaceOne(station_start, station_start, options);
			    
			        if(station_end != null)
			        	station.replaceOne(station_end, station_end, options);
			        
			        return "";
			    }
			};
			try {
			    /*
			       Step 4: Use .withTransaction() to start a transaction,
			       execute the callback, and commit (or abort on error).
			    */
	
			    clientSession.withTransaction(txnBody);
			} catch (RuntimeException e) {
				e.printStackTrace();
				return false;
			} 
		}
		
		clientSession.close();
				
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean updateStationRedundancy() {
		List<Document> documents = new ArrayList<Document>();

		for (String city : getCities()) {
			database.getCollection("station").deleteMany(eq("city", city));
			System.err.println("[D] preparing redundancy for " + city + " ");

			List<Bson> pipeline = Arrays.asList(
					new Document("$match", new Document("city", city)),
					new Document("$set", new Document("stations", new BsonArray(Arrays.asList(new BsonString("$space.station_start"), new BsonString("$space.station_end"))))),
					new Document("$project", new Document("stations", 1)),
			        // new Document("$limit", 100), // TODO
			        new Document("$group", new Document("_id", null).append("s", new Document("$addToSet", new Document("$arrayElemAt", new BsonArray(Arrays.asList(new BsonString("$stations"), new BsonInt32(0))))))));

			AggregateIterable<Document> output = database.getCollection("trip").aggregate(pipeline);
			List<String> stations = null;
			if (output != null) {
				Document result = output.first();
				if (((ArrayList<String>) (result.get("s"))).get(0) != null) {/* safe type conversion, as long as database is not screwed up */
					stations = result.getList("s", String.class);
				}
			}
			if (stations == null || stations.isEmpty() || stations.get(0) == null) {
				System.err.println("EMPTY");
				continue;
			}

			for (String station : stations) {
				Document station_doc = new Document();
				station_doc.append("name", station).append("city", city);
				documents.add(station_doc);
			}

			System.err.println("OK");
		}

		try {
			database.getCollection("station").insertMany(documents);
		} catch (Exception e) {
			return false;
		}

		return true;
	}
	
	public List<String> getStationsForCity(String city) {
		List<Bson> pipeline = Arrays.asList(
				new Document("$match", new Document("city", city))
				);
		AggregateIterable<Document> output = database.getCollection("station").aggregate(pipeline);
		
		if (output == null)
			return null;
		
		MongoCursor<Document> cursor = output.cursor();
		List<String> stations = new ArrayList<String>();
		while (cursor.hasNext()) {
			Document doc = cursor.next();
			stations.add(doc.getString("name"));
		}

		cursor.close();

		if (stations.isEmpty())
			return null;
		
		Collections.sort(stations, (d1, d2) -> {
			return Integer.parseInt(d1.substring(4)) - Integer.parseInt(d2.substring(4));
		});
				
		return stations;
	}
	

	//AnyTime
	public List<Document> tripsForEachCity(String collectionName) {
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
	
	// trips for each city for various years
	public List<Document> tripsForCityDuringYears(String city) {
		ArrayList<Field<?>> addYear = new ArrayList<Field<?>>();
		addYear.add(new Field<Document>("year", new Document("$year", "$time.timestamp_start")));

		List<Bson> pipeline = Arrays.asList(Aggregates.match(new Document("city", city)), Aggregates.addFields(addYear), Aggregates.group("$year", Accumulators.sum("count", 1)));

		MongoCursor<Document> cursor = database.getCollection("trip").aggregate(pipeline).iterator();

		List<Document> result = new ArrayList<Document>();

		while (cursor.hasNext()) {
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
	// TODO: can be optimized!!!
	
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
		
		MongoCursor<String> cursor = database.getCollection("station").distinct("city", String.class).iterator();
		while(cursor.hasNext()) {
			cities.add(cursor.next());
		}
		
		return cities;
	}
	
	/*public List<Integer> getYears(){
		List<Integer> years = new ArrayList<Integer>();
		
		ArrayList<Field<?>> addYear = new ArrayList<Field<?>>();
		//addYear.add(new Field<Document>("year",new Document("$year","$time.timestamp_start")));
		
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
	}*/
	
	public List<Integer> getYears(){

		List<Integer> years = new ArrayList<Integer>();
		MongoCursor<Integer> cursor = database.getCollection("trip").distinct("time.year", Integer.class).iterator();
		
		while(cursor.hasNext()) {
			int year = cursor.next();									
			years.add(year);
		}
		
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
			newLevel = "C";
		} else if(currentLevel.equals("C")) {
			return false; 
		}
		
		List<Bson> pipeline = Arrays.asList(
		        Aggregates.addFields(new Field<String>("status", newLevel))
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

	public List<Document> tripsPerStationWeek(String city, String station, int year, int week) {
		List<Bson> pipeline = Arrays.asList(
				new Document("$match", new Document("city", city).append("space.station_start", station)),
				new Document("$set", new Document("time", "$time.timestamp_start")),
				new Document("$set", new Document("doy",
						new Document("$dayOfYear", "$time"))
						.append("dow", new Document("$dayOfWeek", "$time"))
						.append("year", new Document("$year", "$time"))),
				new Document("$set", new Document("week", new Document("$floor", new Document("$divide", new BsonArray(Arrays.asList(new BsonString("$doy"), new BsonInt32(7))))))),
				new Document("$match", new Document("week", week).append("year", year)),
				new Document("$group", new Document("_id", "$dow").append("count", new Document("$sum", 1)))
		);
		
		AggregateIterable<Document> output = database.getCollection("trip").aggregate(pipeline);
		System.err.println("[D] Done");
		
		MongoCursor<Document> cursor = output.cursor();
		
		List<Document> list = new ArrayList<Document>();

		while (cursor.hasNext()) {
			list.add(cursor.next());
		}

		cursor.close();

		return list;
	}	
	

}
