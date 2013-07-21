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
package com.ferox.renderer.builder;

import com.ferox.renderer.VertexBuffer;

/**
 * VertexBufferBuilder is the builder for {@link VertexBuffer}.
 *
 * @author Michael Ludwig
 */
public interface VertexBufferBuilder extends BufferBuilder<VertexBufferBuilder>, Builder<VertexBuffer> {
    /**
     * Provide the data array that will back the vertex buffer. The data type will be {@link
     * com.ferox.renderer.DataType#FLOAT}. The length of the buffer will be the length of the array.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder from(float[] data);

    /**
     * Provide the data array that will back the vertex buffer. The data type will be {@link
     * com.ferox.renderer.DataType#NORMALIZED_INT}. The length of the buffer will be the length of the array.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder fromNormalized(int[] data);

    /**
     * Provide the data array that will back the vertex buffer. The data type will be {@link
     * com.ferox.renderer.DataType#NORMALIZED_SHORT}. The length of the buffer will be the length of the
     * array.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder fromNormalized(short[] data);

    /**
     * Provide the data array that will back the vertex buffer. The data type will be {@link
     * com.ferox.renderer.DataType#NORMALIZED_BYTE}. The length of the buffer will be the length of the
     * array.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder fromNormalized(byte[] data);

    /**
     * Provide the data array that will back the vertex buffer. The data type will be {@link
     * com.ferox.renderer.DataType#INT}. The length of the buffer will be the length of the array. Buffers
     * with this type should only be used with shader attributes of an appropriate type.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder from(int[] data);

    /**
     * Provide the data array that will back the vertex buffer. The data type will be {@link
     * com.ferox.renderer.DataType#SHORT}. The length of the buffer will be the length of the array. Buffers
     * with this type should only be used with shader attributes of an appropriate type.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder from(short[] data);

    /**
     * Provide the data array that will back the vertex buffer. The data type will be {@link
     * com.ferox.renderer.DataType#BYTE}. The length of the buffer will be the length of the array. Buffers
     * with this type should only be used with shader attributes of an appropriate type.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder from(byte[] data);
}
