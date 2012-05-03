package com.ferox.scene;

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
    
    public Influences setInfluenced(Entity e, boolean canInfluence) {
        if (e == null)
            throw new NullPointerException("Entity cannot be null");
        
        // SetFactory ensures that this is not null
        Set<Entity> set = entities.get(getIndex(), 0);
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
        Set<Entity> set = entities.get(getIndex(), 0);
        return set.contains(e);
    }
    
    private static class SetFactory implements PropertyFactory<ObjectProperty<Set<Entity>>> {
        @Override
        public ObjectProperty<Set<Entity>> create() {
            return new ObjectProperty<Set<Entity>>(1);
        }

        @Override
        public void setDefaultValue(ObjectProperty<Set<Entity>> property, int index) {
            property.set(new HashSet<Entity>(), index, 0);
        }

        @Override
        public void clone(ObjectProperty<Set<Entity>> src, int srcIndex,
                          ObjectProperty<Set<Entity>> dst, int dstIndex) {
            Set<Entity> toClone = src.get(srcIndex, 0);
            dst.set(new HashSet<Entity>(toClone), dstIndex, 0);
        }
    }
}
