package com.ferox.renderer.impl;

import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Renderer;

/**
 * Context represents an OpenGL context. When a context is current, OpenGL calls
 * can be made on the calling Thread. It is left up to implementations to
 * specify this behavior and manage it properly. The only assertion that Context
 * makes is that it has a single {@link Renderer} that can be used for
 * RenderPasses.
 * 
 * @author Michael Ludwig
 */
public interface Context {
	/**
	 * Return the unique Renderer instance associated with this Context.
	 * 
	 * @return This Context's Renderer
	 */
	public Renderer getRenderer();

    /**
     * Invoke the given Runnable within a valid lock, and with its underlying
     * OpenGL context current. The lock should be released when this is
     * completed. If necessary this can invoke the Runnable on a different
     * Thread, and should block until it's completed.
     * 
     * @param run The Runnable to invoke
     */
	public void runWithLock(Runnable run);
	
	/**
     * @return A ThreadLocal FrameStatistics instance last assigned via
     *         {@link #setFrameStatistics(FrameStatistics)}
     */
	public FrameStatistics getFrameStatistics();

    /**
     * Assign <tt>stats</tt> to this Context's thread local current
     * FrameStatistics, for use by Renderer implementations to update geometry
     * counts. The RenderManager is responsible for actually measuring the
     * timing, however.
     * 
     * @param stats The new FrameStatistics, may be null
     */
	public void setFrameStatistics(FrameStatistics stats);
}
