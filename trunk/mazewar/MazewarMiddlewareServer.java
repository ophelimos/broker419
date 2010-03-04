import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Vector;

import ch.ethz.iks.slp.ServiceLocationEnumeration;
import ch.ethz.iks.slp.ServiceLocationException;
import ch.ethz.iks.slp.ServiceURL;

/**
 * The Mazewar middleware server that sits between the network and the queues,
 * handling who's connected to who and communicating through either the network
 * or the queues
 * 
 * @author robin162
 * 
 */
public class MazewarMiddlewareServer extends Thread {
	
	// List of my peers on the network
	private Vector<Peer> networkPeers = new Vector<Peer>();
	
	// Queues used for communication - moved to MazeWar
//	public clientQueue toNetwork = new clientQueue();
//	public clientQueue toMaze = new clientQueue();
	
	// Identifier for this node's SLP server (passed in constructor)
	private MazewarSLP slpServer = null;

	public MazewarMiddlewareServer(MazewarSLP slpServer_in) {
		this.slpServer = slpServer_in;
	}
	
	// Go through the list of clients given from SLP and convert them to Peer
	// structures
	public void connectPeers(ServiceLocationEnumeration slpPeers) {
		if (slpPeers == null) {
			// If I don't have any peers, don't do anything
			return;
		}
		try {
			checkSLPPeers: for (ServiceURL cur = (ServiceURL) slpPeers.next(); slpPeers
					.hasMoreElements(); cur = (ServiceURL) slpPeers.next()) {

				// Check if it's already in the vector
				for (int i = 0; i < networkPeers.size(); i++) {
					if (networkPeers.get(i).hostname.equals(cur.getHost())) {
						continue checkSLPPeers;
					}
				}

				// Otherwise, it's not. Connect to it and add it
				Socket curSocket = new Socket(cur.getHost(), cur.getPort());
				// Only spend a maximum of a millisecond waiting for a packet
				// that might not be coming
				curSocket.setSoTimeout(1);
				ObjectInputStream socketIn = new ObjectInputStream(curSocket
						.getInputStream());
				ObjectOutputStream socketOut = new ObjectOutputStream(curSocket
						.getOutputStream());
				Peer newPeer = new Peer(cur.getHost(), curSocket, socketIn,
						socketOut);
				networkPeers.add(newPeer);
			}
		} catch (ServiceLocationException e) {
			Mazewar.consolePrintLn("Exception while sorting SLP values");
			Mazewar.consolePrintLn("Error Code: " + e.getErrorCode());
		} catch (UnknownHostException e) {
			Mazewar.consolePrintLn("Invalid hostname received from SLP!!!");
		} catch (IOException e) {
			Mazewar
					.consolePrintLn("I/O exception connecting to SLP-received address");
		}
	}
	
	// Receive packets from the network
	private void receivePackets() {
		// Iterate through our network peers, receiving one packet from each
		Peer curPeer = null;
		for (int i = 0; i < networkPeers.size(); i++) {
			try {
				curPeer = networkPeers.get(i);
				gamePacket receivedPacket = (gamePacket) curPeer.in
						.readObject();
				// I'm not sure when we get null here, but it means the
				// connection's down
				if (receivedPacket == null) {
					IOException nullReceived = new IOException();
					throw nullReceived;
				}
				// Otherwise, we got a packet, synchronize our timestamps
				Mazewar.localtimestamp.max(receivedPacket.timeogram);
				// and throw it on the (sorted) toMaze queue
				Mazewar.toMaze.addtoSortedQueue(receivedPacket);
			} catch (SocketTimeoutException e) {
				// On timeout, simply try the next peer
				continue;
			} catch (IOException e) {
				Mazewar.consolePrintLn("Connection failed with "
						+ curPeer.hostname
						+ "\n Removing from connection list...");
				networkPeers.remove(i);
			} catch (ClassNotFoundException e) {
				System.out.println("Node " + curPeer.hostname
						+ "sent unrecognized packet!");
				e.printStackTrace();
			}
		}
	}

	private void broadcastPackets() {
		// Grab a packet on the output stream
		gamePacket packetToSend = Mazewar.toNetwork.getElement();
		// Make sure we actually got one, otherwise, don't bother
		if (packetToSend != null) {
			// Iterate through our network peers
			Peer curPeer = null;
			for (int i = 0; i < networkPeers.size(); i++) {
				try {
					curPeer = networkPeers.get(i);
					curPeer.out.writeObject(packetToSend);
				} catch (IOException e) {
					Mazewar.consolePrintLn("Connection failed with "
							+ curPeer.hostname
							+ "\n Removing from connection list...");
					networkPeers.remove(i);
				}
			}
		}
	}
	
	public void run() {

		/*
		 * The middleware does two things: 1. Connect to other nodes. 2. Send
		 * and receive packets from those nodes
		 * 
		 * However, most of the interesting parts of connecting to other nodes
		 * will be handled by MazewarSLP. Connections will be handled by calling
		 * functions from it.
		 */

		// Connect to any initially located nodes
		connectPeers(slpServer.getMazewarClients());

		while (true) {
			// Local packets are added to the input queue automatically by the
			// maze (it calls the appropriate function, which adds the packet to
			// the queue)

			// Receive remote packets
			receivePackets();
			
			// Local packets handled by another thread
			
			// Send remote packets
			broadcastPackets();
			
			// Check for new peers
			if (slpServer.clientsChanged) {
				connectPeers(slpServer.getMazewarClients());
			}
		}
	}
}
