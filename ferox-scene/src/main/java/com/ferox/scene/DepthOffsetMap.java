package com.ferox.scene;

import com.ferox.resource.Texture;
import com.ferox.resource.TextureFormat;
import com.lhkbob.entreri.TypeId;

/**
 * <p>
 * DepthOffsetMap provides per-pixel depth offsets to the geometry of an Entity.
 * The depth offsets are distances along each normal of the polygon the pixels
 * are mapped to. The DepthOffsetMap can be used to create parallax normal
 * mapping if combined with a {@link NormalMap}, or could be used to generate a
 * normal map on the fly.
 * </p>
 * <p>
 * The textures used by DepthOffsetMap must have single-component texture
 * formats. If the format of the depth map is {@link TextureFormat#R_FLOAT},
 * then the depth values in the texture are taken as is. If it is any other
 * format, then the depth values are packed into the range [0, 1] and are
 * converted to [-.5, .5] with <code>d - .5</code> when used in a shader.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class DepthOffsetMap extends TextureMap<DepthOffsetMap> {
    /**
     * The shared TypedId representing DepthOffsetMap.
     */
    public static final TypeId<DepthOffsetMap> ID = TypeId.get(DepthOffsetMap.class);

    private DepthOffsetMap() { }

    @Override
    protected void validate(Texture tex) {
        if (tex.getFormat().getNumComponents() != 1) {
            throw new IllegalArgumentException("Cannot specify a depth map that has more than one component: "
                    + tex.getFormat());
        }
    }
}
