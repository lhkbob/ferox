package com.ferox.scene;

import com.ferox.resource.Texture;
import com.lhkbob.entreri.TypeId;

/**
 * <p>
 * DiffuseColorMap provides diffuse material colors, much like
 * {@link DiffuseColor}, except that it uses a {@link Texture} to have per-texel
 * coloration instead of a single color across the entire Entity. It is not
 * defined how the geometry of the Entity is mapped onto the texture, but will
 * likely use texture coordinates stored in the geometry. This should be
 * configured by the rendering controller, or in other component types.
 * </p>
 * <p>
 * Alpha values within the texture map will be treated as per-texel opacity
 * values, with the same definition as {@link Transparent}, although they will
 * be ignored if the Transparent component is not added to the Entity. When the
 * Entity is transparent, the opacity of the Transparent component and the
 * texture are multiplied together to get the final opacity for a pixel.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class DiffuseColorMap extends TextureMap<DiffuseColorMap> {
    /**
     * The shared TypedId representing DiffuseColorMap.
     */
    public static final TypeId<DiffuseColorMap> ID = TypeId.get(DiffuseColorMap.class);

    private DiffuseColorMap() {}

    @Override
    protected void validate(Texture tex) {
        // do nothing
    }
}
