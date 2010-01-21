package broker.broker3;

import java.io.*;
import java.net.*;
import java.util.*;

public class BrokerExchange {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		Socket clientSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			/* variables for hostname/port */
			BrokerLocation serverinfo = new BrokerLocation("localhost", 4444, "tse");
			//serverinfo.broker_host = "localhost";
			//serverinfo.broker_port = 4444;
			
			if(args.length == 3 ) {
				serverinfo.broker_host = args[0];
				serverinfo.broker_port = Integer.parseInt(args[1]);
				serverinfo.broker_name = args[2];
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			clientSocket = new Socket(serverinfo.broker_host, serverinfo.broker_port);

			out = new ObjectOutputStream(clientSocket.getOutputStream());
			in = new ObjectInputStream(clientSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connedelimct!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;
		boolean validnum = false;
//		end of int validation
		System.out.print(">");
		while ((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("x") == -1) {
			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			packetToServer.symbol = userInput;
			
			//validate the integer value if present
			StringTokenizer str = new StringTokenizer(userInput);
			validnum = false;
			
			int i = -1;
			
			while(str.hasMoreElements()) {
			    //extract the number here, IF found
				i = Integer.parseInt(str.nextToken());
			}
			
			if ((i <= 300) && (i >= 0)) {
				validnum = true;
			}
			else {
				validnum = false;
			}
			//end of int validation
			
			//Check what does the user want to do: ADD, UPDATE, REMOVE
			if ((userInput.toLowerCase().indexOf("add") != -1) && validnum == true) {
				packetToServer.type = BrokerPacket.EXCHANGE_ADD;
				out.writeObject(packetToServer);
			}
			else if ((userInput.toLowerCase().indexOf("update") != -1) && validnum == true) {
				packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
				out.writeObject(packetToServer);
			}
			else if (userInput.toLowerCase().indexOf("remove") != -1) {
				packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;
				out.writeObject(packetToServer);
			}
			else { //Error for invalid input
				//packetToServer.type = BrokerPacket.ERROR_INVALID_EXCHANGE; // mark as such so that we dont expect a packet later, because we wont send any
				System.out.println("Invalid command.\nUsage: add | update | remove <stock name>\n");
			}
		
			//sending to server
			if (packetToServer.type == BrokerPacket.EXCHANGE_REMOVE || packetToServer.type == BrokerPacket.EXCHANGE_UPDATE || packetToServer.type == BrokerPacket.EXCHANGE_ADD) {
				/* print server reply */
				BrokerPacket packetFromServer;
				packetFromServer = (BrokerPacket) in.readObject();
	
				if (packetFromServer.type == BrokerPacket.EXCHANGE_REPLY) { // will print only if non-NULL has been sent
					System.out.println(packetFromServer.quote);
				}
				else if (packetFromServer.type == BrokerPacket.ERROR_INVALID_SYMBOL){
					System.out.println(packetFromServer.symbol + " invalid.\n");
				}
				else if (packetFromServer.type == BrokerPacket.ERROR_OUT_OF_RANGE){
					System.out.println(packetFromServer.symbol + " out of range.\n");
				}
				else if(packetFromServer.type == BrokerPacket.ERROR_SYMBOL_EXISTS){
					System.out.println(packetFromServer.symbol + " exists.\n");
				}
				/* re-print console prompt */
				System.out.print(">");
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
