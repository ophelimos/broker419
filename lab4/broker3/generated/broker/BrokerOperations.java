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
/***/

public interface BrokerOperations
{
    //
    // IDL:Broker/quote:1.0
    //
    /***/

    int
    quote(String symbol)
        throws broker.BrokerPackage.BrokerErrorException;

    //
    // IDL:Broker/addSymbol:1.0
    //
    /***/

    void
    addSymbol(String symbol)
        throws broker.BrokerPackage.BrokerErrorException;

    //
    // IDL:Broker/removeSymbol:1.0
    //
    /***/

    void
    removeSymbol(String symbol)
        throws broker.BrokerPackage.BrokerErrorException;

    //
    // IDL:Broker/updateQuote:1.0
    //
    /***/

    void
    updateQuote(String symbol,
                int quote)
        throws broker.BrokerPackage.BrokerErrorException;
}
