package bikesharing;

import java.net.URI;
import java.nio.charset.MalformedInputException;
import java.nio.file.*;
import java.util.List;

public class FileManager {

	private Path filePath;
	
	public FileManager(URI path) throws InvalidPathException {
		filePath = Paths.get(path);
	}
	
	public List<String> readLines() {
		
		List<String> lines;
		try {
			lines = Files.readAllLines(filePath);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
				
		return lines;
	}
	
}
