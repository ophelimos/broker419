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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;

import broker.Broker;
import broker.BrokerHelper;
import broker.BrokerPackage.BrokerErrorException;

public class Exchange extends java.applet.Applet implements ActionListener {
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

		try {
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(
					System.in));
			String userInput;

			// end of int validation
			System.out.println("Enter command or 'x' to exit:");
			System.out.print("> ");
			while ((userInput = stdIn.readLine()) != null) {

				try {
					// Scan through the input line
					Scanner inputLine = new Scanner(userInput);

					String curword = inputLine.next();

					// Check what does the user want to do: ADD, UPDATE, REMOVE
					if (curword.equalsIgnoreCase("add")) {
						broker.addSymbol(inputLine.next());
					} else if (curword.equalsIgnoreCase("update")) {
						broker.updateQuote(inputLine.next(), inputLine
								.nextInt());
					} else if (curword.equalsIgnoreCase("remove")) {
						broker.removeSymbol(inputLine.next());
					} else if (curword.equalsIgnoreCase("x")
							|| curword.equalsIgnoreCase("q")
							|| curword.equalsIgnoreCase("quit")) {
						break;
					} else {// Error for invalid input
						InputMismatchException invalid = new InputMismatchException(
								curword);
						throw invalid;
					}

					/* re-print console prompt */
					System.out.print("> ");

				} catch (NoSuchElementException e) {
					// Also covers InputMismatchException
					System.out.print("Usage: add <symbol> "
							+ "|| update <symbol> <value> "
							+ "|| remove <symbol>\n");
					/* re-print console prompt */
					System.out.print("> ");
					continue;
				} catch (BrokerErrorException e) {
					System.out.println(e.getMessage());
				}
			}
		} catch (IOException e) {
			System.out.println("Error getting input");
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
