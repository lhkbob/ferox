package com.ferox.scene;

import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.TypeId;

public final class Influences extends EntitySetComponent<Influences> {
    /**
     * TypeId for Influences Component type
     */
    public static final TypeId<Influences> ID = TypeId.get(Influences.class);
    
    private Influences() { }
    
    public Influences setInfluenced(Entity e, boolean canInfluence) {
        return setInfluenced(e.getId(), canInfluence);
    }
    
    public Influences setInfluenced(int entityId, boolean canInfluence) {
        if (canInfluence)
            addInternal(entityId);
        else
            removeInternal(entityId);
        return this;
    }
    
    public boolean canInfluence(Entity e) {
        return canInfluence(e.getId());
    }
    
    public boolean canInfluence(int entityId) {
        return containsInternal(entityId);
    }
}
