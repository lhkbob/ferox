package com.ferox.renderer.impl.jogl;

import java.util.IdentityHashMap;
import java.util.Map.Entry;

import javax.media.opengl.GLAutoDrawable;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * Provides fbo support for JoglTextureSurface. These should only be
 * instantiated when fbo support is available.
 * 
 * @author Michael Ludwig
 */
public class FboDelegate extends TextureSurfaceDelegate {
	private final JoglContextManager factory;
	private final IdentityHashMap<GLAutoDrawable, JoglFbo> fbos;
	private final boolean useDepthRB;

	/**
	 * A context must be current when this is created. It will create the fbo
	 * and make sure that it is usable. If it's not usable, it will mark itself
	 * as a zombie and throw an exception (signaling the JoglTextureSurface to
	 * fallback to pbuffers).
	 */
	public FboDelegate(JoglContextManager factory, DisplayOptions options, 
					   TextureTarget colorTarget, TextureTarget depthTarget, 
					   int width, int height, 
					   TextureImage[] colors, TextureImage depth, 
					   boolean useDepthRenderBuffer) {
		super(options, colorTarget, depthTarget, width, height, colors, depth);
		this.factory = factory;
		fbos = new IdentityHashMap<GLAutoDrawable, JoglFbo>();
		useDepthRB = useDepthRenderBuffer;
	}

	@Override
	public GLAutoDrawable getGLAutoDrawable() {
		return null;
	}

	@Override
	public JoglStateRecord getStateRecord() {
		return factory.getRecord();
	}

	@Override
	public void destroySurface() {
		for (Entry<GLAutoDrawable, JoglFbo> e : fbos.entrySet())
			factory.notifyFboZombie(e.getKey(), e.getValue());
	}

	@Override
	public void preRenderAction(int layer) {
		GLAutoDrawable current = factory.getDisplayingDrawable();
		JoglFbo fbo = fbos.get(current);
		if (fbo == null) {
			fbo = new JoglFbo(factory, getWidth(), getHeight(), 
							  getColorTarget(), getDepthTarget(), 
							  getColorBuffers(), getDepthBuffer(), 
							  layer, useDepthRB);
			fbos.put(current, fbo);
		}
		fbo.bind(factory.getGL(), factory.getRecord(), layer);
	}

	@Override
	public void postRenderAction(JoglRenderSurface next) {
		if (next != null) {
			if (next instanceof JoglTextureSurface) {
				TextureSurfaceDelegate ts = ((JoglTextureSurface) next).getDelegate();
				if (ts instanceof FboDelegate)
					return; // preRenderAction() will take care of everything
			}
			GLAutoDrawable current = factory.getDisplayingDrawable();
			fbos.get(current).release(factory.getGL(), factory.getRecord());
		}
	}

	@Override
	public void init() {
		// do nothing
	}
}
