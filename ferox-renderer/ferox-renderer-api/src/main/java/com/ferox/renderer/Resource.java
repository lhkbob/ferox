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
package com.ferox.renderer;

import java.util.concurrent.Future;

/**
 * Resource is the root interface for all objects wrapping OpenGL objects on the GPU.
 * These include vertex buffer objects, the various supported textures, and GLSL shaders.
 * Resources are not created directly, but are instead built using the builders exposed by
 * a Framework.
 * <p/>
 * Once created a Resource is almost always immutable.  This is done to make the
 * multi-threading between an application thread and the framework thread easier.  Once
 * built there is no required synchronization, and critical state such as data type or
 * size cannot change. The method {@link #refresh()} can be used to update the GPU's
 * version of the data. Generally this is performed by flushing the original primitive
 * data array to the graphics card either immediately if it was called on the framework
 * thread, or at the next point the resource is used by a renderer.
 * <p/>
 * A resource instance can only be used by the framework that created it.
 *
 * @author Michael Ludwig
 */
public interface Resource extends Destructible {
    /**
     * @return The framework that created the resource
     */
    public Framework getFramework();

    /**
     * Refresh the state of this resource by transferring the current state of the
     * primitive array instances that provided its initial data. Depending on the resource
     * type, this may refresh from multiple arrays as is the case of textures with
     * multiple layers or mipmaps; or may do nothing, in the case of shaders that have no
     * primitive array data.
     * <p/>
     * Refreshing can be used to animate the underlying data in the resource without
     * having to recreate a new resource. However, because of the flexibility in how a
     * resource stores its data, the source arrays are not exposed by the resource
     * interface. This means that application code must maintain references to the
     * primitive arrays it wishes to animate.
     * <p/>
     * If this is called from the internal framework thread, the refresh task is run
     * immediately and the returned future is already complete. If it's called on any
     * other thread, a task is queued to the framework to perform the refresh action. In
     * this case, the exact state of the array data at the time the task is invoked is
     * used, instead of cloning the state when refresh() was originally called.
     * <p/>
     * If performance is critical, the {@link HardwareAccessLayer#refresh(Resource)} is
     * preferred because no allocation of a hidden task or future is performed and the
     * resource is refreshed directly.
     */
    public Future<Void> refresh();
}
