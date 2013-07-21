/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer;

/**
 * DepthMaps are samplers that store depth and possibly stencil data instead of color data. They are commonly
 * used for shadow-mapping and deferred rendering techniques. They can also be used to provide the depth
 * buffer when doing any render-to-texture technique that might need the same depth buffer later on.
 * <p/>
 * All DepthMap instances will return DEPTH or DEPTH_STENCIL as their base format.
 *
 * @author Michael Ludwig
 */
public interface DepthMap extends Sampler {
    /**
     * Get the comparison function used. If a null value is returned, the depth-comparison operation is not
     * performed and depth values are returned from the sampler as is. Otherwise, the the depth value is
     * compared with the texture coordinate and a 0 or 1 is returned depending on the result.
     *
     * @return The comparison, or null if it's disabled
     */
    public Renderer.Comparison getDepthComparison();

    /**
     * Get the depth value used when texture coordinates sample the border of the depth map.
     *
     * @return The depth value
     */
    public double getBorderDepth();
}
