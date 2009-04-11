package com.ferox.renderer.impl.jogl.drivers;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GL;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.record.PackUnpackRecord;
import com.ferox.resource.BufferData;
import com.ferox.resource.texture.TextureFormat;
import com.ferox.resource.texture.TextureImage;
import com.ferox.resource.texture.TextureImage.TextureDirtyDescriptor;

/** TextureImageDriver is a class that provides common functionality
 * and utilities that will be useful to any TextureImage driver (of any target type).
 * It is not a ResourceDriver itself, subclasses should implement this on their 
 * own and delegate to this class as needed.
 * 
 * It is not even necessary for image drivers to subclass this, they could instantiate
 * a driver internally.
 * 
 * @author Michael Ludwig
 *
 */
public class TextureImageDriver {
	// extensions present
	private boolean npotSupported; // whether or not npot's are supported - according to constructor factory
	private boolean floatSupported; // whether or not unclamped floating formats are okay
	
	// maximum allowed dimensions
	private int maxRectSize;
	private int maxCubeSize;
	private int max3dSize;
	private int maxTexSize;
	
	private float maxAnisoLevel;
	
	/** Construct the TextureImageDriver, that provides useful utilities when
	 * creating any type of texture image. 
	 * 
	 * The caps must not be null, and must reflect the capabilities of the rendering
	 * the instance will be used on. */
	public TextureImageDriver(RenderCapabilities caps) {
		this.npotSupported = caps.getNpotTextureSupport();
		this.floatSupported = caps.getUnclampedFloatTextureSupport();
		
		this.maxRectSize = caps.getMaxTextureRectangleSize();
		this.maxCubeSize = caps.getMaxTextureCubeMapSize();
		this.max3dSize = caps.getMaxTexture3DSize();
		this.maxTexSize = caps.getMaxTextureSize();
		
		this.maxAnisoLevel = caps.getMaxAnisotropicLevel();
	}
	
	/** Utility method to configure the unpack state of the given gl so
	 * that the next glTexImage or glTexSubImage will load data from
	 * the given sub-block of memory.  Here it is assumed that a buffer
	 * will be sent for the entire image level, and this will configure
	 * things to extract the given rectangle. This will also configure byte swapping 
	 * if the system's endian order needs it to get the expected color information across.
	 * 
	 * The block is configured as follows:
	 * blockWidth and blockHeight determine the dimensions of the entire 2d face.
	 * xyzOffsets are offsets into the 2d block (and select a 2d face for zOffset).
	 * The width/height and number of 2d faces read of the sub-block is determined by 
	 * the glTexImage/glTexSubImage() call.
	 * 
	 * It is recommended to always rely on this method, since then all
	 * unpack record modification is done in one place.
	 * 
	 * It is assumed that the given dimension are valid and will fit the
	 * next rectangle correctly. */
	public void setUnpackRegion(GL gl, PackUnpackRecord pr, int xOffset, int yOffset, int zOffset, int blockWidth, int blockHeight) {
		// skip pixels
		if (pr.unpackSkipPixels != xOffset) {
			pr.unpackSkipPixels = xOffset;
			gl.glPixelStorei(GL.GL_UNPACK_SKIP_PIXELS, xOffset);
		}
		// skip rows
		if (pr.unpackSkipRows != yOffset) {
			pr.unpackSkipRows = yOffset;
			gl.glPixelStorei(GL.GL_UNPACK_SKIP_ROWS, yOffset);
		}
		// skip images
		if (pr.unpackSkipImages != zOffset) {
			pr.unpackSkipImages = zOffset;
			gl.glPixelStorei(GL.GL_UNPACK_SKIP_IMAGES, zOffset);
		}
		
		// width of whole face
		if (pr.unpackRowLength != blockWidth) {
			pr.unpackRowLength = blockWidth;
			gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, blockWidth);
		}
		// height of whole face
		if (pr.unpackImageHeight != blockHeight) {
			pr.unpackImageHeight = blockHeight;
			gl.glPixelStorei(GL.GL_UNPACK_IMAGE_HEIGHT, blockHeight);
		}
		
		// configure the alignment
		if (pr.unpackAlignment != 1) {
			pr.unpackAlignment = 1;
			gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
		}
	}

	/** Convenience method to set all of the texture parameters on the given handle, based on 
	 * the texture image.  This method will force the parameters through if ti's dirty descriptor
	 * says so, or if forceAll is true.
	 * 
	 * It is assumed that the handle is the correct TextureHandle for the given image, and that
	 * the texture object of the handle is already bound to the given gl object. 
	 * 
	 * This does not clear the dirty descriptor, since it may be needed later on for other
	 * parts of updating.
	 * 
	 * It is assumed that all values are valid, and not null. */
	public void setTextureParameters(GL gl, TextureHandle handle, TextureImage ti, boolean forceAll) {
		TextureDirtyDescriptor tdd = ti.getDirtyDescriptor(); // won't be null
		
		handle.setFilter(gl, ti.getFilter(), tdd.isFilterDirty() || forceAll);
		
		handle.setWrapS(gl, ti.getWrapS(), tdd.isTextureWrapDirty() || forceAll);
		handle.setWrapT(gl, ti.getWrapT(), tdd.isTextureWrapDirty() || forceAll);
		handle.setWrapR(gl, ti.getWrapR(), tdd.isTextureWrapDirty() || forceAll);
		
		handle.setDepthTest(gl, ti.getDepthCompareTest(), tdd.isDepthCompareDirty() || forceAll);
		handle.setDepthMode(gl, ti.getDepthMode(), tdd.isDepthCompareDirty() || forceAll);
		handle.setDepthCompareEnabled(gl, ti.isDepthCompareEnabled(), tdd.isDepthCompareDirty() || forceAll);
		
		handle.setAnisotropicLevel(gl, ti.getAnisotropicFiltering(), this.maxAnisoLevel, tdd.isAnisotropicFilteringDirty() || forceAll);
	}
	
	/** This is a utility method to create a valid TextureHandle object.  It should
	 * only be called when the given gl is valid.  It assumes that ti is not null
	 * and the image should not have a resource status of ERROR.
	 * e.g. this should not be called if texture rectangles or s3tc compression
	 * aren't supported and the image depends on that support.
	 * 
	 * It can be correctly called with npot dimensions, or unclamped floating point
	 * formats.  These are recoverable errors.
	 * 
	 * The returned handle will have a valid gl enum values assigned to it, and its
	 * id value will be okay to use in glBindTexture() calls.  The dimensions
	 * will be powers-of-two if hardware can't support them (and it's not a T_RECT).
	 * The dimensions will also be clamped to the hardware maximums for each target type. */
	public TextureHandle createNewTexture(GL gl, TextureImage ti) {
		// determine the valid dimensions - based on hardware constraints and extensions
		int width = ti.getWidth(0);
		int height = ti.getHeight(0);
		int depth = ti.getDepth(0);
		
		if (!this.npotSupported) {
			// we don't need to scale T_RECT, though
			switch(ti.getTarget()) {
			case T_3D: 
				depth = ceilPot(depth);
			case T_2D: 
				height = ceilPot(height);
			case T_1D: case T_CUBEMAP:
				width = ceilPot(width);
			}
		}
		
		int minSize = 0;
		switch(ti.getTarget()) {
		case T_1D: case T_2D: minSize = this.maxTexSize; break;
		case T_3D: minSize = this.max3dSize; break;
		case T_CUBEMAP: minSize = this.maxCubeSize; break;
		case T_RECT: minSize = this.maxRectSize; break;
		}
		
		width = Math.min(width, minSize);
		height = Math.min(height, minSize);
		depth = Math.min(depth, minSize);
		
		// pick glTarget
		int glTarget = EnumUtil.getGLTextureTarget(ti.getTarget());
		
		// choose src/dst formats and type
		TextureFormat format = ti.getFormat();
		if (!this.floatSupported) {
			// our best bet is to just clamp the float values
			switch(format) {
			case ALPHA_FLOAT: format = TextureFormat.ALPHA;
			case LUMINANCE_ALPHA_FLOAT: format = TextureFormat.LUMINANCE_ALPHA;
			case LUMINANCE_FLOAT: format = TextureFormat.LUMINANCE;
			case RGB_FLOAT: format = TextureFormat.RGB;
			case RGBA_FLOAT: format = TextureFormat.RGBA;
			}
		}
		
		int glSrcFormat = EnumUtil.getGLSrcFormat(format);
		int glDstFormat = EnumUtil.getGLDstFormat(format, ti.getType());
		int glType = (format.isPackedFormat() ? EnumUtil.getGLPackedType(format) : EnumUtil.getGLType(ti.getType()));
		
		// generate the new texture's id
		int[] id = new int[1];
		gl.glGenTextures(1, id, 0);
		
		return new TextureHandle(id[0], glTarget, glType, glSrcFormat, glDstFormat, width, height, depth, ti.getNumMipmaps());
	}
	
	/** Determines if the handle is dirty - e.g. it's format and type differ
	 * or its dimensions were changed. */
	public boolean isDirty(TextureImage image, TextureHandle handle) {
		int expectedDst = EnumUtil.getGLDstFormat(image.getFormat(), image.getType());
		return image.getWidth(0) != handle.width || image.getHeight(0) != handle.height || image.getDepth(0) != handle.depth 
			   || expectedDst != handle.glDstFormat;
	}
	
	/** Return a string dirty message that explains why isDirty() would return true. */
	public String getDirtyStatusMessage(TextureImage image, TextureHandle handle) {
		int expectedDst = EnumUtil.getGLDstFormat(image.getFormat(), image.getType());
		String msg = "";
		if (expectedDst != handle.glDstFormat)
			msg += "TextureFormat was changed to meet hardware support";
		if (image.getWidth(0) != handle.width || image.getHeight(0) != handle.height || image.getDepth(0) != handle.depth)
			msg += "Texture dimensions scaled to power-of-two sizes";
		
		return msg;
	}
	
	/** Delete the texture object for the given texture handle.
	 * It is assumed that the handle is not null, and hasn't already
	 * been deleted. */
	public void destroyTexture(GL gl, TextureHandle handle) {
		gl.glDeleteTextures(1, new int[] {handle.id}, 0);
	}
	
	/** Wrap the given data's array in a Buffer of the appropriate type. 
	 * Assumes that the data and its array are not null. */
	public Buffer wrap(BufferData data) {
		Object array = data.getData();
		
		switch(data.getType()) {
		case BYTE: case UNSIGNED_BYTE: return ByteBuffer.wrap((byte[]) array);
		case INT: case UNSIGNED_INT: return IntBuffer.wrap((int[]) array);
		case SHORT: case UNSIGNED_SHORT: return ShortBuffer.wrap((short[]) array);
		case FLOAT: return FloatBuffer.wrap((float[]) array);
		}
		// shouldn't happen
		return null;
	}
	
	// Return smallest POT >= num
	private static int ceilPot(int num) {
		int pot = 1;
		while(pot < num)
			pot = pot << 1;
		return pot;
	}
}
