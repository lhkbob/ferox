package com.ferox.scene;

import com.ferox.resource.Texture;
import com.lhkbob.entreri.TypeId;

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
    public static final TypeId<EmittedColorMap> ID = TypeId.get(EmittedColorMap.class);

    private EmittedColorMap() { }

    @Override
    protected void validate(Texture tex) {
        // do nothing
    }
}
