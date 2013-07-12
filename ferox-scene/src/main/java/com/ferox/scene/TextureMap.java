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
import com.ferox.renderer.texture.Texture;
import com.ferox.renderer.VertexAttribute;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.property.ObjectProperty;

/**
 * <p/>
 * TextureMap is an abstract Component type that is shared by the Components that store a
 * single Texture, such as {@link NormalMap}, {@link DepthOffsetMap} and {@link
 * DiffuseMap}.
 *
 * @param <T> Concrete TextureMap type
 *
 * @author Michael Ludwig
 */
public abstract class TextureMap<T extends TextureMap<T>> extends ComponentData<T> {
    private ObjectProperty<Texture> texture;
    private ObjectProperty<VertexAttribute> textureCoordinates;

    protected TextureMap() {
    }

    /**
     * @return The texture coordinates used to access this TextureMap's texture
     */
    public final VertexAttribute getTextureCoordinates() {
        return textureCoordinates.get(getIndex());
    }

    /**
     * Set the texture coordinates used to access this TextureMap's texture. Texture
     * coordinates are used when rendering an entity that is a {@link Renderable}. There
     * must be one texture coordinate for every vertex used in the geometry of the
     * Renderable.
     *
     * @param texCoords The new vertex attribute holding texture coord data
     *
     * @return This component for chaining purposes
     *
     * @throws IllegalArgumentException if texCoords data is not of type FLOAT
     * @throws NullPointerException     if texCoords is null
     */
    @SuppressWarnings("unchecked")
    public final T setTextureCoordinates(VertexAttribute texCoords) {
        if (texCoords == null) {
            throw new NullPointerException("Texture coordinates cannot be null");
        }
        if (texCoords.getVBO().getData().getDataType() != DataType.FLOAT) {
            throw new IllegalArgumentException("VertexAttribute must have FLOAT data");
        }

        textureCoordinates.set(texCoords, getIndex());
        updateVersion();
        return (T) this;
    }

    /**
     * Return the non-null Texture that is used by this TextureMap. The interpretation of
     * the Texture when rendering a scene is dependent on the actual subtype of
     * TextureMap.
     *
     * @return This TextureMap's texture
     */
    public final Texture getTexture() {
        return texture.get(getIndex());
    }

    /**
     * Set the Texture to use with this TextureMap. Depending on the actual Component
     * type, this texture will mean different things. Some possibilities include diffuse
     * color, or normal vectors.
     *
     * @param texture The new Texture
     *
     * @return This component for chaining purposes
     *
     * @throws NullPointerException     if texture is null
     * @throws IllegalArgumentException if the texture is invalid according to the rules
     *                                  of the subclass
     */
    @SuppressWarnings("unchecked")
    public final T setTexture(Texture texture) {
        if (texture == null) {
            throw new NullPointerException("Texture cannot be null");
        }
        validate(texture);
        this.texture.set(texture, getIndex());
        updateVersion();
        return (T) this;
    }

    /**
     * Throw an IllegalArgumentException if the given texture is not valid for the given
     * subclass.
     *
     * @param tex The potentially new texture, will not be null
     */
    protected abstract void validate(Texture tex);
}
