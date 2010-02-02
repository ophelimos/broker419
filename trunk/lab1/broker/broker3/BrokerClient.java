import java.io.*;
import java.net.*;
import java.util.Scanner;

public class BrokerClient {

	private static BrokerLocation curBroker = null;

	// Global I/O streams
	private static Socket lookupSocket = null;

	private static ObjectOutputStream lookupServerOut = null;

	private static ObjectInputStream lookupServerIn = null;

	private static Socket brokerSocket = null;

	private static ObjectOutputStream brokerServerOut = null;

	private static ObjectInputStream brokerServerIn = null;

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		// Get Lookup Server variables (command-line arguments)
		try {
			/* variables for hostname/port */
			BrokerLocation lookupServerInfo = new BrokerLocation("localhost",
					4444, "BrokerLookupServer");

			if (args.length == 2) {
				lookupServerInfo.broker_host = args[0];
				lookupServerInfo.broker_port = Integer.parseInt(args[1]);
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			lookupSocket = new Socket(lookupServerInfo.broker_host,
					lookupServerInfo.broker_port);

			lookupServerOut = new ObjectOutputStream(lookupSocket
					.getOutputStream());
			lookupServerIn = new ObjectInputStream(lookupSocket
					.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(
				System.in));
		String userInput;

		System.out.print("> ");
		while ((userInput = stdIn.readLine()) != null
				&& userInput.toLowerCase().indexOf("x") == -1) {

			if (userInput.indexOf("local") != -1) {
				// Scan the server word out
				Scanner inputLine = new Scanner(userInput);
				// Skip the "local" part
				inputLine.next();

				if (inputLine.hasNext()) {
					lookupBroker(inputLine.next());
				}

				// Wait for more input
				continue;
			}

			// Make sure we actually have one
			if (curBroker == null) {
				System.out
						.println("Need to select a broker (use the 'local' command)");
				// Wait for more input
				continue;
			}

			/* Otherwise, just a normal server request ** */

			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			packetToServer.type = BrokerPacket.BROKER_REQUEST;
			packetToServer.symbol = userInput;

			lookupServerOut.writeObject(packetToServer);

			/* print server reply */
			BrokerPacket packetFromServer;
			packetFromServer = (BrokerPacket) lookupServerIn.readObject();

			if (packetFromServer.type == BrokerPacket.BROKER_QUOTE) {
				System.out.println("Quote from broker: "
						+ packetFromServer.quote);
			}
			if (packetFromServer.type == BrokerPacket.ERROR_OUT_OF_RANGE) {
				System.err.println(packetFromServer.quote + " invalid.\n");
			}
			/* re-print console prompt */
			System.out.print("> ");
		}

		/* tell both servers that i'm quitting */
		BrokerPacket packetToServer = new BrokerPacket();
		packetToServer.type = BrokerPacket.BROKER_BYE;
		// packetToServer.message = "Bye!";
		if (brokerServerOut != null) {
			brokerServerOut.writeObject(packetToServer);
			// Disconnect from the current broker
			brokerServerOut.close();
			brokerServerIn.close();
			brokerSocket.close();
		}
		lookupServerOut.writeObject(packetToServer);

		lookupServerOut.close();
		lookupServerIn.close();
		stdIn.close();
		lookupSocket.close();
	}

	private static void lookupBroker(String reqBroker) {
		// Now we get the actual broker information
		// We will use the curBroker to determine which broker server to request
		// to the lookup server

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
				curBroker = lookupResponse.locations[0];

				// Disconnect from the current broker
				brokerServerOut.close();
				brokerServerIn.close();
				brokerSocket.close();

				try {
					// Connect to the given server
					brokerSocket = new Socket(curBroker.broker_host,curBroker.broker_port);

					brokerServerOut = new ObjectOutputStream(brokerSocket.getOutputStream());
					brokerServerIn = new ObjectInputStream(brokerSocket.getInputStream());
				} catch (IOException e) {
					System.out.println("Failed to connect to give broker server.  Nameserver failure?");
				}

				System.out.println(curBroker.broker_name + "is local");
			} else /*
					 * if (lookupResponse.type ==
					 * BrokerPacket.ERROR_INVALID_EXCHANGE)
					 */{
				// Error: the Broker server is probably not up yet

				// Just print an error message
				System.out.println("Strange packet received from " + lookupResponse.symbol);
			}

		} catch (IOException e) {
			System.err.println("Failed to connect to lookup server!");
			System.exit(1);
		} catch (ClassNotFoundException e) {
			System.err.println("Don't understand input data");
		}

	}
}
