package com.ferox.scene;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.entreri.AxisAlignedBoxProperty;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.Unmanaged;
import com.lhkbob.entreri.property.BooleanProperty;

public final class InfluenceRegion extends ComponentData<InfluenceRegion> {
    /**
     * TypeId for InfluenceRegion component type.
     */
    public static final TypeId<InfluenceRegion> ID = TypeId.get(InfluenceRegion.class);
    
    private AxisAlignedBoxProperty bounds;
    private BooleanProperty negate;
    
    @Unmanaged
    private final AxisAlignedBox boundsCache = new AxisAlignedBox();
    
    private InfluenceRegion() { }
    
    public @Const AxisAlignedBox getBounds() {
        return boundsCache;
    }
    
    public InfluenceRegion setBounds(@Const AxisAlignedBox bounds) {
        if (bounds == null)
            throw new NullPointerException("Bounds cannot be null");
        boundsCache.set(bounds);
        this.bounds.set(bounds, getIndex());
        return this;
    }
    
    public boolean isNegated() {
        return negate.get(getIndex(), 0);
    }
    
    public InfluenceRegion setNegated(boolean negate) {
        this.negate.set(negate, getIndex(), 0);
        return this;
    }
    
    @Override
    protected void onSet(int index) {
        bounds.get(index, boundsCache);
    }
}
