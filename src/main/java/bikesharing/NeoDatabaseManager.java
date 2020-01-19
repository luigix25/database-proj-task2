package bikesharing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;

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

	public boolean insertBatch(List<String> data) {
		try (Session session = driver.session()) {
			Transaction tx = session.beginTransaction();
			for (String json : data) {
				Document trip;
				try {
					/* parse json from input file */
					trip = Document.parse(json);
					Document space = (Document) trip.get("space");

					/* create document for both start and end station */
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

					ArrayList<Document> station_list = new ArrayList<Document>();
					station_list.add(station_start);
					station_list.add(station_end);

					for (Document station : station_list) {
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

						tx.run(query, parameters);
						System.err.println("[NEO] " + query);
					}
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}

			tx.success();
		}
		return true;
	}

	@Override
	public void close() throws Exception {
		driver.close();
	}
}
