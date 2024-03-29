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
final public class BrokerHelper
{
    public static void
    insert(org.omg.CORBA.Any any, Broker val)
    {
        any.insert_Object(val, type());
    }

    public static Broker
    extract(org.omg.CORBA.Any any)
    {
        if(any.type().equivalent(type()))
            return narrow(any.extract_Object());

        throw new org.omg.CORBA.BAD_OPERATION();
    }

    private static org.omg.CORBA.TypeCode typeCode_;

    public static org.omg.CORBA.TypeCode
    type()
    {
        if(typeCode_ == null)
        {
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();
            typeCode_ = orb.create_interface_tc(id(), "Broker");
        }

        return typeCode_;
    }

    public static String
    id()
    {
        return "IDL:Broker:1.0";
    }

    public static Broker
    read(org.omg.CORBA.portable.InputStream in)
    {
        org.omg.CORBA.Object _ob_v = in.read_Object();

        if(_ob_v != null)
        {
            if(_ob_v instanceof Broker)
                return (Broker)_ob_v;

            org.omg.CORBA.portable.ObjectImpl _ob_impl;
            _ob_impl = (org.omg.CORBA.portable.ObjectImpl)_ob_v;
            _BrokerStub _ob_stub = new _BrokerStub();
            _ob_stub._set_delegate(_ob_impl._get_delegate());
            return _ob_stub;
        }

        return null;
    }

    public static void
    write(org.omg.CORBA.portable.OutputStream out, Broker val)
    {
        out.write_Object(val);
    }

    public static Broker
    narrow(org.omg.CORBA.Object val)
    {
        if(val != null)
        {
            if(val instanceof Broker)
                return (Broker)val;

            if(val._is_a(id()))
            {
                org.omg.CORBA.portable.ObjectImpl _ob_impl;
                _BrokerStub _ob_stub = new _BrokerStub();
                _ob_impl = (org.omg.CORBA.portable.ObjectImpl)val;
                _ob_stub._set_delegate(_ob_impl._get_delegate());
                return _ob_stub;
            }

            throw new org.omg.CORBA.BAD_PARAM();
        }

        return null;
    }

    public static Broker
    unchecked_narrow(org.omg.CORBA.Object val)
    {
        if(val != null)
        {
            if(val instanceof Broker)
                return (Broker)val;

            org.omg.CORBA.portable.ObjectImpl _ob_impl;
            _BrokerStub _ob_stub = new _BrokerStub();
            _ob_impl = (org.omg.CORBA.portable.ObjectImpl)val;
            _ob_stub._set_delegate(_ob_impl._get_delegate());
            return _ob_stub;
        }

        return null;
    }
}
