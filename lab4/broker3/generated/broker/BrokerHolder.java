// **********************************************************************
//
// Generated by the ORBacus IDL to Java Translator
//
// Copyright (c) 2005
// IONA Technologies, Inc.
// Waltham, MA, USA
//
// All Rights Reserved
//
// **********************************************************************

// Version: 4.3.3

package broker;

//
// IDL:Broker:1.0
//
final public class BrokerHolder implements org.omg.CORBA.portable.Streamable
{
    public Broker value;

    public
    BrokerHolder()
    {
    }

    public
    BrokerHolder(Broker initial)
    {
        value = initial;
    }

    public void
    _read(org.omg.CORBA.portable.InputStream in)
    {
        value = BrokerHelper.read(in);
    }

    public void
    _write(org.omg.CORBA.portable.OutputStream out)
    {
        BrokerHelper.write(out, value);
    }

    public org.omg.CORBA.TypeCode
    _type()
    {
        return BrokerHelper.type();
    }
}
