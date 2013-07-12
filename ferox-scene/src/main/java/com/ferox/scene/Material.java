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

import com.ferox.resource.BufferData.DataType;
import com.ferox.renderer.VertexAttribute;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.property.ObjectProperty;

/**
 * <p/>
 * Material subclasses represent lighting models that compute shading for a rendered
 * entity, based on the presence of lights in the scene. The materials or lighting models
 * can be combined with the various color components and texture maps to add variety to
 * the rendered entity.
 * <p/>
 * The absence of a Material component implies that the entity should be rendered without
 * any light shading. It is undefined what it means to have an entity with multiple
 * lighting models.
 *
 * @param <T> The concrete type of Material
 *
 * @author Michael Ludwig
 */
public abstract class Material<T extends Material<T>> extends ComponentData<T> {
    private ObjectProperty<VertexAttribute> normals;

    protected Material() {
    }

    /**
     * Set the normal vectors store per-vertex normal data used when computing lighting.
     * Normals are used when rendering an entity that is a {@link Renderable}. There must
     * be one normal for every vertex used in the geometry of the Renderable.
     *
     * @param normals The new vertex attribute holding normal vector data
     *
     * @return This material for chaining purposes
     *
     * @throws NullPointerException     if normals is null
     * @throws IllegalArgumentException if normals has an element size other than 3, or is
     *                                  not float data
     */
    @SuppressWarnings("unchecked")
    public final T setNormals(VertexAttribute normals) {
        if (normals == null) {
            throw new NullPointerException("Normals cannot be null");
        }
        if (normals.getVBO().getData().getDataType() != DataType.FLOAT) {
            throw new IllegalArgumentException("Normals must have FLOAT data");
        }
        if (normals.getElementSize() != 3) {
            throw new IllegalArgumentException(
                    "Normals must have an element size of 3, not: " +
                    normals.getElementSize());
        }

        this.normals.set(normals, getIndex());
        updateVersion();
        return (T) this;
    }

    /**
     * @return The normal vector data to use for lighting calculations
     */
    public final VertexAttribute getNormals() {
        return normals.get(getIndex());
    }
}
