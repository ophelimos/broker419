package broker.broker2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * A synchronized reader-biased class to manage the flat file stock quote
 * database
 * 
 * @author James Robinson
 * 
 */
public class QuoteDB {

	// Actual database used to store key-value pairs
	private Hashtable<String, Long> DB;

	private String persistentFileName;

	QuoteDB(String fileName) {
		this.open(fileName);
	}

	/**
	 * Open the flat file containing the key-value pairs
	 * 
	 * @param fileName
	 */
	public void open(String fileName) {

		// Make sure it's not already open - if so, close it

		// Set the persistent file name
		persistentFileName = fileName;

		// Read the quotes file into an internal hash table
		try {
			Scanner quoteInput = new Scanner(new BufferedReader(new FileReader(
					fileName)));

			while (quoteInput.hasNext()) {

				String symbol = quoteInput.next();
				symbol.toLowerCase();
				Long value = Long.parseLong(quoteInput.next());

				DB.put(symbol, value);
			}

			quoteInput.close();

		} catch (FileNotFoundException e) {
			System.out.println("Can't find database file " + fileName);
			System.exit(-1);
		} catch (NoSuchElementException e) {
			System.out.println("Error reading database file " + fileName);
			System.out.println("Continuing");
			/* quoteInput.close(); */
		}
	}

	public void close() {
		// Flush database contents to disk
		this.flush();
		// Null out the pointer
		DB = null;
		// Clear the file name
		persistentFileName = null;
	}

	public void flush() throws IOException {

		File temp = File.createTempFile("pattern", ".suffix"); 
		BufferedWriter quoteOutput = new BufferedWriter(new FileWriter());
		
		// Pull the key-value pairs out of the database
		Enumeration<String> keys = DB.keys();
		
		for (String curKey = keys.nextElement();
			keys.hasMoreElements();
			curKey = keys.nextElement()) {
			
			Long curValue = DB.get(curKey);
			// Print it to the file
			quoteOutput.write(str)
			
			
		}

	}
}
