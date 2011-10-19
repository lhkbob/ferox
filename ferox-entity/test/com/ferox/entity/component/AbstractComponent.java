package com.ferox.entity.component;

import com.ferox.entity.Component;
import com.ferox.entity.EntitySystem;
import com.ferox.entity.property.ObjectProperty;
import com.ferox.entity.property.Parameter;

/**
 * A test component that is abstract to test valid class hierarchies.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractComponent extends Component {
    @Parameter(type=int.class, value="1")
    private ObjectProperty property;
    
    protected AbstractComponent(EntitySystem system, int index) {
        super(system, index);
    }
    
    public Object getUserData() {
        return property.getIndexedData()[getIndex()];
    }
    
    public void setUserData(Object value) {
        property.getIndexedData()[getIndex()] = value;
    }
}
