import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Vector;

import ch.ethz.iks.slp.ServiceLocationEnumeration;
import ch.ethz.iks.slp.ServiceLocationException;
import ch.ethz.iks.slp.ServiceURL;

public class MazewarComm extends Thread implements ClientListener, MazeListener {

	private PriorityQueue<MazewarPacket> serverInQueue = null;

	private MazewarSLP slpServer = null;

	private Vector<Peer> networkPeers = new Vector<Peer>();
	
	private clientQueue inputQueue = new clientQueue();

	/**
	 * Maintain a set of listeners.
	 */
	private Set<CommLocalListener> localListenerSet = new HashSet<CommLocalListener>();

	private int sequenceLastEx = -1;

	public MazewarComm(MazewarSLP slpServer_in) {
		this.slpServer = slpServer_in;
	}

	// public MazewarComm(String hostname, int port) {
	//
	// if (hostname.length() == 0 || port < 1000) {
	// System.err.println("ERROR: Invalid arguments!");
	// System.exit(-1);
	// }
	// try {
	// socket = new Socket(hostname, port);
	// socketIn = new ObjectInputStream(socket.getInputStream());
	// socketOut = new ObjectOutputStream(socket.getOutputStream());
	// // socketIn = new ObjectInputStream(socket.getInputStream());
	// serverInQueue = new PriorityQueue<MazewarPacket>();
	// } catch (UnknownHostException e) {
	// System.err.println("ERROR: Don't know where to connect!!");
	// System.exit(1);
	// } catch (IOException e) {
	// System.err.println("ERROR: Couldn't get I/O for the connection.");
	// System.exit(1);
	// }
	// }

//	public boolean sendMsg(MazewarMsg msg) {
//
//		MazewarPacket packet = new MazewarPacket();
//
//		packet.msg = msg;
//		packet.type = MazewarPacket.MW_UPDATE;
//
//		try {
//			socketOut.writeObject(packet);
//		} catch (IOException e) {
//			// TODO yeah whatever status
//			return false;
//		}
//
//		return true;
//	}

	/*
	 * implements ClientListener's clientUpdate
	 */
	public void clientUpdate(Client c, ClientEvent ce) {
		// When a client turns, update our state.
		MazewarMsg msg = new MazewarMsg();
		msg.cw = new CommClientWrapper(c.getName(), c.getPoint(), c
				.getOrientation());

		if (ce.equals(ClientEvent.turnLeft)) {
			msg.action = MazewarMsg.MW_MSG_LEFT;
		} else if (ce.equals(ClientEvent.turnRight)) {
			msg.action = MazewarMsg.MW_MSG_RIGHT;
		} else if (ce.equals(ClientEvent.moveForward)) {
			msg.action = MazewarMsg.MW_MSG_FWD;
		} else if (ce.equals(ClientEvent.moveBackward)) {
			msg.action = MazewarMsg.MW_MSG_BKWD;
		} else if (ce.equals(ClientEvent.fire)) {
			msg.action = MazewarMsg.MW_MSG_FIRE;
		}
		
		// Make an appropriate gamePacket
		gamePacket newPacket = new gamePacket();
		newPacket.msg = msg;
		inputQueue.addtoQueue(newPacket);
	}

	/*
	 * implements MazeListener's mazeUpdate()
	 */
	public void mazeUpdate() {
		// do nothing
	}

	/*
	 * implements MAzeListener's clientAdded();
	 */
	public void clientAdded(Client c) {
		if (c instanceof GUIClient) {
			MazewarMsg msg = new MazewarMsg();
			msg.cw = new CommClientWrapper(c.getName(), c.getPoint(), c
					.getOrientation());

			msg.action = MazewarMsg.MW_MSG_CLIENT_ADDED;
//			 Make an appropriate gamePacket
			gamePacket newPacket = new gamePacket();
			newPacket.msg = msg;
			inputQueue.addtoQueue(newPacket);
		} else {
			// do nothing
		}
	}

	/*
	 * implements MazeListener's clientRemoved();
	 */
	public void clientRemoved(Client c) {
		if (c instanceof GUIClient) {
			MazewarMsg msg = new MazewarMsg();
			msg.cw = new CommClientWrapper(c.getName());

			msg.action = MazewarMsg.MW_MSG_CLIENT_REMOVED;
			
//			 Make an appropriate gamePacket
			gamePacket newPacket = new gamePacket();
			newPacket.msg = msg;
			inputQueue.addtoQueue(newPacket);
		}
	}

	/*
	 * implements MazeListener's clientKilled();
	 */
	public void clientKilled(Client source, Client target) {
		if (target instanceof GUIClient) {
			MazewarMsg msg = new MazewarMsg();
			msg.cw = new CommClientWrapper(source.getName(), source.getPoint(),
					source.getOrientation());
			msg.cw_optional = new CommClientWrapper(target.getName(), target
					.getPoint(), target.getOrientation());
			msg.action = MazewarMsg.MW_MSG_CLIENT_KILLED;
			
//			 Make an appropriate gamePacket
			gamePacket newPacket = new gamePacket();
			newPacket.msg = msg;
			inputQueue.addtoQueue(newPacket);
		}
	}

	/*
	 * implements MazeListerner's clientFired
	 */
	public void clientFired(Client c) {

	}

	public void addCommLocalListener(CommLocalListener cl) {
		assert (cl != null);
		localListenerSet.add(cl);
	}

	/**
	 * Remove an object from the action notification queue.
	 * 
	 * @param cl
	 *            The {@link ClientListener} to remove.
	 */
	public void removeCommListener(CommLocalListener cl) {
		localListenerSet.remove(cl);
	}

	private void notifyLocalListeners(MazewarMsg msg) {
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

		Iterator<CommLocalListener> i = localListenerSet.iterator();
		while (i.hasNext()) {
			CommLocalListener cl = i.next();
			cl.commLocalClientUpdate(msg.cw, ce, msg.cw_optional);
		}
	}

	// Go through the list of clients given from SLP and convert them to Peer
	// structures
	public void connectPeers(ServiceLocationEnumeration slpPeers) {
		try {
			checkSLPPeers: for (ServiceURL cur = (ServiceURL) slpPeers.next(); slpPeers
					.hasMoreElements(); cur = (ServiceURL) slpPeers.next()) {

				// Check if it's already in the vector
				for (int i = 0; i < networkPeers.size(); i++) {
					if (networkPeers.get(i).hostname.equals(cur.getHost())) {
						continue checkSLPPeers;
					}
				}

				// Otherwise, it's not.  Connect to it and add it
				Socket curSocket = new Socket(cur.getHost(), cur.getPort());
				ObjectInputStream socketIn = new ObjectInputStream(curSocket.getInputStream());
				ObjectOutputStream socketOut = new ObjectOutputStream(curSocket.getOutputStream());
				Peer newPeer = new Peer(cur.getHost(), curSocket, socketIn, socketOut);
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
			// Process local packets
			
			// Receive remote packets
			
			// Send remote packets
		}

//		MazewarPacket packet = null;
//		try {
//
//			packet = (MazewarPacket) socketIn.readObject();
//			while (packet != null) {
//				serverInQueue.add(packet);
//
//				packet = serverInQueue.peek();
//				/*
//				 * Since there is no guarantee that packets arrive in order from
//				 * the server we need to make sure that we execute packets in
//				 * order
//				 */
//				if ((packet != null)) {
//					packet = serverInQueue.poll(); // remove the packet from
//					// the queue
//
//					// need to process the message
//					notifyListeners(packet.msg);
//
//					sequenceLastEx++; // increment the sequence number
//					// look at the next packet to see if we can process anymore
//					// messages
//					packet = serverInQueue.peek();
//				}
//
//				packet = (MazewarPacket) socketIn.readObject();
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}
}
