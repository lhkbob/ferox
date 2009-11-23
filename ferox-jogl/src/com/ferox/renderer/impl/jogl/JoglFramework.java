package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLProfile;
import javax.media.opengl.Threading;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FullscreenSurface;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.WindowSurface;
import com.ferox.renderer.impl.MultithreadFramework;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

public abstract class JoglFramework extends MultithreadFramework {
	private final GLProfile glProfile;
	private final JoglContext shadowContext;
	
	private final JoglResourceManager resourceManager;

	private volatile int windowSurfaces;
	private volatile boolean fullscreenSurface;
	
	public JoglFramework(GLProfile profile) {
		if (Threading.isSingleThreaded())
			Threading.disableSingleThreading();
		
		RenderCapabilitiesDetector detector = new RenderCapabilitiesDetector();
		RenderCapabilities caps = detector.detect(profile);
		
		resourceManager = new JoglResourceManager(this, caps);
		init(resourceManager, new JoglRenderManager(this, true), caps);
		
		glProfile = profile;
		windowSurfaces = 0;
		fullscreenSurface = false;
		
		if (caps.getPbufferSupport())
			shadowContext = PbufferShadowContext.create(this, profile);
		else
			shadowContext = OnscreenShadowContext.create(this, profile);
	}

	@Override
	public FullscreenSurface createFullscreenSurface(DisplayOptions options, int width, int height) {
		try {
			getFrameworkLock().lock();
			ensureNotDestroyed();

			synchronized(getSurfaceLock()) {
				// must lock so that we don't have race conditions that would
				// create multiple fullscreen/window surface combinations
				if (windowSurfaces > 0)
					throw new SurfaceCreationException("Cannot create a FullscreenSurface when a WindowedSurface exists");
				if (fullscreenSurface)
					throw new SurfaceCreationException("Cannot create a new FullscreenSurface when a FullscreenSurface already exists");
				
				JoglFullscreenSurface s = new JoglFullscreenSurface(this, options, width, height);
				addNotify(s);
				
				fullscreenSurface = true;
				return s;
			}
		} finally {
			getFrameworkLock().unlock();
		}
	}

	@Override
	public TextureSurface createTextureSurface(DisplayOptions options, TextureTarget target, 
											   int width, int height, int depth, int layer, 
											   int numColorTargets, boolean useDepthRenderBuffer) {
		try {
			getFrameworkLock().lock();
			ensureNotDestroyed();

			synchronized(getSurfaceLock()) {
				JoglTextureSurface s = new JoglTextureSurface(this, options, target, width, height, 
														  	  depth, layer, numColorTargets, useDepthRenderBuffer);
				addNotify(s);
				lockTextures(s);
				return s;
			}
		} finally {
			getFrameworkLock().unlock();
		}
	}

	@Override
	public TextureSurface createTextureSurface(TextureSurface share, int layer) {
		try {
			getFrameworkLock().lock();
			ensureNotDestroyed();

			synchronized(getSurfaceLock()) {
				if (!(share instanceof JoglTextureSurface) || share.getFramework() != this)
					throw new SurfaceCreationException("Cannot create a TextureSurface that's shared with a TextureSurface from another Framework");
				if (share.isDestroyed())
					throw new SurfaceCreationException("Cannot create a TextureSurface that's shared with a destroyed TextureSurface");
				
				JoglTextureSurface s = new JoglTextureSurface(this, (JoglTextureSurface) share, layer);
				addNotify(s);
				lockTextures(s);
				return s;
			}
		} finally {
			getFrameworkLock().unlock();
		}
	}

	@Override
	public WindowSurface createWindowSurface(DisplayOptions options, int x, int y, 
											 int width, int height, boolean resizable, boolean undecorated) {
		try {
			getFrameworkLock().lock();
			ensureNotDestroyed();

			synchronized(getSurfaceLock()) {
				// must lock so that we don't have race conditions that would
				// create multiple fullscreen/window surface combinations
				if (fullscreenSurface)
					throw new SurfaceCreationException("Cannot create a WindowSurface when a FullscreenSurface exists");
				
				JoglWindowSurface s = new JoglWindowSurface(this, options, x, y, width, height, resizable, undecorated);
				addNotify(s);
				
				windowSurfaces++;
				return s;
			}
		} finally {
			getFrameworkLock().unlock();
		}
	}

	@Override
	protected void innerDestroy(RenderSurface surface) {
		if (!(surface instanceof JoglRenderSurface) || surface.getFramework() != this)
			throw new IllegalArgumentException("Cannot destroy a RenderSurface from another Framework");
		
		JoglRenderSurface rs = (JoglRenderSurface) surface;
		removeNotify(rs);
		
		JoglContext context = rs.getContext();
		if (context != null && !context.getLock().tryLock()) {
			Thread contextThread = JoglContext.getThread(context);
			contextThread.interrupt();
			context.getLock().lock();
		}
		
		synchronized(rs.getLock()) {
			rs.destroy();
		}
		
		if (context != null)
			context.getLock().unlock();
		
		// clean-up counters
		if (rs instanceof JoglWindowSurface)
			windowSurfaces--;
		else if (rs instanceof JoglFullscreenSurface)
			fullscreenSurface = false;
		else if (rs instanceof JoglTextureSurface)
			unlockTextures((JoglTextureSurface) rs);
	}
	
	@Override
	protected void innerDestroy() {
		shadowContext.getLock().lock();
		shadowContext.destroy();
		shadowContext.getLock().unlock();
	}
	
	protected abstract Renderer createRenderer(JoglContext context);
	
	public GLProfile getGLProfile() {
		return glProfile;
	}
	
	JoglResourceManager getResourceManager() {
		return resourceManager;
	}
	
	JoglContext getShadowContext() {
		return shadowContext;
	}

	private void unlockTextures(JoglTextureSurface surface) {
		for (int i = 0; i < surface.getNumColorTargets(); i++)
			resourceManager.unlock(surface.getColorBuffer(i));
		TextureImage depth = surface.getDepthBuffer();
		if (depth != null)
			resourceManager.unlock(depth);
	}
	
	private void lockTextures(JoglTextureSurface surface) {
		for (int i = 0; i < surface.getNumColorTargets(); i++)
			resourceManager.lock(surface.getColorBuffer(i));
		TextureImage depth = surface.getDepthBuffer();
		if (depth != null)
			resourceManager.lock(depth);
	}
}
