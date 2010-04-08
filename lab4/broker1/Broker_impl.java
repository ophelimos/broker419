import broker.BrokerPOA;

public class Broker_impl extends BrokerPOA {

	//
	// The servants default POA
	//
	private org.omg.PortableServer.POA poa_;

	Broker_impl(org.omg.PortableServer.POA poa) {
		poa_ = poa;
	}

	public int quote(String symbol) {
		// Look up the given symbol in the hash table
		Long quote = Server.quoteDB.get(symbol);
		if (quote == null) {
			return 0;
		}
		return quote.intValue();
	}

	public void shutdown() {
		_orb().shutdown(false);
	}

	public org.omg.PortableServer.POA _default_POA() {
		return poa_;
	}

}
