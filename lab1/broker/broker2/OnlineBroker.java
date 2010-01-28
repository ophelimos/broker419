import java.io.IOException;
import java.net.ServerSocket;

public class OnlineBroker {

	// Table of quotes file
	private static final String quoteDBFileName = "nasdaq";
	
	public static void main(String[] args) throws IOException {

		ServerSocket serverSocket = null;
		boolean listening = true;
		try {
			if (args.length == 1) {
				serverSocket = new ServerSocket(Integer.parseInt(args[0]));
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

		while (listening) {
			new BrokerServerHandlerThread(serverSocket.accept(), quoteDB).start();
		}

		serverSocket.close();
	}
}
