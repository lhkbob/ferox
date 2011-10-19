package com.ferox.entity.component;

import com.ferox.entity.EntitySystem;

/**
 * A test Component that extends a non-abstract component so it should fail.
 * 
 * @author Michael Ludwig
 */
public class InvalidHierarchyComponent extends FloatComponent {
    protected InvalidHierarchyComponent(EntitySystem system, int index) {
        super(system, index);
    }
}
