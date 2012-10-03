package com.ferox.scene;

import com.ferox.resource.Texture;
import com.lhkbob.entreri.TypeId;

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
    public static final TypeId<SpecularColorMap> ID = TypeId.get(SpecularColorMap.class);

    private SpecularColorMap() {}

    @Override
    protected void validate(Texture tex) {
        // do nothing
    }
}
