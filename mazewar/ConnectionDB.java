import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.event.ListDataListener;

/**
 * This class is made to handle a problem: I've got a list of connections, and
 * multiple threads accessing it to try and figure out who I'm connected to at
 * any one time. I need to synchronize their access so that nobody corrupts the
 * data structure, while still accepting the fact it can be in a constant state
 * of flux at all times. Thus, lots of synchronized functions.
 * 
 * @author robin162
 * 
 */
public class ConnectionDB {

	// List of my peers on the network
	public Vector<InputPeer> inputPeers = new Vector<InputPeer>();

	public Vector<OutputPeer> outputPeers = new Vector<OutputPeer>();

	// Used to update the listBox
	private DefaultListModel stringified_peers = new DefaultListModel();

	/**
	 * Ensure that to use the connectedPeers variable, you need to get it once
	 * and work with your temporary copy. This ensures that you don't end up
	 * working with a changing data structure.
	 * 
	 * On the other hand, these are still shallow copies, so
	 * _don't_change_the_data!_
	 */
	@SuppressWarnings("unchecked")
	public synchronized Enumeration<InputPeer> getInputPeers() {
		Vector copy = (Vector) inputPeers.clone();
		// Don't worry about the warning
		return (Enumeration<InputPeer>) copy.elements();
	}

	@SuppressWarnings("unchecked")
	public synchronized Enumeration<OutputPeer> getOutputPeers() {
		Vector copy = (Vector) outputPeers.clone();
		// Don't worry about the warning
		return (Enumeration<OutputPeer>) copy.elements();
	}

	public synchronized void addInputPeer(InputPeer peer) {

		// We're already connected to the peer, since this has been received
		// from an accept() call
		try {
			// Only spend a maximum of a millisecond waiting for a packet
			// that might not be coming
			peer.socket.setSoTimeout(1);

			peer.in = new ObjectInputStream(peer.socket.getInputStream());

		} catch (IOException e) {
			Mazewar.consoleErrorPrintLn("I/O exception forming input stream");
			return;
		}

		inputPeers.add(peer);
		Mazewar
				.consolePrintLn("Received connection from peer "
						+ peer.hostname);
	}

	public synchronized boolean addOutputPeer(OutputPeer peer) {

		// Check if it's already in the vector
		for (int i = 0; i < outputPeers.size(); i++) {
			OutputPeer tmp = (OutputPeer) outputPeers.get(i);
			if (tmp.hostname.equals(peer.hostname)) {
				return false;
			}
		}

		// Knowing that it's a new peer, we need to connect to it
		try {
			peer.socket = new Socket(peer.hostname, peer.port);

			// Only spend a maximum of a millisecond waiting for a packet
			// that might not be coming
			peer.socket.setSoTimeout(1);

			peer.out = new ObjectOutputStream(peer.socket.getOutputStream());

			// Send it a message telling it what our name is
			gamePacket nameMessage = new gamePacket();
			nameMessage.type = gamePacket.GP_MYNAME;
			nameMessage.senderName = Mazewar.localName;
			nameMessage.wantACK = false;
			nameMessage.ACK = false;

			peer.out.writeObject(nameMessage);

			// Also put it in the list of stringified peers
			stringified_peers.addElement(peer.hostname);

			// And if it's already got an inputPeer, notify anyone who's waiting
			for (int i = 0; i < inputPeers.size(); i++) {
				if (inputPeers.get(i).hostname.equals(peer.hostname)) {
					notify();
				}
			}

		} catch (UnknownHostException e) {
			Mazewar.consoleErrorPrintLn("Invalid hostname received from SLP!!!");
			return false;
		} catch (IOException e) {
			Mazewar
					.consoleErrorPrintLn("I/O exception connecting to SLP-received address");
			return false;
		}

		outputPeers.addElement(peer);
		Mazewar.consolePrintLn("Connected to peer " + peer.hostname);
		return true;
	}

	public synchronized boolean addPlayerName(String playerName, String hostname) {

		// Add them to our local timestamp
		vectorobj newguy = new vectorobj(0, playerName);
		Mazewar.localtimestamp.addplayer(newguy);

		while (stringified_peers.indexOf(hostname) == -1) {
			// We need to wait until it actually gets there (adds the output
			// peer), thus requiring some waiting and signaling
			try {
				wait();
			} catch (InterruptedException e) {
				Mazewar
						.consoleErrorPrintLn("Waiting for peer to appear on output list interrupted!");
			}
		}

		int position = stringified_peers.indexOf(hostname);

		// Otherwise, it's there, replace it
		stringified_peers.set(position, playerName + "@" + hostname);

		// Do the same thing with inputPeers and outputPeers
		inputPeers.get(position).playerName = playerName;
		outputPeers.get(position).playerName = playerName;
		return true;
	}

	/**
	 * Remove a peer from *both* input and output lists. Returns whether or not
	 * it successfully removed the peer.
	 */
	public synchronized boolean removePeer(Peer peer) {
		boolean success = true;
		// Remove them from the timestamp - if we've already connected
		if (peer.playerName != null) {
			success = Mazewar.localtimestamp.removePlayer(peer.playerName);
			if (success) {
				Mazewar.consoleErrorPrintLn("Peer removed from timestamp");
			}
			success = inputPeers.remove(peer);
			if (success) {
				Mazewar.consoleErrorPrintLn("Peer removed from inputPeers");
			}
			success = outputPeers.removeElement(peer);
			if (success && Mazewar.debugMode) {
				Mazewar.consoleErrorPrintLn("Peer removed from outputPeers");
			}
			success = stringified_peers.removeElement(peer.playerName + "@" + peer.hostname);
			if (success) {
				Mazewar.consoleErrorPrintLn("Peer removed from stringified peers");
			}
		}
		return success;
	}

	/**
	 * Expose the dataListener functions of outputPeers
	 */
	public synchronized void addListDataListener(ListDataListener l) {
		stringified_peers.addListDataListener(l);
	}

	public synchronized void removeListDataListener(ListDataListener l) {
		stringified_peers.removeListDataListener(l);
	}

	public synchronized DefaultListModel getListModel() {
		return stringified_peers;
	}
}
