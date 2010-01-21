import java.io.*;
import java.net.*;

public class BrokerExchange {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		Socket brokerSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			/* variables for hostname/port */
			// Hardcoded not allowed, but these will _never_ be used, and only
			// exist for debugging
			String hostname = "localhost";
			int port = 4444;

			if (args.length == 2) {
				hostname = args[0];
				port = Integer.parseInt(args[1]);
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			brokerSocket = new Socket(hostname, port);

			out = new ObjectOutputStream(brokerSocket.getOutputStream());
			in = new ObjectInputStream(brokerSocket.getInputStream());

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

		System.out.println("Enter queries or x for exit:");
		System.out.print("> ");
		
		try {
			while ((userInput = stdIn.readLine()) != null
					&& userInput.toLowerCase().indexOf("bye") == -1) {

				// If we receive "x", quit
				if (userInput.equalsIgnoreCase("x")) {
					break;
				}

				/* make a new request packet */
				BrokerPacket packetToServer = new BrokerPacket();
				packetToServer.type = BrokerPacket.BROKER_REQUEST;
				packetToServer.symbol = userInput;
				out.writeObject(packetToServer);

				/* print server reply */
				BrokerPacket packetFromServer;
				packetFromServer = (BrokerPacket) in.readObject();

				String printQuote = "0";
				if (packetFromServer.type == BrokerPacket.BROKER_QUOTE) {
					// Sanitize received quote
					if (packetFromServer.quote != null) {
						printQuote = String.valueOf(packetFromServer.quote);
					}
						
					System.out.println("Quote from broker: "
							+ printQuote);
				}
					

				/* re-print console prompt */
				System.out.print("> ");
			}

			/* tell server that i'm quitting */
			BrokerPacket packetToServer = new BrokerPacket();
			packetToServer.type = BrokerPacket.BROKER_BYE;
			packetToServer.symbol = "Bye!";
			out.writeObject(packetToServer);

		} catch (SocketException e) {
			System.out.println("Server has disconnected");
		}

		out.close();
		in.close();
		stdIn.close();
		brokerSocket.close();
	}
}
