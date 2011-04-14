package com.ferox.renderer.impl2;

import com.ferox.renderer.Context;
import com.ferox.renderer.Framework;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Surface;
import com.ferox.renderer.Task;

/**
 * FlushSurfaceTask is a simple task that flushes a surface, for use with
 * {@link Framework#flush(Surface, String)}.
 * 
 * @author Michael Ludwig
 */
public class FlushSurfaceTask implements Task<Void> {
    private final Surface surface;

    /**
     * Create a new FlushSurfaceTask that will flush the given Surface when it
     * is run.
     * 
     * @param surface The Surface to flush
     * @throws NullPointerException if surface is null
     */
    public FlushSurfaceTask(Surface surface) {
        if (surface == null)
            throw new NullPointerException("Surface cannot be null");
        this.surface = surface;
    }
    
    @Override
    public Void run(HardwareAccessLayer access) {
        Context context = access.setActiveSurface(surface);
        if (context != null)
            context.flush();
        return null;
    }
}
