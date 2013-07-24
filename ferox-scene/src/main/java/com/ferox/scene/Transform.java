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

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.entreri.Matrix4Property.DefaultMatrix4;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.SharedInstance;

/**
 * <p/>
 * Transform represents an affine transform that transforms an Entity from its local coordinate space into a
 * coordinate space shared by all Entities within a system (i.e. the world). This can be used to place lights,
 * physics objects, or objects to be rendered.
 *
 * @author Michael Ludwig
 */
public interface Transform extends Component {
    /**
     * Copy the given transform matrix into this Transform's matrix.
     *
     * @param m The new affine transform
     *
     * @return This Transform for chaining purposes
     *
     * @throws NullPointerException if m is null
     */
    public Transform setMatrix(@Const Matrix4 m);

    /**
     * Return the matrix of this Transform. The returned Matrix4 instance is reused by this Transform instance
     * so it should be cloned before changing which Component is referenced
     *
     * @return The current world affine transform matrix
     */
    @Const
    @SharedInstance
    @DefaultMatrix4(m00 = 1.0, m01 = 0.0, m02 = 0.0, m03 = 0.0,
                    m10 = 0.0, m11 = 1.0, m12 = 0.0, m13 = 0.0,
                    m20 = 0.0, m21 = 0.0, m22 = 1.0, m23 = 0.0,
                    m30 = 0.0, m31 = 0.0, m32 = 0.0, m33 = 1.0)
    public Matrix4 getMatrix();
}
