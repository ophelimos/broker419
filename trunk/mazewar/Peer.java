import java.net.Socket;

/**
 * A structure to hold the connection information for a peer node in the
 * network. Subclasses InputPeer and OutputPeer handle specific types of
 * connections.
 * 
 * @author robin162
 * 
 */

public abstract class Peer {
	Socket socket = null;
	String hostname = null;
	int port = 0;
	
	Peer(String name_in, int port_in) {
		hostname = name_in;
		port = port_in;
	}

	Peer(String name_in, int port_in, Socket socket_in) {
		hostname = name_in;
		port = port_in;
		socket = socket_in;
	}
}
