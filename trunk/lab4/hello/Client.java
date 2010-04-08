// **********************************************************************
//
// Copyright (c) 2002
// IONA Technologies, Inc.
// Waltham, MA, USA
//
// All Rights Reserved
//
// **********************************************************************

package hello;

import java.awt.*;
import java.awt.event.*;
import hello.generated.hello.*;

public class Client extends java.applet.Applet implements ActionListener
{
    static int
    run(org.omg.CORBA.ORB orb, String[] args)
	throws org.omg.CORBA.UserException
    {
	//
	// Get "hello" object
	//
	org.omg.CORBA.Object obj =
	    orb.string_to_object("relfile:/Hello.ref");
	if(obj == null)
	{
	    System.err.println("hello.Client: cannot read IOR from Hello.ref");
	    return 1;
	}

	Hello hello = HelloHelper.narrow(obj);

	//
	// Main loop
	//
	System.out.println("[HELLO] :: Enter 'h' for hello, 's' for shutdown or 'x' " +
                           "for exit:");

	int c;

	try
	{
	    String input;
	    java.io.BufferedReader in = new java.io.BufferedReader(
		new java.io.InputStreamReader(System.in));
	    do
	    {
		System.out.print("> ");
		input = in.readLine();

		if(input.equals("h"))
		    hello.say_hello();
                else if(input.equals("s"))
		    hello.shutdown();
	    }
	    while(!input.equals("x"));
	}
	catch(java.io.IOException ex)
	{
	    System.err.println("Can't read from `" + ex.getMessage() + "'");
	    return 1;
	}

	return 0;
    }

    //
    // Standalone program initialization
    //
    public static void
    main(String args[])
    {
	int status = 0;
	org.omg.CORBA.ORB orb = null;

	java.util.Properties props = System.getProperties();
	props.put("org.omg.CORBA.ORBClass", "com.ooc.CORBA.ORB");
	props.put("org.omg.CORBA.ORBSingletonClass",
		  "com.ooc.CORBA.ORBSingleton");
	props.put("ooc.orb.id", "Hello-Client");

	try
	{
            orb = org.omg.CORBA.ORB.init(args, props);
	    status = run(orb, args);
	}
	catch(Exception ex)
	{
	    ex.printStackTrace();
	    status = 1;
	}

	if(orb != null)
	{
	    try
	    {
		orb.destroy();
	    }
	    catch(Exception ex)
	    {
		ex.printStackTrace();
		status = 1;
	    }
	}

	System.exit(status);
    }

    //
    // Members only needed for applet
    //
    private Hello hello_;
    private Button button_;

    //
    // Applet initialization
    //
    public void
    init()
    {
	String ior = getParameter("ior");

	//
	// Create ORB
	//
	org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init(this, null);

	//
	// Create client object
	//
	org.omg.CORBA.Object obj = orb.string_to_object(ior);
	if(obj == null)
	    throw new RuntimeException();

	hello_ = HelloHelper.narrow(obj);

	//
	// Add hello button
	//
	button_ = new Button("Hello");
	button_.addActionListener(this);
	this.add(button_);
    }

    //
    // Handle events
    //
    public void
    actionPerformed(ActionEvent event)
    {
	hello_.say_hello();
    }
}
