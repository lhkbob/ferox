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
package com.ferox.renderer.texture;

import com.ferox.renderer.*;
import com.ferox.renderer.builder.Texture2DBuilder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p/>
 * DDSTexture is a utility class that can be used to read most of the sampler types from DDS files. It
 * supports Texture2D, Texture2DArray, TextureCubeMap, Texture3D, DepthMap2D, and DepthCubeMap.
 * <p/>
 * It is recommended to use TextureLoader for input however, since it will delegate to DDSTexture when
 * needed.
 *
 * @author Michael Ludwig
 */
@SuppressWarnings("unused")
public class DDSTexture {
    // dds header read in from the input stream
    private final DDSHeader header;

    // representation of the dds texture
    private DXGIPixelFormat format;
    private DDPFMap oldFormat;

    private Class<? extends Sampler> target;

    private int mipmapCount; // at least 1
    // depth may be unused for cube maps and 2D textures
    // (in which case it's set to 1)
    private int width, height, depth;
    private Object[][] data; // accessed [face][mipmap]

    // FIXME massive format switch if I'm not careful here
    private TextureProxy<Texture2D> asTexture2D() {
        return new TextureProxy<Texture2D>() {
            @Override
            public Texture2D convert(Framework framework) {
                Texture2DBuilder t = framework.newTexture2D().width(width).height(height).anisotropy(0.0)
                                              .interpolated();
                return null;
            }
        };
    }

    private TextureProxy<Texture2DArray> asTexture2DArray() {
        return new TextureProxy<Texture2DArray>() {
            @Override
            public Texture2DArray convert(Framework framework) {
                return null;
            }
        };
    }

    private TextureProxy<Texture3D> asTexture3D() {
        return new TextureProxy<Texture3D>() {
            @Override
            public Texture3D convert(Framework framework) {
                return null;
            }
        };
    }

    private TextureProxy<TextureCubeMap> asTextureCubeMap() {
        return new TextureProxy<TextureCubeMap>() {
            @Override
            public TextureCubeMap convert(Framework framework) {
                return null;
            }
        };
    }

    private TextureProxy<DepthMap2D> asDepthMap2D() {
        return new TextureProxy<DepthMap2D>() {
            @Override
            public DepthMap2D convert(Framework framework) {
                return null;
            }
        };
    }

    private TextureProxy<DepthCubeMap> asDepthCubeMap() {
        return new TextureProxy<DepthCubeMap>() {
            @Override
            public DepthCubeMap convert(Framework framework) {
                return null;
            }
        };
    }

    /**
     * <p/>
     * Read in and create a new Texture from the given stream. This image will be a Texture2D, Texture2DArray,
     * Texture3D, or TextureCubeMap. An IOException will be thrown if the stream doesn't represent a valid DDS
     * texture or if its header can't be identified to a supported Sampler.TexelFormat, etc.
     * <p/>
     * It assumes that the stream starts at the first byte of the header section for the DDS texture. The
     * stream will not be closed.
     *
     * @param stream The InputStream to read the texture from
     *
     * @return The Texture read from stream
     *
     * @throws IOException if an IOException occurs while reading, or if the stream is an invalid or
     *                     unsupported DDS texture
     */
    public static TextureProxy<?> readTexture(InputStream stream) throws IOException {
        if (stream == null) {
            throw new IOException("Cannot read a texture from a null stream");
        }

        DDSTexture texture = new DDSTexture(stream);
        if (texture.target.equals(Texture2D.class)) {
            return texture.asTexture2D();
        } else if (texture.target.equals(Texture2DArray.class)) {
            return texture.asTexture2DArray();
        } else if (texture.target.equals(Texture3D.class)) {
            return texture.asTexture3D();
        } else if (texture.target.equals(TextureCubeMap.class)) {
            return texture.asTextureCubeMap();
        } else if (texture.target.equals(DepthMap2D.class)) {
            return texture.asDepthMap2D();
        } else if (texture.target.equals(DepthCubeMap.class)) {
            return texture.asDepthCubeMap();
        } else {
            // shouldn't happen unless the implementation gets beyond this method
            throw new UnsupportedOperationException("Unsupported texture target: " + texture.target);
        }
    }

    /**
     * <p/>
     * Determine if the given stream represents the start of a valid DDS file. All it checks is the header
     * portion of the given stream. As such, it is possible that it appears as a DDS file, but the rest of the
     * file is not valid.
     * <p/>
     * The stream will mark and then read the 124 bytes of the header. If an IOException occurs while reading
     * or parsing the header, then false is returned. When finished, the stream will be reset back to the
     * mark.
     * <p/>
     * <i>The stream will not be closed.</i>
     *
     * @param stream The InputStream that represents the data source for a DDS texture
     *
     * @return Whether or not stream is a DDS texture, doesn't check if the texture is supported
     *
     * @throws NullPointerException if stream is null
     */
    public static boolean isDDSTexture(InputStream stream) {
        if (stream == null) {
            throw new NullPointerException("Cannot test a null stream");
        }

        if (!(stream instanceof BufferedInputStream)) {
            stream = new BufferedInputStream(stream); // this way marking is supported
        }
        try {
            DDSHeader header;
            try {
                // mark the stream and read the header
                stream.mark(124);
                header = readHeader(stream);
            } finally {
                // must make sure the stream is reset at all times
                stream.reset();
            }

            validateHeader(header);
            // if we've gotten here, we're valid
            return true;
        } catch (IOException ioe) {
            // something didn't work - either serious IO, or thrown from
            // validateHeader()
            return false;
        }
    }

    /*
     * Construct a DDSTexture that immediately reads its data in from the given
     * InputStream. Throws an IOException if the input stream doesn't represent
     * a DDS texture file, if the DDS texture isn't formatted correctly, or if
     * the DDS texture type isn't supported.
     * 
     * Assumes that the first byte returned by this input stream is the first
     * byte of the DDS header in the file. If the dds header represents a valid
     * and supported dds texture, the entire file will be read within this
     * constructor.
     */
    private DDSTexture(InputStream in) throws IOException {
        header = readHeader(in);

        // validate and interpret the header
        validateHeader(header);
        identifyBuildParams();
        identifyTextureFormat();

        readData(in);
    }

    /*
     * Structs to hold the DDS file header. DDS constants and maps between DDS
     * formats and OpenGL texture format/types.
     */

    // Stores a DDS header (equivalent for DX9 and DX10, DX10 may have non-null
    // DDSHeader_DX10 header, too)
    private static class DDSHeader {
        // DDS_HEADER or DDSURFACEDESC2
        int magic;
        int size;
        int flags;
        int height;
        int width;
        int linearSize;
        int depth;
        int mipmapCount;
        int[] reserved1 = new int[11];

        DDSPixelFormat pixelFormat;

        // DDS_CAPS2 (embedded in DDS_HEADER, not DDSURFACEDESC2)
        int caps1;
        int caps2;
        int caps3;
        int caps4;

        int reserved2;

        // Not really part of the header, but it follows immediately
        // Not null if this is a DX10 dds texture
        DDSHeader_DX10 headerDX10;
    }

    // Stores the pixel format information for the dds texture
    // If the fourCC is valid and set to 'DX10', then the pixel
    // format is stored in a DXGIPixelFormat enum instead.
    private static class DDSPixelFormat {
        // PIXELFORMAT
        int size;
        int flags;
        int fourCC;
        int rgbBitCount;
        int rBitMask;
        int gBitMask;
        int bBitMask;
        int rgbAlphaBitMask;

        @Override
        public String toString() {
            String first = "RGB (" + isFlagSet(flags, DDPF_RGB) + "), LUM (" +
                           isFlagSet(flags, DDPF_LUMINANCE) + "), ALPHA (" +
                           isFlagSet(flags, DDPF_ALPHA) + "), FourCC (" +
                           isFlagSet(flags, DDPF_FOURCC) + ")";
            String second;
            if (isFlagSet(flags, DDPF_FOURCC)) {
                second = "FourCC = " + unmakeFourCC(fourCC);
            } else {
                second = "Total bits = " + rgbBitCount + ", has alpha = " +
                         isFlagSet(flags, DDPF_ALPHAPIXELS) + " Bit masks: r/l = " +
                         Integer.toHexString(rBitMask) + ", g = " +
                         Integer.toHexString(gBitMask) + ", b = " +
                         Integer.toHexString(bBitMask) + ", a = " +
                         Integer.toHexString(rgbAlphaBitMask);
            }
            return first + "\n" + second;
        }
    }

    // Only present if header.pixelFormat.fourCC == 'DX10'
    private static class DDSHeader_DX10 {
        DXGIPixelFormat dxgiFormat;
        int resourceDimension;

        int miscFlag;
        int arraySize;
        int reserved;
    }

    /*
     * DX10 pixel formats. Default constructor implies not supported by this DDS
     * reader. If the valid boolean is set, the DXGIPixelFormat contains a valid
     * map between DX10 pixel formats and openGL source, dest, and type enums
     * for a texture.
     * 
     * Also keeps track of byte size of the glType primitive and the number of
     * primitives required to store a color element.
     */
    private static enum DXGIPixelFormat {
        DXGI_FORMAT_UNKNOWN,
        DXGI_FORMAT_R32G32B32A32_TYPELESS,
        DXGI_FORMAT_R32G32B32A32_FLOAT(DataType.FLOAT, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R32G32B32A32_UINT(DataType.UNSIGNED_INT, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R32G32B32A32_SINT(DataType.INT, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R32G32B32_TYPELESS,
        DXGI_FORMAT_R32G32B32_FLOAT(DataType.FLOAT, Sampler.TexelFormat.RGB),
        DXGI_FORMAT_R32G32B32_UINT(DataType.UNSIGNED_INT, Sampler.TexelFormat.RGB),
        DXGI_FORMAT_R32G32B32_SINT(DataType.INT, Sampler.TexelFormat.RGB),
        DXGI_FORMAT_R16G16B16A16_TYPELESS,
        DXGI_FORMAT_R16G16B16A16_FLOAT(DataType.HALF_FLOAT, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R16G16B16A16_UNORM(DataType.UNSIGNED_NORMALIZED_SHORT, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R16G16B16A16_UINT(DataType.UNSIGNED_SHORT, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R16G16B16A16_SNORM(DataType.NORMALIZED_SHORT, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R16G16B16A16_SINT(DataType.SHORT, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R32G32_TYPELESS,
        DXGI_FORMAT_R32G32_FLOAT(DataType.FLOAT, Sampler.TexelFormat.RG),
        DXGI_FORMAT_R32G32_UINT(DataType.UNSIGNED_INT, Sampler.TexelFormat.RG),
        DXGI_FORMAT_R32G32_SINT(DataType.INT, Sampler.TexelFormat.RG),
        DXGI_FORMAT_R32G8X24_TYPELESS,
        DXGI_FORMAT_D32_FLOAT_S8X24_UINT,
        DXGI_FORMAT_R32_FLOAT_X8X24_TYPELESS,
        DXGI_FORMAT_X32_TYPELESS_G8X24_UINT,
        DXGI_FORMAT_R10G10B10A2_TYPELESS,
        DXGI_FORMAT_R10G10B10A2_UNORM,
        DXGI_FORMAT_R10G10B10A2_UINT,
        DXGI_FORMAT_R11G11B10_FLOAT(DataType.INT_BIT_FIELD, Sampler.TexelFormat.RGB),
        DXGI_FORMAT_R8G8B8A8_TYPELESS,
        DXGI_FORMAT_R8G8B8A8_UNORM(DataType.UNSIGNED_NORMALIZED_BYTE, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R8G8B8A8_UNORM_SRGB(DataType.UNSIGNED_NORMALIZED_BYTE, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R8G8B8A8_UINT(DataType.UNSIGNED_BYTE, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R8G8B8A8_SNORM(DataType.NORMALIZED_SHORT, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R8G8B8A8_SINT(DataType.SHORT, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_R16G16_TYPELESS,
        DXGI_FORMAT_R16G16_FLOAT(DataType.HALF_FLOAT, Sampler.TexelFormat.RG),
        DXGI_FORMAT_R16G16_UNORM(DataType.UNSIGNED_NORMALIZED_SHORT, Sampler.TexelFormat.RG),
        DXGI_FORMAT_R16G16_UINT(DataType.UNSIGNED_SHORT, Sampler.TexelFormat.RG),
        DXGI_FORMAT_R16G16_SNORM(DataType.NORMALIZED_SHORT, Sampler.TexelFormat.RG),
        DXGI_FORMAT_R16G16_SINT(DataType.SHORT, Sampler.TexelFormat.RG),
        DXGI_FORMAT_R32_TYPELESS,
        DXGI_FORMAT_D32_FLOAT(DataType.FLOAT, Sampler.TexelFormat.DEPTH),
        DXGI_FORMAT_R32_FLOAT(DataType.FLOAT, Sampler.TexelFormat.R),
        DXGI_FORMAT_R32_UINT(DataType.UNSIGNED_INT, Sampler.TexelFormat.R),
        DXGI_FORMAT_R32_SINT(DataType.INT, Sampler.TexelFormat.R),
        DXGI_FORMAT_R24G8_TYPELESS,
        DXGI_FORMAT_D24_UNORM_S8_UINT(DataType.INT_BIT_FIELD, Sampler.TexelFormat.DEPTH_STENCIL),
        DXGI_FORMAT_R24_UNORM_X8_TYPELESS,
        DXGI_FORMAT_X24_TYPELESS_G8_UINT,
        DXGI_FORMAT_R8G8_TYPELESS,
        DXGI_FORMAT_R8G8_UNORM(DataType.UNSIGNED_NORMALIZED_BYTE, Sampler.TexelFormat.RG),
        DXGI_FORMAT_R8G8_UINT(DataType.UNSIGNED_BYTE, Sampler.TexelFormat.RG),
        DXGI_FORMAT_R8G8_SNORM(DataType.NORMALIZED_BYTE, Sampler.TexelFormat.RG),
        DXGI_FORMAT_R8G8_SINT(DataType.BYTE, Sampler.TexelFormat.RG),
        DXGI_FORMAT_R16_TYPELESS,
        DXGI_FORMAT_R16_FLOAT(DataType.HALF_FLOAT, Sampler.TexelFormat.R),
        DXGI_FORMAT_D16_UNORM(DataType.UNSIGNED_NORMALIZED_SHORT, Sampler.TexelFormat.DEPTH),
        DXGI_FORMAT_R16_UNORM(DataType.UNSIGNED_NORMALIZED_SHORT, Sampler.TexelFormat.R),
        DXGI_FORMAT_R16_UINT(DataType.UNSIGNED_SHORT, Sampler.TexelFormat.R),
        DXGI_FORMAT_R16_SNORM(DataType.NORMALIZED_SHORT, Sampler.TexelFormat.R),
        DXGI_FORMAT_R16_SINT(DataType.SHORT, Sampler.TexelFormat.R),
        DXGI_FORMAT_R8_TYPELESS,
        DXGI_FORMAT_R8_UNORM(DataType.UNSIGNED_NORMALIZED_BYTE, Sampler.TexelFormat.R),
        DXGI_FORMAT_R8_UINT(DataType.UNSIGNED_BYTE, Sampler.TexelFormat.R),
        DXGI_FORMAT_R8_SNORM(DataType.NORMALIZED_BYTE, Sampler.TexelFormat.R),
        DXGI_FORMAT_R8_SINT(DataType.BYTE, Sampler.TexelFormat.R),
        DXGI_FORMAT_A8_UNORM(DataType.UNSIGNED_NORMALIZED_BYTE, Sampler.TexelFormat.R),
        DXGI_FORMAT_R1_UNORM,
        DXGI_FORMAT_R9G9B9E5_SHAREDEXP,
        DXGI_FORMAT_R8G8_B8G8_UNORM,
        DXGI_FORMAT_G8R8_G8B8_UNORM,
        // DXT1
        DXGI_FORMAT_BC1_TYPELESS(DataType.UNSIGNED_BYTE, Sampler.TexelFormat.COMPRESSED_RGB),
        // DXT1
        DXGI_FORMAT_BC1_UNORM(DataType.UNSIGNED_BYTE, Sampler.TexelFormat.COMPRESSED_RGB),
        // DXT1
        DXGI_FORMAT_BC1_UNORM_SRGB(DataType.UNSIGNED_BYTE, Sampler.TexelFormat.COMPRESSED_RGB),
        // DXT3
        DXGI_FORMAT_BC2_TYPELESS(DataType.UNSIGNED_BYTE, Sampler.TexelFormat.COMPRESSED_RGBA),
        // DXT3
        DXGI_FORMAT_BC2_UNORM(DataType.UNSIGNED_BYTE, Sampler.TexelFormat.COMPRESSED_RGBA),
        // DXT3
        DXGI_FORMAT_BC2_UNORM_SRGB(DataType.UNSIGNED_BYTE, Sampler.TexelFormat.COMPRESSED_RGBA),
        // DXT5
        DXGI_FORMAT_BC3_TYPELESS(DataType.UNSIGNED_BYTE, Sampler.TexelFormat.COMPRESSED_RGBA),
        // DXT5
        DXGI_FORMAT_BC3_UNORM(DataType.UNSIGNED_BYTE, Sampler.TexelFormat.COMPRESSED_RGBA),
        // DXT5
        DXGI_FORMAT_BC3_UNORM_SRGB(DataType.UNSIGNED_BYTE, Sampler.TexelFormat.COMPRESSED_RGBA),
        DXGI_FORMAT_BC4_TYPELESS,
        DXGI_FORMAT_BC4_UNORM,
        DXGI_FORMAT_BC4_SNORM,
        DXGI_FORMAT_BC5_TYPELESS,
        DXGI_FORMAT_BC5_UNORM,
        DXGI_FORMAT_BC5_SNORM,
        DXGI_FORMAT_B5G6R5_UNORM,
        DXGI_FORMAT_B5G5R5A1_UNORM,
        DXGI_FORMAT_B8G8R8A8_UNORM(DataType.UNSIGNED_NORMALIZED_BYTE, Sampler.TexelFormat.RGBA),
        DXGI_FORMAT_B8G8R8X8_UNORM;

        final boolean supported;
        final DataType type;
        final Sampler.TexelFormat format;

        private DXGIPixelFormat() {
            this.format = null;
            this.type = null;
            supported = false;
        }

        private DXGIPixelFormat(DataType type, Sampler.TexelFormat format) {
            supported = true;
            this.format = format;
            this.type = type;
        }
    }

    /*
     * A mapping between color component masks to the DDS pixel formats
     */
    private static class DDPFMap {
        public static enum Swizzle {
            R,
            RG,
            RGB,
            BGR,
            RGBA,
            BGRA,
            ARGB
        }

        final int bitCount;
        final int rMask;
        final int gMask;
        final int bMask;
        final int aMask;
        final Swizzle swizzle;

        final DataType type;

        public DDPFMap(int bitCount, int rMask, int gMask, int bMask, int aMask, DataType type,
                       Swizzle swizzle) {
            this.bitCount = bitCount;
            this.rMask = rMask;
            this.gMask = gMask;
            this.bMask = bMask;
            this.aMask = aMask;

            this.type = type;
            this.swizzle = swizzle;
        }

        public boolean equals(DDSPixelFormat pf) {
            return this.equals(pf.rgbBitCount, pf.rBitMask, pf.gBitMask, pf.bBitMask, pf.rgbAlphaBitMask);
        }

        public boolean equals(int bitCount, int rMask, int gMask, int bMask, int aMask) {
            return (this.bitCount == bitCount && this.rMask == rMask &&
                    this.gMask == gMask && this.bMask == bMask && this.aMask == aMask);
        }
    }

    /*
     * Only 5 general types of pre-DX10 pixel format: RGB, RGBA, L, LA, and A
     * Each defined by bit masks for the component types.
     */

    // Supported RGB types
    private static final DDPFMap[] pfRGB = new DDPFMap[] {
            new DDPFMap(24, 0xff0000, 0xff00, 0xff, 0, DataType.UNSIGNED_NORMALIZED_BYTE,
                        DDPFMap.Swizzle.RGB),
            new DDPFMap(24, 0xff, 0xff00, 0xff0000, 0, DataType.UNSIGNED_NORMALIZED_BYTE,
                        DDPFMap.Swizzle.BGR),
    };

    // Supported RGBA types
    private static final DDPFMap[] pfRGBA = new DDPFMap[] {
            new DDPFMap(32, 0xff0000, 0xff00, 0xff, 0xff000000, DataType.INT_BIT_FIELD, DDPFMap.Swizzle.ARGB),
            new DDPFMap(32, 0xff000000, 0xff0000, 0xff00, 0x000000ff, DataType.UNSIGNED_NORMALIZED_BYTE,
                        DDPFMap.Swizzle.RGBA),
            new DDPFMap(32, 0xff00, 0xff0000, 0xff000000, 0xff, DataType.UNSIGNED_NORMALIZED_BYTE,
                        DDPFMap.Swizzle.BGRA)
    };

    // Supported Luminance types
    private static final DDPFMap[] pfL = new DDPFMap[] {
            new DDPFMap(8, 0xff, 0, 0, 0, DataType.UNSIGNED_NORMALIZED_BYTE, DDPFMap.Swizzle.R)
    };

    // Supported Luminance/Alpha types
    private static final DDPFMap[] pfLA = new DDPFMap[] {
            new DDPFMap(16, 0xff, 0, 0, 0xff00, DataType.UNSIGNED_NORMALIZED_BYTE, DDPFMap.Swizzle.RG)
    };

    // Supported Alpha types
    private static final DDPFMap[] pfA = new DDPFMap[] {
            new DDPFMap(8, 0, 0, 0, 0xff, DataType.UNSIGNED_NORMALIZED_BYTE, DDPFMap.Swizzle.R)
    };

    /*
     * More constants for the DDS header
     */

    // Selected bits in DDSHeader flags
    private static final int DDSD_CAPS = 0x00000001; // Capacities are valid
    private static final int DDSD_HEIGHT = 0x00000002; // Height is valid
    private static final int DDSD_WIDTH = 0x00000004; // Width is valid
    private static final int DDSD_PITCH = 0x00000008; // Pitch is valid
    private static final int DDSD_PIXELFORMAT = 0x00001000; // ddpfPixelFormat
    // is valid
    private static final int DDSD_MIPMAPCOUNT = 0x00020000; // Mip map count is
    // valid
    private static final int DDSD_LINEARSIZE = 0x00080000; // dwLinearSize is
    // valid
    private static final int DDSD_DEPTH = 0x00800000; // dwDepth is valid

    // Selected bits in DDSPixelFormat flags
    private static final int DDPF_ALPHAPIXELS = 0x00000001; // Alpha channel is present
    private static final int DDPF_ALPHA = 0x00000002; // Only contains alpha information
    private static final int DDPF_LUMINANCE = 0x00020000; // luminance data
    private static final int DDPF_FOURCC = 0x00000004; // FourCC code is valid
    private static final int DDPF_RGB = 0x00000040; // RGB data is present

    // Selected bits in DDS capabilities flags
    private static final int DDSCAPS_TEXTURE = 0x00001000; // Can be used as a texture
    private static final int DDSCAPS_MIPMAP = 0x00400000; // Is one level of a mip-map
    private static final int DDSCAPS_COMPLEX = 0x00000008; // Complex surface structure, such as a cube map

    // Selected bits in DDS capabilities 2 flags
    private static final int DDSCAPS2_CUBEMAP = 0x00000200;
    private static final int DDSCAPS2_CUBEMAP_POSITIVEX = 0x00000400;
    private static final int DDSCAPS2_CUBEMAP_NEGATIVEX = 0x00000800;
    private static final int DDSCAPS2_CUBEMAP_POSITIVEY = 0x00001000;
    private static final int DDSCAPS2_CUBEMAP_NEGATIVEY = 0x00002000;
    private static final int DDSCAPS2_CUBEMAP_POSITIVEZ = 0x00004000;
    private static final int DDSCAPS2_CUBEMAP_NEGATIVEZ = 0x00008000;
    private static final int DDSCAPS2_CUBEMAP_ALL_FACES = 0x0000fc00;
    private static final int DDSCAPS2_VOLUME = 0x00200000;

    // Selected bits in DDSHeader_DX10 misc flags
    private static final int D3D10_MISC_RESOURCE_GENERATE_MIPS = 0x1;
    private static final int D3D10_MISC_RESOURCE_SHARED = 0x2;
    private static final int D3D10_MISC_RESOURCE_TEXTURECUBE = 0x4;

    // D3D10 Resource Dimension enum
    private static final int D3D10_RESOURCE_DIMENSION_UNKNOWN = 0;
    private static final int D3D10_RESOURCE_DIMENSION_BUFFER = 1;
    private static final int D3D10_RESOURCE_DIMENSION_TEXTURE1D = 2;
    private static final int D3D10_RESOURCE_DIMENSION_TEXTURE2D = 3;
    private static final int D3D10_RESOURCE_DIMENSION_TEXTURE3D = 4;

    // FourCC codes
    private static final int FOURCC_DDS = makeFourCC("DDS ");
    private static final int FOURCC_DX10 = makeFourCC("DX10");
    private static final int FOURCC_DXT1 = makeFourCC("DXT1");
    private static final int FOURCC_DXT3 = makeFourCC("DXT3");
    private static final int FOURCC_DXT5 = makeFourCC("DXT5");

    /*
     * Methods to interpret the DDS header and convert it to OpenGL specific
     * enums
     */

    /**
     * Makes sure that the DDSHeader read in has the minimal correct values identifying it as a valid DDS
     * texture.
     */
    private static void validateHeader(DDSHeader h) throws IOException {
        // Must have the magic number 'DDS '
        // Size must be 124, although devIL reports that some files have 'DDS '
        // in the size var as well, so we'll support that.
        if (h.magic != FOURCC_DDS || (h.size != 124 && h.size != FOURCC_DDS)) {
            throw new IOException("DDS header is invalid");
        }
        if (h.pixelFormat.size != 32) {
            throw new IOException("DDS pixel format header is invalid");
        }

        // Give me some valid assumptions
        if (h.size == FOURCC_DDS) {
            h.size = 124;
        }
        if (h.mipmapCount == 0) {
            h.mipmapCount = 1;
        }
        if (h.depth == 0) {
            h.depth = 1;
        }

        // Header flags will be further validated as the header is interpreted
        // in identifyX() methods
    }

    /* Validates and looks at the DDSHeader for dimensions and texture target. */
    private void identifyBuildParams() throws IOException {
        if (!isFlagSet(header.flags, DDSD_CAPS)) {
            throw new IOException("DDS header is missing required flag DDSD_CAPS");
        }

        // check 2d dimensions, must be present for any texture type
        if (isFlagSet(header.flags, DDSD_WIDTH)) {
            width = header.width;
        } else {
            throw new IOException("DDS header is missing required flag DDSD_WIDTH");
        }

        if (isFlagSet(header.flags, DDSD_HEIGHT)) {
            height = header.height;
        } else {
            throw new IOException("DDS header is missing required flag DDSD_HEIGHT");
        }

        if (!isFlagSet(header.caps1, DDSCAPS_TEXTURE)) {
            throw new IOException("DDS surface capabilities missing required flag DDSCAPS_TEXTURE");
        }

        // We won't check for DDSCAPS_COMPLEX, since some files seem to ignore
        // it when creating cube maps or 3d textures
        if (isFlagSet(header.caps2, DDSCAPS2_VOLUME)) {
            target = Texture3D.class;
        } else if (isFlagSet(header.caps2, DDSCAPS2_CUBEMAP)) {
            target = TextureCubeMap.class;
        } else {
            target = Texture2D.class;
        }

        depth = 1;
        // further validate the dimensions
        if (target.equals(Texture3D.class)) {
            if (isFlagSet(header.flags, DDSD_DEPTH)) {
                depth = header.depth;
            } else {
                throw new IOException("DDSD header is missing required flag DDSD_DEPTH for a volume texture");
            }
        } else if (target.equals(TextureCubeMap.class)) {
            if (!isFlagSet(header.caps2, DDSCAPS2_CUBEMAP_ALL_FACES)) {
                throw new IOException("Cube map must have 6 faces present");
            }
            if (width != height) {
                throw new IOException("Cube map must have square faces");
            }
        }

        // validate the DX10 header as well
        if (header.headerDX10 != null) {
            if (target.equals(Texture2D.class)) {
                if (header.headerDX10.resourceDimension != D3D10_RESOURCE_DIMENSION_TEXTURE2D) {
                    throw new IOException("DX10 header and surface caps are inconsistent");
                }
                if (header.headerDX10.arraySize > 1) {
                    target = Texture2DArray.class;
                }
            } else if (target.equals(Texture3D.class)) {
                if (header.headerDX10.resourceDimension != D3D10_RESOURCE_DIMENSION_TEXTURE3D) {
                    throw new IOException("DX10 header and surface caps are inconsistent");
                }
                if (header.headerDX10.arraySize > 1) {
                    throw new IOException("Texture arrays aren't supported");
                }
            } else if (target.equals(Texture3D.class)) {
                if (header.headerDX10.resourceDimension == D3D10_RESOURCE_DIMENSION_TEXTURE2D) {
                    // nvidia sets the dx10 header to be a 2d tex, with
                    // arraySize = 6 for cubemaps
                    if (header.headerDX10.arraySize != 6) {
                        throw new IOException("Cube map must have 6 faces present");
                    }
                } else {
                    throw new IOException("DX10 header and surface caps are inconsistent");
                }
            }
        }

        // check for a mipmap count
        if (isFlagSet(header.flags, DDSD_MIPMAPCOUNT)) {
            mipmapCount = header.mipmapCount;
            if (mipmapCount > 1) {
                if (!isFlagSet(header.caps1, DDSCAPS_MIPMAP) || !isFlagSet(header.caps1, DDSCAPS_COMPLEX)) {
                    throw new IOException("DDS surface capabilities are invalid for a mipmapped texture");
                }
            }
            // make sure all the mipmaps are present
            int expected = (int) (Math.log(Math.max(width, Math.max(height, depth))) / Math.log(2) + 1);
            if (mipmapCount != expected) {
                throw new IOException("Expected " + expected + " but got " + mipmapCount +
                                      " mipmaps instead");
            }
        } else {
            mipmapCount = 1;
        }
    }

    /*
     * Further validates the dds header, and tries to identify a supported
     * opengl texture format.
     */
    private void identifyTextureFormat() throws IOException {
        if (!isFlagSet(header.flags, DDSD_PIXELFORMAT)) {
            throw new IOException("DDSD header is missing required flag DDSD_PIXELFORMAT");
        }

        if (header.headerDX10 != null) {
            // the pixel format is stored in the dxgiFormat
            if (!header.headerDX10.dxgiFormat.supported) {
                throw new IOException("Unsupported dxgi pixel format: " + header.headerDX10.dxgiFormat);
            } else {
                format = header.headerDX10.dxgiFormat;
            }

            // possibly promote the texture target to a depth map
            if (format == DXGIPixelFormat.DXGI_FORMAT_D24_UNORM_S8_UINT ||
                format == DXGIPixelFormat.DXGI_FORMAT_D32_FLOAT) {
                if (target.equals(Texture2D.class)) {
                    target = DepthMap2D.class;
                } else if (target.equals(TextureCubeMap.class)) {
                    target = DepthCubeMap.class;
                } else {
                    throw new IOException(target + " does not support depth formats");
                }
            }
        } else if (isFlagSet(header.pixelFormat.flags, DDPF_FOURCC)) {
            // interpret the FOURCC flag. Currently only supports DXT1, DXT3, and DXT5
            if (header.pixelFormat.fourCC == FOURCC_DXT1) {
                format = DXGIPixelFormat.DXGI_FORMAT_BC1_UNORM;
            } else if (header.pixelFormat.fourCC == FOURCC_DXT3) {
                format = DXGIPixelFormat.DXGI_FORMAT_BC2_UNORM;
            } else if (header.pixelFormat.fourCC == FOURCC_DXT5) {
                format = DXGIPixelFormat.DXGI_FORMAT_BC3_UNORM;
            } else {
                throw new IOException("Unrecognized fourCC value in pixel format: " +
                                      unmakeFourCC(header.pixelFormat.fourCC));
            }
        } else {
            // choose the correct DDPFMap array
            DDPFMap[] supported;
            if (isFlagSet(header.pixelFormat.flags, DDPF_LUMINANCE)) {
                if (isFlagSet(header.pixelFormat.flags, DDPF_ALPHAPIXELS)) {
                    supported = pfLA;
                } else {
                    supported = pfL;
                }
            } else if (isFlagSet(header.pixelFormat.flags, DDPF_RGB)) {
                if (isFlagSet(header.pixelFormat.flags, DDPF_ALPHAPIXELS)) {
                    supported = pfRGBA;
                } else {
                    supported = pfRGB;
                }
            } else if (isFlagSet(header.pixelFormat.flags, DDPF_ALPHA)) {
                if (isFlagSet(header.pixelFormat.flags, DDPF_ALPHAPIXELS)) {
                    supported = pfA;
                } else {
                    throw new IOException("Invalid pixel format header");
                }
            } else {
                throw new IOException("Invalid pixel format header");
            }

            for (int i = 0; i < supported.length; i++) {
                if (supported[i].equals(header.pixelFormat)) {
                    oldFormat = supported[i];
                    break;
                }
            }

            if (oldFormat == null) {
                throw new IOException("Unsupported pixel format: " + header.pixelFormat);
            }
        }
    }

    /*
     * Methods to read in a DDS file from an input stream.
     */

    /* Create a new DDSHeader and fill its values based on the input stream. */
    private static DDSHeader readHeader(InputStream in) throws IOException {
        DDSHeader h = new DDSHeader();

        h.magic = readLEInt(in);
        h.size = readLEInt(in);
        h.flags = readLEInt(in);
        h.height = readLEInt(in);
        h.width = readLEInt(in);
        h.linearSize = readLEInt(in);
        h.depth = readLEInt(in);
        h.mipmapCount = readLEInt(in);
        for (int i = 0; i < h.reserved1.length; i++) {
            h.reserved1[i] = readLEInt(in);
        }

        h.pixelFormat = new DDSPixelFormat();
        h.pixelFormat.size = readLEInt(in);
        h.pixelFormat.flags = readLEInt(in);
        h.pixelFormat.fourCC = readLEInt(in);
        h.pixelFormat.rgbBitCount = readLEInt(in);
        h.pixelFormat.rBitMask = readLEInt(in);
        h.pixelFormat.gBitMask = readLEInt(in);
        h.pixelFormat.bBitMask = readLEInt(in);
        h.pixelFormat.rgbAlphaBitMask = readLEInt(in);

        h.caps1 = readLEInt(in);
        h.caps2 = readLEInt(in);
        h.caps3 = readLEInt(in);
        h.caps4 = readLEInt(in);

        h.reserved2 = readLEInt(in);

        if (h.pixelFormat.fourCC == FOURCC_DX10) { // According to AMD, this is how we know if it's present
            h.headerDX10 = new DDSHeader_DX10();
            int dxgi = readLEInt(in);
            h.headerDX10.dxgiFormat = (dxgi < 0 || dxgi >= DXGIPixelFormat.values().length ? DXGIPixelFormat
                    .values()[0] : DXGIPixelFormat.values()[dxgi]);
            h.headerDX10.resourceDimension = readLEInt(in);
            h.headerDX10.miscFlag = readLEInt(in);
            h.headerDX10.arraySize = readLEInt(in);
            h.headerDX10.reserved = readLEInt(in);
        } else {
            h.headerDX10 = null;
        }

        return h;
    }

    /**
     * Read the data from the input stream, assuming that it is a fully valid DDS file, where the next byte
     * read is the first byte in the texture data (header already read from stream).
     */
    private void readData(InputStream in) throws IOException {
        int width, height, depth, size;
        int arrayCount;
        if (target.equals(TextureCubeMap.class)) {
            arrayCount = 6; // faces are ordered px, nx, py, ny, pz, nz
        } else if (target.equals(Texture2DArray.class)) {
            arrayCount = header.headerDX10.arraySize;
        } else {
            arrayCount = 1;
        }

        data = new Object[arrayCount][mipmapCount];
        byte[] image;
        for (int i = 0; i < arrayCount; i++) {
            for (int m = 0; m < mipmapCount; m++) {
                size = getBufferSize(m);

                image = new byte[size];
                readAll(in, image);
                data[i][m] = createBuffer(image);
            }
        }
    }

    private int getBufferSize(int mipmap) {
        int w = Math.max(width >> mipmap, 1);
        int h = Math.max(height >> mipmap, 1);
        int d = Math.max(depth >> mipmap, 1);

        if (format != null) {
            // use the DXGI pixel format
            switch (format) {
            case DXGI_FORMAT_D24_UNORM_S8_UINT:
            case DXGI_FORMAT_R11G11B10_FLOAT:
                // packed formats
                return w * h * d * format.type.getByteCount();
            case DXGI_FORMAT_BC1_TYPELESS:
            case DXGI_FORMAT_BC1_UNORM:
            case DXGI_FORMAT_BC1_UNORM_SRGB:
                // DXT1 buffer size
                return (int) (8 * Math.ceil(w / 4.0) * Math.ceil(4.0));
            case DXGI_FORMAT_BC2_TYPELESS:
            case DXGI_FORMAT_BC2_UNORM:
            case DXGI_FORMAT_BC2_UNORM_SRGB:
            case DXGI_FORMAT_BC3_TYPELESS:
            case DXGI_FORMAT_BC3_UNORM:
            case DXGI_FORMAT_BC3_UNORM_SRGB:
                // DXT3 and DXT5 buffer size
                return (int) (16 * Math.ceil(w / 4.0) * Math.ceil(4.0));
            default:
                return format.format.getComponentCount() * format.type.getByteCount() * w * h * d;
            }
        } else {
            // use the old pixel format (type is always UNSIGNED_NORMALIZED_BYTE, or INT_BIT_FIELD)
            if (oldFormat.type == DataType.INT_BIT_FIELD) {
                return w * h * d;
            } else {
                // length of name of the swizzle is the number of components
                return w * h * d * oldFormat.swizzle.name().length();
            }
        }
    }

    // create an appropriately typed nio buffer based the DDSTexture's glType and the byte[] image.
    // for int, short, and float primitive types, it converts the byte ordering into big endian.
    private Object createBuffer(byte[] image) throws IOException {
        DataType type = (format != null ? format.type : oldFormat.type);

        if (type.getJavaPrimitive().equals(float.class)) {
            float[] data = new float[image.length / 4];
            for (int i = 0; i < data.length; i++) {
                data[i] = bytesToFloat(image, i << 2);
            }
            return data;
        } else if (type.getJavaPrimitive().equals(int.class)) {
            int[] data = new int[image.length / 4];
            for (int i = 0; i < data.length; i++) {
                data[i] = bytesToInt(image, i << 2);
            }
            return data;
        } else if (type.getJavaPrimitive().equals(short.class)) {
            short[] data = new short[image.length / 2];
            for (int i = 0; i < data.length; i++) {
                data[i] = bytesToShort(image, i << 1);
            }
            return data;
        } else {
            return image;
        }
    }

    /*
     * Utility methods relating to I/O and the DDS file format
     */

    // check whether or not flag is set in the flags bit field
    private static boolean isFlagSet(int flags, int flag) {
        return (flags & flag) == flag;
    }

    // create a 4cc code from the given string. The string must have length = 4
    private static int makeFourCC(String c) {
        if (c.length() != 4) {
            throw new IllegalArgumentException("Input string for a 4CC must have size of 4");
        }
        char[] cc = c.toCharArray();
        return ((cc[3] & 0xff) << 24) | ((cc[2] & 0xff) << 16) | ((cc[1] & 0xff) << 8) |
               ((cc[0] & 0xff));
    }

    // convert a 4cc code back into string form
    private static String unmakeFourCC(int fourcc) {
        char[] cc = new char[4];
        cc[3] = (char) ((fourcc & 0xff000000) >> 24);
        cc[2] = (char) ((fourcc & 0xff0000) >> 16);
        cc[1] = (char) ((fourcc & 0xff00) >> 8);
        cc[0] = (char) ((fourcc & 0xff));
        return new String(cc);
    }

    // as bytesToInt, but for shorts (converts 2 bytes instead of 4)
    // assuming little endian
    private static short bytesToShort(byte[] in, int offset) {
        return (short) ((in[offset] & 0xff) | ((in[offset + 1] & 0xff) << 8));
    }

    // as bytesToInt, but for floats
    private static float bytesToFloat(byte[] in, int offset) {
        return Float.intBitsToFloat(bytesToInt(in, offset));
    }

    // convert 4 bytes starting at offset into an integer, assuming
    // the bytes are ordered little endian.
    private static int bytesToInt(byte[] in, int offset) {
        return ((in[offset] & 0xff) | ((in[offset + 1] & 0xff) << 8) |
                ((in[offset + 2] & 0xff) << 16) | ((in[offset + 3] & 0xff) << 24));
    }

    // read bytes from the given stream until the array is full
    // fails if the end-of-stream happens before the array is full
    private static void readAll(InputStream in, byte[] array) throws IOException {
        int remaining = array.length;
        int offset = 0;
        int read;
        while (remaining > 0) {
            read = in.read(array, offset, remaining);
            if (read < 0) {
                throw new IOException("Unexpected end of stream");
            }
            offset += read;
            remaining -= read;
        }
    }

    // read an integer represented in little endian from the given input stream
    private static int readLEInt(InputStream in) throws IOException {
        byte[] b = new byte[4];
        readAll(in, b);
        return bytesToInt(b, 0);
    }
}
