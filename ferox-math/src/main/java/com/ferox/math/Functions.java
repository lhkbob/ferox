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
package com.ferox.math;

/**
 * Functions is a static collection of functions that provide additional or more
 * performant alternatives to {@link Math}.
 * 
 * @author Michael Ludwig
 */
public final class Functions {
    private Functions() {}

    /**
     * <p>
     * Compute the smallest power-of-two that is greater than or equal to the
     * given integer. If num is less than or equal to 0, 1 is always returned.
     * If num is already a power-of-two, num is returned.
     * </p>
     * <p>
     * This runs in constant time.
     * </p>
     * 
     * @param num The input
     * @return Smallest power-of-two greater than or equal to num
     */
    public static int potCeil(int num) {
        if (num <= 0) {
            return 1;
        }

        num--;
        num |= (num >> 1);
        num |= (num >> 2);
        num |= (num >> 4);
        num |= (num >> 8);
        num |= (num >> 16);
        num++;

        return num;
    }

    /**
     * Compute the integer log base 2 of the given number. If num is not a power
     * of two (i.e. its log base 2 is not an integral value), this effectively
     * returns <code>log2(potCeil(num))</code>. If num is less than or equal to
     * 0, results are undefined.
     * 
     * @param num The number to compute the log base 2 of
     * @return The log base 2 of the number, after rounding up to the nearest
     *         power of two
     */
    public static int log2(int num) {
        return 32 - Integer.numberOfLeadingZeros(num - 1);
    }

    /**
     * Return true if the given integer is a power of two. This is an efficient,
     * constant time implementation. Numbers less than or equal to 0 will always
     * return false.
     * 
     * @param num The number to check
     * @return True if num is a power of two
     */
    public static boolean isPowerOfTwo(int num) {
        if (num <= 0) {
            return false;
        }
        return (num & (num - 1)) == 0;
    }
}
