package com.ferox.renderer.impl.jogl;

import java.util.IdentityHashMap;
import java.util.Map.Entry;

import com.ferox.renderer.DisplayOptions;
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
	private IdentityHashMap<JoglContext, JoglFbo> fbos;
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
		this.fbos = new IdentityHashMap<JoglContext, JoglFbo>();
		this.useDepthRB = useDepthRenderBuffer;
	}

	@Override
	public JoglContext getContext() {
		return null;
	}

	@Override
	public void onDestroySurface() {
		for (Entry<JoglContext, JoglFbo> e: this.fbos.entrySet()) {
			this.factory.notifyFboZombie(e.getKey(), e.getValue());
		}
	}

	@Override
	public void onMakeCurrent(int layer) {
		JoglContext current = this.factory.getCurrentContext();
		JoglFbo fbo = this.fbos.get(current);
		if (fbo == null) {
			fbo = new JoglFbo(this.factory, this.getWidth(), this.getHeight(), this.getColorTarget(), this.getDepthTarget(),
							  this.getColorBuffers(), this.getDepthBuffer(), layer, this.useDepthRB);
			this.fbos.put(current, fbo);
		}
		fbo.bind(current, layer);
	}

	@Override
	public void onRelease(JoglRenderSurface next) {
		if (next != null) {
			if (next instanceof JoglTextureSurface) {
				TextureSurfaceDelegate ts = ((JoglTextureSurface) next).getDelegate();
				if (ts instanceof FboDelegate)
					return; // onMakeCurrent() will take care of everything
			}
			JoglContext current = this.factory.getCurrentContext();
			this.fbos.get(current).release(current);
		}
	}

	@Override
	public void swapBuffers() {
		// do nothing
	}
}
