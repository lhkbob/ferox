package com.ferox.entity.component;

import com.ferox.entity.Component;
import com.ferox.entity.EntitySystem;
import com.ferox.entity.property.Factory;
import com.ferox.entity.property.FloatProperty;
import com.ferox.entity.property.FloatPropertyFactory;
import com.ferox.entity.property.MultiParameterProperty;
import com.ferox.entity.property.NoParameterProperty;
import com.ferox.entity.property.Parameter;
import com.ferox.entity.property.Parameters;

/**
 * A Component that tests a variety of property constructors.
 * 
 * @author Michael Ludwig
 */
public class MultiPropertyComponent extends Component {
    @Parameters({@Parameter(type=int.class, value="2"),
                 @Parameter(type=float.class, value="0.3")})
    protected MultiParameterProperty multi;
    
    protected NoParameterProperty noparams;
    
    @Factory(FloatPropertyFactory.class)
    protected FloatProperty fromFactory;
    
    protected MultiPropertyComponent(EntitySystem system, int index) {
        super(system, index);
    }
    
    public void setFloat(int offset, float f) {
        multi.setFloat(offset + getIndex() * 2, f);
    }
    
    public float getFloat(int offset) {
        return multi.getFloat(offset + getIndex() * 2);
    }
    
    public NoParameterProperty getCompactProperty() {
        return noparams;
    }
}
