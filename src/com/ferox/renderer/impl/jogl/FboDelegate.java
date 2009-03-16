package com.ferox.renderer.impl.jogl;

import java.util.IdentityHashMap;
import java.util.Map.Entry;

import javax.media.opengl.GLAutoDrawable;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

/** Provides fbo support for JoglTextureSurface.  These should only
 * be instantiated when fbo support is available.
 * 
 * @author Michael Ludwig
 *
 */
public class FboDelegate extends TextureSurfaceDelegate {
	private JoglSurfaceFactory factory;
	private IdentityHashMap<GLAutoDrawable, JoglFbo> fbos;
	private boolean useDepthRB;
	
	/** A context must be current when this is created.  It will create the fbo
	 * and make sure that it is usable.  If it's not usable, it will mark itself as 
	 * a zombie and throw an exception (signaling the JoglTextureSurface to fallback to
	 * pbuffers).
	 */
	public FboDelegate(JoglSurfaceFactory factory, DisplayOptions options, 
					   TextureTarget colorTarget, TextureTarget depthTarget, int width, int height,
					   TextureImage[] colors, TextureImage depth, boolean useDepthRenderBuffer) {
		super(options, colorTarget, depthTarget, width, height, colors, depth);
		this.factory = factory;
		this.fbos = new IdentityHashMap<GLAutoDrawable, JoglFbo>();
		this.useDepthRB = useDepthRenderBuffer;
	}

	@Override
	public GLAutoDrawable getGLAutoDrawable() {
		return null;
	}
	
	@Override
	public JoglStateRecord getStateRecord() {
		return this.factory.getRecord();
	}

	@Override
	public void destroySurface() {
		for (Entry<GLAutoDrawable, JoglFbo> e: this.fbos.entrySet()) {
			this.factory.notifyFboZombie(e.getKey(), e.getValue());
		}
	}

	@Override
	public void preRenderAction(int layer) {
		GLAutoDrawable current = this.factory.getDisplayingDrawable();
		JoglFbo fbo = this.fbos.get(current);
		if (fbo == null) {
			fbo = new JoglFbo(this.factory, this.getWidth(), this.getHeight(), this.getColorTarget(), this.getDepthTarget(),
							  this.getColorBuffers(), this.getDepthBuffer(), layer, this.useDepthRB);
			this.fbos.put(current, fbo);
		}
		fbo.bind(this.factory.getGL(), this.factory.getRecord(), layer);
	}

	@Override
	public void postRenderAction(JoglRenderSurface next) {
		if (next != null) {
			if (next instanceof JoglTextureSurface) {
				TextureSurfaceDelegate ts = ((JoglTextureSurface) next).getDelegate();
				if (ts instanceof FboDelegate)
					return; // preRenderAction() will take care of everything
			}
			GLAutoDrawable current = this.factory.getDisplayingDrawable();
			this.fbos.get(current).release(this.factory.getGL(), this.factory.getRecord());
		}
	}

	@Override
	public void init() {
		// do nothing
	}
}
