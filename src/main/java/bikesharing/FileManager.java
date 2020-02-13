package bikesharing;

import java.net.URI;
import java.nio.file.*;
import java.util.List;

public class FileManager {

	private Path filePath;
	
	public FileManager(URI path) throws InvalidPathException {
		filePath = Paths.get(path);
	}
	
	
	//Reads all the lines of the file and put each one of them inside a list of String
	public List<String> readLines() throws Exception {
		
		List<String> lines;
		try {
			lines = Files.readAllLines(filePath);
		} catch(Exception e) {
			System.err.println("[ReadLines] Error reading file");
			throw new Exception("Error Reading File");
		}
				
		return lines;
	}
	
}
