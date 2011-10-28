package com.ferox.scene;

import com.ferox.entity2.Component;
import com.ferox.entity2.Template;
import com.ferox.entity2.TypedId;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;

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
    public static final TypedId<DiffuseColorMap> ID = Component.getTypedId(DiffuseColorMap.class);

    /**
     * Create a DiffuseColorMap that uses the given Texture as the source for
     * per-pixel diffuse material colors.
     * 
     * @param diffuse The diffuse texture
     * @param texCoords The texture coordinates used to access diffuse
     * @throws NullPointerException if diffuse or texCoords is null
     */
    public DiffuseColorMap(Texture diffuse, VertexAttribute texCoords) {
        super(diffuse, texCoords);
    }

    /**
     * Create a DiffuseColorMap that is a clone of <tt>clone</tt> for use with a
     * {@link Template}.
     * 
     * @param clone The component to clone
     * @throws NullPointerException if clone is null
     */
    public DiffuseColorMap(DiffuseColorMap clone) {
        super(clone);
    }

    @Override
    protected void validate(Texture tex) {
        // do nothing
    }
}
