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

	private Locator locator = null;

	private ServiceLocationEnumeration mazewarClients = null;

	private long scanWaitTime = (long) 3000; // milliseconds
	
	public boolean clientsChanged = false;

	private void serverInit() {
		// Server
		try {
			advertiser = ServiceLocationManager.getAdvertiser(new Locale("en"));
			mazewarService = new ServiceURL("service:mazewar:server://"
					+ advertiser.getMyIP() + ":" + Mazewar.port,
					ServiceURL.LIFETIME_PERMANENT);

			// some attributes for the service
			Hashtable<String, String> attributes = new Hashtable<String, String>();

			// Nothing starts before we have a name.  Nothing.
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

	public synchronized ServiceLocationEnumeration findNodes() {
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

	public synchronized ServiceLocationEnumeration getMazewarClients() {
		return mazewarClients;
	}

	public void run() {
		serverInit();
		clientInit();

		// Find at least ourself
		findNodes();

		// Scan the network every few seconds
		ServiceLocationEnumeration newClients = mazewarClients;
		while (true) {
			try {
				sleep(scanWaitTime);
				newClients = findNodes();
			} catch (InterruptedException e) {
				findNodes();
			}
			if (!newClients.equals(mazewarClients)) {
				clientsChanged = true;
			}
		}

	}

}
