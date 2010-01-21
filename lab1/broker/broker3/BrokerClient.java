package broker.broker3;

import java.io.*;
import java.net.*;

public class BrokerClient {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		Socket clientSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			/* variables for hostname/port */
			BrokerLocation serverinfo = new BrokerLocation("localhost", 4444, "tse");
			
			if(args.length == 2 ) {
				serverinfo.broker_host = args[0];
				serverinfo.broker_port = Integer.parseInt(args[1]);
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			clientSocket = new Socket(serverinfo.broker_host, serverinfo.broker_port);

			out = new ObjectOutputStream(clientSocket.getOutputStream());
			in = new ObjectInputStream(clientSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;

		//serverinfo.broker_host = "localhost";
		//serverinfo.broker_port = 4444;
		System.out.print(">");
		while ((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("x") == -1) {
			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			packetToServer.type = BrokerPacket.BROKER_REQUEST;
			packetToServer.symbol = userInput;
			
			if (userInput.indexOf("locavl") != -1) {
				//change the server
			}
			out.writeObject(packetToServer);

			/* print server reply */
			BrokerPacket packetFromServer;
			packetFromServer = (BrokerPacket) in.readObject();

			if (packetFromServer.type == BrokerPacket.BROKER_QUOTE) {
				System.out.println("Quote from broker: " + packetFromServer.quote);
			}
			if (packetFromServer.type == BrokerPacket.ERROR_OUT_OF_RANGE) {
				System.out.println(packetFromServer.quote + " invalid.\n");
			}
			/* re-print console prompt */
			System.out.print(">");
		}

		/* tell server that i'm quitting */
		BrokerPacket packetToServer = new BrokerPacket();
		packetToServer.type = BrokerPacket.BROKER_BYE;
		//packetToServer.message = "Bye!";
		out.writeObject(packetToServer);

		out.close();
		in.close();
		stdIn.close();
		clientSocket.close();
	}
}
