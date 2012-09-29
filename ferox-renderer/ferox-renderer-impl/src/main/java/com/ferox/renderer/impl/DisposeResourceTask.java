package com.ferox.renderer.impl;

import com.ferox.renderer.Framework;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Task;
import com.ferox.resource.Resource;

/**
 * DisposeResourceTask is a simple task that provides the behavior for
 * {@link Framework#dispose(Resource)}.
 * 
 * @author Michael Ludwig
 */
public class DisposeResourceTask implements Task<Void> {
    private final Resource resource;

    /**
     * Create a new DisposeResourceTask that will dispose the provided resource
     * when it is run.
     * 
     * @param resource The resource to dispose
     * @throws NullPointerException if resource is null
     */
    public DisposeResourceTask(Resource resource) {
        if (resource == null) {
            throw new NullPointerException("Resourcea cannot be null");
        }
        this.resource = resource;
    }

    @Override
    public Void run(HardwareAccessLayer access) {
        access.dispose(resource);
        return null;
    }
}
