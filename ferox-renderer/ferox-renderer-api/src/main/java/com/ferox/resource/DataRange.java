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
package com.ferox.resource;

/**
 * DataRange is a small object that tracks changes within an array or buffer
 * that is one-dimensional. It has both an offset into the data and a length;
 * these are measured in number of indices and not any other concept such as
 * pixel or vector.
 * 
 * @author Michael Ludwig
 */
public class DataRange {
    private final int offset;
    private final int length;

    /**
     * Create a DataRange with the given offset and length of data. The offset
     * cannot be less than 0 and the length must be at least 1.
     * 
     * @param offset The offset into the array or buffer of data
     * @param length The length of modified data starting at offset
     * @throws IllegalArgumentException if offset < 0 or length < 1
     */
    public DataRange(int offset, int length) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be at least 0, not: " + offset);
        }
        if (length < 1) {
            throw new IllegalArgumentException("Length must be at least 1, not: " + length);
        }

        this.offset = offset;
        this.length = length;
    }

    /**
     * @return The offset into the modified array or buffer, will be at least 0
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @return The length of modified data, will be at least 1
     */
    public int getLength() {
        return length;
    }
}
