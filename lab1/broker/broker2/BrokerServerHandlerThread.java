import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

//import java.net.SocketException;

public class BrokerServerHandlerThread extends Thread {

	private Socket socket = null;

	private QuoteDB quoteDB = null;

	public BrokerServerHandlerThread(Socket socket, QuoteDB quoteDB) {
		super("BrokerServerHandlerThread");
		this.socket = socket;
		this.quoteDB = quoteDB;
		System.out.println("Created new thread " + this.getId()
				+ " to handle client");
	}

	public void run() {

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

				// For sanity, always copy the symbol over
				packetToClient.symbol = packetFromClient.symbol;

				// Default quote of 0
				packetToClient.quote = (long) 0;
				packetToClient.error_code = 0;

				/* process message */
				if (packetFromClient.type == BrokerPacket.BROKER_REQUEST) {

					packetToClient.type = BrokerPacket.BROKER_QUOTE;

					// Look up the given symbol in the hash table
					packetToClient.quote = quoteDB.get(packetFromClient.symbol);

					if (packetToClient.quote == null) {
						// Return 0, not anything real
						packetToClient.quote = (long) 0;
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
					// break = quit, continue = keep waiting for packets
					break;
				}

				if (packetFromClient.type == BrokerPacket.EXCHANGE_ADD) {
					packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
					
					// Lower-case the symbol
					packetFromClient.symbol = packetFromClient.symbol.toLowerCase();

					// Check if the symbol exists
					if (quoteDB.containsKey(packetFromClient.symbol)) {
						packetToClient.error_code = BrokerPacket.ERROR_SYMBOL_EXISTS;
						toClient.writeObject(packetToClient);
						continue;
					}

					// There is no quote sent with an "add" request

					// Otherwise, add it
					quoteDB.put(packetFromClient.symbol, (long) 0);

					/* send reply back to client */
					packetToClient.exchange = "added.";
					toClient.writeObject(packetToClient);

					/* wait for next packet */
					continue;
				}

				if (packetFromClient.type == BrokerPacket.EXCHANGE_REMOVE) {
					packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
					
					// Lower-case the symbol
					packetFromClient.symbol = packetFromClient.symbol.toLowerCase();

					// Check if the symbol exists
					if (!quoteDB.containsKey(packetFromClient.symbol)) {
						packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
						toClient.writeObject(packetToClient);
						continue;
					}

					// If so, delete it - final value put in packet for
					// error-checking
					packetToClient.quote = quoteDB
							.remove(packetFromClient.symbol);

					/* send reply back to client */
					packetToClient.exchange = "removed.";
					toClient.writeObject(packetToClient);

					/* wait for next packet */
					continue;
				}

				if (packetFromClient.type == BrokerPacket.EXCHANGE_UPDATE) {
					packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
					
					// Lower-case the symbol
					packetFromClient.symbol = packetFromClient.symbol.toLowerCase();

					// Check if the symbol exists
					if (!quoteDB.containsKey(packetFromClient.symbol)) {
						packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
						toClient.writeObject(packetToClient);
						continue;
					}

					// Check if the quote is in range
					if (packetFromClient.quote == null
							|| packetFromClient.quote > 300
							|| packetFromClient.quote < 1) {
						packetToClient.error_code = BrokerPacket.ERROR_OUT_OF_RANGE;
						toClient.writeObject(packetToClient);
						continue;
					}

					// Update means remove and re-add
					quoteDB.remove(packetFromClient.symbol);
					quoteDB
							.put(packetFromClient.symbol,
									packetFromClient.quote);

					/* send reply back to client */
					packetToClient.exchange = "updated to "
							+ quoteDB.get(packetFromClient.symbol) + ".";
					toClient.writeObject(packetToClient);

					/* wait for next packet */
					continue;
				}

				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown ECHO_* packet!!");
				System.exit(-1);
			}

			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

		} catch (EOFException e) {
			System.out.println("Client disconnected due to EOF, thread "
					+ this.getId() + " exiting");
			return;
			// } catch (SocketException e) {
			// System.out.println("Client abruptly disconnected...");
		} catch (IOException e) {
			if (!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if (!gotByePacket)
				e.printStackTrace();
		} finally {
			try {
				quoteDB.flush();
			} catch (IOException e) {
				System.err.println("ERROR: Couldn't flush database file!");
			}
		}

		System.out.println("Client disconnected, thread " + this.getId()
				+ " exiting");
	}
}
