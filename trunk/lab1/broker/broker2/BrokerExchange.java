import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class BrokerExchange {
	
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		Socket clientSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			/* variables for hostname/port */
			BrokerLocation serverinfo = new BrokerLocation("localhost", 4444);
			// serverinfo.broker_host = "localhost";
			// serverinfo.broker_port = 4444;

			if (args.length == 2) {
				serverinfo.broker_host = args[0];
				serverinfo.broker_port = Integer.parseInt(args[1]);
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			clientSocket = new Socket(serverinfo.broker_host,
					serverinfo.broker_port);

			out = new ObjectOutputStream(clientSocket.getOutputStream());
			in = new ObjectInputStream(clientSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connedelimct!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(
				System.in));
		String userInput;

		// end of int validation
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

					out.writeObject(packetToServer);
				} else if (curword.equalsIgnoreCase("update")) {
					packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;

					// Pull the input
					packetToServer.symbol = inputLine.next();
					packetToServer.quote = inputLine.nextLong();

					out.writeObject(packetToServer);
				} else if (curword.equalsIgnoreCase("remove")) {
					packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;

					// Get the symbol
					packetToServer.symbol = inputLine.next();

					out.writeObject(packetToServer);
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
					packetFromServer = (BrokerPacket) in.readObject();

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

		/* tell server that i'm quitting */
		BrokerPacket packetToServer = new BrokerPacket();
		packetToServer.type = BrokerPacket.BROKER_BYE;
		out.writeObject(packetToServer);

		out.close();
		in.close();
		stdIn.close();
		clientSocket.close();
	}
}
