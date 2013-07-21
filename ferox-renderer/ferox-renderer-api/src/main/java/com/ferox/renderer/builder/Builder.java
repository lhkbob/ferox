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
package com.ferox.renderer.builder;

import com.ferox.renderer.Resource;

/**
 * Builder is the interface inherited by all resource builders, although the sub-interfaces of builder provide
 * the actual configuration points tied to each resource type. A Builder instance can only be used once. After
 * {@link #build()} has been called it is no longer usable.
 *
 * @author Michael Ludwig
 */
public interface Builder<T extends Resource> {
    /**
     * Construct the resource that was configured by the builder. If this is called on the framework thread,
     * the resource will be created immediately. If it is not the framework thread, a task to create the
     * resource will be queued and blocked on.
     * <p/>
     * If the resource type is not supported by the hardware, or is configured beyond the limits of the
     * hardware (such as too many uniforms in a shader, even though shaders are supported), an exception is
     * thrown and no GPU resources will remain allocated in conjunction with the resource.
     *
     * @return The new resource
     *
     * @throws com.ferox.renderer.ResourceException
     *                               if the resource is unsupported or cannot be created in the current
     *                               configuration
     * @throws IllegalStateException if build() is called a second time
     */
    public T build();
}
