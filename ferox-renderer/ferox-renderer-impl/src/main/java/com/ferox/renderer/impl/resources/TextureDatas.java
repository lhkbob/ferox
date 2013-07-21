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
package com.ferox.renderer.impl.resources;

import com.ferox.renderer.builder.DepthMapBuilder;
import com.ferox.renderer.builder.TextureBuilder;

/**
 *
 */
public final class TextureDatas {
    public static interface ImageSpecifier {
        public void setImageData(int image, int mipmap, Object array, TextureImpl.FullFormat format);
    }

    private TextureDatas() {
    }

    public static DepthMapBuilder.DepthData forDepth(final ImageSpecifier builder, final int image,
                                                     final int mipmap) {
        return new DepthMapBuilder.DepthData() {
            @Override
            public void from(float[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.DEPTH_FLOAT);
            }

            @Override
            public void fromUnsignedNormalized(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.DEPTH_24BIT);
            }

            @Override
            public void fromUnsignedNormalized(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.DEPTH_16BIT);
            }
        };
    }

    public static DepthMapBuilder.DepthStencilData forDepthStencil(final ImageSpecifier builder,
                                                                   final int image, final int mipmap) {
        return new DepthMapBuilder.DepthStencilData() {
            @Override
            public void fromBits(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.DEPTH_24BIT_STENCIL_8BIT);
            }
        };
    }

    public static TextureBuilder.BasicColorData forR(final ImageSpecifier builder, final int image,
                                                     final int mipmap) {
        return new TextureBuilder.BasicColorData() {
            @Override
            public void fromUnsignedNormalized(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.R_NORMALIZED_UBYTE);
            }

            @Override
            public void fromUnsignedNormalized(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.R_NORMALIZED_USHORT);
            }

            @Override
            public void fromUnsignedNormalized(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.R_NORMALIZED_UINT);
            }

            @Override
            public void fromUnsigned(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.R_UBYTE);
            }

            @Override
            public void fromUnsigned(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.R_USHORT);
            }

            @Override
            public void fromUnsigned(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.R_UINT);
            }

            @Override
            public void from(float[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.R_FLOAT);
            }

            @Override
            public void from(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.R_BYTE);
            }

            @Override
            public void from(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.R_SHORT);
            }

            @Override
            public void from(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.R_INT);
            }

            @Override
            public void fromHalfFloats(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.R_HALF_FLOAT);
            }
        };
    }

    public static TextureBuilder.BasicColorData forRG(final ImageSpecifier builder, final int image,
                                                      final int mipmap) {
        return new TextureBuilder.BasicColorData() {
            @Override
            public void fromUnsignedNormalized(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RG_NORMALIZED_UBYTE);
            }

            @Override
            public void fromUnsignedNormalized(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RG_NORMALIZED_USHORT);
            }

            @Override
            public void fromUnsignedNormalized(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RG_NORMALIZED_UINT);
            }

            @Override
            public void fromUnsigned(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RG_UBYTE);
            }

            @Override
            public void fromUnsigned(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RG_USHORT);
            }

            @Override
            public void fromUnsigned(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RG_UINT);
            }

            @Override
            public void from(float[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RG_FLOAT);
            }

            @Override
            public void from(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RG_BYTE);
            }

            @Override
            public void from(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RG_SHORT);
            }

            @Override
            public void from(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RG_INT);
            }

            @Override
            public void fromHalfFloats(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RG_HALF_FLOAT);
            }
        };
    }

    public static TextureBuilder.CompressedRGBData forRGB(final ImageSpecifier builder, final int image,
                                                          final int mipmap) {
        return new TextureBuilder.CompressedRGBData() {
            @Override
            public void fromUnsignedNormalized(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_NORMALIZED_UBYTE);
            }

            @Override
            public void fromUnsignedNormalized(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_NORMALIZED_USHORT);
            }

            @Override
            public void fromUnsignedNormalized(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_NORMALIZED_UINT);
            }

            @Override
            public void fromUnsigned(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_UBYTE);
            }

            @Override
            public void fromUnsigned(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_USHORT);
            }

            @Override
            public void fromUnsigned(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_UINT);
            }

            @Override
            public void from(float[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_FLOAT);
            }

            @Override
            public void from(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_BYTE);
            }

            @Override
            public void from(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_SHORT);
            }

            @Override
            public void from(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_INT);
            }

            @Override
            public void fromHalfFloats(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_HALF_FLOAT);
            }

            @Override
            public void fromDXT1(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_DXT1);
            }

            @Override
            public void fromPackedFloats(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGB_PACKED_FLOAT);
            }
        };
    }

    public static TextureBuilder.BasicColorData forBGR(final ImageSpecifier builder, final int image,
                                                       final int mipmap) {
        return new TextureBuilder.BasicColorData() {
            @Override
            public void fromUnsignedNormalized(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGR_NORMALIZED_UBYTE);
            }

            @Override
            public void fromUnsignedNormalized(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGR_NORMALIZED_USHORT);
            }

            @Override
            public void fromUnsignedNormalized(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGR_NORMALIZED_UINT);
            }

            @Override
            public void fromUnsigned(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGR_UBYTE);
            }

            @Override
            public void fromUnsigned(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGR_USHORT);
            }

            @Override
            public void fromUnsigned(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGR_UINT);
            }

            @Override
            public void from(float[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGR_FLOAT);
            }

            @Override
            public void from(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGR_BYTE);
            }

            @Override
            public void from(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGR_SHORT);
            }

            @Override
            public void from(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGR_INT);
            }

            @Override
            public void fromHalfFloats(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGR_HALF_FLOAT);
            }
        };
    }

    public static TextureBuilder.CompressedRGBAData forRGBA(final ImageSpecifier builder, final int image,
                                                            final int mipmap) {
        return new TextureBuilder.CompressedRGBAData() {
            @Override
            public void fromUnsignedNormalized(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_NORMALIZED_UBYTE);
            }

            @Override
            public void fromUnsignedNormalized(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_NORMALIZED_USHORT);
            }

            @Override
            public void fromUnsignedNormalized(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_NORMALIZED_UINT);
            }

            @Override
            public void fromUnsigned(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_UBYTE);
            }

            @Override
            public void fromUnsigned(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_USHORT);
            }

            @Override
            public void fromUnsigned(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_UINT);
            }

            @Override
            public void from(float[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_FLOAT);
            }

            @Override
            public void from(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_BYTE);
            }

            @Override
            public void from(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_SHORT);
            }

            @Override
            public void from(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_INT);
            }

            @Override
            public void fromHalfFloats(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_HALF_FLOAT);
            }

            @Override
            public void fromDXT1(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_DXT1);
            }

            @Override
            public void fromDXT3(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_DXT3);
            }

            @Override
            public void fromDXT5(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.RGBA_DXT5);
            }
        };
    }

    public static TextureBuilder.BasicColorData forBGRA(final ImageSpecifier builder, final int image,
                                                        final int mipmap) {
        return new TextureBuilder.BasicColorData() {
            @Override
            public void fromUnsignedNormalized(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGRA_NORMALIZED_UBYTE);
            }

            @Override
            public void fromUnsignedNormalized(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGRA_NORMALIZED_USHORT);
            }

            @Override
            public void fromUnsignedNormalized(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGRA_NORMALIZED_UINT);
            }

            @Override
            public void fromUnsigned(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGRA_UBYTE);
            }

            @Override
            public void fromUnsigned(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGRA_USHORT);
            }

            @Override
            public void fromUnsigned(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGRA_UINT);
            }

            @Override
            public void from(float[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGRA_FLOAT);
            }

            @Override
            public void from(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGRA_BYTE);
            }

            @Override
            public void from(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGRA_SHORT);
            }

            @Override
            public void from(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGRA_INT);
            }

            @Override
            public void fromHalfFloats(short[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.BGRA_HALF_FLOAT);
            }
        };
    }

    public static TextureBuilder.ARGBData forARGB(final ImageSpecifier builder, final int image,
                                                  final int mipmap) {
        return new TextureBuilder.ARGBData() {
            @Override
            public void fromUnsignedNormalized(byte[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.ARGB_NORMALIZED_UBYTE);
            }

            @Override
            public void fromPackedBytes(int[] data) {
                builder.setImageData(image, mipmap, data, TextureImpl.FullFormat.ARGB_PACKED_INT);
            }
        };
    }
}
