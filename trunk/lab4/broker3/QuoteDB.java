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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jdbc.JDBCExample;

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
    	int    databaseHostPort = 6969;
    	String databaseName = "jay";
    	String databaseUserName = "robin162";
    	String databaseUserPassword = "";
    	
    	try {
    		/* load the driver */
    		Class.forName(databaseDriverName);
    		/* open a connection */
    		String url = this.URL_PREFIX + databaseHostName + ":" + databaseHostPort + "/" + databaseName;
    		/* format: DriverManager.getConnection(url, username, password); */
    		System.out.println("DEBUG: Opening connection to " + url);
    		db = DriverManager.getConnection(url, databaseUserName, databaseUserPassword);
    		
    		/* create table and insert values */
    		String createString = "CREATE TABLE " + tableName + " ( symbol text , quote integer )";
    		
    		Statement st = db.createStatement();
    		st.executeUpdate(createString);
    		st.close();
    		
    		

    	} catch (ClassNotFoundException e) {
    		System.err.println("ERROR: JDBC Driver class not found!!");
    		System.exit(-1);
    	} catch (SQLException e) {
    		System.err.println("ERROR: SQL Exception!!");
    		e.printStackTrace();
    		System.exit(-1);	
    	}
	}

	public void close() throws IOException {
		/* finally delete table */
		dropTable(db, table);
		db.close();
	}

	public void flush() throws IOException {

	}

	public Long get(String key) {

	}

	public void put(String key, Long value) {
		PreparedStatement ps = db.prepareStatement("INSERT INTO " + tableName + " VALUES ( ? , ? )");
		for(int i=0; i < PROVINCES.length; i++) {
			ps.setInt(1, i);
			ps.setString(2, PROVINCES[i]);
			rs = ps.executeUpdate();
		}
		ps.close();
	}

	public boolean containsKey(String key) {
	}

	public Long remove(String key) {
	}

}
