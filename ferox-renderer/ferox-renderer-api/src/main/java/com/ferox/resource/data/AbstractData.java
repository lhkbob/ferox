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
package com.ferox.resource.data;

import com.ferox.resource.DataType;

/**
 * AbstractData is an abstract implementation of OpenGLData that provides implementations
 * for the common and trivial methods defined in that interface.
 *
 * @param <T> The data type wrapped by the OpenGLData class
 *
 * @author Michael Ludwig
 */
public abstract class AbstractData<T> implements OpenGLData<T> {
    private final DataType type;
    private final int length;

    // key is an identity specifier that does not prevent the BufferData from
    // being GC'ed, so the Framework can store the keys instead of the actual
    // BufferData when it tracks a resource's state
    private final Object key = new Object();

    /**
     * <p/>
     * Create an AbstractData that will store <var>length</var> primitives of the given
     * data type, <var>type</var>. This constructor verifies that the length is at least 0
     * and it is assumed subclasses will properly allocate their data store to that
     * length.
     *
     * @param type   The data type of this AbstractData
     * @param length The length of the buffer data
     *
     * @throws NullPointerException     if type is null
     * @throws IllegalArgumentException if length is less than 0
     */
    public AbstractData(DataType type, int length) {
        if (type == null) {
            throw new NullPointerException("DataType cannot be null");
        }
        if (length < 0) {
            throw new IllegalArgumentException(
                    "Length must be at least 0, not: " + length);
        }

        this.length = length;
        this.type = type;
    }

    @Override
    public Object getKey() {
        return key;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public DataType getType() {
        return type;
    }
}
