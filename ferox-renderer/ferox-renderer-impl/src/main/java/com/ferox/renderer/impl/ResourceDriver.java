/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.impl;

import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.resource.Resource;

/**
 * <p>
 * A ResourceDriver manages the lifecycle of a given Resource type. It is used
 * by {@link ResourceManager} to perform the actual low-level graphics
 * operations when requested by the HardwareAccessLayer or when a Resource must
 * be updated becaue of an ON_DEMAND update policy. Implementations may assume
 * that a low-level context is current on the running thread.
 * <p>
 * Implementations do not need to synchronize on the resource during updates
 * because the {@link ResourceManager} will have already locked the resource.
 * 
 * @author Michael Ludwig
 */
public interface ResourceDriver {
    /**
     * <p>
     * Initialize a handle for the given resource. This should not perform any
     * OpenGL work for initialization, but just prepare the initial state of the
     * handle for a subsequent call to
     * {@link #update(OpenGLContext, Resource, Object)}.
     * <p>
     * The returned handle should not hold any references to the resource so
     * that the resource can be garbage-collected.
     * 
     * @param resource The resource being seen by the framework for the first
     *            time
     * @return A new ResourceHandle associated with the given resource
     */
    public Object init(Resource resource);

    /**
     * <p>
     * Perform an update on the given resource. This is responsible for
     * performing the operations required by
     * {@link HardwareAccessLayer#update(Resource)}. The handle was the instance
     * returned by a prior call to {@link #init(Resource)} for the given
     * Resource.
     * <p>
     * If the update is successful, the returned string is considered to be the
     * new status message. The resource manager will automatically mark the
     * associated resource's status as READY.
     * <p>
     * If the update could not be performed, an UpdateResourceException should
     * be thrown with the appropriate message. The resource manager will set the
     * resource's status to ERROR and take the message from the exception as its
     * status message.
     * 
     * @param context The current context
     * @param res The resource to update
     * @param handle The handle for the resource
     * @return The successfully updated status message
     * @throws UpdateResourceException if the resource could not be updated
     */
    public String update(OpenGLContext context, Resource res, Object handle) throws UpdateResourceException;

    /**
     * Reset any internal data that is tracking the state of the resource so
     * that the next update is a full update. See
     * {@link HardwareAccessLayer#reset(Resource)}. This does not have to delete
     * any of the resource's low-level data since it does not act like a
     * dispose. Unlike the other resource operations, this must not depend on a
     * current context.
     * 
     * @param handle The ResourceHandle to reset
     */
    public void reset(Object handle);

    /**
     * <p>
     * Dispose of all low-level graphics resources that are associated with this
     * ResourceHandle. The given handle was, at some point, earlier returned by
     * a call to {@link #init(Resource)} on this driver. There is a chance that
     * the Resource instance associated with the handle has already been garbage
     * collected.
     * <p>
     * The resource manager will automatically mark the associated resource's
     * status as DISPOSED.
     * 
     * @param context The current context
     * @param handle The ResourceHandle to dispose of
     */
    public void dispose(OpenGLContext context, Object handle);

    /**
     * Return the class type that this ResourceDriver can process. This should
     * return the class of the most abstract Resource type the driver supports.
     * 
     * @return The resource type processed by the driver
     */
    public Class<? extends Resource> getResourceType();
}
