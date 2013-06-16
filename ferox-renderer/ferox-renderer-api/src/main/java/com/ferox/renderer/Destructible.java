package com.ferox.renderer;

import java.util.concurrent.Future;

/**
 * Destructibles are objects that have internal, potentially heavyweight data tied to the
 * GPU and generally an implementation dependent internal framework thread, similar to how
 * the AWT EDT thread functions. Destructibles will always be properly disposed of and
 * cleaned up if they are garbage-collected by the JVM. However, they expose a {@link
 * #destroy()} method to give more explicit control over the timing of the potentially
 * expensive task of cleaning up the underlying GPU resources.
 *
 * @author Michael Ludwig
 */
public interface Destructible {
    /**
     * Manually trigger the destruction of the resource if it's critical the internal
     * structures are disposed of ASAP instead of waiting for the garbage collector to
     * detect when it is weak reference-able. All Destructibles will have their internal
     * structures disposed of when they are weak reference-able but this provides more
     * control.
     * <p/>
     * This method is thread safe and can be called on the Framework's internal thread or
     * not.  If called on the Framework's internal thread the destruction will occur
     * immediately and the returned Future will already be completed.  Otherwise the
     * disposal will be queued into the Framework's internal thread.
     * <p/>
     * Regardless of when the internal resources are actually disposed of, this instance
     * is considered destroyed immediately upon calling this method. When destroyed, all
     * behavior besides calling destroy() or {@link #isDestroyed()} is undefined.
     *
     * @return A Future to check when destruction has completed
     */
    public Future<Void> destroy();

    /**
     * @return True if {@link #destroy()} has been invoked manually
     */
    public boolean isDestroyed();
}
