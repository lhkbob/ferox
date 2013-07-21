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
 * Buffer is the high-level resource type representing an array of bytes stored on the GPU. Depending on the
 * primitive data type, the bytes are interpreted in different ways, e.g. as signed or unsigned ints, floats,
 * etc. Depending on how a buffer is used, different data types are valid. The usage of a buffer is encoded in
 * the type system where {@link VertexBuffer} or {@link ElementBuffer} are used as appropriate.
 * <p/>
 * When a Buffer object is refreshed by {@link #refresh()}, the contents of the original primitive array that
 * defined its data are pushed to the GPU. To animate buffers, you should maintain the reference to the
 * original data and mutate as necessary, and refresh() when ready. If a buffer is to be refreshed every frame
 * or multiple times per frame, {@link com.ferox.renderer.builder.BufferBuilder#dynamic()} should be called
 * when constructing the buffer.
 *
 * @author Michael Ludwig
 */
public interface Buffer extends Resource {
    /**
     * Get the length of the buffer. The length of the buffer will not change over time. It is possible to use
     * a shorter sequence of elements and a dynamic buffer to support buffers that shrink and grow within the
     * upper bound of {@code length}. If a larger buffer is required, a new one must be initialized through
     * the framework.
     * <p/>
     * The length is measured in units of the data type. Thus the total number of bytes is {@code getLength()
     * * getDataType().getByteCount()}.
     *
     * @return The maximum number of primitive elements in the buffer
     */
    public int getLength();

    /**
     * @return The primitive interpretation of the bytes within the buffer
     */
    public DataType getDataType();
}
