package com.ferox.scene;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.Factory;
import com.lhkbob.entreri.PropertyFactory;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.property.ObjectProperty;

public final class Influences extends ComponentData<Influences> {
    /**
     * TypeId for Influences Component type
     */
    public static final TypeId<Influences> ID = TypeId.get(Influences.class);
    
    @Factory(SetFactory.class)
    private ObjectProperty<Set<Entity>> entities;
    
    private Influences() { }
    
    public Set<Entity> getInfluencedSet() {
        return Collections.unmodifiableSet(entities.get(getIndex()));
    }
    
    public Influences setInfluenced(Entity e, boolean canInfluence) {
        if (e == null)
            throw new NullPointerException("Entity cannot be null");
        
        // SetFactory ensures that this is not null
        Set<Entity> set = entities.get(getIndex());
        if (canInfluence)
            set.add(e);
        else
            set.remove(e);
        return this;
    }
    
    public boolean canInfluence(Entity e) {
        if (e == null)
            throw new NullPointerException("Entity cannot be null");
        
        // SetFactory ensures that this is not null
        Set<Entity> set = entities.get(getIndex());
        return set.contains(e);
    }
    
    private static class SetFactory implements PropertyFactory<ObjectProperty<Set<Entity>>> {
        @Override
        public ObjectProperty<Set<Entity>> create() {
            return new ObjectProperty<Set<Entity>>();
        }

        @Override
        public void setDefaultValue(ObjectProperty<Set<Entity>> property, int index) {
            property.set(new HashSet<Entity>(), index);
        }

        @Override
        public void clone(ObjectProperty<Set<Entity>> src, int srcIndex,
                          ObjectProperty<Set<Entity>> dst, int dstIndex) {
            Set<Entity> toClone = src.get(srcIndex);
            dst.set(new HashSet<Entity>(toClone), dstIndex);
        }
    }
}
