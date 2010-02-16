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
	public static int port = 2048;

	public static void startNetworking() {
		new MazewarSLP().start();
	}

	private void serverInit() {
		// Server
		try {
			advertiser = ServiceLocationManager.getAdvertiser(new Locale("en"));
			mazewarService = new ServiceURL("service:mazewar:server://"
					+ advertiser.getMyIP() + ":" + port,
					ServiceURL.LIFETIME_PERMANENT);

			// some attributes for the service
			Hashtable<String, String> attributes = new Hashtable<String, String>();

			// Make sure we actually have a name before trying to add a "name"
			// attribute
				while (Mazewar.name == null) {
					Mazewar.nameLock.lock();
				}
			attributes.put("name", Mazewar.name);
			advertiser.register(mazewarService, attributes);
		} catch (ServiceLocationException e) {
			Mazewar.consolePrintLn("Failed to initialize jSLP server");
			Mazewar.consolePrintLn("Error Code: " + e.getErrorCode());
		}
	}

	private void clientInit() {
		// Connect to other clients
		Locator locator;
		try {
			locator = ServiceLocationManager.getLocator(new Locale("en"));
			// find all mazewar servers on the local network
			ServiceLocationEnumeration mazewarClients = locator.findServices(
					new ServiceType("service:mazewar:server"), null, null);
			// iterate over the results
			while (mazewarClients.hasMoreElements()) {
				ServiceURL foundClient = (ServiceURL) mazewarClients
						.nextElement();
				System.out.println(foundClient);
			}
		} catch (ServiceLocationException e) {
			Mazewar.consolePrintLn("Failed to initialize jSLP locator");
			Mazewar.consolePrintLn("Error Code: " + e.getErrorCode());
		}
	}

	public void run() {
		serverInit();
		clientInit();
	}
}
