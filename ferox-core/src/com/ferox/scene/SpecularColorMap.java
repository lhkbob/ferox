package com.ferox.scene;

import com.ferox.entity.Component;
import com.ferox.entity.Template;
import com.ferox.entity.TypedId;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;

/**
 * <p>
 * SpecularColorMap functions like {@link SpecularColor} except that it provides
 * per-texel specular material. Black texel values will effectively disable
 * specular highlights for those regions of the texture. Like
 * {@link EmittedColorMap}, any alpha component has no defined behavior and can
 * be used by the renderer in different ways (possibly as an exponent or
 * shininess factor as used in a {@link BlinnPhongMaterial}).
 * </p>
 * <p>
 * It is not defined how the geometry of the Entity is mapped onto the texture,
 * but will likely use texture coordinates stored in the geometry. This should
 * be configured by the rendering controller, or in other component types. Any
 * texture mapping should likely match the texture mapping used for a
 * {@link DiffuseColorMap}.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class SpecularColorMap extends TextureMap<SpecularColorMap> {
    /**
     * The shared TypedId representing DepthOffsetMap.
     */
    public static final TypedId<SpecularColorMap> ID = Component.getTypedId(SpecularColorMap.class);
    
    /**
     * Create a SpecularColorMap that uses the given texture as the source for
     * specular material colors.
     * 
     * @param spec The initial specular texture map
     * @param texCoords The texture coordinates to access the specular texture map
     * @throws NullPointerException if spec or texCorods is null
     */
    public SpecularColorMap(Texture spec, VertexAttribute texCoords) {
        super(spec, texCoords);
    }

    /**
     * Create an SpecularColorMap that is a clone of <tt>clone</tt> for use with
     * a {@link Template}.
     * 
     * @param clone The component to clone
     * @throws NullPointerException if clone is null
     */
    public SpecularColorMap(SpecularColorMap clone) {
        super(clone);
    }

    @Override
    protected void validate(Texture tex) {
        // do nothing
    }
}
