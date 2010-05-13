package com.ferox.renderer.impl.jogl;

import java.util.IdentityHashMap;
import java.util.Map.Entry;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.impl.Action;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * FboSurfaceDelegate is a TextureSurfaceDelegate that uses
 * {@link FramebufferObject}'s to render to textures.  This should
 * not be created directly, but is created internally by a {@link JoglTextureSurface}.
 * 
 * @author Michael Ludwig
 */
public class FboSurfaceDelegate extends TextureSurfaceDelegate {
	private final JoglFramework framework;
	
	private final IdentityHashMap<JoglContext, FramebufferObject> fbos;
	private final boolean useDepthRB;

	public FboSurfaceDelegate(JoglFramework framework, DisplayOptions options, 
					   		  TextureTarget colorTarget, TextureTarget depthTarget, 
					   		  int width, int height, TextureImage[] colors, TextureImage depth, 
					   		  boolean useDepthRenderBuffer) {
		super(options, colorTarget, depthTarget, width, height, colors, depth);
		this.framework = framework;
		
		fbos = new IdentityHashMap<JoglContext, FramebufferObject>();
		useDepthRB = useDepthRenderBuffer;
	}

	@Override
	public JoglContext getContext() {
		return null;
	}

	@Override
	public void destroy() {
		for (Entry<JoglContext, FramebufferObject> e : fbos.entrySet())
			e.getKey().notifyFboZombie(e.getValue());
	}

	@Override
	public void preRender(int layer) {
		JoglContext current = JoglContext.getCurrent();
		FramebufferObject fbo = fbos.get(current);
		if (fbo == null) {
			fbo = new FramebufferObject(framework, getWidth(), getHeight(), 
										getColorTarget(), getDepthTarget(), 
										getColorBuffers(), getDepthBuffer(), 
										layer, useDepthRB);
			fbos.put(current, fbo);
		}
		fbo.bind(layer);
	}

	@Override
	public void postRender(Action next) {
		if (next != null) {
			RenderSurface s = next.getRenderSurface();
			if (s instanceof JoglTextureSurface) {
				TextureSurfaceDelegate ts = ((JoglTextureSurface) s).getDelegate();
				if (ts instanceof FboSurfaceDelegate)
					return; // preRenderAction() will take care of everything
			}
		}
		
		JoglContext current = JoglContext.getCurrent();
		FramebufferObject fbo = fbos.get(current);
		if (fbo != null)
			fbo.release(); // this can be null if preRender() failed to create the fbo
	}

	@Override
	public void init() {
		// do nothing
	}
}
