package src.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens and closes the SQLite connection.
 *
 * The user types a database name such as "company" and this class opens
 * resources/company.db. A name that already ends in ".db" is also accepted.
 * Only one connection is kept open at a time. Opening a new one closes the old one.
 */
public class DatabaseManager {

	private static final String FOLDER = "resources";
	private static Connection connection = null;
	private static String currentName = null;

	public static Connection open(String dbName) throws SQLException {
		closeConnection();

		dbName = dbName == null ? "" : dbName.trim();
		if (dbName.isEmpty()) {
			throw new SQLException("Please type a database name first.");
		}
		String fileName = dbName.toLowerCase().endsWith(".db") ? dbName : dbName + ".db";
		File file = new File(FOLDER, fileName);

		// SQLite would silently create a new empty file, so check that it exists first.
		if (!file.isFile()) {
			throw new SQLException("File not found: " + FOLDER + "/" + fileName);
		}

		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			throw new SQLException("SQLite JDBC driver not found. Put the sqlite-jdbc jar from lib/ on the classpath.");
		}

		connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());
		currentName = dbName;
		return connection;
	}

	public static Connection getConnection() throws SQLException {
		if (connection == null || connection.isClosed()) {
			throw new SQLException("No database is open. Type a name and press Fill.");
		}
		return connection;
	}

	public static boolean isOpen() {
		try {
			return connection != null && !connection.isClosed();
		} catch (SQLException e) {
			return false;
		}
	}

	public static String getCurrentName() {
		return currentName;
	}

	public static void closeConnection() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		connection = null;
		currentName = null;
	}
}
