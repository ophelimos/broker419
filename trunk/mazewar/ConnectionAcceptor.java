import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * A simple thread that just waits for connections and accepts them. Needs to be
 * done in a thread (i.e. concurrently) so that loopback connections actually
 * work properly.
 * 
 * @author robin162
 * 
 */
public class ConnectionAcceptor extends Thread {

	private ConnectionDB connectionDB;

	private ServerSocket serverSocket;

	// How long before acceptor times out on kill (milliseconds)
	private final int acceptorTimeout = 1000;

	public ConnectionAcceptor(ConnectionDB connectionDB, int port) {
		this.connectionDB = connectionDB;
		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(acceptorTimeout);
		} catch (IOException e) {
			System.err.println("ERROR: Could not listen on port " + port);
			System.exit(-1);
		}
	}

	public void run() {
		while (Mazewar.acceptingNewConnections) {
			try {
				Socket receivedConnection = serverSocket.accept();
				InputPeer newPeer = new InputPeer(receivedConnection
						.getInetAddress().getHostAddress(), receivedConnection
						.getPort(), receivedConnection);
				connectionDB.addInputPeer(newPeer);
			} catch (SocketTimeoutException e) {
				continue;
			} catch (IOException e) {
				Mazewar.consolePrintLn("Connection dropped");
			}
		}
	}

}
