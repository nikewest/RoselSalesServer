package roselsalesserver;

import java.util.Properties;
import roselsalesserver.db.DatabaseManager;

/**
 *
 * @author nikiforovnikita
 */
public final class ServerSettings {
    
    public static final String DB_TYPE = "db_type";
    public static final String DB_SERVER = "db_server";
    public static final String DB_NAME = "db_name";
    public static final String DB_LOGIN = "db_login";
    public static final String DB_PASSWORD = "db_password";        
    
    public static final String EMAIL_HOST = "em_host";        
    public static final String EMAIL_PORT = "em_port";        
    public static final String EMAIL_FROM = "em_from";        
    public static final String EMAIL_LOGIN = "em_login";        
    public static final String EMAIL_PASSWORD = "em_password";       
    
    public static final Properties getSettingsMap(){        
        Properties settings = new Properties();
        settings.setProperty(DB_TYPE, "");
        settings.setProperty(DB_SERVER, "");
        settings.setProperty(DB_NAME, "");
        settings.setProperty(DB_LOGIN, "");
        settings.setProperty(DB_PASSWORD, "");
        settings.setProperty(EMAIL_HOST, "");
        settings.setProperty(EMAIL_PORT, "");
        settings.setProperty(EMAIL_FROM, "");
        settings.setProperty(EMAIL_LOGIN, "");
        settings.setProperty(EMAIL_PASSWORD, "");
        return settings;
    }
}
