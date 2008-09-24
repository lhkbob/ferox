package com.ferox.impl.jsr231.peers;

import java.nio.Buffer;

import javax.media.opengl.GL;
import javax.media.opengl.TraceGL;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.states.*;
import com.ferox.core.states.StateAtom.StateRecord;
import com.ferox.core.states.atoms.*;
import com.ferox.core.states.atoms.TextureCubeMap.Face;
import com.ferox.core.states.atoms.TextureData.*;
import com.ferox.core.util.FeroxException;
import com.ferox.core.util.DataTransfer.Block;
import com.ferox.core.util.DataTransfer.Slice;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLTextureDataPeer extends SimplePeer<TextureData, TextureRecord> {
	public static interface TextureTransfer<T extends TextureData> {
		void updateData(TextureRecord t, T data, GL gl);
		void submitData(TextureRecord t, T data, GL gl);
		void setData(TextureRecord t, T data, Block region, Face face, int level, Buffer in, Slice slice, GL gl);
		void getData(TextureRecord t, T data, Face face, int level, Buffer out, Slice slice, GL gl);
		void copyData(TextureRecord t, T data, Block region, Face face, int level, int sx, int sy, GL gl);
		void validate(T data, GL gl);
	}
	
	private Texture2DTransfer t2d;
	private Texture3DTransfer t3d;
	private TextureCubeMapTransfer tcm;
	
	public JOGLTextureDataPeer(JOGLRenderContext context) {
		super(context);
		
		this.t2d = new Texture2DTransfer();
		this.t3d = new Texture3DTransfer();
		this.tcm = new TextureCubeMapTransfer();
	}
	
	private static int getGLWrapMode(TexClamp wrap) {
		switch(wrap) {
		case CLAMP:
			return GL.GL_CLAMP_TO_EDGE;
		case MIRROR:
			return GL.GL_MIRRORED_REPEAT;
		case REPEAT:
			return GL.GL_REPEAT;
		default:
			throw new FeroxException("Illegal wrap mode");
		}
	}
	
	private static int getGLCompareMode(DepthCompare c) {
		switch(c) {
		case TC_R_TO_DEPTH:
			return GL.GL_COMPARE_R_TO_TEXTURE;
		case NONE:
			return GL.GL_NONE;
		default:
			throw new FeroxException("Invalid compare mode");
		}
	}
	
	private static int getGLDepthMode(DepthMode depth) {
		switch(depth) {
		case ALPHA:
			return GL.GL_ALPHA;
		case INTENSITY:
			return GL.GL_INTENSITY;
		case LUMINANCE:
			return GL.GL_LUMINANCE;
		default:
			throw new FeroxException("Invalid depth mode");
		}
	}
	
	private static int getGLMinFilter(MinFilter filter, boolean hasMipmaps) {
		switch(filter) {
		case LINEAR:
			return GL.GL_LINEAR;
		case NEAREST:
			return GL.GL_NEAREST;
		case LINEAR_MIP_LINEAR:
			if (hasMipmaps)
				return GL.GL_LINEAR_MIPMAP_LINEAR;
			else
				return GL.GL_LINEAR;
		case LINEAR_MIP_NEAREST:
			if (hasMipmaps)
				return GL.GL_LINEAR_MIPMAP_NEAREST;
			else
				return GL.GL_LINEAR;
		case NEAREST_MIP_LINEAR:
			if (hasMipmaps)
				return GL.GL_NEAREST_MIPMAP_LINEAR;
			else
				return GL.GL_NEAREST;
		case NEAREST_MIP_NEAREST:
			if (hasMipmaps)
				return GL.GL_NEAREST_MIPMAP_NEAREST;
			else
				return GL.GL_NEAREST;
		default:
			throw new FeroxException("Invalid min filter");
		}
	}
	
	private static int getGLMagFilter(MagFilter filter) {
		switch(filter) {
		case LINEAR:
			return GL.GL_LINEAR;
		case NEAREST:
			return GL.GL_NEAREST;
		default:
			throw new FeroxException("Invalid mag filter");
		}
	}
	
	public void copyTextureData(TextureData data, Block region, Face face, int level, int sx, int sy) {
		TextureRecord r = (TextureRecord)data.getStateRecord(this.context.getRenderManager());
		if (data instanceof Texture2D) 
			this.t2d.copyData(r, (Texture2D)data, region, face, level, sx, sy, this.context.getGL());
		else if (data instanceof Texture3D)
			this.t3d.copyData(r, (Texture3D)data, region, face, level, sx, sy, this.context.getGL());
		else if (data instanceof TextureCubeMap)
			this.tcm.copyData(r, (TextureCubeMap)data, region, face, level, sx, sy, this.context.getGL());
	}

	public void getTextureData(TextureData data, Face face, int level, Buffer out, Slice slice) {
		TextureRecord r = (TextureRecord)data.getStateRecord(this.context.getRenderManager());
		if (data instanceof Texture2D) 
			this.t2d.getData(r, (Texture2D)data, face, level, out, slice, this.context.getGL());
		else if (data instanceof Texture3D)
			this.t3d.getData(r, (Texture3D)data, face, level, out, slice, this.context.getGL());
		else if (data instanceof TextureCubeMap)
			this.tcm.getData(r, (TextureCubeMap)data, face, level, out, slice, this.context.getGL());
	}

	public void readPixels(Buffer in, Slice slice, TextureType type, TextureFormat format, Block region) {
		TextureRecord temp = new TextureRecord();
		setGLSrcTypeEnums(temp, type, format);
		if (in != null) {
			int pos = in.position();
			int lim = in.limit();
			in.position(slice.getOffset());
			in.limit(slice.getOffset() + slice.getLength());
			
			this.context.getGL().glReadPixels(region.getXOffset(), region.getYOffset(), region.getWidth(), region.getHeight(), temp.srcFormat, temp.dataType, in);
			
			in.limit(lim);
			in.position(pos);
		} else
			this.context.getGL().glReadPixels(region.getXOffset(), region.getYOffset(), region.getWidth(), region.getHeight(), temp.srcFormat, temp.dataType, slice.getOffset() * type.getByteSize());
	}

	public void setTextureData(TextureData data, Block region, Face face, int level, Buffer in, Slice slice) {
		TextureRecord r = (TextureRecord)data.getStateRecord(this.context.getRenderManager());
		if (data instanceof Texture2D) 
			this.t2d.setData(r, (Texture2D)data, region, face, level, in, slice, this.context.getGL());
		else if (data instanceof Texture3D)
			this.t3d.setData(r, (Texture3D)data, region, face, level, in, slice, this.context.getGL());
		else if (data instanceof TextureCubeMap)
			this.tcm.setData(r, (TextureCubeMap)data, region, face, level, in, slice, new TraceGL(this.context.getGL(), System.out));
	}
		
	private int generateID(GL gl) {
		int[] i = new int[1];
		gl.glGenTextures(1, i, 0);
		return i[0];
	}
	
	private void bindTexture(GL gl, int target, int id) {
		gl.glBindTexture(target, id);
	}
	
	protected void applyState(TextureData prevA, TextureRecord prevR, TextureData nextA, TextureRecord t, GL gl_) {
		GL gl = this.context.getGL();
		this.bindTexture(gl, t.target, t.texID);
		
		if (t.texID == 0)
			return;
		
		// Make any changes to this texture objects parameters.
		if (t.minFilter != nextA.getMinFilter()) {
			gl.glTexParameteri(t.target, GL.GL_TEXTURE_MIN_FILTER, getGLMinFilter(nextA.getMinFilter(), nextA.isMipmapped()));
			t.minFilter = nextA.getMinFilter();
		}
		if (t.magFilter != nextA.getMagFilter()) {
			gl.glTexParameteri(t.target, GL.GL_TEXTURE_MAG_FILTER, getGLMagFilter(nextA.getMagFilter()));
			t.magFilter = nextA.getMagFilter();
		}
		if (t.depthMode != nextA.getDepthMode()) {
			gl.glTexParameteri(t.target, GL.GL_DEPTH_TEXTURE_MODE, getGLDepthMode(nextA.getDepthMode()));
			t.depthMode = nextA.getDepthMode();
		}
		if (t.compareMode != nextA.getCompareMode()) {
			gl.glTexParameteri(t.target, GL.GL_TEXTURE_COMPARE_MODE, getGLCompareMode(nextA.getCompareMode()));
			t.compareMode = nextA.getCompareMode();
		}
		if (t.compareFunc != nextA.getCompareFunction()) {
			gl.glTexParameteri(t.target, GL.GL_TEXTURE_COMPARE_FUNC, JOGLRenderContext.getGLFragmentTest(nextA.getCompareFunction()));
			t.compareFunc = nextA.getCompareFunction();
		}
		if (t.wrapR != nextA.getTexClampR()) {
			gl.glTexParameteri(t.target, GL.GL_TEXTURE_WRAP_R, getGLWrapMode(nextA.getTexClampR()));
			t.wrapR = nextA.getTexClampR();
		}
		if (t.wrapS != nextA.getTexClampS()) {
			gl.glTexParameteri(t.target, GL.GL_TEXTURE_WRAP_S, getGLWrapMode(nextA.getTexClampS()));
			t.wrapS = nextA.getTexClampS();
		}
		if (t.wrapT != nextA.getTexClampT()) {
			gl.glTexParameteri(t.target, GL.GL_TEXTURE_WRAP_T, getGLWrapMode(nextA.getTexClampT()));
			t.wrapT = nextA.getTexClampT();
		}
		if (t.aniso != nextA.getAnisotropicFilterLevel()) {
			t.aniso = nextA.getAnisotropicFilterLevel();
			gl.glTexParameterf(t.target, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, t.aniso * TextureData.getMaxAnisotropicFilterLevel() + 1f);
		}
	}

	public void cleanupStateAtom(StateRecord record) {
		this.context.getGL().glDeleteTextures(1, new int[] {((TextureRecord)record).texID}, 0);
	}

	public StateRecord initializeStateAtom(StateAtom a) {
		TextureData atom = (TextureData)a;
		TextureRecord t = new TextureRecord();
		this.initAtom(atom, t);
		return t;
	}

	private void initAtom(TextureData atom, TextureRecord t) {
		GL gl = this.context.getGL();
		t.target = JOGLTexturePeer.getGLTarget(atom);
		
		t.texID = this.generateID(gl);
		t.compareFunc = null;
		t.compareMode = null;
		t.depthMode = null;
		t.magFilter = null;
		t.minFilter = null;
		t.wrapR = null;
		t.wrapS = null;
		t.wrapT = null;
		t.aniso = -1;
		
		setGLSrcTypeEnums(t, atom.getDataType(), atom.getDataFormat());
		setGLDstEnum(t, atom.getDataType(), atom.getDataFormat(), atom.getDataCompression());
		getTextureDimensions(t, atom);
		this.bindTexture(gl, t.target, t.texID);
		
		UnpackPixelStore p = UnpackPixelStore.get(gl);
		UnpackPixelStore.setUseful(gl);
		this.submitData(t, atom, gl);
		p.set(gl);
		
		int[] v = new int[1];
		if (t.target == GL.GL_TEXTURE_CUBE_MAP)
			gl.glGetTexLevelParameteriv(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, GL.GL_TEXTURE_INTERNAL_FORMAT, v, 0);
		else
			gl.glGetTexLevelParameteriv(t.target, 0, GL.GL_TEXTURE_INTERNAL_FORMAT, v, 0);
		if (t.dstFormat != v[0]) {
			System.err.println("WARNING: Dst texture format was changed from: " + t.dstFormat + " to " + v[0]);
			t.dstFormat = v[0];
		}
		
		this.bindTexture(gl, t.target, 0);
	}
	
	private static void setGLSrcTypeEnums(TextureRecord t, TextureType dataType, TextureFormat dataFormat) {
		switch(dataType) {
		case PACKED_INT_8888:
			if (dataFormat == TextureFormat.BGRA || dataFormat == TextureFormat.RGBA)
				t.dataType = GL.GL_UNSIGNED_INT_8_8_8_8_REV;
			else
				t.dataType = GL.GL_UNSIGNED_INT_8_8_8_8;
			break;
		case PACKED_SHORT_4444:
			if (dataFormat == TextureFormat.BGRA || dataFormat == TextureFormat.RGBA)
				t.dataType = GL.GL_UNSIGNED_SHORT_4_4_4_4_REV;
			else
				t.dataType = GL.GL_UNSIGNED_SHORT_4_4_4_4;
			break;
		case PACKED_SHORT_5551:
			if (dataFormat == TextureFormat.BGRA || dataFormat == TextureFormat.RGBA)
				t.dataType = GL.GL_UNSIGNED_SHORT_1_5_5_5_REV;
			else
				t.dataType = GL.GL_UNSIGNED_SHORT_5_5_5_1;
			break;
		case PACKED_SHORT_565:
			if (dataFormat == TextureFormat.RGB)
				t.dataType = GL.GL_UNSIGNED_SHORT_5_6_5_REV;
			else
				t.dataType = GL.GL_UNSIGNED_SHORT_5_6_5;
			break;
		case UNSIGNED_BYTE:
			t.dataType = GL.GL_UNSIGNED_BYTE;
			break;
		case FLOAT: case UNCLAMPED_FLOAT:
			t.dataType = GL.GL_FLOAT;
			break;
		case UNSIGNED_INT:
			t.dataType = GL.GL_UNSIGNED_INT;
			break;
		case UNSIGNED_SHORT:
			t.dataType = GL.GL_UNSIGNED_SHORT;
			break;
		}
		
		switch(dataFormat) {
		case BGR:
			if (dataType.isTypeUnpacked())
				t.srcFormat = GL.GL_BGR;
			else
				t.srcFormat = GL.GL_RGB;
			break;
		case BGRA: case ARGB:
			t.srcFormat = GL.GL_BGRA;
			break;
		case RGB:
			t.srcFormat = GL.GL_RGB;
			break;
		case RGBA: case ABGR:
			t.srcFormat = GL.GL_RGBA;
			break;
		case DEPTH:
			t.srcFormat = GL.GL_DEPTH_COMPONENT;
			break;
		case LUMINANCE:
			t.srcFormat = GL.GL_RED;
			break;
		case LUMINANCE_ALPHA:
			t.srcFormat = GL.GL_LUMINANCE_ALPHA;
			break;
		case ALPHA:
			t.srcFormat = GL.GL_ALPHA;
			break;
		case COMPRESSED_RGB_DXT1: case COMPRESSED_RGBA_DXT1:
		case COMPRESSED_RGBA_DXT3: case COMPRESSED_RGBA_DXT5:
			t.srcFormat = -1;
			break;
		}
	}
	
	private static void setGLDstEnum(TextureRecord t, TextureType dataType, TextureFormat dataFormat, TextureCompression dataComp) {
		if (dataFormat.isClientCompressed() || dataComp == TextureCompression.NONE || !RenderManager.getSystemCapabilities().isS3TCSupported()) {
			switch(dataFormat) {
			case COMPRESSED_RGB_DXT1:
				t.dstFormat = GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
				break;
			case COMPRESSED_RGBA_DXT1:
				t.dstFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
				break;
			case COMPRESSED_RGBA_DXT3:
				t.dstFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
				break;
			case COMPRESSED_RGBA_DXT5:
				t.dstFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
				break;
			case ALPHA:
				t.dstFormat = pickDstAlpha(dataType);
				break;
			case LUMINANCE:
				t.dstFormat = pickDstLum(dataType);
				break;
			case DEPTH:
				t.dstFormat = pickDstDepth(dataType);
				break;
			case LUMINANCE_ALPHA:
				t.dstFormat = pickDstLA(dataType);
				break;
			default:
				if (dataFormat.getNumComponents() == 4) {
					t.dstFormat = pickDstRGBA(dataType);
				} else if (dataFormat.getNumComponents() == 3) {
					t.dstFormat = pickDstRGB(dataType);
				}
				break;
			}
		} else {
			switch(dataComp) {
			case DXT1:
				if (dataFormat.getNumComponents() == 4)
					t.dstFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
				else 
					t.dstFormat = GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
				break;
			case DXT3:
				t.dstFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
				break;
			case DXT5:
				t.dstFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
				break;
			}
		}
	}
	
	private static int pickDstRGB(TextureType type) {
		switch(type) {
		case PACKED_SHORT_565:
			return GL.GL_RGB8;
		case UNSIGNED_BYTE:
			return GL.GL_RGB8;
		case UNSIGNED_INT:
			return GL.GL_RGB16; 
		case FLOAT:
			return GL.GL_RGB;
		case UNCLAMPED_FLOAT:
			if (TextureData.areUnclampedFloatTexturesSupported())
				return GL.GL_RGB32F_ARB;
			else {
				System.err.println("WARNING: unclamped float textures are unsupported");
				return GL.GL_RGB;
			}
		case UNSIGNED_SHORT:
			return GL.GL_RGB16;
		default:
			return GL.GL_RGB;
		}
	}
	
	private static int pickDstRGBA(TextureType type) {
		switch(type) {
		case PACKED_INT_8888:
			return GL.GL_RGBA8;
		case PACKED_SHORT_4444:
			return GL.GL_RGBA4;
		case PACKED_SHORT_5551:
			return GL.GL_RGB5_A1;
		case UNSIGNED_BYTE:
			return GL.GL_RGBA8;
		case UNSIGNED_INT:
			return GL.GL_RGBA16;
		case FLOAT:
			return GL.GL_RGBA;
		case UNCLAMPED_FLOAT:
			if (TextureData.areUnclampedFloatTexturesSupported())
				return GL.GL_RGBA32F_ARB;
			else {
				System.err.println("WARNING: unclamped float textures are unsupported");
				return GL.GL_RGBA;
			}
		case UNSIGNED_SHORT:
			return GL.GL_RGBA16;
		default:
			return GL.GL_RGBA;
		}
	}
	
	private static int pickDstLA(TextureType type) {
		switch(type) {
		case UNSIGNED_BYTE:
			return GL.GL_LUMINANCE8_ALPHA8;
		case UNSIGNED_INT:
			return GL.GL_LUMINANCE16_ALPHA16;
		case FLOAT:
			return GL.GL_LUMINANCE_ALPHA;
		case UNCLAMPED_FLOAT:
			if (TextureData.areUnclampedFloatTexturesSupported())
				return GL.GL_LUMINANCE_ALPHA32F_ARB;
			else {
				System.err.println("WARNING: unclamped float textures are unsupported");
				return GL.GL_LUMINANCE_ALPHA;
			}
		case UNSIGNED_SHORT:
			return GL.GL_LUMINANCE16_ALPHA16;
		default:
			return GL.GL_LUMINANCE_ALPHA;
		}
	}
	
	private static int pickDstDepth(TextureType type) {
		switch(type) {
		case UNSIGNED_BYTE:
			return GL.GL_DEPTH_COMPONENT16;
		case UNSIGNED_INT:
			return GL.GL_DEPTH_COMPONENT32;
		case FLOAT: case UNCLAMPED_FLOAT:
			return GL.GL_DEPTH_COMPONENT32;
		case UNSIGNED_SHORT:
			return GL.GL_DEPTH_COMPONENT24;
		default:
			return GL.GL_DEPTH_COMPONENT;
		}
	}
	
	private static int pickDstLum(TextureType type) {
		switch(type) {
		case UNSIGNED_BYTE:
			return GL.GL_LUMINANCE8;
		case UNSIGNED_INT:
			return GL.GL_LUMINANCE16; 
		case FLOAT:
			return GL.GL_LUMINANCE;
		case UNCLAMPED_FLOAT:
			if (TextureData.areUnclampedFloatTexturesSupported())
				return GL.GL_LUMINANCE32F_ARB;
			else {
				System.err.println("WARNING: unclamped float textures are unsupported");
				return GL.GL_LUMINANCE;
			}
		case UNSIGNED_SHORT:
			return GL.GL_LUMINANCE16;
		default:
			return GL.GL_LUMINANCE;
		}
	}
	
	private static int pickDstAlpha(TextureType type) {
		switch(type) {
		case UNSIGNED_BYTE:
			return GL.GL_ALPHA8;
		case UNSIGNED_INT:
			return GL.GL_ALPHA16;
		case FLOAT:
			return GL.GL_ALPHA;
		case UNCLAMPED_FLOAT:
			if (TextureData.areUnclampedFloatTexturesSupported())
				return GL.GL_ALPHA32F_ARB;
			else {
				System.err.println("WARNING: unclamped float textures are unsupported");
				return GL.GL_ALPHA;
			}
		case UNSIGNED_SHORT:
			return GL.GL_ALPHA16;
		default:
			return GL.GL_ALPHA;
		}
	}

	private static void getTextureDimensions(TextureRecord r, TextureData data) {
		switch(data.getTarget()) {
		case TEX2D:
			Texture2D t2d = (Texture2D)data;
			r.width = t2d.getWidth();
			r.height = t2d.getHeight();
			r.depth = 1;
			break;
		case TEX3D:
			Texture3D t3d = (Texture3D)data;
			r.width = t3d.getWidth();
			r.height = t3d.getHeight();
			r.depth = t3d.getDepth();
			break;
		case CUBEMAP:
			TextureCubeMap tcm = (TextureCubeMap)data;
			r.width = tcm.getSideLength();
			r.height = tcm.getSideLength();
			r.depth = 1;
			break;
		}
	}
	
	protected void restoreState(TextureData cleanA, TextureRecord t, GL gl) {
		this.bindTexture(gl, t.target, 0);
	}

	public void setUnit(StateUnit unit) {
		JOGLTexturePeer peer = (JOGLTexturePeer)this.context.getStateAtomPeer(Texture.class);
		int prevUnit = peer.texUnit;
		int nextUnit = ((NumericUnit)unit).ordinal();
		if (prevUnit != nextUnit) {
			((JOGLRenderContext)context).getGL().glActiveTexture(GL.GL_TEXTURE0 + nextUnit);
			peer.texUnit = nextUnit;
		}
	}

	public void updateStateAtom(StateAtom a, StateRecord record) {
		TextureRecord t = (TextureRecord)record;
		TextureData atom = (TextureData)a;
		GL gl = this.context.getGL();
		TextureRecord temp = new TextureRecord();
		setGLSrcTypeEnums(temp, atom.getDataType(), atom.getDataFormat());
		setGLDstEnum(temp, atom.getDataType(), atom.getDataFormat(), atom.getDataCompression());
		getTextureDimensions(temp, atom);
		
		if (t.needsInit(temp)) {
			this.initAtom(atom, t);
		} else {
			UnpackPixelStore p = UnpackPixelStore.get(gl);
			UnpackPixelStore.setUseful(gl);
			
			this.bindTexture(gl, t.target, t.texID);
			this.updateData(t, (TextureData)atom, gl);
			this.bindTexture(gl, t.target, 0);

			p.set(gl);
		}
	}
	
	private void submitData(TextureRecord t, TextureData texture, GL gl) {
		switch(t.target) {
		case GL.GL_TEXTURE_2D: case GL.GL_TEXTURE_RECTANGLE_ARB:
			this.t2d.submitData(t, (Texture2D)texture, gl);
			break;
		case GL.GL_TEXTURE_CUBE_MAP:
			this.tcm.submitData(t, (TextureCubeMap)texture, gl);
			break;
		case GL.GL_TEXTURE_3D:
			this.t3d.submitData(t, (Texture3D)texture, gl);
			break;
		default:
			throw new FeroxException("Unsupported texture target");
		}
	}
	
	private void updateData(TextureRecord t, TextureData texture, GL gl) {
		switch(t.target) {
		case GL.GL_TEXTURE_2D: case GL.GL_TEXTURE_RECTANGLE_ARB:
			this.t2d.updateData(t, (Texture2D)texture, gl);
			break;
		case GL.GL_TEXTURE_CUBE_MAP:
			this.tcm.updateData(t, (TextureCubeMap)texture, gl);
			break;
		case GL.GL_TEXTURE_3D:
			this.t3d.updateData(t, (Texture3D)texture, gl);
			break;
		default:
			throw new FeroxException("Unsupported texture target");
		}
	}
	
	public void validateStateAtom(StateAtom atom) {
		TextureData t = (TextureData)atom;
		int target = JOGLTexturePeer.getGLTarget(t);
		if (target == GL.GL_TEXTURE_3D && !RenderManager.getSystemCapabilities().is3DTexturingSupported())
			throw new StateUpdateException(atom, "Can't create a 3D texture when 3D textures aren't supported");
		if (target == GL.GL_TEXTURE_CUBE_MAP && !RenderManager.getSystemCapabilities().isCubeMapTexturingSupported())
			throw new StateUpdateException(atom, "Can't create a cube map when cube maps aren't supported");
		if (target == GL.GL_TEXTURE_RECTANGLE_ARB && !RenderManager.getSystemCapabilities().areRectangularTexturesSupported())
			throw new StateUpdateException(atom, "Can't create a rectangular textures when they aren't supported");
		
		if ((t.getTarget() == TextureTarget.TEX2D || t.getTarget() == TextureTarget.CUBEMAP) 
			&& t.getDataFormat().isClientCompressed() && !RenderManager.getSystemCapabilities().areNpotTexturesSupported()) {
			int s1 = (t.getTarget() == TextureTarget.TEX2D ? ((Texture2D)t).getWidth() : ((TextureCubeMap)t).getSideLength());
			int s2 = (t.getTarget() == TextureTarget.TEX2D ? ((Texture2D)t).getHeight() : s1);
			if (potCeil(s1) != s1 || potCeil(s2) != s2)
				throw new StateUpdateException(atom, "NPOT textures aren't supported, can't rescale a client compressed texture to POT dimensions");
		}
		

		if (!RenderManager.getSystemCapabilities().isS3TCSupported() && t.getDataFormat().isClientCompressed())
			throw new StateUpdateException(atom, "Compressed textures aren't supported");
		
		switch(target) {
		case GL.GL_TEXTURE_2D: case GL.GL_TEXTURE_RECTANGLE_ARB:
			this.t2d.validate((Texture2D)t, this.context.getGL());
			break;
		case GL.GL_TEXTURE_CUBE_MAP:
			this.tcm.validate((TextureCubeMap)t, this.context.getGL());
			break;
		case GL.GL_TEXTURE_3D:
			this.t3d.validate((Texture3D)t, this.context.getGL());
			break;
		default:
			throw new StateUpdateException(atom, "Unsupported texture target");
		}
	}
	
	static int potCeil(int num) {
		int pot = 1;
		while (pot < num)
			pot *= 2;
		return pot;
	}
}
