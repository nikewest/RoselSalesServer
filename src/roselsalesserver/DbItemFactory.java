package roselsalesserver;

public abstract class DbItemFactory {
        
    public abstract DbItem fillFromJSONString(String jsonString);
    
}
