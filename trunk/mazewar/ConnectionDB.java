import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

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
	private Vector<InputPeer> inputPeers = new Vector<InputPeer>();

	private Vector<OutputPeer> outputPeers = new Vector<OutputPeer>();

	/**
	 * Ensure that to use the connectedPeers variable, you need to get it once
	 * and work with your temporary copy. This ensures that you don't end up
	 * working with a changing data structure.
	 */
	public synchronized Vector<InputPeer> getInputPeers() {
		return inputPeers;
	}

	public synchronized Vector<OutputPeer> getOutputPeers() {
		return outputPeers;
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
			Mazewar.consolePrintLn("I/O exception forming input stream");
			return;
		}

		inputPeers.add(peer);
		Mazewar
				.consolePrintLn("Received connection from peer "
						+ peer.hostname);
	}

	public synchronized void addOutputPeer(OutputPeer peer) {

		// Check if it's already in the vector
		for (int i = 0; i < outputPeers.size(); i++) {
			if (outputPeers.get(i).hostname.equals(peer.hostname)) {
				return;
			}
		}

		// Knowing that it's a new peer, we need to connect to it
		try {
			peer.socket = new Socket(peer.hostname, peer.port);

			// Only spend a maximum of a millisecond waiting for a packet
			// that might not be coming
			peer.socket.setSoTimeout(1);

			peer.out = new ObjectOutputStream(peer.socket.getOutputStream());

		} catch (UnknownHostException e) {
			Mazewar.consolePrintLn("Invalid hostname received from SLP!!!");
			return;
		} catch (IOException e) {
			Mazewar
					.consolePrintLn("I/O exception connecting to SLP-received address");
			return;
		}

		outputPeers.add(peer);
		Mazewar.consolePrintLn("Connected to peer " + peer.hostname);
	}

}
