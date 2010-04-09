import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

	public final String URL_PREFIX = "jdbc:postgresql://";

	private Connection db;

	private String tableName = "quotes";

	private String persistentFileName;

	private String COL_1 = "symbol";

	private String COL_2 = "quote";

	PreparedStatement insertStatement;

	PreparedStatement getStatement;

	PreparedStatement deleteStatement;

	// Platform independent newline
	public static String newline = System.getProperty("line.separator");

	QuoteDB(String fileName) {
		try {
			this.open(fileName);
		} catch (IOException e) {
			System.out.println("Failed to open database file " + fileName);
		}
	}

	QuoteDB() {
		// Don't open anything
	}

	/**
	 * Open PostgreSQL database
	 * 
	 * @param fileName
	 */
	public void open(String fileName) throws IOException {
		/* parse input arguments */
		String databaseDriverName = "org.postgresql.Driver";
		String databaseHostName = "localhost";
		int databaseHostPort = 6969;
		String databaseName = "jay";
		String databaseUserName = "robin162";
		String databaseUserPassword = "";

		try {
			/* load the driver */
			Class.forName(databaseDriverName);
			/* open a connection */
			String url = this.URL_PREFIX + databaseHostName + ":"
					+ databaseHostPort + "/" + databaseName;
			/* format: DriverManager.getConnection(url, username, password); */
			System.out.println("DEBUG: Opening connection to " + url);
			db = DriverManager.getConnection(url, databaseUserName,
					databaseUserPassword);

			boolean haveTable = false;

			/* create table */
			
			try {
				String createString = "CREATE TABLE " + tableName + " ( "
						+ COL_1 + " text , " + COL_2 + " integer )";

				Statement st = db.createStatement();
				st.executeUpdate(createString);
				st.close();
			} catch (SQLException e) {
				// Looks like we already have a table.  Just keep going
				System.out.println("Table already exists");
			}
			// Make some prepared statements
			insertStatement = db.prepareStatement("INSERT INTO " + tableName
					+ " VALUES ( ? , ? )");
			getStatement = db.prepareStatement("SELECT * FROM " + tableName
					+ " WHERE " + COL_1 + " = ?");
			deleteStatement = db.prepareStatement("DELETE FROM " + tableName
					+ " WHERE ( " + COL_1 + " = ?)");

			/* Insert values from flat file */
			try {

				// Set the persistent file name
				persistentFileName = fileName;

				// Read the quotes file into an internal hash table
				Scanner quoteInput = new Scanner(new BufferedReader(
						new FileReader(fileName)));

				while (quoteInput.hasNext()) {

					String symbol = quoteInput.next();
					symbol = symbol.toLowerCase();
					Long value = Long.parseLong(quoteInput.next());

					this.put(symbol, value);
				}

				quoteInput.close();

			} catch (FileNotFoundException e) {
				System.out.println("Can't find database file: "
						+ e.getMessage() + " in directory "
						+ System.getProperty("user.dir"));
				System.exit(-1);
			} catch (NoSuchElementException e) {
				System.out.println("Error reading database file " + fileName);
				System.out.println("Continuing");
				/* quoteInput.close(); */
			}

		} catch (ClassNotFoundException e) {
			System.err.println("ERROR: JDBC Driver class not found!!");
			System.exit(-1);
		} catch (SQLException e) {
			System.err.println("ERROR: SQL Exception during open");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void close() throws IOException {
		// Flush table to disk
		this.flush();
		// Clear the file name
		persistentFileName = null;

		/* finally delete table */
		String createString = "DROP TABLE " + tableName;

		try {
			Statement st = db.createStatement();
			st.executeUpdate(createString);

			db.close();
		} catch (SQLException e) {
			System.err.println("ERROR: SQL Exception during close");
			System.out.println(e.getMessage());
		}

	}

	public Long get(String key) {
		try {
			getStatement.setString(1, key);
			ResultSet rs = getStatement.executeQuery();
			if (rs != null) {
				rs.next();
				Long returnval = rs.getLong(COL_2);
				return returnval;
			}
		} catch (SQLException e) {
			System.err.println("ERROR: SQL Exception during get");
			System.out.println(e.getMessage());
		}
		return null;
	}

	public void put(String key, Long value) {
		try {
			insertStatement.setString(1, key);
			insertStatement.setLong(2, value);
			insertStatement.executeUpdate();
		} catch (SQLException e) {
			System.err.println("ERROR: SQL Exception during put");
			System.out.println(e.getMessage());
		}
	}

	public boolean containsKey(String key) {
		try {
			getStatement.setString(1, key);
			ResultSet rs = getStatement.executeQuery();
			if (rs != null) {
				rs.next();
				Long returnval = rs.getLong(COL_2);
				return true;
			}
		} catch (SQLException e) {
			// Return false
		}
		
		return false;
	}

	public Long remove(String key) {
		try {
			deleteStatement.setString(1, key);
			deleteStatement.executeUpdate();
		} catch (SQLException e) {
			System.err.println("ERROR: SQL Exception during remove");
			System.out.println(e.getMessage());
		}

		Long returnval = (long) 0;
		return returnval;
	}

	public void flush() throws IOException {
		try {
			// Write the DB to a temporary file first, and then overwrite,
			// rather
			// than accidentally truncating it
			File cwd = new File(System.getProperty("user.dir"));
			File tempFile = File.createTempFile("dbtemp", ".tmp", cwd);
			BufferedWriter quoteOutput = new BufferedWriter(new FileWriter(
					tempFile));

			// Pull everything out of the database
			Statement stmt = db
					.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
							ResultSet.CONCUR_READ_ONLY);
			ResultSet srs = stmt.executeQuery("SELECT * FROM " + tableName);
			while (srs.next()) {
				String name = srs.getString(COL_1);
				int value = srs.getInt(COL_2);
				quoteOutput.write(name + " " + value + newline);
			}

			// Flush the temporary file
			quoteOutput.flush();

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
		} catch (SQLException e) {
			System.err.println("ERROR: SQL Exception during remove");
			System.out.println(e.getMessage());
		}
	}

}
