import java.io.IOException;
import java.net.ServerSocket;

/**
 * Thread that runs server shutdown to ensure the connection is cleanly dropped
 * and the database is cleanly flushed
 * 
 * @author robin162
 * 
 */

public class ShutdownThread extends Thread {

	//private ServerSocket serverSocket = null;

	private QuoteDB quoteDB = null;

	public ShutdownThread(ServerSocket serverSocket, QuoteDB quoteDB) {
		super("ShutdownThread");
		//this.serverSocket = serverSocket;
		this.quoteDB = quoteDB;
	}

	public void run() {
		System.out.println("Shutting down cleanly...");

		// Disabled for now, since to really do it properly, it should contact
		// each of the threads and wait for them to shut down before shutting
		// the main socket, which takes more work to implement

		// try {
		// serverSocket.close();
		// } catch (IOException e) {
		// System.out.println("ERROR: Failed to properly close server socket");
		// }

		try {
			quoteDB.close();
		} catch (IOException e) {
			System.out.println("ERROR: Failed to properly close database file");
		}

	}
}