package com.ferox.scene;

import com.ferox.resource.Texture;
import com.lhkbob.entreri.TypeId;

/**
 * <p>
 * DecalColorMap is a texture map that should be modulated with the diffuse
 * color or diffuse color map to act as a decal. This means that areas where
 * it's transparent should match the underlying diffuse color, but where it's
 * opaque it should be unaffected by the underlying color.
 * </p>
 * <p>
 * This is different than the modulation between a diffuse color and texture,
 * where it is multiplicative.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class DecalColorMap extends TextureMap<DecalColorMap> {
    /**
     * TypeId for DecalColorMap.
     */
    public static final TypeId<DecalColorMap> ID = TypeId.get(DecalColorMap.class);
    
    private DecalColorMap() { }
    
    @Override
    protected void validate(Texture tex) {
        // do nothing
    }
}
