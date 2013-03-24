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
package com.ferox.resource.texture;


import com.ferox.resource.data.DataType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;


public enum TextureFormat {
    R(1, 1, false, false, EnumSet.allOf(DataType.class)),
    RG(2, 2, false, false, EnumSet.allOf(DataType.class)),
    RGB(3, 3, false, false, EnumSet.allOf(DataType.class)),
    RGBA(4, 4, true, false, EnumSet.allOf(DataType.class)),
    BGR(3, 3, false, false, EnumSet.allOf(DataType.class)),
    BGRA(4, 4, true, false, EnumSet.allOf(DataType.class)),

    R_UINT(1, 1, false, true, EnumSet.of(DataType.BYTE, DataType.SHORT, DataType.INT)),
    RG_UINT(2, 2, false, true, EnumSet.of(DataType.BYTE, DataType.SHORT, DataType.INT)),
    RGB_UINT(3, 3, false, true, EnumSet.of(DataType.BYTE, DataType.SHORT, DataType.INT)),
    RGBA_UINT(4, 4, true, true, EnumSet.of(DataType.BYTE, DataType.SHORT, DataType.INT)),
    BGR_UINT(3, 3, false, true, EnumSet.of(DataType.BYTE, DataType.SHORT, DataType.INT)),
    BGRA_UINT(4, 4, true, true, EnumSet.of(DataType.BYTE, DataType.SHORT, DataType.INT)),

    ARGB_BYTE(4, 4, true, false, EnumSet.of(DataType.BYTE)),
    ARGB_PACKED_INT(1, 4, true, false, EnumSet.of(DataType.INT)),
    DEPTH(1, 1, false, false, EnumSet.of(DataType.FLOAT, DataType.INT)),

    DEPTH_STENCIL(1, 2, false, true, EnumSet.of(DataType.INT)),
    RGB_PACKED_FLOAT(1, 3, false, false, EnumSet.of(DataType.INT)),

    RGB_DXT1(-1, 3, false, false, EnumSet.of(DataType.BYTE)),
    RGBA_DXT1(-1, 4, true, false, EnumSet.of(DataType.BYTE)),
    RGBA_DXT3(-1, 4, true, false, EnumSet.of(DataType.BYTE)),
    RGBA_DXT5(-1, 4, true, false, EnumSet.of(DataType.BYTE));

    private final Set<DataType> types;
    private final boolean hasAlpha, integer;
    private final int pPerC, numC;

    private TextureFormat(int pPerC, int numC, boolean alpha, boolean integer,
                          EnumSet<DataType> supportedTypes) {
        this.types = Collections.unmodifiableSet(supportedTypes);
        this.pPerC = pPerC;
        this.numC = numC;
        this.integer = integer;
        hasAlpha = alpha;
    }

    /**
     * Return true if this format is for an integer texture. Some texture formats have
     * mixed component types, and this will return true if at least one component is an
     * integer component (such as DEPTH_STENCIL).
     * <p/>
     * Integer textures require newer hardware, and different type declarations in GLSL.
     * <p/>
     * If this is false, the format is either compressed, contains native float values, or
     * the integer data types will be normalized to float components.
     *
     * @return True if the texture format is unnormalized integers
     */
    public boolean hasIntegerComponents() {
        return integer;
    }

    /**
     * Return true if this format has its color components (R,G, B, etc) packed into fewer
     * primitives than color components.
     *
     * @return Whether or not color components are packed together into primitives
     */
    public boolean isPackedFormat() {
        return pPerC != numC;
    }

    /**
     * Return the number of components representing the color. An RGB color would have 3
     * components and an RGBA color would have 4, etc.
     *
     * @return The number of components in this format
     */
    public int getComponentCount() {
        return numC;
    }

    /**
     * Get the number of primitives in the TexelData per complete color. Returns -1 if the
     * format is client compressed, since there is no meaningful primitive/component
     * value.
     *
     * @return The number of primitives used to hold an entire color
     */
    public int getPrimitivesPerColor() {
        return pPerC;
    }

    /**
     * Whether or not this texture has image data that is client compressed. If this
     * returns true, data interpretation as done by the TexelData API will be incorrect.
     *
     * @return Whether or not the texture data is compressed
     */
    public boolean isCompressed() {
        return pPerC <= 0;
    }

    /**
     * Whether or not this texture has image data with alpha values.
     *
     * @return Whether or not the format stores alpha information
     */
    public boolean hasAlpha() {
        return hasAlpha;
    }

    /**
     * Return the DataType that is required by the TextureFormat.
     *
     * @return The required DataType of texture data for this format
     */
    public Set<DataType> getSupportedTypes() {
        return types;
    }

    /**
     * <p/>
     * Compute the size of a texture, in primitive elements, for this format and the given
     * dimensions. For one-dimensional or two-dimensional textures that don't need the
     * higher dimension, a value of 1 should be used.
     * <p/>
     * Returns -1 if any of the dimensions are <= 0, or if width and height aren't
     * multiples of 4 for compressed textures (with the exception if the values are 1 or
     * 2, which are also allowed). If depth is not 1 for compressed textures, -1 is
     * returned.
     *
     * @param width  The width of the texture image
     * @param height The height of the texture image
     * @param depth  The depth of the texture image
     *
     * @return The number of primitives required to hold all texture data for a texture of
     *         the given dimensions with this format
     */
    public int getBufferSize(int width, int height, int depth) {
        if (width <= 0 || height <= 0 || depth <= 0) {
            return -1;
        }
        if (isCompressed() && (width % 4 != 0 || height % 4 != 0)) {
            // compression needs to have multiple of 4 dimensions
            if (width != 1 && width != 2 && height != 1 && height != 2) {
                return -1;
            }
        }
        if (isCompressed() && depth != 1) {
            return -1;
        }

        if (isCompressed()) {
            return (int) ((this == RGBA_DXT1 || this == RGB_DXT1 ? 8 : 16) *
                          Math.ceil(width / 4f) * Math.ceil(height / 4f));
        } else {
            return width * height * depth * getPrimitivesPerColor();
        }
    }
}
