package com.ferox.renderer.impl.resource;

import java.nio.Buffer;

import com.ferox.renderer.Renderer.Comparison;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.Texture.Filter;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.Texture.WrapMode;

/**
 * TextureHandle is a concrete subclass of ResourceHandle that represents the
 * persisted state of a Texture, and is used by any
 * {@link AbstractTextureResourceDriver} when they manage Textures.
 * 
 * @author Michael Ludwig
 */
public class TextureHandle extends ResourceHandle {
	// formatting
	public final Target target;
	public final TextureFormat format;
	public final Class<? extends Buffer> type;

	// mipmap region
	public int baseMipmap;
	public int maxMipmap;

	// texture parameters
	public Filter filter;

	public WrapMode wrapS;
	public WrapMode wrapT;
	public WrapMode wrapR;

	public Comparison depthTest;
	public boolean enableDepthCompare;

	public float anisoLevel;
	
	public TextureHandle(int id, Target target, TextureFormat format, Class<? extends Buffer> type) {
		super(id);
		this.target = target;
		this.format = format;
		this.type = type;
		
		// blank parameters
		baseMipmap = -1;
		maxMipmap = -1;
		
		filter = null;
		wrapS = null;
		wrapT = null;
		wrapR = null;
		
		depthTest = null;
		enableDepthCompare = false;
		
		anisoLevel = -1f;
	}
}
