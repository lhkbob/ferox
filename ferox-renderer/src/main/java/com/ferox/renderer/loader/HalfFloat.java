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
package com.ferox.renderer.loader;

/**
 * This is a very direct port of the Half.h, eLUT.cpp, and toFloat.cpp files provided in the ILM-core
 * library as part of OpenEXR.
 */
public class HalfFloat {
    private static final short[] exponentLUT = new short[1 << 9];
    private static final float[] halfLUT = new float[1 << 16];

    static {
        initializeExponentLUT();
        initializeHalfLUT();
    }

    private static void initializeExponentLUT() {
        for (int i = 0; i < 0x100; i++) {
            int e = (i & 0x0ff) - (127 - 15);

            if (e <= 0 || e >= 30) {
                // special case (can't be normalized)
                exponentLUT[i] = 0;
                exponentLUT[i | 0x100] = 0;
            } else {
                exponentLUT[i] = (short) (e << 10);
                exponentLUT[i | 0x100] = (short) ((e << 10) | 0x8000);
            }
        }
    }

    private static void initializeHalfLUT() {
        for (int i = 0; i < halfLUT.length; i++) {
            halfLUT[i] = Float.intBitsToFloat(halfToFloatBits((short) i));
        }
    }

    private static int halfToFloatBits(short y) {
        int s = (y >> 15) & 0x00000001;
        int e = (y >> 10) & 0x0000001f;
        int m = y & 0x000003ff;

        if (e == 0) {
            if (m == 0) {
                // plus or minus 0
                return s << 31;
            } else {
                // denormalized number, so renormalize it
                while ((m & 0x00000400) == 0) {
                    m <<= 1;
                    e -= 1;
                }

                e += 1;
                m &= ~0x00000400;
            }
        } else if (e == 31) {
            if (m == 0) {
                // positive or negative infinity
                return (s << 31) | 0x7f800000;
            } else {
                // NaN - preserve sign and significant bits
                return (s << 31) | 0x7f800000 | (m << 13);
            }
        }

        // now a normalized number
        e = e + (127 - 15);
        m = m << 13;
        return (s << 31) | (e << 23) | m;
    }

    private static short floatBitsToHalf(int bits) {
        int s = (bits >> 16) & 0x00008000;
        int e = ((bits >> 23) & 0x000000ff) - (127 - 15);
        int m = bits & 0x007fffff;

        if (e <= 0) {
            if (e < -10) {
                // exponent of float is less than -10, so the absolute value of
                // the float is less than the resolution of half, so turn it
                // to an appropriately signed 0
                return (short) s;
            }

            // exponent is between -10 and 0, so the float is a normalized float
            // but less than the normalized representation in half so make
            // it a denormalized half
            m = m | 0x00800000;
            int t = 14 - e;
            int a = (1 << (t - 1)) - 1;
            int b = (m >> t) & 1;
            m = (m + a + b) >> t; // round m to nearest (10+e) value, rounding to even on a tie
            return (short) (s | m);
        } else if (e == 0xff - (127 - 15)) {
            if (m == 0) {
                // float is infinity
                return (short) (s | 0x7c00);
            } else {
                // float is NaN, so make a half NaN that preserves sign and 10
                // leftmost bits of significand
                m >>= 13;
                if (m == 0)
                    m = 1; // make sure there's at least some non-zero in the mantissa so it doesn't look like infiity
                return (short) (s | 0x7c00 | m);
            }
        } else {
            // exponent greater than 0, float is normalized and should be convertable
            // to a normalized half float

            m = m + 0x00000fff + ((m >> 13) & 1);
            if ((m & 0x00800000) != 0) {
                // overflow in mantissa
                m = 0;
                e += 1;
            }

            if (e > 30) {
                // overflow in exponent
                return (short) (s | 0x7c00);
            } else {
                return (short) (s | (e << 10) | (m >> 13));
            }
        }
    }

    public static float halfToFloat(short half) {
        return halfLUT[(0xffff) & half];
    }

    public static short floatToHalf(float v) {
        int floatBits = Float.floatToIntBits(v);
        short h;
        if (v == 0) {
            // special case, preserve sign bit
            h = (short) (floatBits >> 16);
        } else {
            // get the sign and exponent from the float and lookup the corresponding
            // half values from the table
            int e = (floatBits >> 23) & 0x000001ff;
            e = exponentLUT[e];
            if (e != 0) {
                // normal case, round the significant to 10 bits and combine with exponent
                int m = (floatBits & 0x007fffff);
                h = (short) (e + ((m + 0x00000fff + ((m >> 13) & 1)) >> 13));
            } else {
                // not a fast case so use general method
                h = floatBitsToHalf(floatBits);
            }
        }

        return h;
    }

    public static void main(String[] args) {
        System.out.println("Values from 0 to 1000");
        for (int i = 0; i < 1000; i++) {
            float original = (float) Math.random() * 1000;
            short half = floatToHalf(original);
            float reconstructed = halfToFloat(half);

            if (Math.abs(original - reconstructed) > 0.5) {
                System.out.println(original + " > " + half + " > " + reconstructed);
            }
        }

        System.out.println("Values from 0 to 1e-5");
        for (int i = 0; i < 1000; i++) {
            float original = (float) Math.random() / (float) (Math.random() * 100000);
            short half = floatToHalf(original);
            float reconstructed = halfToFloat(half);

            if (Math.abs(original - reconstructed) > 0.5) {
                System.out.println(original + " > " + half + " > " + reconstructed);
            }
        }
    }
}
