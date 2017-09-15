package modules.serverlogic;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

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
    
    private static Set getSettingsKeySet(){
        
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
    
    public static final Properties getSettingsMap(){ 
        
        Properties settings = new Properties();
        Set keySet = getSettingsKeySet();        
        Iterator it = keySet.iterator();
        while(it.hasNext()){
            settings.setProperty((String) it.next(), "");
        }
        return settings;
        
    }
    
    public static final Properties getEmptySettings(){   
        
        Properties settings = new Properties();
        Set keySet = getSettingsKeySet();        
        Iterator it = keySet.iterator();
        while(it.hasNext()){
            settings.setProperty((String) it.next(), "");
        }
        return settings;
        
    }
    
    public static boolean checkSettings(Properties settings){
        
        boolean res = true;
        Set keySet = getSettingsKeySet();        
        Iterator it = keySet.iterator();
        while(it.hasNext() && res){            
            res = settings.containsKey((String) it.next());
        }
        return res;    
        
    }
}
