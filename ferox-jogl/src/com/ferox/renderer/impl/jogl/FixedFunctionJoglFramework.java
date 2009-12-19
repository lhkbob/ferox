package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Framework;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.FixedFunctionRendererImpl;

/**
 * FixedFunctionJoglFramework is a complete implementation of {@link Framework}
 * that uses the JOGL2 binding to OpenGL. Thus, it is required that JOGL2 be
 * correctly configured when using this Framework. The Renderers used by this
 * Framework are all implementations of {@link FixedFunctionRenderer} that use
 * the fixed-function OpenGL API that's exposed in versions < 3.1. Because of
 * this, a FixedFunctionJoglFramework is only usable on a computer that supports
 * the "legacy" versions of OpenGL. At the moment, this is still very common.
 * 
 * @author Michael Ludwig
 */
public class FixedFunctionJoglFramework extends JoglFramework {
	private static final int FORCE_BITS = RenderCapabilitiesDetector.FORCE_NO_GLSL;

	/**
	 * Create a new FixedFunctionJoglFramework where all invocations of
	 * {@link Framework#render()} are serialized onto a single internal Thread.
	 * 
	 * @throws GLException If a fixed-function implementation of OpenGL is
	 *             unavailable
	 */
	public FixedFunctionJoglFramework() {
		this(true);
	}

	/**
	 * <p>
	 * Create a new FixedFunctionJoglFramework that's rendering behavior is
	 * controlled by the boolean, <tt>serializeRenders</tt>. If
	 * <tt>serializeRenders</tt> is true then all invocations of
	 * {@link Framework#render()} are serialized onto a single internal Thread.
	 * If it is false, then all calls to render() will be executed on the Thread
	 * that it was called on.
	 * </p>
	 * <p>
	 * De-serializing the renders can lead to increased performance when
	 * rendering to multiple RenderSurfaces. A RenderSurface cannot be rendered
	 * into from multiple Threads at the same time, however.
	 * </p>
	 * 
	 * @param serializeRenders True if all renders should be done on a single,
	 *            internal thread
	 * @throws GLException If a fixed-function implementation of OpenGL is
	 *             unavailable
	 */
	public FixedFunctionJoglFramework(boolean serializeRenders) {
		super(GLProfile.get(GLProfile.GL2), FORCE_BITS, serializeRenders);
	}

	@Override
	protected Renderer createRenderer(JoglContext context) {
		JoglFixedFunctionRendererDelegate ffp = new JoglFixedFunctionRendererDelegate(context, this);
		JoglRendererDelegate core = new JoglRendererDelegate(context);
		return new FixedFunctionRendererImpl(core, ffp);
	}
}
