package com.grocery.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages JDBC connections to MySQL database for Online Grocery Ordering System.
 * Supports configurable connection properties through environment variables,
 * JVM system properties, or a local db.properties file.
 */
public class DatabaseConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/online_grocery_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean driverLoaded = false;

    static {
        loadConfiguration();
        ensureDriver();
    }

    private static void ensureDriver() {
        if (!driverLoaded) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                System.err.println("[DatabaseConnection] Warning: MySQL JDBC driver class 'com.mysql.cj.jdbc.Driver' not found.");
                System.err.println("Ensure lib/mysql-connector-j.jar is present on the classpath.");
            }
        }
    }

    /**
     * Loads database configuration with priority:
     * 1. Environment variables (DB_URL, DB_USER, DB_PASSWORD)
     * 2. System properties (db.url, db.user, db.password)
     * 3. Local file db.properties (in working directory or classpath)
     * 4. Safe defaults
     */
    public static synchronized void loadConfiguration() {
        Properties props = new Properties();

        // 1. Try loading from working directory db.properties
        File file = new File("db.properties");
        if (file.exists() && file.isFile()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (Exception ignored) {
            }
        } else {
            // Try loading from classpath
            try (InputStream is = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) {
                    props.load(is);
                }
            } catch (Exception ignored) {
            }
        }

        // URL resolution
        String envUrl = System.getenv("DB_URL");
        String sysUrl = System.getProperty("db.url");
        String propUrl = props.getProperty("db.url");
        dbUrl = (envUrl != null && !envUrl.isBlank()) ? envUrl.trim() :
                (sysUrl != null && !sysUrl.isBlank()) ? sysUrl.trim() :
                (propUrl != null && !propUrl.isBlank()) ? propUrl.trim() :
                DEFAULT_URL;

        // User resolution
        String envUser = System.getenv("DB_USER");
        String sysUser = System.getProperty("db.user");
        String propUser = props.getProperty("db.user");
        dbUser = (envUser != null && !envUser.isBlank()) ? envUser.trim() :
                 (sysUser != null && !sysUser.isBlank()) ? sysUser.trim() :
                 (propUser != null && !propUser.isBlank()) ? propUser.trim() :
                 DEFAULT_USER;

        // Password resolution
        String envPass = System.getenv("DB_PASSWORD");
        String sysPass = System.getProperty("db.password");
        String propPass = props.getProperty("db.password");
        dbPassword = (envPass != null) ? envPass :
                     (sysPass != null) ? sysPass :
                     (propPass != null) ? propPass :
                     DEFAULT_PASSWORD;
    }

    /**
     * Obtains a new database connection from DriverManager.
     * Callers must close this connection using try-with-resources.
     *
     * @return active java.sql.Connection
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        ensureDriver();
        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            System.err.println("\n[ERROR] Database connection failed to URL: " + dbUrl);
            System.err.println("[ERROR] User: " + dbUser);
            System.err.println("[ERROR] Reason: " + e.getMessage());
            System.err.println("[HINT] Ensure MySQL server is running and database 'online_grocery_db' is imported.");
            System.err.println("[HINT] Configure credentials in 'db.properties' or via DB_USER & DB_PASSWORD environment variables.");
            throw e;
        }
    }

    /**
     * Tests the database connection.
     *
     * @return true if connection succeeded, false otherwise
     */
    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con != null && !con.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getDbUser() {
        return dbUser;
    }

    /**
     * Programmatic override for testing or interactive configuration.
     */
    public static synchronized void setCredentials(String url, String user, String password) {
        if (url != null && !url.isBlank()) dbUrl = url;
        if (user != null && !user.isBlank()) dbUser = user;
        if (password != null) dbPassword = password;
    }
}

