package com.ferox.renderer;

import com.ferox.resource.Resource;

/**
 * <p>
 * Tasks are used to run arbitrary set of operations on the internal threads of
 * a {@link Framework} with an active {@link HardwareAccessLayer}. The context
 * provides access to {@link Renderer renderers} and allows for manual updating
 * and disposing of {@link Resource resources}.
 * <p>
 * Tasks are executed using {@link Framework#queue(Task, String)}.
 * 
 * @see HardwareAccessLayer
 * @see Context
 * @author Michael Ludwig
 * @param <T>
 */
public interface Task<T> {
    /**
     * Perform operations of this task, using the provided HardwareAccessLayer.
     * The access layer should only be used within this method and should not be
     * stored for later use as the Framework has full control over when the
     * context is valid and on which threads it may be used.
     * 
     * @param access The access layer providing access to renderers and resource
     *            management
     * @return Some response that will be returned with the Future created when
     *         the task was queued
     */
    public T run(HardwareAccessLayer access);
}
