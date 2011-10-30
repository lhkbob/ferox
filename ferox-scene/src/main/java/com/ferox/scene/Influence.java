package com.ferox.scene;

import java.util.Set;

import com.ferox.entity2.Entity;
import com.ferox.entity2.TypedComponent;
import com.ferox.math.bounds.AxisAlignedBox;

// FIXME: does it make more sense to set this on the actor,
// or the things that can be acted upon?
// e.g. say a renderable claims it must or cannot be influenced by a light?
public class Influence extends TypedComponent<Influence> {
    private final AxisAlignedBox bounds;
    private float radius;
    
    private final Set<Entity> exclude;
    private final Set<Entity> limitTo;
    
    public Influence() {
        super(null, false);
        // FIXME:
    }
    
    public Influence(Influence clone) {
        super(clone, true);
        // FIXME:
    }
    //FIXME: Influences will be a limitTo-only component and is added to the actor
    // FIXME: Add a NotInfluenced that is exclude-only and is added to the acted-on

}
