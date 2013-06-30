package com.ferox.renderer.builder;

import com.ferox.renderer.Resource;

/**
 * Builder is the interface inherited by all resource builders, although the
 * sub-interfaces of builder provide the actual configuration points tied to each resource
 * type. A Builder instance can only be used once. After {@link #build()} has been called
 * it is no longer usable.
 *
 * @author Michael Ludwig
 */
public interface Builder<T extends Resource> {
    /**
     * Construct the resource that was configured by the builder. If this is called on the
     * framework thread, the resource will be created immediately. If it is not the
     * framework thread, a task to create the resource will be queued and blocked on.
     * <p/>
     * If the resource type is not supported by the hardware, or is configured beyond the
     * limits of the hardware (such as too many uniforms in a shader, even though shaders
     * are supported), an exception is thrown and no GPU resources will remain allocated
     * in conjunction with the resource.
     *
     * @return The new resource
     *
     * @throws com.ferox.renderer.ResourceException
     *                               if the resource is unsupported or cannot be created
     *                               in the current configuration
     * @throws IllegalStateException if build() is called a second time
     */
    public T build();
}
