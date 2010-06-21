package com.ferox.renderer.impl;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Framework;

/**
 * <p>
 * A RenderManager takes on the responsibility of rendering a set of Actions.
 * The AbstractFramework (or something else) computes the set of Actions
 * necessary to render a frame correctly, and then delegates responsibility to
 * the RenderManager.
 * </p>
 * <p>
 * The RenderManager must be thread safe. Implementations may choose to render
 * each frame on the calling thread, or serialize them onto an inner thread.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface RenderManager {
    /**
     * Destroy the RenderManager. Do nothing if the RenderManager has already
     * been destroyed. If there are any queued frames, or currently rendering
     * frames, they should all be canceled or interrupted so that the destroy()
     * operation can complete quickly.
     */
    public void destroy();

    /**
     * Complete the initialization of the RenderManager and begin any Threads
     * needed for correct functioning.
     * 
     * @param lock The lock controlling access to the Framework using this
     *            manager, the lock should be used to manage the work of any
     *            started Threads
     */
    public void initialize(ReentrantReadWriteLock lock);

    /**
     * Perform all of the actions present in the given List of Actions. Return a
     * Future that is tied to the render task and is suitable for returning from
     * a Framework's {@link Framework#render()} method. The RenderManager is
     * responsible for calling prepare() on each Action and discarding those
     * that don't return true. It also must respect the associated Surface
     * for each Action.
     * 
     * @param actions The actions to be performed
     * @return A Future tied to the render
     */
    public Future<FrameStatistics> render(List<Action> actions);
}
