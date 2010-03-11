import java.util.Hashtable;
import java.util.Locale;

import ch.ethz.iks.slp.Advertiser;
import ch.ethz.iks.slp.Locator;
import ch.ethz.iks.slp.ServiceLocationEnumeration;
import ch.ethz.iks.slp.ServiceLocationException;
import ch.ethz.iks.slp.ServiceLocationManager;
import ch.ethz.iks.slp.ServiceType;
import ch.ethz.iks.slp.ServiceURL;

/**
 * Set up the SLP-based server and client on the local network
 * 
 * @author James Robinson
 * 
 */
public class MazewarSLP extends Thread {

	// Global jSLP networking variables
	public static Advertiser advertiser = null;

	public static ServiceURL mazewarService = null;

	boolean killServer = false;

	private Locator locator = null;

	private ServiceLocationEnumeration slpPeers = null;

	private long scanWaitTime = (long) 3000; // milliseconds

	private ConnectionDB connectionDB;

	public MazewarSLP(ConnectionDB connectionDB_in) {
		super("Mazewar SLP Locator");
		connectionDB = connectionDB_in;
	}

	private void startServer() {
		// Server
		try {
			advertiser = ServiceLocationManager.getAdvertiser(new Locale("en"));
			mazewarService = new ServiceURL("service:mazewar:server://"
					+ advertiser.getMyIP() + ":" + Mazewar.slpPort,
					ServiceURL.LIFETIME_PERMANENT);

			// some attributes for the service
			Hashtable<String, String> attributes = new Hashtable<String, String>();

			// Nothing starts before we have a name. Nothing.
			attributes.put("name", Mazewar.localName);
			advertiser.register(mazewarService, attributes);
		} catch (ServiceLocationException e) {
			Mazewar.consolePrintLn("Failed to initialize jSLP server");
			Mazewar.consolePrintLn("Error Code: " + e.getErrorCode());
		}
	}

	private void clientInit() {
		// Connect to other clients
		try {
			locator = ServiceLocationManager.getLocator(new Locale("en"));
		} catch (ServiceLocationException e) {
			Mazewar.consolePrintLn("Failed to initialize jSLP locator");
			Mazewar.consolePrintLn("Error Code: " + e.getErrorCode());
		}
	}

	public ServiceLocationEnumeration findNodes() {
		ServiceLocationEnumeration tmpClients = null;
		try {
			// find all mazewar servers on the local network
			// returns ServiceURLs
			tmpClients = locator.findServices(new ServiceType(
					"service:mazewar:server"), null, null);
		} catch (ServiceLocationException e) {
			Mazewar.consolePrintLn("Failed to scan network");
			Mazewar.consolePrintLn("Error Code: " + e.getErrorCode());
		}

		return tmpClients;
	}

	// Go through the list of clients given from SLP and convert them to Peer
	// structures (slpPeers -> outputPeers)
	public void connectPeers(ServiceLocationEnumeration slpPeers) {
		if (slpPeers == null) {
			// If I don't have any peers, don't do anything
			return;
		}
		while (slpPeers.hasMoreElements()) {
			ServiceURL cur;
			try {
				cur = (ServiceURL) slpPeers.next();
			} catch (ServiceLocationException e) {
				Mazewar.consolePrintLn("Error iterating through SLP peers");
				return;
			}
			// Give ConnectionDB the information it needs to add it

			// Drop the leading '/' on SLP's hostname (otherwise, creating a
			// socket breaks)
			String hostname = cur.getHost();
			String hostnameCleaned = hostname.substring(1);
			
			// Do not add ourselves
			if (hostnameCleaned.equals(Mazewar.hostname)) {
				continue;
			}

			// Only create an output stream here. The acceptor part will
			// create the input stream. Essentially, all socket connections
			// in this node-to-node network will be one-way
			OutputPeer newPeer = new OutputPeer(hostnameCleaned,
					Mazewar.directPort);
			connectionDB.addOutputPeer(newPeer);
		}
	}

	public void stopServer() {
		try {
			killServer = true;
			mazewarService = new ServiceURL("service:mazewar:server://"
					+ advertiser.getMyIP() + ":" + Mazewar.slpPort,
					ServiceURL.LIFETIME_PERMANENT);
			advertiser.deregister(mazewarService);
			advertiser = null;
			mazewarService = null;
			locator = null;
		} catch (ServiceLocationException e) {
			Mazewar.consolePrintLn("Failed to shut down jSLP server");
			Mazewar.consolePrintLn("Error Code: " + e.getErrorCode());
		}
	}

	public void run() {
		startServer();
		clientInit();

		// Find at least ourself
		slpPeers = findNodes();
		// Actually connect to ourself (somewhat useless, but ensures sanity)
		connectPeers(slpPeers);

		// Scan the network every few seconds
		ServiceLocationEnumeration newPeers = slpPeers;
		while (true) {
			try {
				sleep(scanWaitTime);
				if (killServer) {
					return;
				}
				newPeers = findNodes();
			} catch (InterruptedException e) {
				findNodes();
			}
			if (!newPeers.equals(slpPeers)) {
				slpPeers = newPeers;
				connectPeers(slpPeers);
			}
		}

	}

}
