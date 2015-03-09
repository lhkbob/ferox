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
package com.ferox.scene;

import com.ferox.renderer.Texture;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.Reference;

/**
 * <p/>
 * The NormalMap component adds normal-mapping to the lighting effects applied when rendering an Entity. The
 * normal vectors are stored in a 2 or 3-component texture and the vectors can be in object or tangent space.
 * <p/>
 * The normals within the Texture are assumed to be of unit length. If the format of the texture is {@link
 * com.ferox.renderer.DataType#FLOAT} the x, y, and z coordinates are taken as is from the red, green, and
 * blue components of each texel. If the format is an unsigned normalized type, the x, y, and z coordinates
 * are packed into the range, [0, 1] and are converted to [-1, 1] with <code>2 * [x,y,z] - 1</code>.
 * <p/>
 * If the texture is a 2-component texture, the two components encode the x and y coordinates.  The z
 * coordinate must be computed on the fly by the shader.
 * <p/>
 * Tangent space normal vectors require the geometry of the Entity to provide additional vertex attributes to
 * describe the tangent space for each rendered triangle.
 *
 * @author Michael Ludwig
 */
public interface NormalMap extends Component {
    /**
     * Get whether or not normals encoded in the texture are in object space. If they are not in object space,
     * they are assumed to be in the tangent space formed by the entity's normal and tangent vector
     * attributes.
     *
     * @return True if the normal map is in object space
     */
    public boolean isObjectSpace();

    /**
     * Set whether or not normal vectors stored in the normal texture map are in object space or tangent
     * space.
     *
     * @param inObjectSpace True if they are in object space
     */
    public void setObjectSpace(boolean inObjectSpace);

    /**
     * Return the non-null Texture that is used by this SpecularColorMap.
     *
     * @return This TextureMap's texture
     */
    @Reference(nullable = false)
    public Texture getTexture();

    /**
     * Set the Texture to use with this component.
     *
     * @param texture The new Texture
     *
     * @return This component for chaining purposes
     */
    public NormalMap setTexture(Texture texture);
}
