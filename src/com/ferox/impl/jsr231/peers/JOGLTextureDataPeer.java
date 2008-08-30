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
		GL gl = this.context.getGL();
		TextureRecord t = new TextureRecord();
		
		t.target = JOGLTexturePeer.getGLTarget(atom);
		if (t.target == GL.GL_TEXTURE_3D && !RenderManager.getSystemCapabilities().is3DTexturingSupported()) {
			t.texID = 0;
			throw new FeroxException("Can't create a 3D texture when 3D textures aren't supported");
		}
		if (t.target == GL.GL_TEXTURE_CUBE_MAP && !RenderManager.getSystemCapabilities().isCubeMapTexturingSupported()) {
			t.texID = 0;
			throw new FeroxException("Can't create a cube map when cube maps aren't supported");
		}
		if (t.target == GL.GL_TEXTURE_RECTANGLE_ARB && !RenderManager.getSystemCapabilities().areRectangularTexturesSupported()) {
			t.texID = 0;
			throw new FeroxException("Can't create a rectangular textures when they aren't supported");
		}
		
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
		setGLDstEnum(t, atom.getDataType(), atom.getDataFormat());
		
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
			if (t.srcFormat == -1)
				System.err.println("WARNING: Texture unable to be correctly compressed");	
		}
		
		this.bindTexture(gl, t.target, 0);
		
		return t;
	}

	private static void setGLSrcTypeEnums(TextureRecord t, TextureType dataType, TextureFormat dataFormat) {
		switch(dataType) {
		case PACKED_INT_8888:
			//FIXME: support server only compression better with types
			if (dataFormat == TextureFormat.BGRA || dataFormat == TextureFormat.RGBA
				|| dataFormat == TextureFormat.BGRA_DXT1 || dataFormat == TextureFormat.RGBA_DXT1
				|| dataFormat == TextureFormat.BGRA_DXT5)
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
		case FLOAT:
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
		case BGRA: case ARGB: case BGRA_DXT1: case BGRA_DXT5:
			t.srcFormat = GL.GL_BGRA;
			break;
		case RGB: case RGB_DXT1:
			t.srcFormat = GL.GL_RGB;
			break;
		case RGBA: case RGBA_DXT1:
		case RGBA_DXT3: case RGBA_DXT5:
		case ABGR:
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
			if (!RenderManager.getSystemCapabilities().isS3TCSupported())
				throw new FeroxException("Compressed textures aren't supported");
			t.srcFormat = -1;
			break;
		}
	}
	
	private static void setGLDstEnum(TextureRecord t, TextureType dataType, TextureFormat dataFormat) {
		switch(dataFormat) {
		case RGB_DXT1: case COMPRESSED_RGB_DXT1:
			if (!RenderManager.getSystemCapabilities().isS3TCSupported())
				throw new FeroxException("Compressed textures aren't supported");
			t.dstFormat = GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
			break;
		case RGBA_DXT1: case COMPRESSED_RGBA_DXT1: case BGRA_DXT1:
			if (!RenderManager.getSystemCapabilities().isS3TCSupported())
				throw new FeroxException("Compressed textures aren't supported");
			t.dstFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
			break;
		case RGBA_DXT3: case COMPRESSED_RGBA_DXT3:
			if (!RenderManager.getSystemCapabilities().isS3TCSupported())
				throw new FeroxException("Compressed textures aren't supported");
			t.dstFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
			break;
		case RGBA_DXT5: case COMPRESSED_RGBA_DXT5: case BGRA_DXT5:
			if (!RenderManager.getSystemCapabilities().isS3TCSupported())
				throw new FeroxException("Compressed textures aren't supported");
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
			} else
				throw new FeroxException("Unsupported texture format");
			break;
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
		case FLOAT:
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

	public void updateStateAtom(StateAtom atom, StateRecord record) {
		TextureRecord t = (TextureRecord)record;
		GL gl = this.context.getGL();
		
		if (t.texID == 0)
			return;
		
		UnpackPixelStore p = UnpackPixelStore.get(gl);
		UnpackPixelStore.setUseful(gl);
		this.updateData(t, (TextureData)atom, gl);
		p.set(gl);
	}
	
	public void submitData(TextureRecord t, TextureData texture, GL gl) {
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
	
	public void updateData(TextureRecord t, TextureData texture, GL gl) {
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
}
