package broker.broker2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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

	// Platform independent newline
	public static String newline = System.getProperty("line.separator");

	QuoteDB(String fileName) {
		try {
			this.open(fileName);
		} catch (IOException e) {
			System.out.println("Failed to open database file " + fileName);
			System.exit(-1);
		}
	}
	
	QuoteDB() {
		// Don't open anything
		persistentFileName = null;
		DB = null;
	}

	/**
	 * Open the flat file containing the key-value pairs createTempFi
	 * 
	 * @param fileName
	 */
	public void open(String fileName) throws IOException {
		try {
			// Make sure it's not already open - if so, close it
			if (DB != null) {
				close();
			}

			// Set the persistent file name
			persistentFileName = fileName;

			// Read the quotes file into an internal hash table

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

	public void close() throws IOException {
		// Flush database contents to disk
		try {
			this.flush();
		} catch (IOException e) {
			System.out.println("Failed to flush database!");
			System.out.println("Error: e.getMessage()");
			throw e;
		}
		// Null out the pointer
		DB = null;
		// Clear the file name
		persistentFileName = null;
	}

	public void flush() throws IOException {

		// Write the DB to a temporary file first, and then overwrite, rather
		// than accidentally truncating it
		File tempFile = File.createTempFile("dbtemp", "tmp");
		BufferedWriter quoteOutput = new BufferedWriter(
				new FileWriter(tempFile));

		// Pull the key-value pairs out of the database
		Enumeration<String> keys = DB.keys();
		

		for (String curKey = keys.nextElement(); keys.hasMoreElements(); curKey = keys
				.nextElement()) {

			Long curValue = DB.get(curKey);
			// Print it to the file
			quoteOutput.write(curKey + " " + curValue + newline);
		}

		// Now, move the old DB to a backup file
		File dbFile = new File(persistentFileName);
		String backupFileName = persistentFileName.concat(".bak");
		File backupFile = new File(backupFileName);
		boolean success = dbFile.renameTo(backupFile);
		if (!success) {
			IOException noRename = new IOException("Failed to rename file"
					+ persistentFileName + " to " + backupFileName);
			throw noRename;
		}

		// Rename the temporary file to the database file
		success = tempFile.renameTo(dbFile);
		if (!success) {
			IOException noRename = new IOException(
					"Failed to rename temp file to " + persistentFileName);
			throw noRename;
		}

	}
	
	public Long get(String key) {
		return DB.get(key);
	}
	
	public void put(String key, Long value) {
		DB.put(key, value);
	}
}
