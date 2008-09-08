package com.ferox.core.states.atoms;

import java.nio.*;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.states.FragmentTest;
import com.ferox.core.states.NumericUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.states.manager.TextureManager;
import com.ferox.core.system.SystemCapabilities;
import com.ferox.core.util.BufferUtil;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public abstract class TextureData extends StateAtom {
	public static enum TextureTarget {
		TEX2D, TEX3D, CUBEMAP
	}
	
	public static enum TextureCompression {
		NONE(null),
		DXT1(new TextureFormat[] {TextureFormat.RGBA, TextureFormat.BGRA, TextureFormat.ARGB, TextureFormat.ABGR, TextureFormat.BGR, TextureFormat.RGB}), 
		DXT3(new TextureFormat[] {TextureFormat.RGBA, TextureFormat.BGRA, TextureFormat.ARGB, TextureFormat.ABGR}), 
		DXT5(new TextureFormat[] {TextureFormat.RGBA, TextureFormat.BGRA, TextureFormat.ARGB, TextureFormat.ABGR});
		
		private TextureFormat[] comp;
		private TextureCompression(TextureFormat[] compatible) {
			this.comp = compatible;
		}
		
		public boolean isCompatible(TextureFormat f) {
			if (this.comp == null)
				return true;
			for (int i = 0; i < this.comp.length; i++)
				if (this.comp[i] == f)
					return true;
			return false;
		}
	}
	
	public static enum TextureFormat {
		RGBA(4, false, true, new TextureType[] {TextureType.UNSIGNED_INT, TextureType.UNSIGNED_SHORT, TextureType.UNSIGNED_BYTE, TextureType.FLOAT, TextureType.UNCLAMPED_FLOAT, TextureType.PACKED_SHORT_5551, TextureType.PACKED_SHORT_4444, TextureType.PACKED_INT_8888}),
		BGRA(4, false, true, new TextureType[] {TextureType.UNSIGNED_INT, TextureType.UNSIGNED_SHORT, TextureType.UNSIGNED_BYTE, TextureType.FLOAT, TextureType.UNCLAMPED_FLOAT, TextureType.PACKED_SHORT_5551, TextureType.PACKED_SHORT_4444, TextureType.PACKED_INT_8888}),
		ARGB(4, false, true, new TextureType[] {TextureType.PACKED_SHORT_5551, TextureType.PACKED_SHORT_4444, TextureType.PACKED_INT_8888}),
		ABGR(4, false, true, new TextureType[] {TextureType.PACKED_SHORT_5551, TextureType.PACKED_SHORT_4444, TextureType.PACKED_INT_8888}),
		COMPRESSED_RGBA_DXT1(4, true, true, new TextureType[] {TextureType.UNSIGNED_BYTE}),
		COMPRESSED_RGBA_DXT3(4, true, true, new TextureType[] {TextureType.UNSIGNED_BYTE}),
		COMPRESSED_RGBA_DXT5(4, true, true, new TextureType[] {TextureType.UNSIGNED_BYTE}),
		COMPRESSED_RGB_DXT1(3, true, false, new TextureType[] {TextureType.UNSIGNED_BYTE}),
		RGB(3, false, false, new TextureType[] {TextureType.UNSIGNED_INT, TextureType.UNSIGNED_SHORT, TextureType.UNSIGNED_BYTE, TextureType.FLOAT, TextureType.UNCLAMPED_FLOAT, TextureType.PACKED_SHORT_565}),
		BGR(3, false, false, new TextureType[] {TextureType.UNSIGNED_INT, TextureType.UNSIGNED_SHORT, TextureType.UNSIGNED_BYTE, TextureType.FLOAT, TextureType.UNCLAMPED_FLOAT, TextureType.PACKED_SHORT_565}),
		LUMINANCE_ALPHA(2, false, true, new TextureType[] {TextureType.UNSIGNED_INT, TextureType.UNSIGNED_SHORT, TextureType.UNSIGNED_BYTE, TextureType.FLOAT, TextureType.UNCLAMPED_FLOAT, }),
		LUMINANCE(1, false, false, new TextureType[] {TextureType.UNSIGNED_INT, TextureType.UNSIGNED_SHORT, TextureType.UNSIGNED_BYTE, TextureType.FLOAT, TextureType.UNCLAMPED_FLOAT, }),
		ALPHA(1, false, true, new TextureType[] {TextureType.UNSIGNED_INT, TextureType.UNSIGNED_SHORT, TextureType.UNSIGNED_BYTE, TextureType.FLOAT, TextureType.UNCLAMPED_FLOAT, }),
		DEPTH(1, false, false, new TextureType[] {TextureType.UNSIGNED_INT, TextureType.UNSIGNED_SHORT, TextureType.UNSIGNED_BYTE, TextureType.FLOAT, TextureType.UNCLAMPED_FLOAT, });
		
		private int numC; private boolean cComp, alpha; private TextureType[] cTypes;
		private TextureFormat(int numC, boolean cComp, boolean alpha, TextureType[] cTypes) {
			this.numC = numC; this.cComp = cComp; this.alpha = alpha; this.cTypes = cTypes;
		}
		
		public int getNumComponents() {
			return this.numC;
		}
		public boolean isClientCompressed() {
			return this.cComp;
		}
		public boolean hasAlpha() {
			return this.alpha;
		}
		
		public boolean isValid(TextureType t, TextureCompression e) {
			return this.isTypeCompatible(t) && e.isCompatible(this);
		}
		
		public boolean isTypeCompatible(TextureType t) {
			for (int i = 0; i < this.cTypes.length; i++)
				if (this.cTypes[i].equals(t))
					return true;
			return false;
		}
		
		public int getBufferSize(TextureType t, int width, int height) {
			return this.getBufferSize(t, width, height, 1);
		}
		public int getBufferSize(TextureType t, int width, int height, int depth) {
			if (!this.isTypeCompatible(t))
				return 0;
			if (this.isClientCompressed())
				return (int) ((COMPRESSED_RGB_DXT1.equals(this) || COMPRESSED_RGBA_DXT1.equals(this) ? 8 : 16) * Math.ceil(width / 4f) * Math.ceil(height / 4f));
			return width * height * depth * this.getNumComponents() / t.getNumComponents();
		}
	}
	
	public static enum TextureType {
		UNSIGNED_INT(IntBuffer.class, 1, BufferUtil.BYTESIZE_INT, true),
		UNSIGNED_SHORT(ShortBuffer.class, 1, BufferUtil.BYTESIZE_SHORT, true),
		UNSIGNED_BYTE(ByteBuffer.class, 1, BufferUtil.BYTESIZE_BYTE, true),
		UNCLAMPED_FLOAT(FloatBuffer.class, 1, BufferUtil.BYTESIZE_FLOAT, true),
		FLOAT(FloatBuffer.class, 1, BufferUtil.BYTESIZE_FLOAT, true),
		PACKED_SHORT_565(ShortBuffer.class, 3, BufferUtil.BYTESIZE_SHORT, false),
		PACKED_SHORT_5551(ShortBuffer.class, 4, BufferUtil.BYTESIZE_SHORT, false),
		PACKED_SHORT_4444(ShortBuffer.class, 4, BufferUtil.BYTESIZE_SHORT, false),
		PACKED_INT_8888(IntBuffer.class, 4, BufferUtil.BYTESIZE_INT, false);
		
		private Class<? extends Buffer> buffer; private int numC; private int numB; private boolean unpacked;
		private TextureType(Class<? extends Buffer> buffer, int numC, int numB, boolean unpacked) {
			this.buffer = buffer; this.numC = numC; this.numB = numB; this.unpacked = unpacked;
		}
		
		public boolean isTypeUnpacked() {
			return this.unpacked;
		}
		public boolean isTypeValid(Buffer b) {
			if (b == null)
				return false;
			return this.buffer.isAssignableFrom(b.getClass());
		}
		public int getNumComponents() {
			return this.numC;
		}
		public int getByteSize() {
			return this.numB;
		}
	}
	
	public static enum TexClamp {
		CLAMP, REPEAT, MIRROR
	}
	
	public static enum MinFilter {
		NEAREST, LINEAR, NEAREST_MIP_NEAREST, LINEAR_MIP_NEAREST, NEAREST_MIP_LINEAR, LINEAR_MIP_LINEAR
	}
	
	public static enum MagFilter {
		NEAREST, LINEAR
	}
	
	public static enum DepthMode {
		ALPHA, INTENSITY, LUMINANCE
	}
	
	public static enum DepthCompare {
		NONE, TC_R_TO_DEPTH
	}
	
	private static boolean s3tcSupport = false;
	private static boolean s3tcSet = false;
	private static boolean npotSupport = false;
	private static boolean npotSet = false;
	private static boolean rectSupport = false;
	private static boolean rectSet = false;
	private static boolean fpSupport = false;
	private static boolean fpSet = false;
	private static float maxAniso = -1;
	private static boolean maxAnisoSet = false;
	
	private TextureType dataType;
	private TextureFormat dataFormat;
	private TextureCompression dataCompress;
	
	private TexClamp wrapR;
	private TexClamp wrapS;
	private TexClamp wrapT;
	
	private MinFilter minFilter;
	private MagFilter magFilter;
	
	private DepthMode depthMode;
	private DepthCompare compareMode;
	private FragmentTest compareFunction;
	
	private float aniso;
	
	public TextureData(TextureType dataType, TextureFormat dataFormat, MinFilter min, MagFilter mag) {
		this(dataType, dataFormat, TextureCompression.NONE, TexClamp.MIRROR, TexClamp.MIRROR, TexClamp.MIRROR, min, mag);
	}
	
	public TextureData(TextureType dataType, TextureFormat dataFormat, TexClamp clamp) {
		this(dataType, dataFormat, clamp, MinFilter.LINEAR, MagFilter.LINEAR);
	}
	
	public TextureData(TextureType dataType, TextureFormat dataFormat, TexClamp clamp, MinFilter min, MagFilter mag) {
		this(dataType, dataFormat, TextureCompression.NONE, clamp, clamp, clamp, min, mag);
	}
	
	public TextureData(TextureType dataType, TextureFormat dataFormat, TextureCompression comp, TexClamp clampS, TexClamp clampT, TexClamp clampR, MinFilter min, MagFilter mag) {
		super();
		this.setTextureFormat(dataFormat, dataType, comp);
		
		this.aniso = 0f;
		
		this.setTexClampSTR(clampS, clampR, clampT);
		
		this.setMinFilter(min);
		this.setMagFilter(mag);
		
		this.setDepthMode(DepthMode.LUMINANCE);
		this.setCompareMode(DepthCompare.NONE);
		this.setCompareFunction(FragmentTest.LEQUAL);
	}
	
	public abstract TextureTarget getTarget();
	public abstract boolean isMipmapped();
	
	@Override
	public Class<? extends StateAtom> getAtomType() {
		return TextureData.class;
	}
	
	@Override
	public boolean isValidUnit(StateUnit unit) {
		if (!(unit instanceof NumericUnit))
			return false;
		if (TextureManager.getMaxTextureUnits() >= 0 && 
			TextureManager.getMaxTextureUnits() <= unit.ordinal())
			return false;
		return true;
	}
	
	public float getAnisotropicFilterLevel() {
		return this.aniso;
	}
	
	public void setAnisotropicFilterLevel(float l) {
		this.aniso = Math.max(0f, Math.min(1f, l));
	}
	
	protected void setTextureFormat(TextureFormat format, TextureType type, TextureCompression comp) {
		this.dataType = type;
		this.dataFormat = format;
		this.dataCompress = comp;
	}
	
	public TextureType getDataType() {
		return this.dataType;
	}

	public TextureFormat getDataFormat() {
		return this.dataFormat;
	}
	
	public TextureCompression getDataCompression() {
		return this.dataCompress;
	}

	public TexClamp getTexClampS() {
		return this.wrapS;
	}

	public void setTexClampS(TexClamp s) {
		this.wrapS = s;
	}

	public TexClamp getTexClampT() {
		return this.wrapT;
	}

	public void setTexClampT(TexClamp t) {
		this.wrapT = t;
	}
	
	public TexClamp getTexClampR() {
		return this.wrapR;
	}

	public void setTexClampR(TexClamp r) {
		this.wrapR = r;
	}
	
	public void setTexClampSTR(TexClamp all) {
		this.setTexClampSTR(all, all, all);
	}
	
	public void setTexClampSTR(TexClamp s, TexClamp t, TexClamp r) {
		this.setTexClampS(s);
		this.setTexClampT(t);
		this.setTexClampR(r);
	}
	
	public MinFilter getMinFilter() {
		return this.minFilter;
	}

	public void setMinFilter(MinFilter minFilter) {
		this.minFilter = minFilter;
	}

	public void setFilters(MinFilter min, MagFilter mag) {
		this.setMinFilter(min);
		this.setMagFilter(mag);
	}
	
	public MagFilter getMagFilter() {
		return this.magFilter;
	}
	
	public void setMagFilter(MagFilter magFilter) {
		this.magFilter = magFilter;
	}

	public DepthMode getDepthMode() {
		return this.depthMode;
	}

	public void setDepthMode(DepthMode depthMode) {
		this.depthMode = depthMode;
	}

	public DepthCompare getCompareMode() {
		return this.compareMode;
	}

	public void setCompareMode(DepthCompare compareMode) {
		this.compareMode = compareMode;
	}

	public FragmentTest getCompareFunction() {
		return this.compareFunction;
	}

	public void setCompareFunction(FragmentTest compareFunction) {
		this.compareFunction = compareFunction;
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.dataFormat = in.getEnum("format", TextureFormat.class);
		this.dataType = in.getEnum("type", TextureType.class);
		this.dataCompress = in.getEnum("compress", TextureCompression.class);
		this.wrapR = in.getEnum("wrapR", TexClamp.class);
		this.wrapS = in.getEnum("wrapS", TexClamp.class);
		this.wrapT = in.getEnum("wrapT", TexClamp.class);
		this.minFilter = in.getEnum("min", MinFilter.class);
		this.magFilter = in.getEnum("mag", MagFilter.class);
		this.depthMode = in.getEnum("depthMode", DepthMode.class);;
		this.compareMode = in.getEnum("compMode", DepthCompare.class);;
		this.compareFunction = in.getEnum("compFunc", FragmentTest.class);;
		
		this.aniso = in.getFloat("aniso");
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
				
		out.setEnum("format", this.dataFormat);
		out.setEnum("type", this.dataType);
		out.setEnum("compress", this.dataCompress);
		out.setEnum("wrapR", this.wrapR);
		out.setEnum("wrapS", this.wrapS);
		out.setEnum("wrapT", this.wrapT);
		out.setEnum("min", this.minFilter);
		out.setEnum("mag", this.magFilter);
		out.setEnum("depthMode", this.depthMode);
		out.setEnum("compMode", this.compareMode);
		out.setEnum("compFunc", this.compareFunction);
	
		out.setFloat("aniso", this.aniso);
	}
	
	public static boolean isBufferValid(TextureType type, TextureFormat format, TextureCompression comp, int width, int height, Buffer data) {
		return isBufferValid(type, format, comp, width, height, 1, data);
	}
	
	public static boolean isBufferValid(TextureType type, TextureFormat format, TextureCompression comp, int width, int height, int depth, Buffer data) {
		if ((format.isClientCompressed() || comp != TextureCompression.NONE) && depth != 1)
			return false;
		if (width <= 0 || height <= 0 || depth <= 0)
			return false;
		if (format.isValid(type, comp) 
			&& format.getBufferSize(type, width, height, depth) == data.capacity()) {
			return type.isTypeValid(data);
		}
		return false;
	}
	
	public static boolean isServerCompressed(TextureFormat f, TextureCompression c) {
		return f.isClientCompressed() || c != TextureCompression.NONE;
	}
	
	public static boolean isPowerOfTwo(int num) {
		int i = 1;
		while (i < num) {
			i *= 2;
		}
		return i == num;
	}
	
	public static boolean isS3TCSupported() {
		if (!s3tcSet) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				s3tcSupport = caps.isS3TCSupported();
				s3tcSet = true;
			}	
		}
		return s3tcSupport;
	}
	
	public static boolean areNpotTexturesSupported() {
		if (!npotSet) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				npotSupport = caps.areNpotTexturesSupported();
				npotSet = true;
			}	
		}
		return npotSupport;
	}
	
	public static boolean areRectangularTexturesSupported() {
		if (!rectSet) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				rectSupport = caps.areRectangularTexturesSupported();
				rectSet = true;
			}	
		}
		return rectSupport;
	}
	
	public static boolean areUnclampedFloatTexturesSupported() {
		if (!fpSet) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				fpSupport = caps.areUnclampedFloatTexturesSupported();
				fpSet = true;
			}	
		}
		return fpSupport;
	}
	
	
	public static float getMaxAnisotropicFilterLevel() {
		if (!maxAnisoSet) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				maxAniso = caps.getMaxAnisotropicFilterLevel();
				maxAnisoSet = true;
			}
		}
		return maxAniso;
	}
}
