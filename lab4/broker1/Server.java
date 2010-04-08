import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Scanner;

import broker.Broker;

public class Server {
	// Table of quotes file
	private static final String quoteDBFileName = "nasdaq";

	public static Hashtable<String, Long> quoteDB = new Hashtable<String, Long>();

	static void initHashTable() {
		// Read the quotes file into an internal hash table
		try {
			Scanner quoteInput = new Scanner(new BufferedReader(new FileReader(
					quoteDBFileName)));

			while (quoteInput.hasNext()) {

				String symbol = quoteInput.next();
				symbol.toLowerCase();
				Long value = Long.parseLong(quoteInput.next());

				quoteDB.put(symbol, value);
			}

			quoteInput.close();

		} catch (FileNotFoundException e) {
			System.out.println("Can't find database file " + quoteDBFileName);
			System.exit(-1);
		} catch (NoSuchElementException e) {
			System.out.println("Error reading database file");
			System.out.println("Continuing");
			/* quoteInput.close(); */
		}
	}

	static int run(org.omg.CORBA.ORB orb, String[] args)
			throws org.omg.CORBA.UserException {
		//
		// Resolve Root POA
		//
		org.omg.PortableServer.POA rootPOA = org.omg.PortableServer.POAHelper
				.narrow(orb.resolve_initial_references("RootPOA"));

		//
		// Get a reference to the POA manager
		//
		org.omg.PortableServer.POAManager manager = rootPOA.the_POAManager();

		//
		// Create implementation object
		//
		Broker_impl brokerImpl = new Broker_impl(rootPOA);
		Broker broker = brokerImpl._this(orb);

		//
		// Save reference
		//
		try {
			String ref = orb.object_to_string(broker);
			String refFile = "Broker.ref";
			java.io.FileOutputStream file = new java.io.FileOutputStream(
					refFile);
			java.io.PrintWriter out = new java.io.PrintWriter(file);
			out.println(ref);
			out.flush();
			file.close();
		} catch (java.io.IOException ex) {
			System.err.println("broker.Server: can't write to `"
					+ ex.getMessage() + "'");
			return 1;
		}

		//
		// Save reference as html
		//
		try {
			String ref = orb.object_to_string(broker);
			String refFile = "Broker.html";
			java.io.FileOutputStream file = new java.io.FileOutputStream(
					refFile);
			java.io.PrintWriter out = new java.io.PrintWriter(file);
			out.println("<applet codebase=\"classes\" "
					+ "code=\"broker/Client.class\" " + "archive=\"OB.jar\" "
					+ "width=500 height=300>");
			out.println("<param name=ior value=\"" + ref + "\">");
			out.println("<param name=org.omg.CORBA.ORBClass "
					+ "value=com.ooc.CORBA.ORB>");
			//
			// No need to specify ORBSingletonClass - it's ignored anyway
			//
			// out.println("<param name=org.omg.CORBA.ORBSingletonClass " +
			// "value=com.ooc.CORBA.ORBSingleton>");
			out.println("</applet>");
			out.flush();
			file.close();
		} catch (java.io.IOException ex) {
			System.err.println("broker.Server: can't write to `"
					+ ex.getMessage() + "'");
			return 1;
		}

		//
		// Run implementation
		//
		initHashTable();
		manager.activate();
		orb.run();

		return 0;
	}

	public static void main(String args[]) {
		java.util.Properties props = System.getProperties();
		props.put("org.omg.CORBA.ORBClass", "com.ooc.CORBA.ORB");
		props.put("org.omg.CORBA.ORBSingletonClass",
				"com.ooc.CORBA.ORBSingleton");
		props.put("ooc.orb.id", "Broker-Server");

		int status = 0;
		org.omg.CORBA.ORB orb = null;

		try {
			orb = org.omg.CORBA.ORB.init(args, props);
			status = run(orb, args);
		} catch (Exception ex) {
			ex.printStackTrace();
			status = 1;
		}

		if (orb != null) {
			try {
				orb.destroy();
			} catch (Exception ex) {
				ex.printStackTrace();
				status = 1;
			}
		}

		System.exit(status);
	}
}
