package modules.serverlogic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class SettingsManager {

    private volatile Properties settings = getEmptySettings();
    
    private static final String FILE_NAME = "settings.properties";

    public static final String DB_TYPE = "db_type"; //"MS SQL Server", "PostgreSQL", "SQLite" 
    public static final String DB_SERVER = "db_server";
    public static final String DB_NAME = "db_name";
    public static final String DB_LOGIN = "db_login";
    public static final String DB_PASSWORD = "db_password";

    public static final String EMAIL_HOST = "em_host";
    public static final String EMAIL_PORT = "em_port";
    public static final String EMAIL_FROM = "em_from";
    public static final String EMAIL_LOGIN = "em_login";
    public static final String EMAIL_PASSWORD = "em_password";

    public Properties getSettings() {
        return settings;
    }

    public void setSettings(Properties props) {
        settings = props;
    }

    private static Set getSettingsKeySet() {

        Set<String> keySet = new HashSet<String>();

        keySet.add(DB_TYPE);
        keySet.add(DB_SERVER);
        keySet.add(DB_NAME);
        keySet.add(DB_LOGIN);
        keySet.add(DB_PASSWORD);

        keySet.add(EMAIL_HOST);
        keySet.add(EMAIL_PORT);
        keySet.add(EMAIL_FROM);
        keySet.add(EMAIL_LOGIN);
        keySet.add(EMAIL_PASSWORD);

        return keySet;

    }

    private static final Properties getSettingsMap() {

        Properties settings = new Properties();
        Set keySet = getSettingsKeySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            settings.setProperty((String) it.next(), "");
        }
        return settings;

    }

    public static final Properties getEmptySettings() {

        Properties settings = new Properties();
        Set keySet = getSettingsKeySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            settings.setProperty((String) it.next(), "");
        }
        return settings;

    }

    public void loadSettings() throws FileNotFoundException, IOException {
        File settingsFile = new File(FILE_NAME);
        if (settingsFile.exists()) {
            FileReader fileReader = new FileReader(settingsFile);
            settings.load(fileReader);
            if (checkSettings(settings)) {
                return;
            }
        }
        settings = getEmptySettings();        
    }
    
    public void saveSettings() throws IOException {
        File settingsFile = new File(FILE_NAME);
        if (!settingsFile.exists()) {
            settingsFile.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(settingsFile);
        settings.store(fileWriter, null);
        fileWriter.flush();
    }

    public static boolean checkSettings(Properties settings) {

        boolean res = true;
        Set keySet = getSettingsKeySet();
        Iterator it = keySet.iterator();
        while (it.hasNext() && res) {
            res = settings.containsKey((String) it.next());
        }
        return res;

    }
    
    public Properties getEmailProperties() {
        Properties emailprops = new Properties();
        emailprops.setProperty("mail.smtp.host", settings.getProperty(SettingsManager.EMAIL_HOST));
        emailprops.setProperty("mail.smtp.port", settings.getProperty(SettingsManager.EMAIL_PORT));
        emailprops.setProperty("from", settings.getProperty(SettingsManager.EMAIL_FROM));
        emailprops.setProperty("login", settings.getProperty(SettingsManager.EMAIL_LOGIN));
        emailprops.setProperty("pwd", settings.getProperty(SettingsManager.EMAIL_PASSWORD));
        return emailprops;
    }

}
