package com.ferox.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.*;

import com.ferox.core.states.atoms.Texture2D;
import com.ferox.core.states.atoms.Texture3D;
import com.ferox.core.states.atoms.TextureCubeMap;
import com.ferox.core.states.atoms.TextureData;
import com.ferox.core.states.atoms.TextureData.*;


@SuppressWarnings("unused")
public class DDSReader {
	// Stores a DDS header (equivalent for DX9 and DX10, DX10 may have another header, too)
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
		DDSHeader_DX10 headerDX10;
	}
	
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
		
		public String toString() {
			return Integer.toHexString(this.flags) + " (" + unmakeFourCC(this.fourCC) + ") " + this.rgbBitCount + " " +
				   Integer.toHexString(this.rBitMask) + " " + Integer.toHexString(this.gBitMask) + " " + Integer.toHexString(this.bBitMask)
				   + " " + Integer.toHexString(this.rgbAlphaBitMask);
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
	
	private static enum DXGIPixelFormat {
		DXGI_FORMAT_UNKNOWN,
	    DXGI_FORMAT_R32G32B32A32_TYPELESS,
	    DXGI_FORMAT_R32G32B32A32_FLOAT(TextureType.FLOAT, TextureFormat.RGBA),
	    DXGI_FORMAT_R32G32B32A32_UINT,
	    DXGI_FORMAT_R32G32B32A32_SINT,
	    DXGI_FORMAT_R32G32B32_TYPELESS,
	    DXGI_FORMAT_R32G32B32_FLOAT(TextureType.FLOAT, TextureFormat.RGB),
	    DXGI_FORMAT_R32G32B32_UINT,
	    DXGI_FORMAT_R32G32B32_SINT,
	    DXGI_FORMAT_R16G16B16A16_TYPELESS,
	    DXGI_FORMAT_R16G16B16A16_FLOAT,
	    DXGI_FORMAT_R16G16B16A16_UNORM(TextureType.UNSIGNED_SHORT, TextureFormat.RGBA),
	    DXGI_FORMAT_R16G16B16A16_UINT,
	    DXGI_FORMAT_R16G16B16A16_SNORM,
	    DXGI_FORMAT_R16G16B16A16_SINT,
	    DXGI_FORMAT_R32G32_TYPELESS,
	    DXGI_FORMAT_R32G32_FLOAT(TextureType.FLOAT, TextureFormat.LUMINANCE_ALPHA),
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
	    DXGI_FORMAT_R8G8B8A8_UNORM(TextureType.PACKED_INT_8888, TextureFormat.RGBA),
	    DXGI_FORMAT_R8G8B8A8_UNORM_SRGB(TextureType.PACKED_INT_8888, TextureFormat.RGBA),
	    DXGI_FORMAT_R8G8B8A8_UINT,
	    DXGI_FORMAT_R8G8B8A8_SNORM,
	    DXGI_FORMAT_R8G8B8A8_SINT,
	    DXGI_FORMAT_R16G16_TYPELESS,
	    DXGI_FORMAT_R16G16_FLOAT,
	    DXGI_FORMAT_R16G16_UNORM(TextureType.UNSIGNED_SHORT, TextureFormat.LUMINANCE_ALPHA),
	    DXGI_FORMAT_R16G16_UINT,
	    DXGI_FORMAT_R16G16_SNORM,
	    DXGI_FORMAT_R16G16_SINT,
	    DXGI_FORMAT_R32_TYPELESS,
	    DXGI_FORMAT_D32_FLOAT(TextureType.FLOAT, TextureFormat.DEPTH),
	    DXGI_FORMAT_R32_FLOAT(TextureType.FLOAT, TextureFormat.LUMINANCE),
	    DXGI_FORMAT_R32_UINT,
	    DXGI_FORMAT_R32_SINT,
	    DXGI_FORMAT_R24G8_TYPELESS,
	    DXGI_FORMAT_D24_UNORM_S8_UINT,
	    DXGI_FORMAT_R24_UNORM_X8_TYPELESS,
	    DXGI_FORMAT_X24_TYPELESS_G8_UINT,
	    DXGI_FORMAT_R8G8_TYPELESS,
	    DXGI_FORMAT_R8G8_UNORM(TextureType.UNSIGNED_BYTE, TextureFormat.LUMINANCE_ALPHA),
	    DXGI_FORMAT_R8G8_UINT,
	    DXGI_FORMAT_R8G8_SNORM,
	    DXGI_FORMAT_R8G8_SINT,
	    DXGI_FORMAT_R16_TYPELESS,
	    DXGI_FORMAT_R16_FLOAT,
	    DXGI_FORMAT_D16_UNORM(TextureType.UNSIGNED_SHORT, TextureFormat.DEPTH),
	    DXGI_FORMAT_R16_UNORM(TextureType.UNSIGNED_SHORT, TextureFormat.LUMINANCE),
	    DXGI_FORMAT_R16_UINT,
	    DXGI_FORMAT_R16_SNORM,
	    DXGI_FORMAT_R16_SINT,
	    DXGI_FORMAT_R8_TYPELESS,
	    DXGI_FORMAT_R8_UNORM(TextureType.UNSIGNED_BYTE, TextureFormat.LUMINANCE),
	    DXGI_FORMAT_R8_UINT,
	    DXGI_FORMAT_R8_SNORM,
	    DXGI_FORMAT_R8_SINT,
	    DXGI_FORMAT_A8_UNORM(TextureType.UNSIGNED_BYTE, TextureFormat.ALPHA),
	    DXGI_FORMAT_R1_UNORM,
	    DXGI_FORMAT_R9G9B9E5_SHAREDEXP,
	    DXGI_FORMAT_R8G8_B8G8_UNORM,
	    DXGI_FORMAT_G8R8_G8B8_UNORM,
	    DXGI_FORMAT_BC1_TYPELESS,
	    DXGI_FORMAT_BC1_UNORM,
	    DXGI_FORMAT_BC1_UNORM_SRGB,
	    DXGI_FORMAT_BC2_TYPELESS,
	    DXGI_FORMAT_BC2_UNORM,
	    DXGI_FORMAT_BC2_UNORM_SRGB,
	    DXGI_FORMAT_BC3_TYPELESS,
	    DXGI_FORMAT_BC3_UNORM,
	    DXGI_FORMAT_BC3_UNORM_SRGB,
	    DXGI_FORMAT_BC4_TYPELESS,
	    DXGI_FORMAT_BC4_UNORM,
	    DXGI_FORMAT_BC4_SNORM,
	    DXGI_FORMAT_BC5_TYPELESS,
	    DXGI_FORMAT_BC5_UNORM,
	    DXGI_FORMAT_BC5_SNORM,
	    DXGI_FORMAT_B5G6R5_UNORM(TextureType.PACKED_SHORT_565, TextureFormat.BGR),
	    DXGI_FORMAT_B5G5R5A1_UNORM(TextureType.PACKED_SHORT_5551, TextureFormat.BGRA),
	    DXGI_FORMAT_B8G8R8A8_UNORM(TextureType.PACKED_INT_8888, TextureFormat.BGRA),
	    DXGI_FORMAT_B8G8R8X8_UNORM;
	    
	    boolean supported;
	    TextureType fType;
	    TextureFormat fFormat;
		
	    private DXGIPixelFormat() {
	    	this(null, null);
	    	this.supported = false;
	    }
	    
		private DXGIPixelFormat(TextureType type, TextureFormat format) {
			this.supported = true;
			this.fFormat = format;
			this.fType = type;
		}
	}

	private static class DDPFMap {
		int bitCount;
		int rMask;
		int gMask;
		int bMask;
		int aMask;
		TextureFormat fFormat;
		TextureType fType;
		
		public DDPFMap(int bitCount, int rMask, int gMask, int bMask, int aMask, TextureFormat fFormat, TextureType fType) {
			this.bitCount = bitCount;
			this.rMask = rMask;
			this.gMask = gMask;
			this.bMask = bMask;
			this.aMask = aMask;
			this.fFormat = fFormat;
			this.fType = fType;
		}
		
		public boolean equals(DDSPixelFormat pf) {
			return this.equals(pf.rgbBitCount, pf.rBitMask, pf.gBitMask, pf.bBitMask, pf.rgbAlphaBitMask);
		}
		
		public boolean equals(int bitCount, int rMask, int gMask, int bMask, int aMask) {
			return (this.bitCount == bitCount &&
					this.rMask == rMask &&
					this.gMask == gMask &&
					this.bMask == bMask &&
					this.aMask == aMask);
		}
	}
	
	private static final DDPFMap[] pfRGB = new DDPFMap[] {
		new DDPFMap(24, 0xff0000, 0xff00, 0xff, 0, TextureFormat.BGR, TextureType.UNSIGNED_BYTE), //FIXME: do these two need to be swapped?
		new DDPFMap(24, 0xff, 0xff00, 0xff0000, 0, TextureFormat.RGB, TextureType.UNSIGNED_BYTE),
		new DDPFMap(16, 0xf800, 0x7e0, 0x1f, 0, TextureFormat.BGR, TextureType.PACKED_SHORT_565),
		new DDPFMap(16, 0x1f, 0x7e0, 0xf800, 0, TextureFormat.RGB, TextureType.PACKED_SHORT_565)
	};
	
	private static final DDPFMap[] pfRGBA = new DDPFMap[] {
		new DDPFMap(32, 0xff0000, 0xff00, 0xff, 0xff000000, TextureFormat.BGRA, TextureType.PACKED_INT_8888),
		new DDPFMap(32, 0xff000000, 0xff0000, 0xff00, 0x000000ff, TextureFormat.ABGR, TextureType.PACKED_INT_8888),
		new DDPFMap(32, 0xff, 0xff00, 0xff0000, 0xff000000, TextureFormat.RGBA, TextureType.PACKED_INT_8888),
		new DDPFMap(32, 0xff00, 0xff0000, 0xff000000, 0xff, TextureFormat.ARGB, TextureType.PACKED_INT_8888),
	};

	private static final DDPFMap[] pfL = new DDPFMap[] {
		new DDPFMap(8, 0xff, 0, 0, 0, TextureFormat.LUMINANCE, TextureType.UNSIGNED_BYTE)
	};
	
	private static final DDPFMap[] pfLA = new DDPFMap[] {
		new DDPFMap(16, 0xff, 0, 0, 0xff00, TextureFormat.LUMINANCE_ALPHA, TextureType.UNSIGNED_BYTE)
	};
	
	private static final DDPFMap[] pfA = new DDPFMap[] {
		new DDPFMap(8, 0, 0, 0, 0xff, TextureFormat.ALPHA, TextureType.UNSIGNED_BYTE)
	};
	
	// Selected bits in DDSHeader flags
	private static final int DDSD_CAPS            = 0x00000001; // Capacities are valid
	private static final int DDSD_HEIGHT          = 0x00000002; // Height is valid
	private static final int DDSD_WIDTH           = 0x00000004; // Width is valid
	private static final int DDSD_PITCH           = 0x00000008; // Pitch is valid
	private static final int DDSD_PIXELFORMAT     = 0x00001000; // ddpfPixelFormat is valid
	private static final int DDSD_MIPMAPCOUNT     = 0x00020000; // Mip map count is valid
	private static final int DDSD_LINEARSIZE      = 0x00080000; // dwLinearSize is valid
	private static final int DDSD_DEPTH           = 0x00800000; // dwDepth is valid

	// Selected bits in DDSPixelFormat flags
	private static final int DDPF_ALPHAPIXELS     = 0x00000001; // Alpha channel is present
	private static final int DDPF_ALPHA           = 0x00000002; // Only contains alpha information
	private static final int DDPF_LUMINANCE 	  = 0x00020000; // luminance data
	private static final int DDPF_FOURCC          = 0x00000004; // FourCC code is valid
	private static final int DDPF_RGB             = 0x00000040; // RGB data is present
	
	// Selected bits in DDS capabilities flags
	private static final int DDSCAPS_TEXTURE      = 0x00001000; // Can be used as a texture
	private static final int DDSCAPS_MIPMAP       = 0x00400000; // Is one level of a mip-map
	private static final int DDSCAPS_COMPLEX      = 0x00000008; // Complex surface structure, such as a cube map

	// Selected bits in DDS capabilities 2 flags
	private static final int DDSCAPS2_CUBEMAP           = 0x00000200;
	private static final int DDSCAPS2_CUBEMAP_POSITIVEX = 0x00000400;
	private static final int DDSCAPS2_CUBEMAP_NEGATIVEX = 0x00000800;
	private static final int DDSCAPS2_CUBEMAP_POSITIVEY = 0x00001000;
	private static final int DDSCAPS2_CUBEMAP_NEGATIVEY = 0x00002000;
	private static final int DDSCAPS2_CUBEMAP_POSITIVEZ = 0x00004000;
	private static final int DDSCAPS2_CUBEMAP_NEGATIVEZ = 0x00008000;
	private static final int DDSCAPS2_CUBEMAP_ALL_FACES = 0x0000fc00;
	private static final int DDSCAPS2_VOLUME 		   = 0x00200000;
	
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

	private DDSHeader header;
	private TextureType feroxType;
	private TextureFormat feroxFormat;
	private TextureTarget target;
	private int mipmapCount;
	private int width, height, depth;
	
	private DDSReader(DDSHeader header) throws IOException {
		this.header = header;
		this.identifyBuildParams();
		this.identifyTextureFormat();
	}
	
	private void identifyBuildParams() throws IOException {
		if (!isFlagSet(this.header.flags, DDSD_CAPS))
			throw new IOException("DDS header is missing required flag DDSD_CAPS");
		if (isFlagSet(this.header.flags, DDSD_WIDTH))
			this.width = this.header.width;
		else
			throw new IOException("DDS header is missing required flag DDSD_WIDTH");
		if (isFlagSet(this.header.flags, DDSD_HEIGHT))
			this.height = this.header.height;
		else
			throw new IOException("DDS header is missing required flag DDSD_HEIGHT");
		
		if (isFlagSet(this.header.flags, DDSD_MIPMAPCOUNT)) {
			this.mipmapCount = this.header.mipmapCount;
			if (this.mipmapCount > 1) {
				if (!isFlagSet(this.header.caps1, DDSCAPS_MIPMAP) || !isFlagSet(this.header.caps1, DDSCAPS_COMPLEX))
					throw new IOException("DDS surface capabilities are invalid for a mipmapped texture");
			}
			int expected = (int)(Math.log(width) / Math.log(2) + 1);
			if (this.mipmapCount != expected)
				throw new IOException("Expected " + expected + " but got " + this.mipmapCount + " mipmaps instead");
		} else
			this.mipmapCount = 1;
		
		if (!isFlagSet(this.header.caps1, DDSCAPS_TEXTURE))
			throw new IOException("DDS surface capabilities missing required flag DDSCAPS_TEXTURE");
		
		// We won't check for DDSCAPS_COMPLEX, since some files seem to ignore it
		if (isFlagSet(this.header.caps2, DDSCAPS2_VOLUME))
			this.target = TextureTarget.TEX3D;
		else if (isFlagSet(this.header.caps2, DDSCAPS2_CUBEMAP))
			this.target = TextureTarget.CUBEMAP;
		else 
			this.target = TextureTarget.TEX2D;
		
		if (this.header.headerDX10 != null) {
			if (this.target == TextureTarget.TEX2D) {
				if (this.header.headerDX10.resourceDimension != D3D10_RESOURCE_DIMENSION_TEXTURE2D)
					throw new IOException("DX10 header and surface caps are inconsistent");
				if (this.header.headerDX10.arraySize > 1)
					throw new IOException("Texture arrays aren't supported");
			} else if (this.target == TextureTarget.TEX3D) {
				if (this.header.headerDX10.resourceDimension != D3D10_RESOURCE_DIMENSION_TEXTURE3D)
					throw new IOException("DX10 header and surface caps are inconsistent");
				if (this.header.headerDX10.arraySize > 1)
					throw new IOException("Texture arrays aren't supported");
			} else if (this.target == TextureTarget.CUBEMAP) {
				if (this.header.headerDX10.resourceDimension == D3D10_RESOURCE_DIMENSION_TEXTURE2D) {
					// nvidia sets the dx10 header to be a 2d tex, with arraySize = 6 for cubemaps
					if (this.header.headerDX10.arraySize != 6)
						throw new IOException("Cube map must have 6 faces present");
				} else 
					throw new IOException("DX10 header and surface caps are inconsistent");
			}
		}
		
		this.depth = 1;
		if (this.target == TextureTarget.TEX3D) {
			if (isFlagSet(this.header.flags, DDSD_DEPTH))
				this.depth = this.header.depth;
			else
				throw new IOException("DDSD header is missing required flag DDSD_DEPTH for a volume texture");
		} else if (this.target == TextureTarget.CUBEMAP) {
			if (!isFlagSet(this.header.caps2, DDSCAPS2_CUBEMAP_ALL_FACES))
				throw new IOException("Cube map must have 6 faces present");
			if (this.width != this.height)
				throw new IOException("Cube map must have square faces");
		}
	}
	
	private void identifyTextureFormat() throws IOException {
		if (!isFlagSet(this.header.flags, DDSD_PIXELFORMAT))
			throw new IOException("DDSD header is missing required flag DDSD_PIXELFORMAT");
		if (this.header.headerDX10 != null) {
			if (!this.header.headerDX10.dxgiFormat.supported)
				throw new IOException("Unsupported dxgi pixel format");
			else {
				this.feroxFormat = this.header.headerDX10.dxgiFormat.fFormat;
				this.feroxType = this.header.headerDX10.dxgiFormat.fType;
			}
		} else {
			if (isFlagSet(this.header.pixelFormat.flags, DDPF_FOURCC)) {
				this.feroxType = TextureType.UNSIGNED_BYTE;
				
				if (this.header.pixelFormat.fourCC == FOURCC_DXT1) {
					if (isFlagSet(this.header.pixelFormat.flags, DDPF_ALPHAPIXELS))
						this.feroxFormat = TextureFormat.COMPRESSED_RGBA_DXT1;
					else
						this.feroxFormat = TextureFormat.COMPRESSED_RGB_DXT1;
				} else if (this.header.pixelFormat.fourCC == FOURCC_DXT3) 
					this.feroxFormat = TextureFormat.COMPRESSED_RGBA_DXT3;
				else if (this.header.pixelFormat.fourCC == FOURCC_DXT5) 
					this.feroxFormat = TextureFormat.COMPRESSED_RGBA_DXT5;
				else
					throw new IOException("Unrecognized fourCC value in pixel format");
				
			} else {
				DDPFMap[] supported = null;
				if (isFlagSet(this.header.pixelFormat.flags, DDPF_LUMINANCE)) {
					if (isFlagSet(this.header.pixelFormat.flags, DDPF_ALPHAPIXELS)) 
						supported = pfLA;
					else 
						supported = pfL;
				} else if (isFlagSet(this.header.pixelFormat.flags, DDPF_RGB)) {
					if (isFlagSet(this.header.pixelFormat.flags, DDPF_ALPHAPIXELS)) 
						supported = pfRGBA;
					else
						supported = pfRGB;	
				} else if (isFlagSet(this.header.pixelFormat.flags, DDPF_ALPHA)) {
					if (isFlagSet(this.header.pixelFormat.flags, DDPF_ALPHAPIXELS)) {
						supported = pfA;
					} else
						throw new IOException("Invalid pixel format");
				} else
					throw new IOException("Invalid pixel format");
				
				boolean found = false;
				for (int i = 0; i < supported.length; i++) {
					if (supported[i].equals(this.header.pixelFormat)) {
						this.feroxFormat = supported[i].fFormat;
						this.feroxType = supported[i].fType;
						found = true;
						break;
					}
				}

				if (!found)
					throw new IOException("Unsupported pixel format: " + this.header.pixelFormat);
			}
		}
	}
	
	private TextureData readTextureData(InputStream in, MinFilter min, MagFilter mag, boolean buildMips) throws IOException {
		int width, height, depth, size;
		
		int arrayCount = 1;
		if (this.target == TextureTarget.CUBEMAP)
			arrayCount = 6; // faces are ordered px, py, pz, nx, ny, nz
		Buffer[][] data = new Buffer[arrayCount][this.mipmapCount];
		byte[] image;
		for (int i = 0; i < arrayCount; i++) {
			width = this.width;
			height = this.height;
			depth = this.depth;
			for (int m = 0; m < this.mipmapCount; m++) {
				size = this.feroxFormat.getBufferSize(this.feroxType, width, height, depth);
				size *= this.feroxType.getByteSize();
				
				System.out.println(i + " " + m + " | " + size + " " + width + " " + height);
				image = new byte[size];
				IOUtil.readAll(in, image);
				data[i][m] = createBuffer(this.feroxType, image);
				
				width = Math.max(1, (width >> 1));
				height = Math.max(1, (height >> 1));
				depth = Math.max(1, (depth >> 1));
			}
		}
		
		int byteCount = 0;
		try {
			while(true) {
				IOUtil.readByte(in);
				byteCount++;
			}
		} catch (Exception e) {
			System.out.println("extra bytes: " + byteCount);
		}
		
		TextureData tex = null;
		
		if (buildMips) {
			for (int i = 0; i < data.length; i++) {
				if (data[i].length == 1)
					data[i] = TextureUtil.buildMipmaps3D(data[i][0], this.feroxFormat, this.feroxType, this.width, this.height, this.depth);
			}
		}
		
		if (this.target == TextureTarget.CUBEMAP) {
			tex = new TextureCubeMap(data[0], data[1], data[2], data[3], data[4], data[5], this.width, this.feroxType, this.feroxFormat, min, mag);
		} else if (this.target == TextureTarget.TEX3D)
			tex = new Texture3D(data[0], this.width, this.height, this.depth, this.feroxType, this.feroxFormat, min, mag);
		else 
			tex = new Texture2D(data[0], this.width, this.height, this.feroxType, this.feroxFormat, min, mag);
		
		return tex;
	}
	
	private static Buffer createBuffer(TextureType type, byte[] image) throws IOException {
		// DDS files are little endian.  A non-direct buffer is always big endian, so we always want to
		// swap bytes, since we're making non-direct texture buffers
		switch(type) {
		case PACKED_INT_8888: case UNSIGNED_INT: {
			IntBuffer data = BufferUtil.newIntBuffer(image.length / 4, true, false);
			for (int i = 0; i < data.capacity(); i++) 
				data.put(i, ByteOrder.swapInt(IOUtil.unconvertInt(image, (i << 2))));
			return data; }
		case PACKED_SHORT_4444: case PACKED_SHORT_5551: case PACKED_SHORT_565:
		case UNSIGNED_SHORT: {
			ShortBuffer data = BufferUtil.newShortBuffer(image.length / 2, true, false);
			for (int i = 0; i < data.capacity(); i++) 
				data.put(i, ByteOrder.swapShort(IOUtil.unconvertShort(image, (i << 1))));
			return data; }
		case FLOAT:	{
			FloatBuffer data = BufferUtil.newFloatBuffer(image.length / 4, true, false);
			for (int i = 0; i < data.capacity(); i++) 
				data.put(i, ByteOrder.swapFloat(IOUtil.unconvertFloat(image, (i << 2))));
			return data; }
		case UNSIGNED_BYTE: {
			ByteBuffer data = BufferUtil.newByteBuffer(image.length, true, false);
			data.put(image);
			return data; }
		}
		throw new IOException("Unsupported texture data type");
	}
	
	private static boolean isFlagSet(int flags, int flag) {
		return (flags & flag) == flag;
	}
	
    private static int makeFourCC(String c) {
    	if (c.length() != 4)
    		throw new IllegalArgumentException("Input string for a 4CC must have size of 4");
    	char[] cc = c.toCharArray();
    	return ((cc[3] & 0xff) << 24) | 
    		   ((cc[2] & 0xff) << 16) | 
    		   ((cc[1] & 0xff) << 8) | 
    		   ((cc[0] & 0xff) << 0);
    }
	
    private static String unmakeFourCC(int fourcc) {
    	char[] cc = new char[4];
    	cc[3] = (char)((fourcc & 0xff000000) >> 24);
    	cc[2] = (char)((fourcc & 0xff0000) >> 16);
    	cc[1] = (char)((fourcc & 0xff00) >> 8);
    	cc[0] = (char)((fourcc & 0xff) >> 0);
    	return new String(cc);
    }
    
    private static void validateHeader(DDSHeader h) throws IOException {
    	// Must have the magic number 'DDS '
    	// Size must be 124, although devIL reports that some files have 'DDS ' in the
    	// size var as well, so we'll support that.
    	if (h.magic != FOURCC_DDS || (h.size != 124 && h.size != FOURCC_DDS))
    		throw new IOException("DDS header is invalid");
    	if (h.pixelFormat.size != 32)
    		throw new IOException("DDS pixel format header is invalid");
    	
    	// Give me some valid assumptions
    	if (h.size == FOURCC_DDS)
    		h.size = 124;
    	if (h.mipmapCount == 0)
    		h.mipmapCount = 1;
    	if (h.depth == 0)
    		h.depth = 1;

    	// Header flags will be validated as the header is interpreted
    }
    
    private static DDSHeader loadHeader(InputStream in) throws IOException {
    	DDSHeader h = new DDSHeader();
    	
    	h.magic = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.size = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.flags = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.height = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.width = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.linearSize = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.depth = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.mipmapCount = ByteOrder.swapInt(IOUtil.readInt(in));
    	for (int i = 0; i < h.reserved1.length; i++)
    		h.reserved1[i] = ByteOrder.swapInt(IOUtil.readInt(in));
    	
    	h.pixelFormat = new DDSPixelFormat();
    	h.pixelFormat.size = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.pixelFormat.flags = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.pixelFormat.fourCC = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.pixelFormat.rgbBitCount = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.pixelFormat.rBitMask = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.pixelFormat.gBitMask = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.pixelFormat.bBitMask = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.pixelFormat.rgbAlphaBitMask = ByteOrder.swapInt(IOUtil.readInt(in));
    	
    	System.out.println(h.pixelFormat);
    	
    	h.caps1 = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.caps2 = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.caps3 = ByteOrder.swapInt(IOUtil.readInt(in));
    	h.caps4 = ByteOrder.swapInt(IOUtil.readInt(in));
    	
    	h.reserved2 = ByteOrder.swapInt(IOUtil.readInt(in));
    	
    	if (h.pixelFormat.fourCC == FOURCC_DX10) { // According to AMD, this is how we know if it's present
    		h.headerDX10 = new DDSHeader_DX10();
    		int dxgi = ByteOrder.swapInt(IOUtil.readInt(in));
    		h.headerDX10.dxgiFormat = (dxgi < 0 || dxgi >= DXGIPixelFormat.values().length ? DXGIPixelFormat.values()[0] : DXGIPixelFormat.values()[dxgi]);
    		h.headerDX10.resourceDimension = ByteOrder.swapInt(IOUtil.readInt(in));
    		h.headerDX10.miscFlag = ByteOrder.swapInt(IOUtil.readInt(in));
    		h.headerDX10.arraySize = ByteOrder.swapInt(IOUtil.readInt(in));
    		h.headerDX10.reserved = ByteOrder.swapInt(IOUtil.readInt(in));
    	} else 
    		h.headerDX10 = null;
    	
    	return h;
    }
    
	public static TextureData readDDSTexture(File in, MinFilter min, MagFilter mag, boolean buildMips) throws IOException {
		return readDDSTexture(new FileInputStream(in), min, mag, buildMips);
	}
	
	public static TextureData readDDSTexture(InputStream in, MinFilter min, MagFilter mag, boolean buildMips) throws IOException {
		DDSHeader h = loadHeader(in);
		validateHeader(h);
		DDSReader r = new DDSReader(h);
		return r.readTextureData(in, min, mag, buildMips);
	}
	
	public static TextureData readDDSTexture(String fileName, MinFilter min, MagFilter mag, boolean buildMips) throws IOException {
		return readDDSTexture(new File(fileName), min, mag, buildMips);
	}
}
