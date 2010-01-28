import java.io.IOException;
import java.net.ServerSocket;

public class OnlineBroker {

	// Table of quotes file
	private static final String quoteDBFileName = "nasdaq";
	
	public static void main(String[] args) throws IOException {

		int port = 0;
		ServerSocket serverSocket = null;
		boolean listening = true;
		try {
			if (args.length == 1) {
				port = Integer.parseInt(args[0]);
				serverSocket = new ServerSocket(port);
			} else {
				System.err.println("Usage: OnlineBroker <port>");
				System.exit(-1);
			}
		} catch (IOException e) {
			System.err.println("ERROR: Could not listen on port!");
			System.exit(-1);
		}
		
		// Open the local database
		QuoteDB quoteDB = new QuoteDB(quoteDBFileName);

		System.out.println("Accepting new connections on port " + port);
		while (listening) {
			new BrokerServerHandlerThread(serverSocket.accept(), quoteDB).start();
		}

		serverSocket.close();
	}
}
