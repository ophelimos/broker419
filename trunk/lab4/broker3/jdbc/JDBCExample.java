package jdbc;

/**
 *
 * JDBCExample.java
 * ------------------------------
 * Gokul Soundararajan
 * 
 * Simple JDBC Example using the POSTGRES driver
 * Connects to DB, makes a dummy table,
 * Inserts dummy values, queries dummy results
 * Deletes dummy values
 *
 **/

import java.sql.*;

public class JDBCExample {
    
	/* global constants */
	public static final String URL_PREFIX = "jdbc:postgresql://";
	public static final String[] PROVINCES = { 
		"Alberta",
		"Manitoba",
		"Newfoundland and Labrador",
		"Nova Scotia",
		"Ontario",
		"Quebec",
		"Yukon",
		"British Columbia",
		"New Brunswick",
		"Northwest Territories",
		"Nunavut",
		"Prince Edward Island",
		"Saskatchewan"
	};
	
	private static void createAndInsertValues(Connection db, String tableName) throws SQLException {

		String createString = "CREATE TABLE " + tableName + " ( id int , name varchar(25) )";
		
		Statement st = db.createStatement();
		int rs = st.executeUpdate(createString);
		st.close();
		
		PreparedStatement ps = db.prepareStatement("INSERT INTO " + tableName + " VALUES ( ? , ? )");
		for(int i=0; i < PROVINCES.length; i++) {
			ps.setInt(1, i);
			ps.setString(2, PROVINCES[i]);
			rs = ps.executeUpdate();
		}
		ps.close();

	}
	
	private static void runSomeQueries(Connection db, String tableName, int id) throws SQLException {
		
		String query = "SELECT * FROM " + tableName + " WHERE id = ?";
		
		PreparedStatement ps = db.prepareStatement(query);
		ps.setInt(1, id);
		ResultSet rs = ps.executeQuery();
		if(rs != null) {
			while(rs.next()) {
				System.out.println("ID: " + id + " is province " + rs.getString("name"));
			}
			rs.close();
		} else {
			System.out.println("ID: " + id + " not found !!");
		}

		ps.close();		
		
	}
	
	private static void dropTable(Connection db, String tableName) throws SQLException {
		

		String createString = "DROP TABLE " + tableName;
		
		Statement st = db.createStatement();
		int rs = st.executeUpdate(createString);
		st.close();		
		
	}
	
    public static void main(String args[]) {
    	
    	/* parse input arguments */
    	String databaseDriverName = "org.postgresql.Driver";
    	String databaseHostName = "localhost";
    	int    databaseHostPort = 5432;
    	String databaseName = "smaple";
    	String databaseUserName = "sutharja";
    	String databaseUserPassword = "";
    	
    	/* open connection */
    	Connection dbConnection = null;
    	try {
    		/* load the driver */
    		Class.forName(databaseDriverName);
    		/* open a connection */
    		String url = JDBCExample.URL_PREFIX + databaseHostName + ":" + databaseHostPort + "/" + databaseName;
    		/* format: DriverManager.getConnection(url, username, password); */
    		System.out.println("DEBUG: Opening connection to " + url);
    		Connection db = DriverManager.getConnection(url, databaseUserName, databaseUserPassword);
    		/* create table and insert values */
    		String table = "provinces";
    		/* create table and insert some data */
    		createAndInsertValues(db, table);
    		/* query */
    		runSomeQueries(db, table, 1);
    		runSomeQueries(db, table, 2);
    		runSomeQueries(db, table, 3);
    		/* finally delete table */
    		dropTable(db, table);
    		db.close();
    	} catch (ClassNotFoundException e) {
    		System.err.println("ERROR: JDBC Driver class not found!!");
    		System.exit(-1);
    	} catch (SQLException e) {
    		System.err.println("ERROR: SQL Exception!!");
    		e.printStackTrace();
    		System.exit(-1);	
    	}
    	
    }

}
