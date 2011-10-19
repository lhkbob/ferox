package com.ferox.entity.component;

import com.ferox.entity.Component;
import com.ferox.entity.EntitySystem;
import com.ferox.entity.property.FloatProperty;
import com.ferox.entity.property.Parameter;

/**
 * An invalid component definition where a Property is declared as a public
 * field.
 * 
 * @author Michael Ludwig
 */
public class PublicPropertyComponent extends Component {
    @Parameter(type=int.class, value="1")
    public FloatProperty property;
    
    protected PublicPropertyComponent(EntitySystem system, int index) {
        super(system, index);
    }
}
