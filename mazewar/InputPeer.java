import java.io.ObjectInputStream;
import java.net.Socket;


public class InputPeer extends Peer {
	ObjectInputStream in = null;
	
	public InputPeer(String name_in, int port_in, Socket socket_in) {
		super(name_in, port_in, socket_in);
	}

	public InputPeer(String name_in, int port_in, Socket socket_in, ObjectInputStream in_in) {
		super(name_in, port_in, socket_in);
		in = in_in;
	}
}
