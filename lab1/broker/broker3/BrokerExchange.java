import java.io.*;
import java.net.*;
import java.util.*;

public class BrokerExchange {
	
	// Global I/O streams
	private static Socket lookupSocket = null;

	private static ObjectOutputStream lookupServerOut = null;

	private static ObjectInputStream lookupServerIn = null;

	private static Socket brokerSocket = null;

	private static ObjectOutputStream brokerServerOut = null;

	private static ObjectInputStream brokerServerIn = null;
	
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		try {
			/* variables for hostname/port */
			BrokerLocation serverinfo = new BrokerLocation("localhost", 4444, "tse");

			if(args.length == 3 ) {
				serverinfo.broker_host = args[0];
				serverinfo.broker_port = Integer.parseInt(args[1]);
				serverinfo.broker_name = args[2];
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			lookupSocket = new Socket(serverinfo.broker_host, serverinfo.broker_port);

			lookupServerOut = new ObjectOutputStream(lookupSocket.getOutputStream());
			lookupServerIn = new ObjectInputStream(lookupSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}
		
		String reqBroker = args[2];
		
		/* Now, use the lookup server to connect to the exchange server */
		try {
			BrokerPacket connectionPacket = new BrokerPacket();
			connectionPacket.type = BrokerPacket.EXCHANGE_ADD;
			connectionPacket.symbol = reqBroker + "brokerreq";
			lookupServerOut.writeObject(connectionPacket);

			// now we receive the response from lookup server
			BrokerPacket lookupResponse;
			lookupResponse = (BrokerPacket) lookupServerIn.readObject();
			if (lookupResponse.type == BrokerPacket.EXCHANGE_REPLY) {
				// Use the given broker location object
				BrokerLocation curBroker = lookupResponse.locations[0];

				try {
					// Connect to the given server
					brokerSocket = new Socket(curBroker.broker_host,
							curBroker.broker_port);

					brokerServerOut = new ObjectOutputStream(brokerSocket
							.getOutputStream());
					brokerServerIn = new ObjectInputStream(brokerSocket
							.getInputStream());
				} catch (IOException e) {
					System.out
							.println("Failed to connect to give broker server.  Nameserver failure?");
				}

				System.out.println(curBroker.broker_name + "is local");
			} else {
				
				// Just print an error message
				System.out.println("Strange packet received from "
						+ lookupResponse.symbol);
			}

		} catch (IOException e) {
			System.err.println("Failed to connect to lookup server!");
			System.exit(1);
		} catch (ClassNotFoundException e) {
			System.err.println("Don't understand input data");
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;
		
		System.out.println("Enter command or 'x' to exit:");
		System.out.print("> ");
		while ((userInput = stdIn.readLine()) != null) {

			try {
				// Scan through the input line
				Scanner inputLine = new Scanner(userInput);

				String curword = inputLine.next();

				/* make a new request packet */
				BrokerPacket packetToServer = new BrokerPacket();

				// Check what does the user want to do: ADD, UPDATE, REMOVE
				if (curword.equalsIgnoreCase("add")) {
					packetToServer.type = BrokerPacket.EXCHANGE_ADD;

					// Get the symbol
					packetToServer.symbol = inputLine.next();

					brokerServerOut.writeObject(packetToServer);
				} else if (curword.equalsIgnoreCase("update")) {
					packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;

					// Pull the input
					packetToServer.symbol = inputLine.next();
					packetToServer.quote = inputLine.nextLong();

					brokerServerOut.writeObject(packetToServer);
				} else if (curword.equalsIgnoreCase("remove")) {
					packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;

					// Get the symbol
					packetToServer.symbol = inputLine.next();

					brokerServerOut.writeObject(packetToServer);
				} else if (curword.equalsIgnoreCase("x")
						|| curword.equalsIgnoreCase("q")
						|| curword.equalsIgnoreCase("quit")) {
					break;
				} else {// Error for invalid input
					InputMismatchException invalid = new InputMismatchException(
							curword);
					throw invalid;
				}

				// sending to server
				if (packetToServer.type == BrokerPacket.EXCHANGE_REMOVE
						|| packetToServer.type == BrokerPacket.EXCHANGE_UPDATE
						|| packetToServer.type == BrokerPacket.EXCHANGE_ADD) {
					/* print server reply */
					BrokerPacket packetFromServer;
					packetFromServer = (BrokerPacket) brokerServerIn.readObject();

					if ((packetFromServer.type == BrokerPacket.EXCHANGE_REPLY)) {

						if (packetFromServer.error_code == BrokerPacket.ERROR_INVALID_SYMBOL) {
							System.out.println(packetFromServer.symbol
									+ " invalid.");
						} else if (packetFromServer.error_code == BrokerPacket.ERROR_OUT_OF_RANGE) {
							System.out.println(packetFromServer.symbol
									+ " out of range.");
						} else if (packetFromServer.error_code == BrokerPacket.ERROR_SYMBOL_EXISTS) {
							System.out.println(packetFromServer.symbol
									+ " exists.");
						} else {
							System.out.println(packetFromServer.symbol + " "
									+ packetFromServer.exchange);
						}
					} else {
						System.out.println("Strange packet received...");
					}
				}
				
				/* re-print console prompt */
				System.out.print("> ");

			} catch (NoSuchElementException e) {
				// Also covers InputMismatchException
				System.out.print("Usage: add <symbol> "
						+ "|| update <symbol> <value> "
						+ "|| remove <symbol>\n");
				/* re-print console prompt */
				System.out.print("> ");
				continue;
			}
		}
		
		/* tell broker server that i'm quitting */
		/* tell lookup server that i'm quitting */
		BrokerPacket packetToServer = new BrokerPacket();
		packetToServer.type = BrokerPacket.BROKER_BYE;
		brokerServerOut.writeObject(packetToServer);
		lookupServerOut.writeObject(packetToServer);
		
		brokerServerOut.close();
		brokerServerIn.close();
		brokerSocket.close();
		
		lookupServerOut.close();
		lookupServerIn.close();
		stdIn.close();
		lookupSocket.close();
	}
}

