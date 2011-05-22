package com.ferox.renderer.impl;

import com.ferox.renderer.impl.ResourceManager.LockToken;
import com.ferox.resource.Resource;

/**
 * <p>
 * LockListener is a listener that is used by ResourceManager to notify code
 * that had previously locked a Resource of when the manager has to unlock or
 * relock a resource to prevent deadblocks.
 * </p>
 * <p>
 * When
 * {@link ResourceManager#lock(OpenGLContext, Resource, LockListener)
 * locking} a resource, the manager attempts to lock without blocking. If that
 * fails, it releases all currently held resource locks and calls
 * {@link #onForceUnlock(LockToken)} for each registered LockListener. It then
 * re-acquires all previously held locks in a deterministic order, blocking
 * until all are held. When a lock is re-acquired, it will invoke
 * {@link #onRelock(LockToken)}.
 * </p>
 * <p>
 * LockListeners are used primarily by Renderers that need to have longterm
 * locks that are tied to OpenGL state, such as a bound texture or array vbo.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <R> The Resource type processed by this listener
 */
public interface LockListener<R extends Resource> {
    /**
     * Called when the given token is relocked. This is called after the lock is
     * held. If false is returned, the token will automatically be unlocked
     * otherwise the lock remains held. An example of when false could be
     * returned is if the handle stored in the token has been disposed of and so
     * the resource is no longer usable.
     * 
     * @param token The token being relocked.
     * @return True if the token should remain locked
     */
    public boolean onRelock(LockToken<? extends R> token);

    /**
     * Called when the given token must be unlocked before reordering the locks.
     * This is called while the token is still locked. If false is returned, the
     * lock will remain unlocked and {@link #onRelock(LockToken)} will not be
     * called later. If true is returned, the token will be locked again and
     * {@link #onRelock(LockToken)} will be called.
     * 
     * @param token The token being unlocked
     * @return True if the token should be locked after locks have been
     *         reordered
     */
    public boolean onForceUnlock(LockToken<? extends R> token);
}
