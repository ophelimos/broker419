import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;

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

	private boolean debug = true;

	private ConnectionDB connectionDB;

	MazeImpl maze = null;

	MazewarSLP slpserver = null;
	
	MazewarGUI mazewarGUI = null;

	public MazewarMiddlewareServer(ConnectionDB connectionDB_in,
			MazeImpl maze_in, MazewarSLP slpserver_in, MazewarGUI mazewarGUI_in) {
		this.connectionDB = connectionDB_in;
		this.maze = maze_in;
		this.slpserver = slpserver_in;
		this.mazewarGUI = mazewarGUI_in;
	}

	// Receive packets from the network
	private void receivePackets() {
		// Get the current list of input peers
		Enumeration<InputPeer> networkPeers = connectionDB.getInputPeers();
		// Iterate through our network peers, receiving one packet from each
		InputPeer curPeer = null;
		while (networkPeers.hasMoreElements()) {
			try {
				curPeer = networkPeers.nextElement();
				gamePacket receivedPacket = (gamePacket) curPeer.in
						.readObject();
				// I'm not sure when we get null here, but it means the
				// connection's down
				if (receivedPacket == null) {
					IOException nullReceived = new IOException();
					throw nullReceived;
				}
				if (debug) {
					printPacket(receivedPacket);
				}

				// This whole thing could be merged with below, but I'm not
				// smart enough right now
				if (receivedPacket.type == gamePacket.GP_STARTGAME) {

					// There's three things it can be now: an invitation, an ACK
					// for my invitation, or a NACK for my invitation

					if (receivedPacket.NACK) {
						// It's a NACK: purge corresponding invitation
						Mazewar.waitingForAcks.removeFromQueue(receivedPacket);
						continue;
					}

					if (receivedPacket.ACK) {
						// It's an ACK: mark it off
						gamePacket ackedPacket = Mazewar.waitingForAcks
								.haveACK(receivedPacket);
						if (ackedPacket != null) {
							// Put it in the toMaze queue
							Mazewar.toMaze.addtoSortedQueue(ackedPacket);
						}
						continue;
					}

					// All right: it's an invitation. Throw it on the queue, and
					// processPackets() will figure out what to do with it
					Mazewar.toMaze.addtoSortedQueue(receivedPacket);

					// Synchronize time stamps
					Mazewar.localtimestamp.max(receivedPacket.timeogram);

					continue;
				}

				// Handle ACKing
				if (receivedPacket.ACK) {
					// haveALL add the ACK count and it returns true if we have
					// all, returns true, else false
					gamePacket ackedPacket = Mazewar.waitingForAcks
							.haveACK(receivedPacket);
					if (ackedPacket != null) {
						// Put it in the toMaze queue
						Mazewar.toMaze.addtoSortedQueue(ackedPacket);
					}
				}

				if (receivedPacket.wantACK) {
					// Send an ACK
					sendACK(receivedPacket);
				}

				if (receivedPacket.type == gamePacket.GP_COMMAND) {
					// Synchronize our timestamps
					Mazewar.localtimestamp.max(receivedPacket.timeogram);

					// Put it on the toMaze queue
					Mazewar.toMaze.addtoSortedQueue(receivedPacket);
				} else if (receivedPacket.type == gamePacket.GP_MYNAME) {
					connectionDB.addPlayerName(receivedPacket.senderName,
							curPeer.hostname);
				} else {
					Mazewar.consolePrintLn("Error: untyped packet received!");
					printPacket(receivedPacket);
				}

			} catch (SocketTimeoutException e) {
				// On timeout, simply try the next peer
				continue;
			} catch (IOException e) {
				if (debug) {
					System.out.print(e.getStackTrace());
				}
				Mazewar.consolePrint("Connection failed with "
						+ curPeer.hostname
						+ "\n Removing from connection list...");
				if (connectionDB.removePeer(curPeer)) {
					Mazewar.consolePrint("Success!\n");
				} else {
					Mazewar.consolePrint("Failed!\n");
				}
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

			if (Mazewar.toMaze.lineup.isEmpty()) {
				return;
			}

			// If we have something waiting for an ACK that's older than
			// something to send to the maze, wait for it to get ACKed
			if (Mazewar.toMaze.isTimeLessThan(Mazewar.waitingForAcks.lineup
					.get(0), Mazewar.toMaze.lineup.get(0))) {
				Mazewar.consolePrintLn("Waiting for an ACK...");
				break;
			}

			gamePacket mostRecentPacket = Mazewar.toMaze.getElement();
			if (mostRecentPacket == null) {
				// Nothing left to process
				return;
			}

			if (mostRecentPacket.type == gamePacket.GP_STARTGAME) {
				// Make sure we're in the right state
				if (Mazewar.getStatus() == Mazewar.STATUS_PLAYING) {
					Mazewar
							.consolePrintLn("Received invitation, but already playing!!!");
					continue;
				}

				// All right: let's start the game
				startGame(mostRecentPacket);

			} else if (mostRecentPacket.type == gamePacket.GP_COMMAND) {
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
					Mazewar.consolePrintLn("Weird message received!!!");
					break;
				}
				maze.commLocalClientUpdate(msg.cw, ce, msg.cw_optional);
			}
		}

	}

	private void broadcastPackets() {

		// Get the current list of output peers
		Enumeration<OutputPeer> networkPeers = connectionDB.getOutputPeers();

		// Grab a packet on the output stream
		gamePacket packetToSend = Mazewar.toNetwork.getElement();
		// Make sure we actually got one, otherwise, don't bother
		if (packetToSend != null) {
			// Iterate through our network peers
			OutputPeer curPeer = null;
			while (networkPeers.hasMoreElements()) {
				try {
					curPeer = networkPeers.nextElement();
					curPeer.out.writeObject(packetToSend);
				} catch (IOException e) {
					Mazewar.consolePrintLn("Connection failed with "
							+ curPeer.hostname
							+ "\n Removing from connection list...");
					if (connectionDB.removePeer(curPeer)) {
						Mazewar.consolePrint("Success!\n");
					} else {
						Mazewar.consolePrint("Failed!\n");
					}
				}
			}
		}
	}

	/**
	 * Print all the info in a gamePacket, for debugging
	 * 
	 */
	public void printPacket(gamePacket packet) {
		Mazewar.consolePrintLn("----Packet Info----");
		Mazewar.consolePrintLn("Packet Info: type = " + packet.type
				+ " nextmove = " + packet.nextmove + " trackACK = "
				+ packet.trackACK + " senderName = " + packet.senderName
				+ " wantACK = " + packet.wantACK + " ACK = " + packet.ACK
				+ " NACK = " + packet.NACK);

		// Timestamp
		Mazewar.consolePrintLn("Timestamp: " + packet.timeogram.toString());

		// MazewarMsg
		Mazewar.consolePrint("MazewarMsg = ");
		if (packet.msg == null) {
			Mazewar.consolePrint("null\n");
		} else {
			switch (packet.msg.action) {
			case MazewarMsg.MW_MSG_LEFT:
				Mazewar.consolePrint("turnLeft\n");
				break;
			case MazewarMsg.MW_MSG_RIGHT:
				Mazewar.consolePrint("turnRight\n");
				break;
			case MazewarMsg.MW_MSG_FWD:
				Mazewar.consolePrint("moveForward\n");
				break;
			case MazewarMsg.MW_MSG_BKWD:
				Mazewar.consolePrint("moveBackward\n");
				break;
			case MazewarMsg.MW_MSG_FIRE:
				Mazewar.consolePrint("fire\n");
				break;
			case MazewarMsg.MW_MSG_CLIENT_ADDED:
				Mazewar.consolePrint("client_added\n");
				break;
			case MazewarMsg.MW_MSG_CLIENT_REMOVED:
				Mazewar.consolePrint("client_removed\n");
				break;
			case MazewarMsg.MW_MSG_CLIENT_ADDED_FIN:
				Mazewar.consolePrint("client_added_fin\n");
				break;
			case MazewarMsg.MW_MSG_CLIENT_KILLED:
				Mazewar.consolePrint("client_killed\n");
				break;
			default:
				Mazewar.consolePrintLn("BAD\n");
				break;
			}
		}
		Mazewar.consolePrintLn("----End Packet Info----");
	}

	/**
	 * Function to do all the stuff associated with starting a game.
	 * 
	 */
	private void startGame(gamePacket startPacket) {
		// ACK the successfully received packet
		sendACK(startPacket);

		// Shut down SLP
		slpserver.stopServer();
		
		// Set our state to PLAYING
		Mazewar.setPlaying();

		// Boot all nodes except the ones we're playing with
		
		// Get the current list of output peers
		Enumeration<OutputPeer> networkPeers = connectionDB.getOutputPeers();

		// Iterate through our network peers
		OutputPeer curPeer = null;
		while (networkPeers.hasMoreElements()) {
			curPeer = networkPeers.nextElement();
			for (int i = 0; i < startPacket.numPlayers; i++) {
				if (startPacket.playerlist[i].equals(curPeer.hostname)) {
					continue;
				}
			}
			// Kill the connection
			connectionDB.removePeer(curPeer);
		}
		
		// Make remote clients for everyone we're playing with
		for (int i = 0; i < startPacket.numPlayers; i++) {
			RemoteClient newPlayer = new RemoteClient(startPacket.playerlist[i]);
			Mazewar.actualPlayers.add(newPlayer);
			maze.addClient(newPlayer);
		}

		// Attach the keyboard to the GUIclient
		mazewarGUI.turnOnGUIClient();
		Mazewar.consolePrintLn("Starting game!");
	}

	/**
	 * Included twice, so separated
	 * 
	 * @param packetToAck
	 */
	private void sendACK(gamePacket packetToACK) {
		// Send an ACK
		gamePacket ackPacket = new gamePacket(packetToACK);
		ackPacket.ACK = true;
		ackPacket.wantACK = false;
		Mazewar.toNetwork.addtoQueue(ackPacket);
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