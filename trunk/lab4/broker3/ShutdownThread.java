import java.io.IOException;

/**
 * Thread that runs server shutdown to ensure the connection is cleanly dropped
 * and the database is cleanly flushed
 * 
 * @author robin162
 * 
 */

public class ShutdownThread extends Thread {

	private QuoteDB quoteDB = null;

	public ShutdownThread(QuoteDB quoteDB) {
		super("ShutdownThread");
		//this.serverSocket = serverSocket;
		this.quoteDB = quoteDB;
	}

	public void run() {
		System.out.println("Shutting down cleanly...");

		try {
			quoteDB.close();
		} catch (IOException e) {
			System.out.println("ERROR: Failed to properly close database file");
		}

	}
}