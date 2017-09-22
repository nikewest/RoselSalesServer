package modules.data;

public abstract class UpdateStructureFactory {
    
    UpdateItemFactory updateItemFactory;

    public UpdateStructureFactory(UpdateItemFactory updateItemFactory) {
        this.updateItemFactory = updateItemFactory;
    }
    
    public abstract RoselUpdateStructure fillFromJSONString(String JSONString);
    
}
