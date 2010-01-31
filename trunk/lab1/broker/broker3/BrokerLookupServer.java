import java.net.*;
import java.io.*;


public class BrokerLookupServer {
	
	public static void main(String[] args) throws IOException {
	
		ServerSocket namesvrSocket = null;
		Socket clientsocket = null;
		BrokerPacket packetFromClient;
		boolean gotByePacket = false;
		//boolean listening = true;
		try {
			if (args.length == 1) {
				namesvrSocket = new ServerSocket(Integer.parseInt(args[0]));
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
		} catch (IOException e) {
			System.err.println("ERROR: Could not listen on port!");
			System.exit(-1);
		}

		try {//Running state of the server
			/* stream to read from client */
			clientsocket = namesvrSocket.accept();
			//We now have a coonection
			//Get input from the client side
			ObjectInputStream fromClient = new ObjectInputStream(clientsocket.getInputStream());
			ObjectOutputStream toClient = new ObjectOutputStream(clientsocket.getOutputStream());
			
			while ((packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */
				BrokerPacket packetToClient = new BrokerPacket();
				// For sanity, always copy the symbol and quote over
				packetToClient.symbol = packetFromClient.symbol;
				packetToClient.quote = packetFromClient.quote;
				packetToClient.error_code = 0;

				/* process message */
				if (packetFromClient.type == BrokerPacket.BROKER_REQUEST) {

					packetToClient.type = BrokerPacket.BROKER_QUOTE;

					// Look up the given symbol in the hash table
					packetToClient.quote = /// ========== implement this

					if (packetToClient.quote == null) {
						packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
					}

					/* send reply back to client */
					toClient.writeObject(packetToClient);

					/* wait for next packet */
					continue;
				}
				
				/* Sending an BROKER_NULL || BROKER_BYE means quit */
				if (packetFromClient.type == BrokerPacket.BROKER_NULL
						|| packetFromClient.type == BrokerPacket.BROKER_BYE) {
					gotByePacket = true;
					packetToClient.type = BrokerPacket.BROKER_BYE;
					toClient.writeObject(packetToClient);
					break;
				}
				System.exit(-1);
			}
		//Close all client connections now
		toClient.close();
		fromClient.close();
		clientsocket.close();
		}
		catch (IOException e) {
			if (!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if (!gotByePacket)
				e.printStackTrace();
		}
	}
}
