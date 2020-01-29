package bikesharing;

public class Main {

	public static void main(String[] args) {
		
		MainGUI mainGUI = new MainGUI();
		mainGUI.main(args);
		System.out.println("Closing db connection...");
		DatabaseManager.close();
	}


}
