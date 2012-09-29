package com.ferox.renderer.impl;

import com.ferox.renderer.Framework;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Task;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;

/**
 * UpdateResourceTask is a simple task that implements the behavior needed for
 * {@link Framework#update(Resource)}.
 * 
 * @author Michael Ludwig
 */
public class UpdateResourceTask implements Task<Status> {
    private final Resource resource;

    /**
     * Create a new UpdateResourceTask that will update the provided resource
     * when run.
     * 
     * @param resource The Resource to update
     * @throws NullPointerException if resource is null
     */
    public UpdateResourceTask(Resource resource) {
        if (resource == null) {
            throw new NullPointerException("Resource cannot be null");
        }
        this.resource = resource;
    }

    @Override
    public Status run(HardwareAccessLayer access) {
        return access.update(resource);
    }
}
