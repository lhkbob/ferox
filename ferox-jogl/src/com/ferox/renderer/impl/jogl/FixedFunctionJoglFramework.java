package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Framework;

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
    private static final int FORCE_BITS = JoglRenderCapabilities.FORCE_NO_GLSL;

    /**
     * Create a new FixedFunctionJoglFramework that uses two internal threads,
     * one that handles all rendering and one that handles all resource actions.
     * This enables resources to be processed in parallel to rendering. It is
     * capable of processing resources even when no surface has been created.
     * 
     * @throws GLException If a fixed-function implementation of OpenGL is
     *             unavailable
     */
    public FixedFunctionJoglFramework() {
        this(true);
    }

    /**
     * <p>
     * Create a new FixedFunctionJoglFramework that has its resource and
     * rendering behavior specified by <tt>forceNoBackgroundContext</tt>. If
     * this is true, then the Framework can only use OpenGL contexts created for
     * a Surface to perform the resource actions. It's possible to disconnect
     * resources using this mode, but it may be more stable.
     * </p>
     * <p>
     * When it is false, an internal context will be managed on a separate
     * thread from the renderer that shares its resources with the contexts of
     * created surfaces. This allows parallel resource updates and ensures that
     * the resources will not become disconnected if every surface is destroyed.
     * </p>
     * 
     * @param forceNoBackgroundContext True if no internal context should be
     *            used for resource processing
     * @throws GLException If a fixed-function implementation of OpenGL is
     *             unavailable
     */
    public FixedFunctionJoglFramework(boolean forceNoBackgroundContext) {
        super(GLProfile.get(GLProfile.GL2), FORCE_BITS, forceNoBackgroundContext);
    }

    @Override
    protected JoglFixedFunctionRenderer createRenderer() {
        return new JoglFixedFunctionRenderer(this);
    }
}
