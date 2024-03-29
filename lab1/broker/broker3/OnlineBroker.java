import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class OnlineBroker {

	// Table of quotes file
	public static String quoteDBFileName = "nasdaq";
	
	public static BrokerLocation lookupServerInfo;

	public static void main(String[] args) throws IOException {

		if (args.length != 4) {
			System.err
					.println("Usage: server.sh <lookupServer hostname> <lookupServer port> <port> <name>");
			System.exit(-1);
		}

		// First start listening myself
		int port = 0;
		ServerSocket serverSocket = null;
		boolean listening = true;
		try {
			port = Integer.parseInt(args[2]);
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("ERROR: Could not listen on port!");
			System.exit(-1);
		}

		// Next open the local database
		quoteDBFileName = args[3];
		QuoteDB quoteDB = new QuoteDB(quoteDBFileName);

		// Finally, let the lookup server know where we are
		try {
			Socket lookupSocket = null;
			ObjectOutputStream lookupServerOut = null;
			ObjectInputStream lookupServerIn = null;
			lookupServerInfo = new BrokerLocation("localhost",
					4444, "BrokerLookupServer");

			lookupServerInfo.broker_host = args[0];
			lookupServerInfo.broker_port = Integer.parseInt(args[1]);

			lookupSocket = new Socket(lookupServerInfo.broker_host,
					lookupServerInfo.broker_port);

			lookupServerOut = new ObjectOutputStream(lookupSocket
					.getOutputStream());
			lookupServerIn = new ObjectInputStream(lookupSocket
					.getInputStream());

			// Actually talk to the lookup server
			BrokerPacket serverRegistration = new BrokerPacket();
			serverRegistration.type = BrokerPacket.BROKER_REQUEST;
			if (args[3].equalsIgnoreCase("nasdaq")) {
				serverRegistration.symbol = "nasdaqregister";
			} else if (args[3].equalsIgnoreCase("tse")) {
				serverRegistration.symbol = "tseregister";
			} else {
				serverRegistration.symbol = args[3] + "register";
			}

			// Get my registration info
			try {
				InetAddress addr = InetAddress.getLocalHost();
				// Get hostname
				String hostname = addr.getCanonicalHostName();
				Integer serverPort = Integer.parseInt(args[2]);
				String exchangeName = args[3];
				serverRegistration.locations = new BrokerLocation[1];
				serverRegistration.locations[0] = new BrokerLocation(hostname,
						serverPort, exchangeName);
			} catch (UnknownHostException e) {
				System.err.println("ERR: I don't know who I am!");
				System.exit(1);
			}

			lookupServerOut.writeObject(serverRegistration);
			// Receive the ack
			BrokerPacket packetFromServer;
			packetFromServer = (BrokerPacket) lookupServerIn.readObject();

			if (!packetFromServer.symbol.equalsIgnoreCase("registered")) {
				// Something went wrong
				System.err
						.println("Didn't receive proper name server response");
				System.exit(1);
			}

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		} catch (ClassNotFoundException e) {
			System.err.println("ERROR: Couldn't understand server response.");
			System.exit(1);
		}

		// Make things gets closed properly on exit
		Runtime.getRuntime().addShutdownHook(
				new ShutdownThread(serverSocket, quoteDB));

		System.out.println("Accepting new connections on port " + port);
		while (listening) {
			new BrokerServerHandlerThread(serverSocket.accept(), quoteDB)
					.start();
		}

		// Done in the shutdown thread
		// serverSocket.close();
	}
}
