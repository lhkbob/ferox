package com.ferox.entity.component;

import com.ferox.entity.Component;
import com.ferox.entity.EntitySystem;

/**
 * A Component definition with a public constructor.
 * 
 * @author Michael Ludwig
 */
public class PublicConstructorComponent extends Component {

    public PublicConstructorComponent(EntitySystem system, int index) {
        super(system, index);
    }
}
