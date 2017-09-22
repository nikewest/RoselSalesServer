package modules.serverlogic;

import java.util.Properties;

public class SettingsManager {

    private static Properties settings = new Properties();

    private SettingsManager() {
    }
    
    static{
        settings.put("server", "SQL");
        settings.put("database", "mobiletest");
        settings.put("url", "jdbc:sqlserver://" + settings.getProperty("server") + ";databaseName=" + settings.getProperty("database"));
        settings.put("username", "mobileuser");
        settings.put("password", "Robocop2/0");
    }
    
    public static Properties getSettings(){
        return settings;
    }
    
    public static void setSettings(Properties props){
        settings = props;
    }
    
    public static void loadSettings(){
        //load settings from file
    }
    
}
