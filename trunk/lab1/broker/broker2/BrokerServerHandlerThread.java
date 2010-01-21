package broker.broker2;

import java.net.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.NoSuchElementException;

public class BrokerServerHandlerThread extends Thread {
	private Socket socket = null;

	// Table of quotes file
	private static final String quoteDBFileName = "nasdaq";

	public BrokerServerHandlerThread(Socket socket) {
		super("BrokerServerHandlerThread");
		this.socket = socket;
		System.out.println("Created new Thread to handle client");
	}

	public void run() {

		// Initial DB read and setup
		QuoteDB quoteDB = new QuoteDB;
		OnlineBroker.readQuoteDB(quoteDBFileName);

		boolean gotByePacket = false;

		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket
					.getInputStream());
			BrokerPacket packetFromClient;

			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket
					.getOutputStream());

			while ((packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */
				BrokerPacket packetToClient = new BrokerPacket();
				packetToClient.type = BrokerPacket.BROKER_QUOTE;

				/* process message */
				/* just echo in this example */
				if (packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
					
					// For sanity, copy the symbol over
					packetToClient.symbol = packetFromClient.symbol;
					
					// Look up the given symbol in the hash table
					packetToClient.quote = quoteDB.get(packetFromClient.symbol);
					
					/* send reply back to client */
					toClient.writeObject(packetToClient);

					/* wait for next packet */
					continue;
				}

				/* Sending an BROKER_NULL || BROKER_BYE means quit */
				if (packetFromClient.type == BrokerPacket.BROKER_NULL
						|| packetFromClient.type == BrokerPacket.BROKER_BYE) {
					gotByePacket = true;
					packetToClient = new BrokerPacket();
					packetToClient.type = BrokerPacket.BROKER_BYE;
					toClient.writeObject(packetToClient);
					break;
				}

				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown ECHO_* packet!!");
				System.exit(-1);
			}

			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

		} catch (IOException e) {
			if (!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if (!gotByePacket)
				e.printStackTrace();
		}
	}
}
