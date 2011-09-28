package com.ferox.entity;

public class IndexedComponentMap {
    private final Component[] components;
    
    public IndexedComponentMap(Component[] components) {
        if (components == null)
            throw new NullPointerException("Components array cannot be null");
        for (int i = 0; i < components.length; i++) {
            if (components[i] == null)
                throw new NullPointerException("Component element cannot be null in array");
        }
        
        this.components = components;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Component> T get(TypedId<T> id) {
        for (int i = 0; i < components.length; i++) {
            if (components[i].getTypedId().equals(id))
                return (T) components[i];
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Component> T get(TypedId<T> id, int index) {
        Component c = components[index];
        if (!c.getTypedId().equals(id))
            throw new IllegalArgumentException("Component is not expected type, index=" + index + ", expected=" + id.getType() + ", actual=" + c.getClass());
        return (T) c;
    }
    
    public int size() {
        return components.length;
    }
}
