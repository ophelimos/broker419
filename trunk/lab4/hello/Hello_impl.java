import hello.HelloPOA;

// **********************************************************************
//
// Copyright (c) 2002
// IONA Technologies, Inc.
// Waltham, MA, USA
//
// All Rights Reserved
//
// **********************************************************************


public class Hello_impl extends HelloPOA
{
    //
    // The servants default POA
    //
    private org.omg.PortableServer.POA poa_;

    Hello_impl(org.omg.PortableServer.POA poa)
    {
	poa_ = poa;
    }

    public void
    say_hello()
    {
	System.out.println("Hello World!");
    }

    public void
    shutdown()
    {
	_orb().shutdown(false);
    }

    public org.omg.PortableServer.POA
    _default_POA()
    {
	return poa_;
    }
}
