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
package com.ferox.util;

/**
 * HashFunction provides the ability to provide custom hash functions to class types. In essence, for
 * collections which support it, a HashFunction can be used to override the default behavior of {@link
 * Object#hashCode() hashCode()} .
 *
 * @param <E> The Object type that will be hashed
 *
 * @author Michael Ludwig
 */
public interface HashFunction<E> {
    public static final HashFunction<Object> NATURAL_HASHER = new HashFunction<Object>() {
        @Override
        public int hashCode(Object o) {
            return o.hashCode();
        }
    };

    /**
     * Compute and return the hash code for the given <var>value</var>. If <var>value</var> is null, return 0.
     * The rules for this function are that if two instances are equal, with respect to the fields used to
     * generate the hash, the returned hashes must be equivalent. Non-equal inputs can generate the same
     * hash.
     *
     * @param value The value to hash
     *
     * @return Value's hash code based on this function's internal rules
     */
    public int hashCode(E value);
}
