package com.ferox.renderer.impl.jogl;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.DisplayOptions.AntiAliasMode;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.DisplayOptions.StencilFormat;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.ResourceManager;
import com.ferox.resource.Texture1D;
import com.ferox.resource.Texture2D;
import com.ferox.resource.Texture3D;
import com.ferox.resource.TextureCubeMap;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureRectangle;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * JoglTextureSurface is a complete implementation of TextureSurface that works
 * with the JoglFramework. It has support for using framebuffer objects or
 * pbuffers to support render-to-texture capabilities. It automatically chooses
 * which to use based on the capabilities of the JoglFramework. They should not
 * be created directly, but instead should be created via
 * {@link JoglFramework#createTextureSurface(TextureSurface, int)} or
 * {@link JoglFramework#createTextureSurface(DisplayOptions, TextureTarget, int, int, int, int, int, boolean)}
 * 
 * @author Michael Ludwig
 */
public class JoglTextureSurface extends JoglRenderSurface implements TextureSurface {
	private final TextureSurfaceDelegate delegate;
	private final int layer;

	public JoglTextureSurface(JoglFramework framework, DisplayOptions options, 
							  TextureTarget target, int width, int height, int depth, 
							  int layer, int numColorTargets, boolean useDepthRenderBuffer) {
		super(framework);
		RenderCapabilities caps = framework.getCapabilities();
		SurfaceSpecifier s = new SurfaceSpecifier(options, target, width, height, depth, 
												  layer, numColorTargets, useDepthRenderBuffer);

		// validate and adjust s so it meets requirements
		verifySupport(s, caps);
		verifyTarget(s, caps);
		verifyDimensions(s, caps);
		detectOptions(s, caps);
		createColorTextures(s, caps);
		createDepthTexture(s, caps);

		// create texture images and delegates on gfx card
		ResourceManager res = framework.getResourceManager();
		updateTextures(s, res);
		
		try {
			if (s.useFbo) {
				delegate = new FboSurfaceDelegate(framework, s.options, s.colorTarget, s.depthTarget, 
												  s.width, s.height, s.colorBuffers, s.depthBuffer,
												  s.useDepthRenderBuffer);
			} else {
				TextureImage color = (s.colorBuffers == null ? null : s.colorBuffers[0]);
				delegate = new PbufferSurfaceDelegate(framework, s.options, this, s.colorTarget, 
													  s.depthTarget, s.width, s.height, color, s.depthBuffer, 
													  s.useDepthRenderBuffer);
			}
		} catch (RuntimeException e) {
			// clean-up the textures
			if (s.colorBuffers != null)
				for (int i = 0; i < s.colorBuffers.length; i++)
					res.scheduleDispose(s.colorBuffers[i]);
			if (s.depthBuffer != null)
				res.scheduleDispose(s.depthBuffer);

			throw new SurfaceCreationException("Exception occurred while creating a TextureSurfaceDelegate", e);
		}

		// if we've gotten here, we're okay
		this.layer = s.layer;
		delegate.addReference();
	}

	public JoglTextureSurface(JoglFramework framework, JoglTextureSurface shared, int layer) {
		super(framework);
		
		// validate the layer argument
		TextureTarget target = shared.delegate.getColorTarget();
		TextureImage color = shared.getColorBuffer(0);
		TextureImage depth = shared.getDepthBuffer();
		int d = (target == TextureTarget.T_3D ? (color == null ? depth.getDepth(0) : color.getDepth(0)) : 0);

		this.layer = validateLayer(target, d, layer);
		delegate = shared.delegate;
		delegate.addReference();
	}

	@Override
	public void init() {
		delegate.init();
	}

	@Override
	protected void preRender() {
		delegate.preRender(layer);
	}

	@Override
	protected void postRender(Action next) {
		delegate.postRender(next);
	}

	@Override
	public boolean destroy() {
		if (super.destroy()) {
			delegate.removeReference();
			if (delegate.getReferenceCount() <= 0)
				delegate.destroy();
			
			return true;
		} else 
			return false;
	}

	@Override
	public JoglContext getContext() {
		return delegate.getContext();
	}

	@Override
	public TextureImage getColorBuffer(int target) {
		return delegate.getColorBuffer(target);
	}

	@Override
	public TextureImage getDepthBuffer() {
		return delegate.getDepthBuffer();
	}

	@Override
	public int getLayer() {
		return layer;
	}

	@Override
	public int getNumColorTargets() {
		return delegate.getNumColorTargets();
	}

	@Override
	public TextureTarget getColorTarget() {
		return delegate.getColorTarget();
	}

	@Override
	public TextureTarget getDepthTarget() {
		return delegate.getDepthTarget();
	}

	@Override
	public DisplayOptions getDisplayOptions() {
		return delegate.getDisplayOptions();
	}

	@Override
	public int getHeight() {
		return delegate.getHeight();
	}

	@Override
	public int getWidth() {
		return delegate.getWidth();
	}
	
	public TextureSurfaceDelegate getDelegate() {
		return delegate;
	}

	private static void verifySupport(SurfaceSpecifier s, RenderCapabilities caps) {
		if (!caps.getFboSupport() && !caps.getPbufferSupport())
			throw new SurfaceCreationException("Hardware doesn't support pbuffers or fbos, cannot create TextureSurface");
	}

	private static void verifyTarget(SurfaceSpecifier s, RenderCapabilities caps) {
		if (s.colorTarget == null)
			throw new SurfaceCreationException("Cannot specify a null target");
		if (s.colorTarget == TextureTarget.T_RECT && !caps.getRectangularTextureSupport())
			throw new SurfaceCreationException("Rectangular textures aren't supported, can't create a surface with target T_RECT");
	}

	private static void verifyDimensions(SurfaceSpecifier s, RenderCapabilities caps) {
		// validate the dimensions, based on the target
		switch (s.colorTarget) {
		case T_1D:
		case T_CUBEMAP:
			if (s.width <= 0)
				throw new SurfaceCreationException("Invalid dimension 'width': " + s.width + " for target: " + s.colorTarget);
			s.height = s.width;
			s.depth = 1;
			break;
		case T_RECT:
		case T_2D:
			if (s.width <= 0 || s.height <= 0)
				throw new SurfaceCreationException("Invalid dimensions: " + s.width + " x " + s.height + " for target: " + s.colorTarget);
			s.depth = 1;
			break;
		case T_3D:
			if (s.width <= 0 || s.height <= 0 || s.depth <= 0)
				throw new SurfaceCreationException("Invalid dimensions: " + s.width + " x " + s.height + " x " + s.depth + 
												   " for target: " + s.colorTarget);
			break;
		}

		int oldDepth = s.depth;
		// make sure dimensions are pot if needed
		if (!caps.getNpotTextureSupport())
			switch (s.colorTarget) {
			case T_3D:
				s.depth = ceilPot(s.depth);
			case T_2D:
				s.height = ceilPot(s.height);
			case T_1D:
			case T_CUBEMAP:
				s.width = ceilPot(s.width);
			}

		// clamp the dimensions
		int maxSide = 0;
		switch (s.colorTarget) {
		case T_1D:
		case T_2D:
			maxSide = caps.getMaxTextureSize();
			break;
		case T_RECT:
			maxSide = caps.getMaxTextureRectangleSize();
			break;
		case T_CUBEMAP:
			maxSide = caps.getMaxTextureCubeMapSize();
			break;
		case T_3D:
			maxSide = caps.getMaxTexture3DSize();
			break;
		}

		maxSide = Math.min(maxSide, caps.getMaxRenderbufferSize());
		s.width = Math.min(s.width, maxSide);
		s.height = Math.min(s.height, maxSide);
		s.depth = Math.min(s.depth, maxSide);

		if (s.depth != oldDepth && s.colorTarget == TextureTarget.T_3D)
			// adjust the layer so it's still valid
			s.layer = (int) ((float) s.depth / oldDepth);

		// validate the layer
		s.layer = validateLayer(s.colorTarget, s.depth, s.layer);
	}

	private static void detectOptions(SurfaceSpecifier s, RenderCapabilities caps) {
		PixelFormat pf = (s.options == null ? PixelFormat.RGB_24BIT : s.options.getPixelFormat());
		DepthFormat df = (s.options == null ? DepthFormat.DEPTH_24BIT : s.options.getDepthFormat());
		StencilFormat sf = (s.options == null ? StencilFormat.NONE : s.options.getStencilFormat());

		if (pf == PixelFormat.NONE && df == DepthFormat.NONE)
			throw new SurfaceCreationException("Cannot create a surface with a pf of NONE and a df of NONE");

		s.useFbo = caps.getFboSupport() && sf == StencilFormat.NONE;
		if (!s.useFbo && !caps.getPbufferSupport()) {
			// use an fbo anyway, but with no stencil buffer
			s.useFbo = true;
			sf = StencilFormat.NONE;
		}

		if ((s.colorTarget == TextureTarget.T_3D || s.colorTarget == TextureTarget.T_CUBEMAP) && df != DepthFormat.NONE)
			// 3d/cm textures don't support depth textures, so we need to use a
			// different target
			if (!caps.getNpotTextureSupport()) {
				// might have to use a TextureRectangle
				if (s.width != ceilPot(s.width) || s.height != ceilPot(s.height))
					s.depthTarget = TextureTarget.T_RECT;
				else
					s.depthTarget = TextureTarget.T_2D;
			} else
				s.depthTarget = TextureTarget.T_2D;

		if (!caps.getUnclampedFloatTextureSupport() && (pf == PixelFormat.RGB_FLOAT || pf == PixelFormat.RGBA_FLOAT))
			// can't use floating point pixel formats
			pf = (pf == PixelFormat.RGB_FLOAT ? PixelFormat.RGB_24BIT : PixelFormat.RGBA_32BIT);

		// clamp the number of color targets
		if (s.useFbo)
			s.numColorTargets = Math.max(1, Math.min(s.numColorTargets, caps.getMaxColorTargets()));
		else
			s.numColorTargets = 1;

		// make these reflect the options
		s.useDepthRenderBuffer = df == DepthFormat.NONE && s.useDepthRenderBuffer;
		s.numColorTargets = (pf == PixelFormat.NONE ? 0 : s.numColorTargets);
		s.options = new DisplayOptions(pf, df, sf, AntiAliasMode.NONE);
	}

	private static void createColorTextures(SurfaceSpecifier s, RenderCapabilities caps) {
		TextureFormat format = null;
		DataType type = null;
		switch (s.options.getPixelFormat()) {
		case RGB_16BIT:
			format = TextureFormat.RGB_565;
			type = DataType.UNSIGNED_SHORT;
			break;
		case RGB_24BIT:
			format = TextureFormat.RGB;
			type = DataType.UNSIGNED_BYTE;
			break;
		case RGBA_32BIT:
			format = TextureFormat.RGBA;
			type = DataType.UNSIGNED_BYTE;
			break;
		case RGB_FLOAT:
			format = TextureFormat.RGB_FLOAT;
			type = DataType.FLOAT;
			break;
		case RGBA_FLOAT:
			format = TextureFormat.RGBA_FLOAT;
			type = DataType.FLOAT;
			break;
		}

		if (s.numColorTargets >= 1) {
			TextureImage[] colorImages = new TextureImage[s.numColorTargets];
			TextureImage t = null;
			for (int i = 0; i < s.numColorTargets; i++) {
				switch (s.colorTarget) {
				case T_1D:
					t = new Texture1D(null, s.width, format, type);
					break;
				case T_2D:
					t = new Texture2D(null, s.width, s.height, format, type);
					break;
				case T_3D:
					t = new Texture3D(null, s.width, s.height, s.depth, format, type);
					break;
				case T_CUBEMAP:
					t = new TextureCubeMap(null, null, null, null, null, null, s.width, format, type);
					break;
				case T_RECT:
					t = new TextureRectangle(null, s.width, s.height, format, type);
					break;
				}
				colorImages[i] = t;
			}
			s.colorBuffers = colorImages;
		} else {
			s.colorBuffers = null;
			s.colorTarget = null;
		}
	}

	private static void createDepthTexture(SurfaceSpecifier s, RenderCapabilities caps) {
		if (s.options.getDepthFormat() != DepthFormat.NONE) {
			DataType type = (s.options.getDepthFormat() == DepthFormat.DEPTH_16BIT ? DataType.UNSIGNED_SHORT 
																				   : DataType.UNSIGNED_INT);
			switch (s.depthTarget) {
			case T_2D:
				s.depthBuffer = new Texture2D(null, s.width, s.height, TextureFormat.DEPTH, type);
				break;
			case T_RECT:
				s.depthBuffer = new TextureRectangle(null, s.width, s.height, TextureFormat.DEPTH, type);
				break;
			case T_1D:
				s.depthBuffer = new Texture1D(null, s.width, TextureFormat.DEPTH, type);
				break;
			}
		} else {
			s.depthBuffer = null;
			s.depthTarget = null;
		}
	}
	
	private static void updateTextures(SurfaceSpecifier s, ResourceManager res) {
		if (s.colorBuffers != null) {
			for (int i = 0; i < s.colorBuffers.length; i++) {
				if (res.getHandle(s.colorBuffers[i]).getStatus() == Status.ERROR) {
					// something went wrong, so we should fail
					for (int j = 0; j <= i; j++)
						res.scheduleDispose(s.colorBuffers[j]);
					throw new SurfaceCreationException("Requested options created an invalid color texture, " +
													   "cannot construct the TextureSurface: " + s.options);
				}
			}
		}
		if (s.depthBuffer != null) {
			if (res.getHandle(s.depthBuffer).getStatus() == Status.ERROR) {
				// fail like before
				if (s.colorBuffers != null) {
					for (int i = 0; i < s.colorBuffers.length; i++)
						res.scheduleDispose(s.colorBuffers[i]);
				}
				res.scheduleDispose(s.depthBuffer);
				throw new SurfaceCreationException("Requested options created an unsupported depth texture, " + 
					   							   "cannot construct the TextureSurface: " + s.options);
			}
		}
	}

	// Return smallest POT >= num
	private static int ceilPot(int num) {
		int pot = 1;
		while (pot < num)
			pot = pot << 1;
		return pot;
	}

	// Return the new layer to use, throw an exception if it's invalid.
	private static int validateLayer(TextureTarget target, int depth, int layer) {
		switch (target) {
		case T_1D:
		case T_2D:
		case T_RECT:
			return 0; // layer is meaningless
		case T_CUBEMAP:
			if (layer < 0 || layer > 5)
				throw new SurfaceCreationException("Invalid layer: " + layer + " for target: " + target);
			break;
		case T_3D:
			if (layer < 0 || layer > (depth - 1))
				throw new SurfaceCreationException("Invalid layer: " + layer + " for target: " + target + 
												   ", it must be witihin [0, " + (depth - 1) + "]");
		}
		return layer;
	}
	
	/*
	 * Internal class used to permute the requested surface parameters until it
	 * is valid.
	 */
	private static class SurfaceSpecifier {
		TextureTarget colorTarget;
		// depthTarget will match colorTarget, unless colorTarget is T_3D or
		// T_CUBEMAP
		TextureTarget depthTarget;
		int width;
		int height;
		int depth;
		int layer;

		TextureImage[] colorBuffers;
		int numColorTargets;

		TextureImage depthBuffer;
		boolean useDepthRenderBuffer;

		DisplayOptions options;
		boolean useFbo;

		public SurfaceSpecifier(DisplayOptions options, TextureTarget target, 
								int width, int height, int depth, int layer, 
								int numColorTargets, boolean useDepthRenderBuffer) {
			this.options = options;
			colorTarget = target;
			depthTarget = target;
			this.width = width;
			this.height = height;
			this.depth = depth;
			this.layer = layer;
			this.numColorTargets = numColorTargets;
			this.useDepthRenderBuffer = useDepthRenderBuffer;
		}
	}
}
