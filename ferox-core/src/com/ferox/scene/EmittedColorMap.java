package com.ferox.scene;

import com.ferox.entity2.Component;
import com.ferox.entity2.Template;
import com.ferox.entity2.TypedId;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;

/**
 * <p>
 * EmittedColorMap functions much like {@link EmittedColor} except that it
 * defines the emitted light in a texture map to allow for more detail in the
 * final rendering. Like EmittedColor, the emitted light will not influence
 * other objects in the system and is a purely local effect.
 * </p>
 * <p>
 * Alpha values in the texture map do not have an explicit behavior. Controllers
 * may support encoding of additional data within that channel, or use it as an
 * exponent to have a higher range of values.
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
public class EmittedColorMap extends TextureMap<EmittedColorMap> {
    /**
     * The shared TypedId representing DepthOffsetMap.
     */
    public static final TypedId<EmittedColorMap> ID = Component.getTypedId(EmittedColorMap.class);

    /**
     * Create an EmittedColorMap that uses the given texture as its source for
     * locally emitted light.
     * 
     * @param emitted The texture map to use
     * @param texCoords The texture coordinates to access emitted
     * @throws NullPointerException if emitted or texCoords is null
     */
    public EmittedColorMap(Texture emitted, VertexAttribute texCoords) {
        super(emitted, texCoords);
    }

    /**
     * Create an EmittedColorMap that is a clone of <tt>clone</tt>, for use with
     * a {@link Template}.
     * 
     * @param clone The component to clone
     * @throws NullPointerException if clone is null
     */
    public EmittedColorMap(EmittedColorMap clone) {
        super(clone);
    }

    @Override
    protected void validate(Texture tex) {
        // do nothing
    }
}
