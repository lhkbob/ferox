package com.ferox.entity.component;

import com.ferox.entity.Component;
import com.ferox.entity.EntitySystem;

/**
 * A test component that has an illegal constructor so it should fail to get a
 * TypedId.
 * 
 * @author Michael Ludwig
 */
public class BadConstructorComponent extends Component {
    protected BadConstructorComponent(EntitySystem system, int index, Object extraArgument) {
        super(system, index);
    }
}
