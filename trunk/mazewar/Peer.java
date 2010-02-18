import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * A structure to hold the connection information for a peer node in the network
 * @author robin162
 *
 */

public class Peer {
	Socket socket = null;
	ObjectInputStream in = null;
	ObjectOutputStream out = null;
	String hostname = null;
	
	Peer(String name_in, Socket socket_in, ObjectInputStream in_in, ObjectOutputStream out_in) {
		hostname = name_in;
		socket = socket_in;
		in = in_in;
		out = out_in;
	}
}
