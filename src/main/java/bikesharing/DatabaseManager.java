package bikesharing;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.*;

public class DatabaseManager {

	private static DatabaseManager instance = null;
	
	private final String mongoURL = "mongodb://localhost:27017"; 
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
		List<Document> documents = new ArrayList<Document>();
		for(String json : data) {
			Document doc;
			try {
				doc = Document.parse(json);
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
	
}
