import java.io.IOException;

public class ExchangeShutdownThread extends Thread {

	/**
	 * Thread that runs on shutdown to ensure the connection is cleanly dropped
	 * 
	 * @author robin162
	 * 
	 */

	public ExchangeShutdownThread() {
		super("ExchangeShutdownThread");
	}

	public void run() {
		System.out.println("Shutting down cleanly...");

		// Disabled for now, since to really do it properly, it should contact
		// each of the threads and wait for them to shut down before shutting
		// the main socket, which takes more work to implement

		try {
			BrokerPacket packetToServer = new BrokerPacket();
			
			/* tell broker server that i'm quitting */
			if (BrokerExchange.brokerServerOut != null) {
				packetToServer.type = BrokerPacket.BROKER_BYE;
				BrokerExchange.brokerServerOut.writeObject(packetToServer);

				BrokerExchange.brokerServerOut.close();
				BrokerExchange.brokerServerIn.close();
				BrokerExchange.brokerSocket.close();
			}

			/* tell lookup server that i'm quitting */
			if (BrokerExchange.lookupServerOut != null) {
				BrokerExchange.lookupServerOut.writeObject(packetToServer);

				BrokerExchange.lookupSocket.close();
				BrokerExchange.lookupServerOut.close();
				BrokerExchange.lookupServerIn.close();
			}
		} catch (IOException e) {
			System.out
					.println("ERROR: Failed to properly close server sockets");
		}
	}
}
