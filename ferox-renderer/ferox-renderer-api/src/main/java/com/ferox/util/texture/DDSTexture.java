package com.ferox.util.texture;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.ferox.resource.BufferData;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.Mipmap;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.TextureFormat;

/**
 * <p>
 * DDSTexture is a utility class that can be used to read in cube maps, 3d
 * textures and 2d textures from an input stream.
 * </p>
 * <p>
 * It is recommended to use TextureLoader for input however, since it will
 * delegate to DDSTexture when needed.
 * </p>
 * 
 * @author Michael Ludwig
 */
@SuppressWarnings("unused")
public class DDSTexture {
    // dds header read in from the input stream
    private final DDSHeader header;

    // representation of the dds texture
    private DataType type;
    private TextureFormat format;
    private Target target;

    private int mipmapCount; // at least 1
    // depth may be unused for cube maps and 2d textures
    // (in which case it's set to 1)
    private int width, height, depth;
    private BufferData[][] data; // accessed [face][mipmap]

    /**
     * <p>
     * Read in and create a new Texture from the given stream. This image will
     * be a Texture2D, Texture3D, or TextureCubeMap. An IOException will be
     * thrown if the stream doesn't represent a valid DDS texture or if its
     * header can't be identified to a supported TextureFormat, etc.
     * </p>
     * <p>
     * It assumes that the stream starts at the first byte of the header section
     * for the DDS texture. The stream will not be closed.
     * </p>
     * 
     * @param stream The InputStream to read the texture from
     * @return The Texture read from stream
     * @throws IOException if an IOException occurs while reading, or if the
     *             stream is an invalid or unsupported DDS texture
     */
    public static Texture readTexture(InputStream stream) throws IOException {
        if (stream == null) {
            throw new IOException("Cannot read a texture from a null stream");
        }

        DDSTexture texture = new DDSTexture(stream);
        Mipmap[] mips = new Mipmap[texture.data.length];
        for (int i = 0; i < mips.length; i++) {
            mips[i] = new Mipmap(texture.data[i],
                                 texture.width,
                                 texture.height,
                                 texture.depth,
                                 texture.format);
        }
        return new Texture(texture.target, mips);
    }

    /**
     * <p>
     * Determine if the given stream represents the start of a valid DDS file.
     * All it checks is the header portion of the given stream. As such, it is
     * possible that it appears as a DDS file, but the rest of the file is not
     * valid.
     * </p>
     * <p>
     * The stream will mark and then read the 124 bytes of the header. If an
     * IOException occurs while reading or parsing the header, then false is
     * returned. When finished, the stream will be reset back to the mark.
     * </p>
     * <p>
     * <i>The stream will not be closed.</i>
     * </p>
     * 
     * @param stream The InputStream that represents the data source for a DDS
     *            texture
     * @return Whether or not stream is a DDS texture, doesn't check if the
     *         texture is supported
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
            String first = "RGB (" + isFlagSet(flags, DDPF_RGB) + "), LUM (" + isFlagSet(flags,
                                                                                         DDPF_LUMINANCE) + "), ALPHA (" + isFlagSet(flags,
                                                                                                                                    DDPF_ALPHA) + "), FourCC (" + isFlagSet(flags,
                                                                                                                                                                            DDPF_FOURCC) + ")";
            String second;
            if (isFlagSet(flags, DDPF_FOURCC)) {
                second = "FourCC = " + unmakeFourCC(fourCC);
            } else {
                second = "Total bits = " + rgbBitCount + ", has alpha = " + isFlagSet(flags,
                                                                                      DDPF_ALPHAPIXELS) + " Bit masks: r/l = " + Integer.toHexString(rBitMask) + ", g = " + Integer.toHexString(gBitMask) + ", b = " + Integer.toHexString(bBitMask) + ", a = " + Integer.toHexString(rgbAlphaBitMask);
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
        DXGI_FORMAT_R32G32B32A32_FLOAT(DataType.FLOAT, TextureFormat.RGBA_FLOAT),
        DXGI_FORMAT_R32G32B32A32_UINT,
        DXGI_FORMAT_R32G32B32A32_SINT,
        DXGI_FORMAT_R32G32B32_TYPELESS,
        DXGI_FORMAT_R32G32B32_FLOAT(DataType.FLOAT, TextureFormat.RGB_FLOAT),
        DXGI_FORMAT_R32G32B32_UINT,
        DXGI_FORMAT_R32G32B32_SINT,
        DXGI_FORMAT_R16G16B16A16_TYPELESS,
        DXGI_FORMAT_R16G16B16A16_FLOAT,
        DXGI_FORMAT_R16G16B16A16_UNORM(DataType.UNSIGNED_SHORT, TextureFormat.RGBA),
        DXGI_FORMAT_R16G16B16A16_UINT,
        DXGI_FORMAT_R16G16B16A16_SNORM,
        DXGI_FORMAT_R16G16B16A16_SINT,
        DXGI_FORMAT_R32G32_TYPELESS,
        DXGI_FORMAT_R32G32_FLOAT(DataType.FLOAT, TextureFormat.RG_FLOAT),
        DXGI_FORMAT_R32G32_UINT,
        DXGI_FORMAT_R32G32_SINT,
        DXGI_FORMAT_R32G8X24_TYPELESS,
        DXGI_FORMAT_D32_FLOAT_S8X24_UINT,
        DXGI_FORMAT_R32_FLOAT_X8X24_TYPELESS,
        DXGI_FORMAT_X32_TYPELESS_G8X24_UINT,
        DXGI_FORMAT_R10G10B10A2_TYPELESS,
        DXGI_FORMAT_R10G10B10A2_UNORM,
        DXGI_FORMAT_R10G10B10A2_UINT,
        DXGI_FORMAT_R11G11B10_FLOAT,
        DXGI_FORMAT_R8G8B8A8_TYPELESS,
        DXGI_FORMAT_R8G8B8A8_UNORM(DataType.UNSIGNED_INT, TextureFormat.RGBA_8888),
        DXGI_FORMAT_R8G8B8A8_UNORM_SRGB(DataType.UNSIGNED_INT, TextureFormat.RGBA_8888),
        DXGI_FORMAT_R8G8B8A8_UINT,
        DXGI_FORMAT_R8G8B8A8_SNORM,
        DXGI_FORMAT_R8G8B8A8_SINT,
        DXGI_FORMAT_R16G16_TYPELESS,
        DXGI_FORMAT_R16G16_FLOAT,
        DXGI_FORMAT_R16G16_UNORM(DataType.UNSIGNED_SHORT, TextureFormat.RG),
        DXGI_FORMAT_R16G16_UINT,
        DXGI_FORMAT_R16G16_SNORM,
        DXGI_FORMAT_R16G16_SINT,
        DXGI_FORMAT_R32_TYPELESS,
        DXGI_FORMAT_D32_FLOAT(DataType.FLOAT, TextureFormat.DEPTH),
        // not the best mapping, but it works
        DXGI_FORMAT_R32_FLOAT(DataType.FLOAT, TextureFormat.R_FLOAT),
        DXGI_FORMAT_R32_UINT,
        DXGI_FORMAT_R32_SINT,
        DXGI_FORMAT_R24G8_TYPELESS,
        DXGI_FORMAT_D24_UNORM_S8_UINT,
        DXGI_FORMAT_R24_UNORM_X8_TYPELESS,
        DXGI_FORMAT_X24_TYPELESS_G8_UINT,
        DXGI_FORMAT_R8G8_TYPELESS,
        // not the best mapping, but it works
        DXGI_FORMAT_R8G8_UNORM(DataType.UNSIGNED_BYTE, TextureFormat.RG),
        DXGI_FORMAT_R8G8_UINT, DXGI_FORMAT_R8G8_SNORM, DXGI_FORMAT_R8G8_SINT,
        DXGI_FORMAT_R16_TYPELESS, DXGI_FORMAT_R16_FLOAT,
        DXGI_FORMAT_D16_UNORM(DataType.UNSIGNED_SHORT, TextureFormat.DEPTH),
        DXGI_FORMAT_R16_UNORM(DataType.UNSIGNED_SHORT, TextureFormat.R),
        DXGI_FORMAT_R16_UINT, DXGI_FORMAT_R16_SNORM, DXGI_FORMAT_R16_SINT,
        DXGI_FORMAT_R8_TYPELESS, DXGI_FORMAT_R8_UNORM(DataType.UNSIGNED_BYTE,
                                                      TextureFormat.R),
        DXGI_FORMAT_R8_UINT, DXGI_FORMAT_R8_SNORM, DXGI_FORMAT_R8_SINT,
        DXGI_FORMAT_A8_UNORM(DataType.UNSIGNED_BYTE, TextureFormat.R),
        DXGI_FORMAT_R1_UNORM, DXGI_FORMAT_R9G9B9E5_SHAREDEXP,
        DXGI_FORMAT_R8G8_B8G8_UNORM, DXGI_FORMAT_G8R8_G8B8_UNORM,
        DXGI_FORMAT_BC1_TYPELESS, DXGI_FORMAT_BC1_UNORM(DataType.UNSIGNED_BYTE,
                                                        TextureFormat.RGB_DXT1),
        DXGI_FORMAT_BC1_UNORM_SRGB(DataType.UNSIGNED_BYTE, TextureFormat.RGBA_DXT1),
        DXGI_FORMAT_BC2_TYPELESS, DXGI_FORMAT_BC2_UNORM(DataType.UNSIGNED_BYTE,
                                                        TextureFormat.RGBA_DXT3),
        DXGI_FORMAT_BC2_UNORM_SRGB(DataType.UNSIGNED_BYTE, TextureFormat.RGBA_DXT3),
        DXGI_FORMAT_BC3_TYPELESS, DXGI_FORMAT_BC3_UNORM(DataType.UNSIGNED_BYTE,
                                                        TextureFormat.RGBA_DXT5),
        DXGI_FORMAT_BC3_UNORM_SRGB(DataType.UNSIGNED_BYTE, TextureFormat.RGBA_DXT5),
        DXGI_FORMAT_BC4_TYPELESS, DXGI_FORMAT_BC4_UNORM, DXGI_FORMAT_BC4_SNORM,
        DXGI_FORMAT_BC5_TYPELESS, DXGI_FORMAT_BC5_UNORM, DXGI_FORMAT_BC5_SNORM,
        DXGI_FORMAT_B5G6R5_UNORM(DataType.UNSIGNED_SHORT, TextureFormat.BGR_565),
        DXGI_FORMAT_B5G5R5A1_UNORM(DataType.UNSIGNED_SHORT, TextureFormat.BGRA_5551),
        DXGI_FORMAT_B8G8R8A8_UNORM(DataType.UNSIGNED_INT, TextureFormat.BGRA_8888),
        DXGI_FORMAT_B8G8R8X8_UNORM;

        boolean supported;
        DataType type;
        TextureFormat format;

        int typeByteSize; // number of bytes of each primitive element for
        // glType
        int typePrimitiveSize; // number of primitives needed to store the color

        // data for glSrcFormat

        private DXGIPixelFormat() {
            this(null, null);
            supported = false;
        }

        private DXGIPixelFormat(DataType type, TextureFormat format) {
            supported = true;
            this.format = format;
            this.type = type;
        }
    }

    /*
     * A mapping between color component masks to openGL source, dest and type
     * enums.
     * 
     * It also keeps track of the byte size of the glType primitive, as well as
     * the number of primitives required to store a color element.
     */
    private static class DDPFMap {
        int bitCount;
        int rMask;
        int gMask;
        int bMask;
        int aMask;

        TextureFormat format;
        DataType type;

        public DDPFMap(int bitCount, int rMask, int gMask, int bMask, int aMask,
                       DataType type, TextureFormat format) {
            this.bitCount = bitCount;
            this.rMask = rMask;
            this.gMask = gMask;
            this.bMask = bMask;
            this.aMask = aMask;

            this.type = type;
            this.format = format;
        }

        public boolean equals(DDSPixelFormat pf) {
            return this.equals(pf.rgbBitCount, pf.rBitMask, pf.gBitMask, pf.bBitMask,
                               pf.rgbAlphaBitMask);
        }

        public boolean equals(int bitCount, int rMask, int gMask, int bMask, int aMask) {
            return (this.bitCount == bitCount && this.rMask == rMask && this.gMask == gMask && this.bMask == bMask && this.aMask == aMask);
        }
    }

    /*
     * Only 5 general types of pre-DX10 pixel format: RGB, RGBA, L, LA, and A
     * Each defined by bit masks for the component types.
     */

    // Supported RGB types
    private static final DDPFMap[] pfRGB = new DDPFMap[] {
                                                          new DDPFMap(24,
                                                                      0xff0000,
                                                                      0xff00,
                                                                      0xff,
                                                                      0,
                                                                      DataType.UNSIGNED_BYTE,
                                                                      TextureFormat.RGB),
                                                          new DDPFMap(24,
                                                                      0xff,
                                                                      0xff00,
                                                                      0xff0000,
                                                                      0,
                                                                      DataType.UNSIGNED_BYTE,
                                                                      TextureFormat.BGR),
                                                          new DDPFMap(16,
                                                                      0xf800,
                                                                      0x7e0,
                                                                      0x1f,
                                                                      0,
                                                                      DataType.UNSIGNED_BYTE,
                                                                      TextureFormat.RGB_565),
                                                          new DDPFMap(16,
                                                                      0x1f,
                                                                      0x7e0,
                                                                      0xf800,
                                                                      0,
                                                                      DataType.UNSIGNED_BYTE,
                                                                      TextureFormat.BGR_565)};

    // Supported RGBA types
    private static final DDPFMap[] pfRGBA = new DDPFMap[] {
                                                           new DDPFMap(32,
                                                                       0xff0000,
                                                                       0xff00,
                                                                       0xff,
                                                                       0xff000000,
                                                                       DataType.UNSIGNED_INT,
                                                                       TextureFormat.ARGB_8888),
                                                           new DDPFMap(32,
                                                                       0xff000000,
                                                                       0xff0000,
                                                                       0xff00,
                                                                       0x000000ff,
                                                                       DataType.UNSIGNED_INT,
                                                                       TextureFormat.RGBA_8888),
                                                           new DDPFMap(32,
                                                                       0xff,
                                                                       0xff00,
                                                                       0xff0000,
                                                                       0xff000000,
                                                                       DataType.UNSIGNED_INT,
                                                                       TextureFormat.ABGR_8888),
                                                           new DDPFMap(32,
                                                                       0xff00,
                                                                       0xff0000,
                                                                       0xff000000,
                                                                       0xff,
                                                                       DataType.UNSIGNED_INT,
                                                                       TextureFormat.BGRA_8888)};

    // Supported Luminance types
    private static final DDPFMap[] pfL = new DDPFMap[] {new DDPFMap(8,
                                                                    0xff,
                                                                    0,
                                                                    0,
                                                                    0,
                                                                    DataType.UNSIGNED_BYTE,
                                                                    TextureFormat.R)};

    // Supported Luminance/Alpha types
    private static final DDPFMap[] pfLA = new DDPFMap[] {new DDPFMap(16,
                                                                     0xff,
                                                                     0,
                                                                     0,
                                                                     0xff00,
                                                                     DataType.UNSIGNED_BYTE,
                                                                     TextureFormat.RG)};

    // Supported Alpha types
    private static final DDPFMap[] pfA = new DDPFMap[] {new DDPFMap(8,
                                                                    0,
                                                                    0,
                                                                    0,
                                                                    0xff,
                                                                    DataType.UNSIGNED_BYTE,
                                                                    TextureFormat.R)};

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
     * Makes sure that the DDSHeader read in has the minimal correct values
     * identifying it as a valid DDS texture.
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
            target = Target.T_3D;
        } else if (isFlagSet(header.caps2, DDSCAPS2_CUBEMAP)) {
            target = Target.T_CUBEMAP;
        } else {
            target = Target.T_2D;
        }

        depth = 1;
        // further validate the dimensions
        if (target == Target.T_3D) {
            if (isFlagSet(header.flags, DDSD_DEPTH)) {
                depth = header.depth;
            } else {
                throw new IOException("DDSD header is missing required flag DDSD_DEPTH for a volume texture");
            }
        } else if (target == Target.T_CUBEMAP) {
            if (!isFlagSet(header.caps2, DDSCAPS2_CUBEMAP_ALL_FACES)) {
                throw new IOException("Cube map must have 6 faces present");
            }
            if (width != height) {
                throw new IOException("Cube map must have square faces");
            }
        }

        // validate the DX10 header as well
        if (header.headerDX10 != null) {
            if (target == Target.T_2D) {
                if (header.headerDX10.resourceDimension != D3D10_RESOURCE_DIMENSION_TEXTURE2D) {
                    throw new IOException("DX10 header and surface caps are inconsistent");
                }
                if (header.headerDX10.arraySize > 1) {
                    throw new IOException("Texture arrays aren't supported");
                }
            } else if (target == Target.T_3D) {
                if (header.headerDX10.resourceDimension != D3D10_RESOURCE_DIMENSION_TEXTURE3D) {
                    throw new IOException("DX10 header and surface caps are inconsistent");
                }
                if (header.headerDX10.arraySize > 1) {
                    throw new IOException("Texture arrays aren't supported");
                }
            } else if (target == Target.T_CUBEMAP) {
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
                if (!isFlagSet(header.caps1, DDSCAPS_MIPMAP) || !isFlagSet(header.caps1,
                                                                           DDSCAPS_COMPLEX)) {
                    throw new IOException("DDS surface capabilities are invalid for a mipmapped texture");
                }
            }
            // make sure all the mipmaps are present
            int expected = (int) (Math.log(Math.max(width, Math.max(height, depth))) / Math.log(2) + 1);
            if (mipmapCount != expected) {
                throw new IOException("Expected " + expected + " but got " + mipmapCount + " mipmaps instead");
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
                format = header.headerDX10.dxgiFormat.format;
                type = header.headerDX10.dxgiFormat.type;
            }
        } else if (isFlagSet(header.pixelFormat.flags, DDPF_FOURCC)) {
            // interpret the FOURCC flag. Currently only supports DXT1, DXT3, and DXT5
            type = DataType.UNSIGNED_BYTE;
            format = null;

            if (header.pixelFormat.fourCC == FOURCC_DXT1) {
                if (isFlagSet(header.pixelFormat.flags, DDPF_ALPHAPIXELS)) {
                    format = TextureFormat.RGBA_DXT1;
                } else {
                    format = TextureFormat.RGB_DXT1;
                }
            } else if (header.pixelFormat.fourCC == FOURCC_DXT3) {
                format = TextureFormat.RGBA_DXT3;
            } else if (header.pixelFormat.fourCC == FOURCC_DXT5) {
                format = TextureFormat.RGBA_DXT5;
            } else {
                throw new IOException("Unrecognized fourCC value in pixel format: " + unmakeFourCC(header.pixelFormat.fourCC));
            }
        } else {
            // choose the correct DDPFMap array
            DDPFMap[] supported = null;
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
                    format = supported[i].format;
                    type = supported[i].type;
                    return;
                }
            }

            // if we've gotten to here, we didn't find a supported DDPF
            throw new IOException("Unsupported pixel format: " + header.pixelFormat);
        }

        if (format.getPrimitivesPerColor() < 0 && target == Target.T_3D) {
            throw new IOException("Compressed textures are only allowed to have targets of T_CUBEMAP or T_2D");
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
            h.headerDX10.dxgiFormat = (dxgi < 0 || dxgi >= DXGIPixelFormat.values().length ? DXGIPixelFormat.values()[0] : DXGIPixelFormat.values()[dxgi]);
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
     * Read the data from the input stream, assuming that it is a fully valid
     * DDS file, where the next byte read is the first byte in the texture data
     * (header already read from stream).
     */
    private void readData(InputStream in) throws IOException {
        int width, height, depth, size;
        int arrayCount = 1;
        if (target == Target.T_CUBEMAP) {
            arrayCount = 6; // faces are ordered px, nx, py, ny, pz, nz
        }
        data = new BufferData[arrayCount][mipmapCount];
        byte[] image;
        for (int i = 0; i < arrayCount; i++) {
            width = this.width;
            height = this.height;
            depth = this.depth;
            for (int m = 0; m < mipmapCount; m++) {
                size = format.getBufferSize(width, height, depth) * type.getByteCount();

                image = new byte[size];
                readAll(in, image);
                data[i][m] = createBuffer(image);

                width = Math.max(1, (width >> 1));
                height = Math.max(1, (height >> 1));
                depth = Math.max(1, (depth >> 1));
            }
        }
    }

    // create an appropriately typed nio buffer based the DDSTexture's glType and the byte[] image.
    // for int, short, and float primitive types, it converts the byte ordering into big endian.
    private BufferData createBuffer(byte[] image) throws IOException {
        switch (type) {
        case FLOAT: {
            float[] data = new float[image.length / 4];
            for (int i = 0; i < data.length; i++) {
                data[i] = bytesToFloat(image, i << 2);
            }
            return new BufferData(data);
        }
        case UNSIGNED_INT: {
            int[] data = new int[image.length / 4];
            for (int i = 0; i < data.length; i++) {
                data[i] = bytesToInt(image, i << 2);
            }
            return new BufferData(data);
        }
        case UNSIGNED_SHORT: {
            short[] data = new short[image.length / 2];
            for (int i = 0; i < data.length; i++) {
                data[i] = bytesToShort(image, i << 1);
            }
            return new BufferData(data);
        }
        case UNSIGNED_BYTE: {
            return new BufferData(image);
        }
        }
        throw new IOException("Unsupported texture data type: " + type);
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
        return ((cc[3] & 0xff) << 24) | ((cc[2] & 0xff) << 16) | ((cc[1] & 0xff) << 8) | ((cc[0] & 0xff) << 0);
    }

    // convert a 4cc code back into string form
    private static String unmakeFourCC(int fourcc) {
        char[] cc = new char[4];
        cc[3] = (char) ((fourcc & 0xff000000) >> 24);
        cc[2] = (char) ((fourcc & 0xff0000) >> 16);
        cc[1] = (char) ((fourcc & 0xff00) >> 8);
        cc[0] = (char) ((fourcc & 0xff) >> 0);
        return new String(cc);
    }

    // as bytesToInt, but for shorts (converts 2 bytes instead of 4)
    // assuming little endian
    private static short bytesToShort(byte[] in, int offset) {
        return (short) ((in[offset + 0] & 0xff) | ((in[offset + 1] & 0xff) << 8));
    }

    // as bytesToInt, but for floats
    private static float bytesToFloat(byte[] in, int offset) {
        return Float.intBitsToFloat(bytesToInt(in, offset));
    }

    // convert 4 bytes starting at offset into an integer, assuming
    // the bytes are ordered little endian.
    private static int bytesToInt(byte[] in, int offset) {
        return ((in[offset + 0] & 0xff) | ((in[offset + 1] & 0xff) << 8) | ((in[offset + 2] & 0xff) << 16) | ((in[offset + 3] & 0xff) << 24));
    }

    // read bytes from the given stream until the array is full
    // fails if the end-of-stream happens before the array is full
    private static void readAll(InputStream in, byte[] array) throws IOException {
        int remaining = array.length;
        int offset = 0;
        int read = 0;
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
