package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLAutoDrawable;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.DisplayOptions.AntiAliasMode;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.DisplayOptions.StencilFormat;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
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

/** JoglTextureSurface provides the ability to render into either pbuffers or
 * fbos, depending on the support of the current hardware.  This functionality
 * is provided by a TextureSurfaceDelegate that is determined at construction time.
 * 
 * At the moment, the fbo delegate uses a JoglFbo for each context that it must render
 * on because (according to OpenGL 3.0), fbos aren't shared between contexts.  Because
 * of this, it is theoretically possible that a texture surface fails later on when
 * it is actually used.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglTextureSurface extends JoglRenderSurface implements TextureSurface {
	/* Internal class used to permute the requested surface parameters until it is valid. */
	private static class SurfaceSpecifier {
		TextureTarget colorTarget;
		// depthTarget will match colorTarget, unless colorTarget is T_3D or T_CUBEMAP
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
		
		public SurfaceSpecifier(DisplayOptions options, TextureTarget target, int width, int height, int depth, int layer, int numColorTargets, boolean useDepthRenderBuffer) {
			this.options = options;
			this.colorTarget = target;
			this.depthTarget = target;
			this.width = width;
			this.height = height;
			this.depth = depth;
			this.layer = layer;
			this.numColorTargets = numColorTargets;
			this.useDepthRenderBuffer = useDepthRenderBuffer;
		}
	}
	
	private TextureSurfaceDelegate delegate;
	private int layer;
	
	/** Creates a new JoglTextureSurface.  It expects as arguments the creating factory,
	 * and the arguments passed into the factory for its createSurface method.
	 * 
	 * This will validate the input parameters and choose between an fbo or pbuffer surface.
	 * It will throw a RenderException if it cannot create a valid surface. */
	protected JoglTextureSurface(JoglSurfaceFactory factory, int id, DisplayOptions options, TextureTarget target, 
								 int width, int height, int depth, int layer, int numColorTargets, 
								 boolean useDepthRenderBuffer) throws RenderException {
		super(factory, id);
		RenderCapabilities caps = factory.getRenderer().getCapabilities();
		SurfaceSpecifier s = new SurfaceSpecifier(options, target, width, height, depth, layer, numColorTargets, useDepthRenderBuffer);
		
		// validate and adjust s so it meets requirements
		verifySupport(s, caps);
		verifyTarget(s, caps);
		verifyDimensions(s, caps);
		detectOptions(s, caps);
		createColorTextures(s, caps);
		createDepthTexture(s, caps);
		
		// create texture images and delegates of gfx card
		factory.renderFrame(JoglSurfaceFactory.EMPTY_LIST, new UpdateTextureImagesAction(s, factory));
		factory.renderFrame(JoglSurfaceFactory.EMPTY_LIST, new ConstructDelegateAction(s, factory, this));
		
		// if we've gotten here, we're okay
		this.layer = s.layer;
		this.delegate.addReference();
	}
	
	/** Create a surface that shares the given texture surface's delegate.  This will
	 * throw an exception if the layer argument is invalid.  It assumes the given
	 * surface is valid, however. */
	protected JoglTextureSurface(JoglSurfaceFactory factory, int id, JoglTextureSurface shared, int layer) throws RenderException {
		super(factory, id);
		// validate the layer argument
		TextureTarget target = shared.delegate.getColorTarget();
		TextureImage color = shared.getColorBuffer(0);
		TextureImage depth = shared.getDepthBuffer();
		int d = (target == TextureTarget.T_3D ? (color == null ? depth.getDepth(0) : color.getDepth(0)) : 0);

		this.layer = validateLayer(target, d, layer);
		this.delegate = shared.delegate;
		this.delegate.addReference();
	}
	
	/** Return the delegate that is being used by the texture surface. */
	protected TextureSurfaceDelegate getDelegate() {
		return this.delegate;
	}
	
	@Override
	public JoglStateRecord getStateRecord() {
		return this.delegate.getStateRecord();
	}

	@Override
	public void init() {
		this.delegate.init();
	}
	
	@Override
	public void preRenderAction() {
		this.delegate.preRenderAction(this.layer);
	}
	
	@Override
	public void postRenderAction(JoglRenderSurface next) {
		this.delegate.postRenderAction(next);
	}
	
	@Override
	public void destroySurface() {
		this.delegate.removeReference();
		if (this.delegate.getReferenceCount() <= 0)
			this.delegate.destroySurface();
		super.destroySurface();
	}
	
	@Override
	public GLAutoDrawable getGLAutoDrawable() {
		return this.delegate.getGLAutoDrawable();
	}

	@Override
	public TextureImage getColorBuffer(int target) {
		return this.delegate.getColorBuffer(target);
	}

	@Override
	public TextureImage getDepthBuffer() {
		return this.delegate.getDepthBuffer();
	}

	@Override
	public int getLayer() {
		return this.layer;
	}

	@Override
	public int getNumColorTargets() {
		return this.delegate.getNumColorTargets();
	}

	@Override
	public TextureTarget getColorTarget() {
		return this.delegate.getColorTarget();
	}
	
	@Override
	public TextureTarget getDepthTarget() {
		return this.delegate.getDepthTarget();
	}

	@Override
	public DisplayOptions getDisplayOptions() {
		return this.delegate.getDisplayOptions();
	}

	@Override
	public int getHeight() {
		return this.delegate.getHeight();
	}

	@Override
	public int getWidth() {
		return this.delegate.getWidth();
	}
	
	private static void verifySupport(SurfaceSpecifier s, RenderCapabilities caps) throws RenderException {
		if (!caps.getFboSupport() && !caps.getPbufferSupport())
			throw new RenderException("Hardware doesn't support pbuffers or fbos, cannot create TextureSurface");
	}
	
	private static void verifyTarget(SurfaceSpecifier s, RenderCapabilities caps) throws RenderException {
		if (s.colorTarget == null)
			throw new RenderException("Cannot specify a null target");
		if (s.colorTarget == TextureTarget.T_RECT && !caps.getRectangularTextureSupport())
			throw new RenderException("Rectangular textures aren't supported, can't create a surface with target T_RECT");
	}

	private static void verifyDimensions(SurfaceSpecifier s, RenderCapabilities caps) throws RenderException {
		// validate the dimensions, based on the target
		switch(s.colorTarget) {
		case T_1D: case T_CUBEMAP:
			if (s.width <= 0)
				throw new RenderException("Invalid dimension 'width': " + s.width + " for target: " + s.colorTarget);
			s.height = s.width;
			s.depth = 1;
			break;
		case T_RECT: case T_2D:
			if (s.width <= 0 || s.height <= 0)
				throw new RenderException("Invalid dimensions: " + s.width + " x " + s.height + " for target: " + s.colorTarget);
			s.depth = 1;
			break;
		case T_3D:
			if (s.width <= 0 || s.height <= 0 || s.depth <= 0)
				throw new RenderException("Invalid dimensions: " + s.width + " x " + s.height + " x " + s.depth + " for target: " + s.colorTarget);
			break;
		}
		
		int oldDepth = s.depth;
		// make sure dimensions are pot if needed
		if (!caps.getNpotTextureSupport()) {
			switch(s.colorTarget) {
			case T_3D:
				s.depth = ceilPot(s.depth);
			case T_2D:
				s.height = ceilPot(s.height);
			case T_1D: case T_CUBEMAP: 
				s.width = ceilPot(s.width);
			}
		}
		
		// clamp the dimensions
		int minSide = caps.getMaxRenderbufferSize();
		switch(s.colorTarget) {
		case T_1D: case T_2D: minSide = Math.min(minSide, caps.getMaxTextureSize()); break;
		case T_RECT: minSide = Math.min(minSide, caps.getMaxTextureRectangleSize()); break;
		case T_CUBEMAP: minSide = Math.min(minSide, caps.getMaxTextureCubeMapSize()); break;
		case T_3D: minSide = Math.min(minSide, caps.getMaxTexture3DSize()); break;
		}
		
		s.width = Math.min(s.width, minSide);
		s.height = Math.min(s.height, minSide);
		s.depth = Math.min(s.depth, minSide);
		
		if (s.depth != oldDepth && s.colorTarget == TextureTarget.T_3D) {
			// adjust the layer so it's still valid
			s.layer = (int) ((float) s.depth / oldDepth);
		}
		
		// validate the layer
		s.layer = validateLayer(s.colorTarget, s.depth, s.layer);
	}

	private static void detectOptions(SurfaceSpecifier s, RenderCapabilities caps) throws RenderException {
		PixelFormat pf = (s.options == null ? PixelFormat.RGB_24BIT : s.options.getPixelFormat());
		DepthFormat df = (s.options == null ? DepthFormat.DEPTH_24BIT : s.options.getDepthFormat());
		StencilFormat sf = (s.options == null ? StencilFormat.NONE : s.options.getStencilFormat());
		
		if (pf == PixelFormat.NONE && df == DepthFormat.NONE)
			throw new RenderException("Cannot create a surface with a pf of NONE and a df of NONE");
		
		s.useFbo = caps.getFboSupport() && sf == StencilFormat.NONE;
		if (!s.useFbo && !caps.getPbufferSupport()) {
			// use an fbo anyway, but with no stencil buffer
			s.useFbo = true;
			sf = StencilFormat.NONE;
		}
		
		if ((s.colorTarget == TextureTarget.T_3D || s.colorTarget == TextureTarget.T_CUBEMAP) && df != DepthFormat.NONE) {
			// 3d/cm textures don't support depth textures, so we need to use a different target
			if (!caps.getNpotTextureSupport()) {
				// might have to use a TextureRectangle
				if (s.width != ceilPot(s.width) || s.height != ceilPot(s.height))
					s.depthTarget = TextureTarget.T_RECT;
				else
					s.depthTarget = TextureTarget.T_2D;
			} else
				s.depthTarget = TextureTarget.T_2D;
		}
		
		if (!caps.getUnclampedFloatTextureSupport() && (pf == PixelFormat.RGB_FLOAT || pf == PixelFormat.RGBA_FLOAT)) {
			// can't use floating point pixel formats
			pf = (pf == PixelFormat.RGB_FLOAT ? PixelFormat.RGB_24BIT : PixelFormat.RGBA_32BIT);
		}
		
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
		switch(s.options.getPixelFormat()) {
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
				switch(s.colorTarget) {
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
		} else
			s.colorBuffers = null;
	}

	private static void createDepthTexture(SurfaceSpecifier s, RenderCapabilities caps) {
		if (s.options.getDepthFormat() != DepthFormat.NONE) {
			DataType type = (s.options.getDepthFormat() == DepthFormat.DEPTH_16BIT ? DataType.UNSIGNED_SHORT : DataType.UNSIGNED_INT);
			switch(s.depthTarget) {
			case T_2D:
				s.depthBuffer = new Texture2D(null, s.width, s.height, TextureFormat.DEPTH, type); break;
			case T_RECT:
				s.depthBuffer = new TextureRectangle(null, s.width, s.height, TextureFormat.DEPTH, type); break;
			case T_1D:
				s.depthBuffer = new Texture1D(null, s.width, TextureFormat.DEPTH, type); break;
			}
		} else
			s.depthBuffer = null;
	}
	
	/* Runnable action to create the textures on the shared context. */
	private static class UpdateTextureImagesAction implements Runnable {
		private JoglSurfaceFactory factory;
		private SurfaceSpecifier s;
	
		private UpdateTextureImagesAction(SurfaceSpecifier s, JoglSurfaceFactory factory) {
			this.factory = factory;
			this.s = s;
		}
		
		public void run() throws RenderException {
			if (this.s.colorBuffers != null) {
				for (int i = 0; i < this.s.colorBuffers.length; i++) {
					if (this.factory.getRenderer().doUpdate(this.s.colorBuffers[i], true, this.factory) == Status.ERROR) {
						// something is wrong, so we should fail
						for (int j = 0; j <= i; j++)
							this.factory.getRenderer().doCleanUp(this.s.colorBuffers[j], this.factory); // clean-up the textures
						throw new RenderException("Requested options created an unsupported color texture, cannot construct the texture surface: " + this.s.options);
					}
				}
			}
			if (this.s.depthBuffer != null) {
				if (this.factory.getRenderer().doUpdate(this.s.depthBuffer, true, this.factory) == Status.ERROR) {
					// fail like before
					if (this.s.colorBuffers != null) {
						for (int i = 0; i < this.s.colorBuffers.length; i++)
							this.factory.getRenderer().doCleanUp(this.s.colorBuffers[i], this.factory);
					}
					this.factory.getRenderer().doCleanUp(this.s.depthBuffer, this.factory);
					throw new RenderException("Requested options created an unsupported depth texture, cannot construct the texture surface: " + this.s.options);
				}
			}
		}
	}
	
	/* Runnable to create the delegate.  Must be executed on the context so that it
	 * can cleanup the textures if something goes wrong. */
	private static class ConstructDelegateAction implements Runnable {
		private JoglSurfaceFactory factory;
		private SurfaceSpecifier s;
		private JoglTextureSurface surface;
		
		private ConstructDelegateAction(SurfaceSpecifier s, JoglSurfaceFactory factory, JoglTextureSurface surface) {
			this.factory = factory;
			this.s = s;
			this.surface = surface;
		}

		public void run() throws RenderException {
			if (this.s.useFbo) {
				this.surface.delegate = new FboDelegate(this.factory, this.s.options, this.s.colorTarget, this.s.depthTarget,
														this.s.width, this.s.height, this.s.colorBuffers, this.s.depthBuffer, 
														this.s.useDepthRenderBuffer);
			} else {
				try {
					TextureImage color = (this.s.colorBuffers == null ? null : this.s.colorBuffers[0]);
					this.surface.delegate = new PbufferDelegate(this.factory, this.s.options, this.surface,
																this.s.colorTarget, this.s.depthTarget, 
																this.s.width, this.s.height, color, this.s.depthBuffer, 
																this.s.useDepthRenderBuffer);
				} catch (RuntimeException e) {
					// clean-up the textures
					if (this.s.colorBuffers != null) {
						for (int i = 0; i < this.s.colorBuffers.length; i++)
							this.factory.getRenderer().doCleanUp(this.s.colorBuffers[i], this.factory);
					}
					if (this.s.depthBuffer != null)
						this.factory.getRenderer().doCleanUp(this.s.depthBuffer, this.factory);
					
					throw new RenderException("Exception occurred while creating a pbuffer", e);
				}
			}
		}
	}
	
	// Return smallest POT >= num
	private static int ceilPot(int num) {
		int pot = 1;
		while(pot < num)
			pot = pot << 1;
		return pot;
	}
	
	// Return the new layer to use, throw an exception if it's invalid.
	private static int validateLayer(TextureTarget target, int depth, int layer) {
		switch(target) {
		case T_1D: case T_2D: case T_RECT:
			return 0; // layer is meaningless
		case T_CUBEMAP:
			if (layer < 0 || layer > 5)
				throw new RenderException("Invalid layer: " + layer + " for target: " + target);
			break;
		case T_3D:
			if (layer < 0 || layer > (depth - 1))
				throw new RenderException("Invalid layer: " + layer + " for target: " + target + ", it must be witihin [0, " + (depth - 1) + "]");
		}
		return layer;
	}
}
