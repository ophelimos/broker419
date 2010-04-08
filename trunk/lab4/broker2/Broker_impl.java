import broker.BrokerPOA;
import broker.BrokerPackage.*;

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

	public void addSymbol(String symbol) throws BrokerErrorException {
		// Lower-case the symbol
		symbol = symbol.toLowerCase();

		// Check if the symbol exists
		if (Server.quoteDB.containsKey(symbol)) {
			BrokerErrorException error = new BrokerErrorException(symbol
					+ " invalid.", BrokerErrors.InvalidSymbol);
			throw error;
		}

		// There is no quote sent with an "add" request

		// Otherwise, add it
		Server.quoteDB.put(symbol, (long) 0);

	}

	public void removeSymbol(String symbol) throws BrokerErrorException {
		// Lower-case the symbol
		symbol = symbol.toLowerCase();

		// Check if the symbol exists
		if (!Server.quoteDB.containsKey(symbol)) {
			BrokerErrorException error = new BrokerErrorException(symbol
					+ " invalid.", BrokerErrors.InvalidSymbol);
			throw error;
		}
	}

	public void updateQuote(String symbol, int quote)
			throws BrokerErrorException {
		// Lower-case the symbol
		symbol = symbol.toLowerCase();

		// Check if the symbol exists
		if (!Server.quoteDB.containsKey(symbol)) {
			BrokerErrorException error = new BrokerErrorException(symbol
					+ " invalid.", BrokerErrors.InvalidSymbol);
			throw error;
		}

		// Check if the quote is in range
		if (quote > 300 || quote < 1) {
			BrokerErrorException error = new BrokerErrorException(symbol
					+ " out of range.", BrokerErrors.QuoteOutofRange);
			throw error;
		}

		// Update means remove and re-add
		Server.quoteDB.remove(symbol);
		Server.quoteDB.put(symbol, (long) quote);

	}

}
