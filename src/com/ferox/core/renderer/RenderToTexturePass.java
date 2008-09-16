package com.ferox.core.renderer;

import com.ferox.core.scene.SpatialTree;
import com.ferox.core.scene.View;
import com.ferox.core.states.atoms.Texture2D;
import com.ferox.core.states.atoms.Texture3D;
import com.ferox.core.states.atoms.TextureCubeMap;
import com.ferox.core.states.atoms.TextureData;
import com.ferox.core.states.atoms.TextureCubeMap.Face;
import com.ferox.core.states.atoms.TextureData.TextureCompression;
import com.ferox.core.states.atoms.TextureData.TextureFormat;
import com.ferox.core.states.atoms.TextureData.TextureTarget;
import com.ferox.core.states.atoms.TextureData.TextureType;
import com.ferox.core.system.SystemCapabilities;

public class RenderToTexturePass extends RenderPass {
	private static int maxColorAttachments = -1;
	private static boolean maxColorSet = false;
	
	public static int getMaxColorAttachments() {
		if (!maxColorSet) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				maxColorAttachments = caps.getMaxColorAttachments();
				maxColorSet = true;
			}
		}
		
		return maxColorAttachments;
	}
	
	private static class Attachment {
		TextureData data;
		Face face;
		int slice;
	}
	
	private Attachment[] colorArray;
	private Attachment depthAttach;
	
	private boolean useStencilBuffer;
	
	public RenderToTexturePass() {
		this(null, null);
	}
	
	public RenderToTexturePass(SpatialTree scene, View view) {
		super(scene, view);
		
		this.colorArray = new Attachment[1];
		this.depthAttach = new Attachment();
		this.useStencilBuffer = false;
	}
	
	public boolean isStencilBufferUsed() {
		return this.useStencilBuffer;
	}
	
	public void useStencilBuffer(boolean stencil) {
		this.useStencilBuffer = stencil;
	}
	
	public TextureData getDepthBinding() {
		return this.depthAttach.data;
	}
	
	public Face getDepthBindingFace() {
		if (this.depthAttach.data != null && this.depthAttach.data.getTarget() == TextureTarget.CUBEMAP)
			return this.depthAttach.face;
		return null;
	}
	
	public int getDepthBindingSlice() {
		if (this.depthAttach.data != null && this.depthAttach.data.getTarget() == TextureTarget.TEX3D)
			return this.depthAttach.slice;
		return 0;
	}
	
	public void setDepthBinding(Texture2D data) {
		validateDepthFormat(data);
		setAttachment(data, this.depthAttach);
	}
	
	public void setDepthBinding(TextureCubeMap data, Face face) throws IllegalArgumentException {
		validateDepthFormat(data);
		setAttachment(data, face, this.depthAttach);
	}
	
	public void setDepthBinding(Texture3D data, int slice) throws IllegalArgumentException {
		validateDepthFormat(data);
		setAttachment(data, slice, this.depthAttach);
	}
	
	public TextureData getColorBinding(int drawBuffer) {
		Attachment a = null;
		if (drawBuffer >= 0 && drawBuffer < this.colorArray.length)
			a = this.colorArray[drawBuffer];
		
		if (a != null)
			return a.data;
		return null;
	}
	
	public Face getColorBindingFace(int drawBuffer) {
		Attachment a = null;
		if (drawBuffer >= 0 && drawBuffer < this.colorArray.length)
			a = this.colorArray[drawBuffer];
		
		if (a != null && a.data != null && a.data.getTarget() == TextureTarget.CUBEMAP)
			return a.face;
		return null;
	}
	
	public int getColorBindingSlice(int drawBuffer) {
		Attachment a = null;
		if (drawBuffer >= 0 && drawBuffer < this.colorArray.length)
			a = this.colorArray[drawBuffer];
		
		if (a != null && a.data != null && a.data.getTarget() == TextureTarget.TEX3D)
			return a.slice;
		return 0;
	}
	
	private static void validateColorFormat(TextureData data) throws IllegalArgumentException {
		if (data != null && data.getDataFormat() == TextureFormat.DEPTH)
			throw new IllegalArgumentException("Color attachment must have a non-depth format, not: " + data.getDataFormat());
		if (data != null && !(data.getDataFormat().getNumComponents() == 4 || data.getDataFormat().getNumComponents() == 3))
			throw new IllegalArgumentException("Color attachment must have a 3 or 4 component format");
		if (data != null && (data.getDataFormat().isClientCompressed() || data.getDataCompression() != TextureCompression.NONE))
			throw new IllegalArgumentException("Color attachment can't be compressed: " + data.getDataFormat() + " " + data.getDataCompression());
	}
	
	public void setColorBinding(Texture2D data, int drawBuffer) throws IllegalArgumentException {
		if (data == null || !this.isTexturePresent(data)) {
			validateColorFormat(data);
			this.ensureExistence(drawBuffer);
			setAttachment(data, this.colorArray[drawBuffer]);
		}
	}
	
	public void setColorBinding(TextureCubeMap data, Face face, int drawBuffer) throws IllegalArgumentException {
		if (data == null || !this.isTexturePresent(data)) {
			validateColorFormat(data);
			this.ensureExistence(drawBuffer);
			setAttachment(data, face, this.colorArray[drawBuffer]);
		}
	}
	
	public void setColorBinding(Texture3D data, int slice, int drawBuffer) throws IllegalArgumentException {
		if (data == null || !this.isTexturePresent(data)) {
			validateColorFormat(data);
			this.ensureExistence(drawBuffer);
			setAttachment(data, slice, this.colorArray[drawBuffer]);
		}
	}
	
	private boolean isTexturePresent(TextureData data) {
		if (data == null)
			return false;
		for (int i = 0; i < this.colorArray.length; i++) {
			if (this.colorArray[i] != null && this.colorArray[i].data == data)
				return true;
		}
		return false;
	}
	
	public int getWidth() {
		if (this.depthAttach.data != null) 
			return getWidth(this.depthAttach.data);
		for (int i = 0; i < this.colorArray.length; i++)
			if (this.colorArray[i] != null && this.colorArray[i].data != null)
				return getWidth(this.colorArray[i].data);
		return 0;
	}
	
	public int getHeight() {
		if (this.depthAttach.data != null) 
			return getHeight(this.depthAttach.data);
		for (int i = 0; i < this.colorArray.length; i++)
			if (this.colorArray[i] != null && this.colorArray[i].data != null)
				return getHeight(this.colorArray[i].data);
		return 0;
	}
	
	private static int getWidth(TextureData data) {
		if (data.getTarget() == TextureTarget.TEX2D)
			return ((Texture2D)data).getWidth();
		if (data.getTarget() == TextureTarget.TEX3D)
			return ((Texture3D)data).getWidth();
		if (data.getTarget() == TextureTarget.CUBEMAP)
			return ((TextureCubeMap)data).getSideLength();
		return 0;
	}
	
	private static int getHeight(TextureData data) {
		if (data.getTarget() == TextureTarget.TEX2D)
			return ((Texture2D)data).getHeight();
		if (data.getTarget() == TextureTarget.TEX3D)
			return ((Texture3D)data).getHeight();
		if (data.getTarget() == TextureTarget.CUBEMAP)
			return ((TextureCubeMap)data).getSideLength();
		return 0;
	}
	
	public boolean isValid() {
		if (super.isValid()) {
			try {
				validateSizes(this.getWidth(), this.getHeight());
				validateColorFormats();
				if (this.depthAttach.data != null)
					validateDepthFormat(this.depthAttach.data);
			} catch (Exception e) {
				System.err.println("WARNING: Invalid RTT pass, because \n" + e.getMessage());
				return false;
			}
			return true;
		}
		return false;
	}
	
	private void validateSizes(int width, int height) throws IllegalArgumentException {
		int w, h;
		if (this.depthAttach.data != null) {
			w = getWidth(this.depthAttach.data);
			h = getHeight(this.depthAttach.data);
			if (w != width || h != height)
				throw new IllegalArgumentException("Dimensions don't match, was " + w + " X " + h + ", needs to be " + width + " X " + height);
		}
		for (int i = 0; i < this.colorArray.length; i++) {
			if (this.colorArray[i] != null && this.colorArray[i].data != null) {
				w = getWidth(this.colorArray[i].data);
				h = getHeight(this.colorArray[i].data);
				if (w != width || h != height)
					throw new IllegalArgumentException("Dimensions don't match, was " + w + " X " + h + ", needs to be " + width + " X " + height);
			}
		}
	}
	
	private void validateColorFormats() throws IllegalArgumentException {
		TextureFormat f = null;
		TextureType t = null;
		TextureData d;
		for (int i = 0; i < this.colorArray.length; i++) {
			d = (this.colorArray[i] != null ? this.colorArray[i].data : null);
			if (d != null) {
				validateColorFormat(d);
				if (t != null || f != null) {
					if (d.getDataFormat() != f || d.getDataType() != t)
						throw new IllegalArgumentException("Texture format and types don't match, was " + d.getDataFormat() + " | " + d.getDataType() + ", needs to be " + f + " | " + t);
				} else {
					t = d.getDataType();
					f = d.getDataFormat();
				}
			}
		}
	}
	
	private static void validateDepthFormat(TextureData data) throws IllegalArgumentException {
		if (data != null && data.getDataFormat() != TextureFormat.DEPTH)
			throw new IllegalArgumentException("Depth attachment must have a depth format, not: " + data.getDataFormat());
	}
	
	private void ensureExistence(int drawBuffer) throws IllegalArgumentException {
		if (drawBuffer < 0 || (getMaxColorAttachments() >= 0 && drawBuffer >= getMaxColorAttachments()))
			throw new IllegalArgumentException("Illegal draw buffer: " + drawBuffer);
		if (drawBuffer >= this.colorArray.length) {
			Attachment[] temp = new Attachment[drawBuffer + 1];
			System.arraycopy(this.colorArray, 0, temp, 0, this.colorArray.length);
			this.colorArray = temp;
		}
		if (this.colorArray[drawBuffer] == null)
			this.colorArray[drawBuffer] = new Attachment();
	}
	
	private static void setAttachment(Texture2D data, Attachment attach) throws IllegalArgumentException {
		if (data != null && data.getNumMipmaps() > 1)
			throw new IllegalArgumentException("Texture can't be mipmapped");
		attach.data = data;
		attach.slice = 0;
		attach.face = null;
	}
	
	private static void setAttachment(TextureCubeMap data, Face face, Attachment attach) throws IllegalArgumentException {
		if (data != null && data.getNumMipmaps() > 1)
			throw new IllegalArgumentException("Texture can't be mipmapped");
		if (data == null)
			face = null;
		else if (data != null && face == null)
			throw new IllegalArgumentException("Face can't be null when passing a non-null cube map");
		
		attach.data = data;
		attach.slice = 0;
		attach.face = face;
	}
	
	private static void setAttachment(Texture3D data, int slice, Attachment attach) throws IllegalArgumentException {
		if (data != null && data.getNumMipmaps() > 1)
			throw new IllegalArgumentException("Texture can't be mipmapped");
		if (data == null)
			slice = 0;
		else if (data != null && (slice < 0 || slice >= data.getDepth()))
			throw new IllegalArgumentException("Slice must be in bounds for a non-null 3D texture");
		
		attach.data = data;
		attach.slice = slice;
		attach.face = null;
	}
}
