import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Vector;

/**
 * The Mazewar middleware server that sits between the network and the queues,
 * handling who's connected to who and communicating through either the network
 * or the queues
 * 
 * @author robin162
 * 
 */
public class MazewarMiddlewareServer extends Thread {

	// Reasonable batch of packets to process at once
	private final int processBatch = 256;

	private ConnectionDB connectionDB;

	// Queues used for communication - moved to MazeWar
	// public clientQueue toNetwork = new clientQueue();
	// public clientQueue toMaze = new clientQueue();

	MazeImpl maze = null;

	public MazewarMiddlewareServer(ConnectionDB connectionDB_in,
			MazeImpl maze_in) {
		this.connectionDB = connectionDB_in;
		this.maze = maze_in;
	}

	// Receive packets from the network
	private void receivePackets() {
		// Get the current list of input peers
		Vector<InputPeer> networkPeers = connectionDB.getInputPeers();
		boolean allarehere = false;
		// Iterate through our network peers, receiving one packet from each
		InputPeer curPeer = null;
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
				
				if (receivedPacket.ACK) {
					//haveALL add the ACK count and it returns true if we have all, returns true, else false
					allarehere = Mazewar.mytodoList.haveACK(receivedPacket);
					
				} else {
//					 If it's not an ACK, throw it on the (sorted) toMaze queue	
					Mazewar.toMaze.addtoSortedQueue(receivedPacket);
				}
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

	private void processPackets() {
		// Process all the packets on the top of the queue that are ready to go
		// (up to a reasonable limit)
		for (int i = 0; i < processBatch; i++) {
			gamePacket mostRecentPacket = Mazewar.toMaze.getElement();
			if (mostRecentPacket == null) {
				// Nothing left to process
				return;
			}
			MazewarMsg msg = mostRecentPacket.msg;

			ClientEvent ce = null;
			switch (msg.action) {
			case MazewarMsg.MW_MSG_LEFT:
				ce = ClientEvent.turnLeft;
				break;
			case MazewarMsg.MW_MSG_RIGHT:
				ce = ClientEvent.turnRight;
				break;
			case MazewarMsg.MW_MSG_FWD:
				ce = ClientEvent.moveForward;
				break;
			case MazewarMsg.MW_MSG_BKWD:
				ce = ClientEvent.moveBackward;
				break;
			case MazewarMsg.MW_MSG_FIRE:
				ce = ClientEvent.fire;
				break;
			case MazewarMsg.MW_MSG_CLIENT_ADDED:
				ce = ClientEvent.client_added;
				break;
			case MazewarMsg.MW_MSG_CLIENT_REMOVED:
				ce = ClientEvent.client_removed;
				break;
			case MazewarMsg.MW_MSG_CLIENT_ADDED_FIN:
				ce = ClientEvent.client_added_fin;
				break;
			case MazewarMsg.MW_MSG_CLIENT_KILLED:
				ce = ClientEvent.client_killed;
				break;
			default:
				// TODO: enter error code
				break;
			}
			maze.commLocalClientUpdate(msg.cw, ce, msg.cw_optional);
		}

	}

	private void broadcastPackets() {

		// Get the current list of output peers
		Vector<OutputPeer> networkPeers = connectionDB.getOutputPeers();

		// Grab a packet on the output stream
		gamePacket packetToSend = Mazewar.toNetwork.getElement();
		// Make sure we actually got one, otherwise, don't bother
		if (packetToSend != null) {
			// Iterate through our network peers
			OutputPeer curPeer = null;
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
		 * The middleware sends and receives packets with other nodes. It puts
		 * things on queues and takes them off of queues.
		 * 
		 * Actually connecting to other nodes, however, will be handled by
		 * MazewarSLP. Connections will be handled by using data structures
		 * updated by MazewarSLP.
		 */

		while (true) {
			// Local packets are added to the input queue automatically by the
			// maze (it calls the appropriate function, which adds the packet to
			// the queue)

			// Receive remote packets
			receivePackets();

			// Check if any packets can get sent to the maze
			processPackets();

			// Send remote packets
			broadcastPackets();
		}
	}
}
