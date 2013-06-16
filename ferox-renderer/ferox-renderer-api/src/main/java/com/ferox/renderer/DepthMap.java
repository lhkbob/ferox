package com.ferox.renderer;

/**
 * DepthMaps are samplers that store depth and possibly stencil data instead of color
 * data. They are commonly used for shadow-mapping and deferred rendering techniques. They
 * can also be used to provide the depth buffer when doing any render-to-texture technique
 * that might need the same depth buffer later on.
 * <p/>
 * All DepthMap instances will return DEPTH or DEPTH_STENCIL as their base format.
 *
 * @author Michael Ludwig
 */
public interface DepthMap extends Sampler {
    /**
     * Get the comparison function used. If a null value is returned, the depth-comparison
     * operation is not performed and depth values are returned from the sampler as is.
     * Otherwise, the the depth value is compared with the texture coordinate and a 0 or 1
     * is returned depending on the result.
     *
     * @return The comparison, or null if it's disabled
     */
    public Renderer.Comparison getDepthComparison();

    /**
     * Get the depth value used when texture coordinates sample the border of the depth
     * map.
     *
     * @return The depth value
     */
    public double getBorderDepth();
}
