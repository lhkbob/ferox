package com.ferox.scene;

import com.ferox.resource.Texture;
import com.ferox.entity.Template;
import com.ferox.entity.TypedComponent;

/**
 * DepthOffsetMap provides per-pixel depth offsets to the geometry of an Entity.
 * The depth offsets are distances along each normal of the polygon the pixels
 * are mapped to. The DepthOffsetMap can be used to create parallax normal
 * mapping if combined with a {@link NormalMap}, or could be used to generate a
 * normal map on the fly.
 * 
 * @author Michael Ludwig
 */
public final class DepthOffsetMap extends TypedComponent<DepthOffsetMap> {
    private Texture depthMap;

    /**
     * Create a DepthOffsetMap that uses the given texture as its depth map.
     * 
     * @param depthmap The depth map to use initially
     * @throws NullPointerException if depthMap is null
     * @throws IllegalArgumentException if depthMap isn't a 1-component texture
     */
    public DepthOffsetMap(Texture depthmap) {
        super(null, false);
        setDepthMap(depthMap);
    }

    /**
     * Create a DepthOffsetMap that's a clone of <tt>clone</tt> for use with
     * a {@link Template}.
     * 
     * @param clone The DepthOffsetMap to clone
     * @throws NullPointerException if clone is null
     */
    public DepthOffsetMap(DepthOffsetMap clone) {
        super(clone, true);
        depthMap = clone.depthMap;
    }

    /**
     * <p>
     * Set the depth map Texture that provides per-pixel depth offset
     * information for the Entity's geometry. This Texture must have a single
     * component. The depth values are in the same units as the vertex
     * coordinates of any geometry in the Entity.
     * </p>
     * <p>
     * If the texture is not an unclamped floating point texture, the actual
     * depths are transformed from the range [0, 1] to [-.5, .5] to support
     * negative depths. Unclamped floating point values are taken as is.
     * </p>
     * 
     * @param depthMap The depth texture
     * @throws NullPointerException if depthMap is null
     * @throws IllegalArgumentException if the texture doesn't have 1 component
     */
    public void setDepthMap(Texture depthMap) {
        if (depthMap == null)
            throw new NullPointerException("Cannot specify a null depth map");
        if (depthMap.getFormat().getNumComponents() != 1)
            throw new IllegalArgumentException("Cannot specify a depth map that has more than one component: " 
                                               + depthMap.getFormat());
        this.depthMap = depthMap;
    }

    /**
     * Return the Texture containing per-pixel depth offset information for the
     * Entity's geometry.
     * 
     * @return The 1-component depth map, will not be null
     */
    public Texture getDepthMap() {
        return depthMap;
    }
}
