import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Random;
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

	MazeImpl maze = null;

	MazewarSLP slpserver = null;

	MazewarGUI mazewarGUI = null;

	public MazewarMiddlewareServer(ConnectionDB connectionDB_in,
			MazeImpl maze_in, MazewarSLP slpserver_in, MazewarGUI mazewarGUI_in) {
		super("Mazewar Middleware Server");
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
			gamePacket receivedPacket = null;
			try {
				curPeer = networkPeers.nextElement();

				Object receivedInfo = curPeer.in.readObject();
				// I'm not sure when we get null here, but it means the
				// connection's down
				if (receivedInfo == null) {
					if (Mazewar.debugMode) {
						Mazewar
								.consoleErrorPrintLn("Received NULL on input stream");
					}
					IOException nullReceived = new IOException();
					throw nullReceived;
				}

				/*
				 * For some absolutely bizarre reason, about 25% of the time
				 * what gets sent is actually just the timestamp, not the entire
				 * gamePacket. In this circumstance, we're going to have to ask
				 * the thing to resend to us
				 */
				if (receivedInfo instanceof Vector) {
					Vector<vectorobj> receivedVector = (Vector<vectorobj>) receivedInfo;
					// Recreate the gamePacket and send it as a resend request
					gamePacket resendRq = new gamePacket();
					resendRq.type = gamePacket.GP_RESEND;
					timestamp receivedTimestamp = new timestamp();
					receivedTimestamp.mytimestamp = receivedVector;
					resendRq.timeogram = receivedTimestamp;

					Enumeration<OutputPeer> outputs = connectionDB
							.getOutputPeers();
					try {
						// Find the right one
						OutputPeer badSendingPeer = null;
						while (outputs.hasMoreElements()) {
							badSendingPeer = outputs.nextElement();
							if (badSendingPeer.hostname
									.equals(curPeer.hostname)) {
								if (Mazewar.debugMode) {
									Mazewar
											.consoleErrorPrintLn("Asking for a resend!");
								}
								badSendingPeer.out.writeObject(resendRq);
							}
						}
					} catch (IOException e) {
						killConnection(curPeer);
					}
					continue;
				}

				// If it's not NULL, it should be a gamePacket
				receivedPacket = (gamePacket) receivedInfo;
				if (Mazewar.debugMode) {
					printPacket(receivedPacket);
				}

				// If it's a resend, process it and be embarrassed
				if (receivedPacket.type == gamePacket.GP_RESEND) {
					// Find the packet in waitingForAck
					gamePacket packetToResend = Mazewar.waitingForAcks
							.findInQueue(receivedPacket);
					if (packetToResend == null && Mazewar.debugMode) {
						Mazewar
								.consoleErrorPrintLn("Something's really weird: "
										+ "not finding a packet to resend in the waitingForAcks queue");
					}
					// Resend the packet
					Enumeration<OutputPeer> outputs = connectionDB
							.getOutputPeers();
					try {
						// Find the right one
						OutputPeer badReceivingPeer = null;
						while (outputs.hasMoreElements()) {
							badReceivingPeer = outputs.nextElement();
							if (badReceivingPeer.hostname
									.equals(curPeer.hostname)) {
								Mazewar
										.consoleErrorPrintLn("Asking for a resend!");
								badReceivingPeer.out
										.writeObject(packetToResend);
							}
						}
					} catch (IOException e) {
						killConnection(curPeer);
					}

					// Certainly don't try to process past this point
					continue;
				}

				// Synchronize time stamps - every time unless it's an ACK
				if (!receivedPacket.ACK) {
					Mazewar.localtimestamp.max(receivedPacket.timeogram);
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
					continue;
				}

				if (receivedPacket.wantACK) {
					// Send an ACK
					sendACK(receivedPacket);
				}

				/** ** Sorting packets by type ***** */
				if (receivedPacket.type == gamePacket.GP_STARTGAME) {

					// The only thing it can be is a command. Throw it on
					// the toMaze queue, and processPackets() will figure out
					// what to
					// do with it
					Mazewar.toMaze.addtoSortedQueue(receivedPacket);

					continue;

				} else if (receivedPacket.type == gamePacket.GP_COMMAND) {
					// Put it on the toMaze queue
					Mazewar.toMaze.addtoSortedQueue(receivedPacket);

				} else if (receivedPacket.type == gamePacket.GP_MYNAME) {
					connectionDB.addPlayerName(receivedPacket.senderName,
							curPeer.hostname);

				} else {
					Mazewar
							.consoleErrorPrintLn("Error: untyped packet received!");
					printPacket(receivedPacket);
				}

			} catch (ClassCastException e) {

				Mazewar
						.consoleErrorPrintLn("Received garbage packet!  Killing connection...");
				Mazewar.consoleErrorPrintLn(e.getMessage());
				// Mazewar.consoleErrorPrintLn(receivedPacket.toString());
				killConnection(curPeer);
			} catch (SocketTimeoutException e) {
				// On timeout, simply try the next peer
				continue;
			} catch (IOException e) {
				if (Mazewar.debugMode) {
					e.getStackTrace();
					Mazewar.consoleErrorPrintLn("Connection broke on RECEIVE");
				}
				killConnection(curPeer);
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
			if (!Mazewar.waitingForAcks.lineup.isEmpty()) {
				if (Mazewar.toMaze.isTimeLessThan(Mazewar.waitingForAcks.lineup
						.get(0), Mazewar.toMaze.lineup.get(0))) {
					Mazewar.consoleErrorPrintLn("Waiting for an ACK...");
					break;
				}
			}

			gamePacket mostRecentPacket = Mazewar.toMaze.getElement();
			if (mostRecentPacket == null) {
				// Nothing left to process
				return;
			}

			if (mostRecentPacket.type == gamePacket.GP_STARTGAME) {

				// Make sure the packet's actually for us
				boolean forUs = false;
				for (int j = 0; j < mostRecentPacket.numPlayers; j++) {
					if (mostRecentPacket.playerlist[j].equals(Mazewar.hostname)) {
						forUs = true;
						break;
					}
				}

				if (!forUs) {
					if (Mazewar.debugMode) {
						Mazewar
								.consoleErrorPrintLn("Received extra StartGame packet - disregarding");
					}
					continue;
				}

				// Make sure we're in the right state
				if (Mazewar.getStatus() == Mazewar.STATUS_PLAYING) {
					if (Mazewar.debugMode) {
						Mazewar
								.consoleErrorPrintLn("Received invitation, but already playing!!!");
						continue;
					}
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
					if (Mazewar.debugMode) {
						Mazewar
								.consoleErrorPrintLn("Weird message received!!!");
					}
					return;
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
					if (Mazewar.debugMode) {
						Mazewar.consoleErrorPrintLn("Sent packet:");
						printPacket(packetToSend);
					}
				} catch (IOException e) {
					Mazewar.consoleErrorPrintLn("Connection broke on SEND");
					killConnection(curPeer);
				}
			}
		}
	}

	/**
	 * Print all the info in a gamePacket, for debugging
	 * 
	 */
	public void printPacket(gamePacket packet) {
		Mazewar.consoleErrorPrintLn("----Packet Info----");
		Mazewar.consoleErrorPrintLn("Packet Info: type = " + packet.type
				+ " trackACK = " + packet.trackACK + " senderName = "
				+ packet.senderName + " wantACK = " + packet.wantACK
				+ " ACK = " + packet.ACK + " NACK = " + packet.NACK
				+ "\nSender of msg: " + packet.senderName + "\n");

		// Timestamp
		packet.timeogram.printVTS();

		// MazewarMsg
		Mazewar.consoleErrorPrint("MazewarMsg = ");
		if (packet.msg == null) {
			Mazewar.consoleErrorPrint("null\n");
		} else {
			switch (packet.msg.action) {
			case MazewarMsg.MW_MSG_LEFT:
				Mazewar.consoleErrorPrint("turnLeft\n");
				break;
			case MazewarMsg.MW_MSG_RIGHT:
				Mazewar.consoleErrorPrint("turnRight\n");
				break;
			case MazewarMsg.MW_MSG_FWD:
				Mazewar.consoleErrorPrint("moveForward\n");
				break;
			case MazewarMsg.MW_MSG_BKWD:
				Mazewar.consoleErrorPrint("moveBackward\n");
				break;
			case MazewarMsg.MW_MSG_FIRE:
				Mazewar.consoleErrorPrint("fire\n");
				break;
			case MazewarMsg.MW_MSG_CLIENT_ADDED:
				Mazewar.consoleErrorPrint("client_added\n");
				break;
			case MazewarMsg.MW_MSG_CLIENT_REMOVED:
				Mazewar.consoleErrorPrint("client_removed\n");
				break;
			case MazewarMsg.MW_MSG_CLIENT_ADDED_FIN:
				Mazewar.consoleErrorPrint("client_added_fin\n");
				break;
			case MazewarMsg.MW_MSG_CLIENT_KILLED:
				Mazewar.consoleErrorPrint("client_killed\n");
				break;
			default:
				Mazewar.consoleErrorPrint("BAD\n");
				break;
			}
		}
		Mazewar.consoleErrorPrintLn("----End Packet Info----");
	}

	/**
	 * Function to do all the stuff associated with starting a game.
	 * 
	 */
	private void startGame(gamePacket startPacket) {
		// No ACKing startGame packets

		// Shut down SLP
		slpserver.stopServer();

		// Set our state to PLAYING
		Mazewar.setPlaying();

		// Boot all nodes except the ones we're playing with

		// Stop accepting connections
		Mazewar.acceptingNewConnections = false;

		// Get the current list of output peers
		Enumeration<OutputPeer> networkPeers = connectionDB.getOutputPeers();

		// Iterate through our network peers
		OutputPeer curPeer = null;
		PeersInNetwork: while (networkPeers.hasMoreElements()) {
			curPeer = networkPeers.nextElement();
			for (int i = 0; i < startPacket.numPlayers; i++) {
				if (startPacket.playerlist[i].equals(curPeer.hostname)) {
					continue PeersInNetwork;
				}
			}
			// Kill the connection (we haven't gotten out yet)
			connectionDB.removePeer(curPeer);

			// And remove them from the timestamp
			Mazewar.localtimestamp.removePlayer(curPeer.playerName);

		}

		// Remove unneeded graphics
		mazewarGUI.removeAvailablePlayers();
		mazewarGUI.removeStartButton();

		// Add maze graphics - BEFORE doing anything to the maze (i.e. adding
		// players)
		mazewarGUI.addOverheadPanel();
		mazewarGUI.addScoreTable();
		
		// Ensure consistent ordering by sorting the playerlist alphabetically
		Arrays.sort(startPacket.playerlist);

		// Make remote clients for everyone we're playing with
		for (int i = 0; i < startPacket.numPlayers; i++) {
			// If it's ourself, start the GUIclient
			if (startPacket.playerlist[i].equals(Mazewar.hostname)) {
				maze.addClient(mazewarGUI.guiClient);
			} else {
				// Find the player name corresponding to the hostname
				String playerName = getPlayerName(startPacket.playerlist[i]);
				// If that name is NULL, make it something obvious
				if (playerName == null) {
					Random generator = new Random();
					playerName = "NullName"
							+ Integer.toString(generator.nextInt());
				}
				// Add it as a Remote Client
				RemoteClient newPlayer = new RemoteClient(playerName);
				// Mazewar.actualPlayers.add(newPlayer);
				maze.addClient(newPlayer);
			}
		}

		mazewarGUI.pack();

		// Attach the keyboard to the GUIclient
		mazewarGUI.turnOnGUIClient();
		Mazewar.consolePrintLn("Starting game!");
		mazewarGUI.requestFocusInWindow();
	}

	public String getPlayerName(String hostname) {
		for (int i = 0; i < connectionDB.inputPeers.size(); i++) {
			if (hostname.equals(connectionDB.inputPeers.get(i).hostname)) {
				return connectionDB.inputPeers.get(i).playerName;
			}
		}

		return null;
	}

	public boolean restartConnection(Peer curPeer) {
		// Try to re-establish it
		OutputPeer tryagain = new OutputPeer(curPeer.hostname, curPeer.port);
		return connectionDB.addOutputPeer(tryagain);
	}

	public boolean killConnection(Peer curPeer) {
		Mazewar.consolePrint("Connection failed with " + curPeer.hostname
				+ "\n Removing from connection list...");

		boolean success = false;
		success = connectionDB.removePeer(curPeer);

		// Try to re-establish it
		if (Mazewar.acceptingNewConnections) {
			if (restartConnection(curPeer)) {
				Mazewar.consolePrint("Re-established!\n");
				return true;
			} else {
				Mazewar.consoleErrorPrintLn("Failed to re-establish :(");
			}
		}
		// Take it out of the game if we're playing
		if (Mazewar.getStatus() == Mazewar.STATUS_PLAYING) {
			ClientEvent ce = ClientEvent.client_removed;
			CommClientWrapper cw = new CommClientWrapper(curPeer.playerName);
			CommClientWrapper cw_optional = null;
			maze.commLocalClientUpdate(cw, ce, cw_optional);
		}

		if (success) {
			Mazewar.consolePrint("Success!\n");
		} else {
			Mazewar.consolePrint("Failed!\n");
		}

		return success;
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
		// Put my name on the ACK
		ackPacket.senderName = Mazewar.localName;
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
