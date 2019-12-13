package bikesharing;

public class ReplicaHost {
	private	String hostname;
	private int port;
	
	public ReplicaHost(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}	
	public ReplicaHost(String hostname) {
		this(hostname, 27017);
	}
	
	
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}	
}
