import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The thread that sits between the maze and the queues, translating the
 * messages that come off the incoming queue to something that the Maze (GUI)
 * can understand, and translating what the maze gives off into something the
 * outgoing queue can understand
 * 
 * @author robin162
 * 
 */
public class MazewarLocalHandler extends Thread implements ClientListener,
		MazeListener {

	/**
	 * Maintain a set of listeners - these are local GUIs that are receiving
	 * messages from the network and need to react to them. At this point, this
	 * is only the Swing Maze GUI.
	 */
	private Set<CommLocalListener> localListenerSet = new HashSet<CommLocalListener>();

	public MazewarLocalHandler(CommLocalListener cl) {

		// We always want to start off with at least one local listener: the
		// main maze. Otherwise, we could be receiving events with no GUI
		// output!
		this.addCommLocalListener(cl);
	}

	/**
	 * Add a listener that needs to be updated by events from the network.
	 * Should only be called if we add additional output GUIs.
	 * 
	 * @param cl
	 */
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

	/*
	 * Players are represented by "client" objects. When a client does an
	 * action, it's represented as it sending a message using its
	 * "notifyListeners" function, which ends up calling this function in any of
	 * its listeners. Since this is going to be _the_ listener for all clients,
	 * this is where we take something a client wants and send it to the maze.
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
		toNetwork.addtoQueue(newPacket);
	}

	/*
	 * implements MazeListener's clientAdded();
	 */
	public void clientAdded(Client c) {
		if (c instanceof GUIClient) {
			MazewarMsg msg = new MazewarMsg();
			msg.cw = new CommClientWrapper(c.getName(), c.getPoint(), c
					.getOrientation());

			msg.action = MazewarMsg.MW_MSG_CLIENT_ADDED;
			// Make an appropriate gamePacket
			gamePacket newPacket = new gamePacket();
			newPacket.msg = msg;
			toNetwork.addtoQueue(newPacket);
		} else {
			// do nothing
		}
	}

	/*
	 * implements MazeListerner's clientFired
	 */
	public void clientFired(Client c) {

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

			// Make an appropriate gamePacket
			gamePacket newPacket = new gamePacket();
			newPacket.msg = msg;
			toNetwork.addtoQueue(newPacket);
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

			// Make an appropriate gamePacket
			gamePacket newPacket = new gamePacket();
			newPacket.msg = msg;
			toNetwork.addtoQueue(newPacket);
		}
	}

	/*
	 * implements MazeListener's mazeUpdate()
	 */
	public void mazeUpdate() {
		// do nothing
	}

	// Update the Maze (the only local listener) on the contents of a message
	// received from the incoming queue
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

	// MazewarPacket packet = null;
	// try {
	//
	// packet = (MazewarPacket) socketIn.readObject();
	// while (packet != null) {
	// serverInQueue.add(packet);
	//
	// packet = serverInQueue.peek();
	// /*
	// * Since there is no guarantee that packets arrive in order from
	// * the server we need to make sure that we execute packets in
	// * order
	// */
	// if ((packet != null)) {
	// packet = serverInQueue.poll(); // remove the packet from
	// // the queue
	//
	// // need to process the message
	// notifyListeners(packet.msg);
	//
	// sequenceLastEx++; // increment the sequence number
	// // look at the next packet to see if we can process anymore
	// // messages
	// packet = serverInQueue.peek();
	// }
	//
	// packet = (MazewarPacket) socketIn.readObject();
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// } catch (ClassNotFoundException e) {
	// e.printStackTrace();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }

}
