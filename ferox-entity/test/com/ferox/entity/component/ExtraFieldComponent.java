package com.ferox.entity.component;

import com.ferox.entity.Component;
import com.ferox.entity.EntitySystem;
import com.ferox.entity.property.FloatProperty;
import com.ferox.entity.property.Parameter;

/**
 * A test component that defines a non-Property field.
 * 
 * @author Michael Ludwig
 */
public class ExtraFieldComponent extends Component {
    @SuppressWarnings("unused")
    @Parameter(type=int.class, value="1")
    private FloatProperty property;
    
    @SuppressWarnings("unused")
    private Object otherField;
    
    protected ExtraFieldComponent(EntitySystem system, int index) {
        super(system, index);
    }
}
