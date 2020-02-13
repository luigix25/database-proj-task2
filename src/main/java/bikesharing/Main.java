package bikesharing;

public class Main {

	public static void main(String[] args) {
		
		//Start the Main which handles the GUI
		MainGUI mainGUI = new MainGUI();
		mainGUI.main(args);
		System.out.println("Closing db connection...");
		DatabaseManager.close();
	}


}
