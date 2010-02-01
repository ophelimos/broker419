import java.net.*;
import java.io.*;

public class BrokerLookupServer {
	
	public static void main(String[] args) throws IOException {
	
		//We have 4 Broker Location objects to hold the two broker and two exchange info.
		BrokerLocation NSEBroker = null;
		BrokerLocation NSEEx = null;
		BrokerLocation TSEBroker = null;
		BrokerLocation TSEEx = null;
		
		ServerSocket namesvrSocket = null;
		Socket clientsocket = null;
		BrokerPacket packetFromClient;
		boolean gotByePacket = false;
		
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
		while(true) {
			try {//Running state of the server
				/* stream to read from client */
				clientsocket = namesvrSocket.accept();
				//We now have a connection
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
	
					/* process messages */
					/* We will use toe following protocol:
					 * 
					 * -> All the brokers will connect to the server first and provide their info
					 *  using the brokerLocation() packet, thus the lookup server will store an array of brokerLocation() 
					 *  
					 * -> After all connections have been made, lookup server will be ready to forward connections to all the elements
					 * 
					 * ->  
					 */
					
					//This block handles requests from the brokers
					if (packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
	
						packetToClient.type = BrokerPacket.BROKER_QUOTE;
						
						if (packetFromClient.symbol.compareToIgnoreCase("nseregister") == 0) {
							
							NSEBroker = packetFromClient.locations[0];
							packetToClient.symbol = "registered";
						}
						if (packetFromClient.symbol.compareToIgnoreCase("tseregister") == 0) {
							
							TSEBroker = packetFromClient.locations[0];
							packetToClient.symbol = "registered";
						}
						
						if (packetFromClient.symbol.compareToIgnoreCase("nseexchangereq") == 0) {
							
							packetToClient.num_locations = 1;
							packetToClient.locations[0] = NSEEx;
							packetToClient.symbol = "exchangelocated";
						}
						if (packetFromClient.symbol.compareToIgnoreCase("tseexchangereq") == 0) {
							
							packetToClient.num_locations = 1;
							packetToClient.locations[0] = TSEEx;
							packetToClient.symbol = "exchangelocated";
						}
	
						/* send reply back to client */
						toClient.writeObject(packetToClient);
	
						/* wait for next packet */
						continue;
					}
					
					// this block handles requests from the exchange servers
					if (packetFromClient.type == BrokerPacket.EXCHANGE_ADD) {
						
						packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
	
						// Look up the given symbol in the table
						if (packetFromClient.symbol.compareToIgnoreCase("nseregister") == 0) {
							
							NSEEx = packetFromClient.locations[0];
							packetToClient.symbol = "registered";
						}
						if (packetFromClient.symbol.compareToIgnoreCase("tseregister") == 0) {
							
							TSEEx = packetFromClient.locations[0];
							packetToClient.symbol = "registered";
						}
						
						if (packetFromClient.symbol.compareToIgnoreCase("nsebrokerreq") == 0) {
							
							packetToClient.num_locations = 1;
							packetToClient.locations[0] = NSEBroker;
							packetToClient.symbol = "exchangelocated";
						}
						if (packetFromClient.symbol.compareToIgnoreCase("tsebrokerreq") == 0) {
							
							packetToClient.num_locations = 1;
							packetToClient.locations[0] = TSEBroker;
							packetToClient.symbol = "exchangelocated";
						}
						/* send reply back to client */
						toClient.writeObject(packetToClient);
	
						/* wait for next packet */
						continue;
					}
					
					/* Sending an BROKER_NULL || BROKER_BYE means quit */
					if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
						gotByePacket = true;
						packetToClient.type = BrokerPacket.BROKER_BYE;
						toClient.writeObject(packetToClient);
						break;
					}
					
					/* if code comes here, there is an error in the packet */
					System.err.println("ERROR: Unknown ECHO_* packet!!");
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
}
