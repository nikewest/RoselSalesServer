package roselsalesserver.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import modules.serverlogic.ServerSettings;

public class DatabaseManager {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());

    private static final String JDBC_DRIVER_POSTGRE = "org.postgresql.Driver";
    private static final String JDBC_DRIVER_SQLITE = "org.sqlite.JDBC";
    private static final String JDBC_DRIVER_SQL = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private String driverName;
    private String dbUrl;
    private String dbLogin;
    private String dbPwd;
    private Connection dbConnection = null;

    private volatile static DatabaseManager databaseManagerInstance = null;

    private DatabaseManager(Properties prop) throws ClassNotFoundException {
        setConnectionProperties(prop);
    }

    public Connection getDbConnection() throws SQLException {
        if (dbConnection == null || dbConnection.isClosed()) {
            initConnection();
        }
        return dbConnection;
    }

    public static DatabaseManager getDatabaseManager(Properties prop) throws ClassNotFoundException {
        if (databaseManagerInstance == null) {            
            synchronized (DatabaseManager.class) {
                if (databaseManagerInstance == null) {
                    databaseManagerInstance = new DatabaseManager(prop);
                }
            }
        }
        return databaseManagerInstance;
    }

    public final void setConnectionProperties(Properties connectionProperties) {
        switch (connectionProperties.getProperty(ServerSettings.DB_TYPE)) {
            case "MS SQL Server":
                driverName = JDBC_DRIVER_SQL;
                dbUrl = "jdbc:sqlserver://" + connectionProperties.getProperty(ServerSettings.DB_SERVER) + ";databaseName=" + connectionProperties.getProperty(ServerSettings.DB_NAME);
                break;
            case "PostgreSQL":
                driverName = JDBC_DRIVER_POSTGRE;
                dbUrl = "jdbc:postgresql://" + connectionProperties.getProperty(ServerSettings.DB_SERVER) + "/" + connectionProperties.getProperty(ServerSettings.DB_NAME);
                break;
            case "SQLite":
                driverName = JDBC_DRIVER_SQLITE;
                dbUrl = "jdbc:sqlite://" + connectionProperties.getProperty(ServerSettings.DB_NAME);
                break;
        }
        dbLogin = connectionProperties.getProperty(ServerSettings.DB_LOGIN);
        dbPwd = connectionProperties.getProperty(ServerSettings.DB_PASSWORD);
    }

    private static boolean checkProperties(Properties prop) {
        return prop.containsKey(ServerSettings.DB_SERVER) && prop.containsKey(ServerSettings.DB_NAME) && prop.containsKey(ServerSettings.DB_NAME) && prop.containsKey(ServerSettings.DB_TYPE) && prop.containsKey(ServerSettings.DB_LOGIN) && prop.containsKey(ServerSettings.DB_PASSWORD);
    }

    public void initConnection() throws SQLException {
        dbConnection = DriverManager.getConnection(dbUrl, dbLogin, dbPwd);
    }

    public void endWork() {
        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    public void executeQuery(String queryString) throws SQLException {
        try (Statement stmt = getDbConnection().createStatement()) {
            stmt.execute(queryString);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw ex;
        }
    }
}
