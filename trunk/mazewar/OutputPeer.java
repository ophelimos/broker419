import java.io.ObjectOutputStream;
import java.net.Socket;


public class OutputPeer extends Peer {
	ObjectOutputStream out = null;
	
	public OutputPeer(String name_in, int port_in) {
		super(name_in, port_in);
	}
	
	public OutputPeer(String name_in, int port_in, Socket socket_in, ObjectOutputStream out_in) {
		super(name_in, port_in, socket_in);
		out = out_in;
	}
}
