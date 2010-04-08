// **********************************************************************
//
// Copyright (c) 2002
// IONA Technologies, Inc.
// Waltham, MA, USA
//
// All Rights Reserved
//
// **********************************************************************

import java.awt.Button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import broker.Broker;
import broker.BrokerHelper;
import broker.BrokerPackage.BrokerErrorException;

public class Client extends java.applet.Applet implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static int run(org.omg.CORBA.ORB orb, String[] args)
			throws org.omg.CORBA.UserException {
		//
		// Get "broker" object
		//
		org.omg.CORBA.Object obj = orb.string_to_object("relfile:/Broker.ref");
		if (obj == null) {
			System.err
					.println("broker.Client: cannot read IOR from Broker.ref");
			return 1;
		}

		Broker broker = BrokerHelper.narrow(obj);

		//
		// Main loop
		//
		System.out.println("Enter queries or 'x' for exit:");

		try {
			String input;
			java.io.BufferedReader in = new java.io.BufferedReader(
					new java.io.InputStreamReader(System.in));
			do {
				System.out.print("> ");
				input = in.readLine();

				if (input.equals("x"))
					System.exit(0);
				else {
					try {
					System.out.println("Quote from broker: " + broker.quote(input));
					} catch (BrokerErrorException e) {
						System.out.println(e.getMessage());
					}
				}
							
			} while (!input.equals("x"));
		} catch (java.io.IOException ex) {
			System.err.println("Can't read from `" + ex.getMessage() + "'");
			return 1;
		}

		return 0;
	}

	//
	// Standalone program initialization
	//
	public static void main(String args[]) {
		int status = 0;
		org.omg.CORBA.ORB orb = null;

		java.util.Properties props = System.getProperties();
		props.put("org.omg.CORBA.ORBClass", "com.ooc.CORBA.ORB");
		props.put("org.omg.CORBA.ORBSingletonClass",
				"com.ooc.CORBA.ORBSingleton");
		props.put("ooc.orb.id", "Broker-Client");

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

	//
	// Members only needed for applet
	//
	private Broker broker_;

	private Button button_;

	//
	// Applet initialization
	//
	public void init() {
		String ior = getParameter("ior");

		//
		// Create ORB
		//
		org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init(this, null);

		//
		// Create client object
		//
		org.omg.CORBA.Object obj = orb.string_to_object(ior);
		if (obj == null)
			throw new RuntimeException();

		broker_ = BrokerHelper.narrow(obj);

		//
		// Add broker button
		//
		button_ = new Button("Broker");
		button_.addActionListener(this);
		this.add(button_);
	}

	//
	// Handle events
	//
	public void actionPerformed(ActionEvent event) {
		try {
			broker_.quote("MSFT");
		} catch (BrokerErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
