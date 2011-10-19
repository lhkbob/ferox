package com.ferox.entity.component;

import com.ferox.entity.Component;
import com.ferox.entity.EntitySystem;
import com.ferox.entity.property.IntProperty;
import com.ferox.entity.property.Parameter;

/**
 * A test component that tests the parameter constructor for IntProperty.
 * 
 * @author Michael Ludwig
 */
public class IntComponent extends Component {
    @Parameter(type=int.class, value="3")
    private IntProperty property;
    
    protected IntComponent(EntitySystem system, int index) {
        super(system, index);
    }
    
    public int getInt(int offset) {
        int index = getIndex() * 3 + offset;
        return property.getIndexedData()[index];
    }
    
    public void setInt(int offset, int value) {
        int index = getIndex() * 3 + offset;
        property.getIndexedData()[index] = value;
    }
}
