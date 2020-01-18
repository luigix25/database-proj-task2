package bikesharing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

public class NeoDatabaseManager implements AutoCloseable {
	private static NeoDatabaseManager instance = null;
	
	private final Driver driver;
	private final String hostname = "172.16.0.58";
	private final int port = 7687;

	private NeoDatabaseManager() {
		String uri = "bolt://" + hostname + ":" + port;
		driver = GraphDatabase.driver(uri, AuthTokens.basic("neo4j", "NeoTrinityMorpheus99"));
	}

	public static NeoDatabaseManager getInstance() {
		if (instance == null) {
			instance = new NeoDatabaseManager();
		}
		return instance;
	}
	
	public boolean addStation(Document station) {

		try (Session session = driver.session()) {
			/* set some properties */
			Map<String, Object> properties = new HashMap<>();
			for (String key : station.keySet()) {
				properties.put(key, station.get(key));
			}
			/* set node parameters */
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("properties", properties);
			/* build actual query */
			String query = ("CREATE ($properties)");

			session.writeTransaction(new TransactionWork<Void>() {
				@Override
				public Void execute(Transaction tx) {
					/*
					 * TODO -- this is an inefficient auto-commit transaction, maybe we can improve?
					 * no no via, questa cosa non è inefficiente, è DI MENO... cioè, gli ci vuole 30
					 * secondi per caricare 60K di roba, non so se rendo l'idea
					 */
					StatementResult res = tx.run(query, parameters);
					return null;
				}
			});
		}
		return true;
	}

	public boolean insertBatch(List<String> data) {
		for (String json : data) {
			Document trip;
			try {
				trip = Document.parse(json);
				Document space = (Document) trip.get("space");

				Document station_start = new Document();
				station_start.append("name", space.get("station_start"));
				station_start.append("city", trip.get("city"));
				station_start.append("latitude", space.get("latitude_start"));
				station_start.append("longitude", space.get("longitude_start"));

				Document station_end = new Document();
				station_end.append("name", space.get("station_end"));
				station_end.append("city", trip.get("city"));
				station_end.append("latitude", space.get("latitude_end"));
				station_end.append("longitude", space.get("longitude_end"));

				if (!addStation(station_start))
					return false;
				if (!addStation(station_end))
					return false;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	@Override
	public void close() throws Exception {
		driver.close();
	}
}
