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
package com.ferox.resource;

import com.ferox.renderer.Framework;
import com.ferox.renderer.HardwareAccessLayer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p/>
 * An abstract class that represents some type of data stored on the graphics card. A
 * resource is fairly abstract so there many things can be represented (assuming there are
 * hardware capabilities supporting it). Some examples include {@link
 * com.ferox.resource.texture.Texture}, {@link com.ferox.resource.geom.VertexBufferObject},
 * and {@link GlslShader}.
 * <p/>
 * A Resource cannot be used until its been updated by a Framework. There are different
 * ways that a Resource can be updated, some of which are automatic. See {@link
 * UpdatePolicy} for more details.
 * <p/>
 * When a Resource can only be accessed by weak references, a Framework will automatically
 * schedule it for disposal. A Framework that's destroyed will have any remaining
 * Resource's internal data disposed, as well.
 * <p/>
 * To provide thread safety while accessing and using resources, each resource is guarded
 * by its built-in monitor, meaning manipulating the resource should be done with a
 * 'synchronized' block on that resource. Resources that expose simple setters are
 * recommended to be 'synchronized'. Application code should acquire the lock when
 * performing bulk manipulation such as editing any associated {@link BufferData buffer
 * data}.
 *
 * @author Michael Ludwig
 */
public abstract class Resource {
    /**
     * Each resource will have a status with each {@link Framework}. A Resource is usable
     * if it has a status of READY. Resources that are DISPOSED have no stored data on the
     * graphics card or in memory. A Resource that has a status of ERROR is unusable until
     * it's been repaired.
     */
    public static enum Status {
        /**
         * The resource has been updated successfully and is ready to use.
         */
        READY,
        /**
         * <p/>
         * The Framework has tried to update the resource and there may be internal
         * storage used for the Resource, but something is wrong and the Resource isn't
         * usable.
         * <p/>
         * In many cases, the hardware could not support a configuration option, the
         * resource type in general is still supported. An example would be if 3D textures
         * were not supported, but 1D and 2D still were.
         */
        ERROR,
        /**
         * The Framework has no support for the Resource sub-class. Like ERROR it means
         * the Resource is unusable. Unlike ERROR, the Resource cannot ever used, and it's
         * impossible to modify the Resource to change this status.
         */
        UNSUPPORTED,
        /**
         * The Framework has no internal representations of the Resource (never updated,
         * or it's been disposed).
         */
        DISPOSED
    }

    /**
     * The UpdatePolicy of a Resource controls the behavior of Frameworks and how they
     * manage their Resources. By default, created Resources have the ON_DEMAND policy,
     * which checks for changes when a Resource is needed for rendering. The MANUAL policy
     * forces Resources to be updated manually with {@link HardwareAccessLayer#update(Resource)}
     * as needed.
     */
    public static enum UpdatePolicy {
        /**
         * Changes are applied automatically, however, they are only flushed immediately
         * before the Resource is used by a Renderer. Manual updates can still be done.
         */
        ON_DEMAND,
        /**
         * Frameworks track changes to a Resource but do not apply the changes until
         * specifically requested with a call to {@link HardWareAccessLayer#update(Resource)}.
         * Changes are not flushed even if the Resource is used by a Renderer.
         */
        MANUAL
    }

    private static AtomicInteger idCounter = new AtomicInteger(0);

    private final int id;
    private volatile UpdatePolicy policy;

    public Resource() {
        id = idCounter.incrementAndGet();
        policy = UpdatePolicy.ON_DEMAND;
    }

    /**
     * @return The current UpdatePolicy
     */
    public UpdatePolicy getUpdatePolicy() {
        return policy;
    }

    /**
     * Set the update policy for the Resource. It is not necessary to synchronize on the
     * Resource to safely call this method, as it is stored as a volatile field.
     *
     * @param policy The new policy
     *
     * @throws NullPointerException if policy is null
     */
    public void setUpdatePolicy(UpdatePolicy policy) {
        if (policy == null) {
            throw new NullPointerException("UpdatePolicy cannot be null");
        }
        this.policy = policy;
    }

    /**
     * Return a unique numeric id that's assigned to this Resource instance. Each
     * instantiated Resource is assigned an id, starting at 0, which is valid only for the
     * lifetime of the current JVM.
     *
     * @return This Resource's unique id
     */
    public final int getId() {
        return id;
    }

    @Override
    public final int hashCode() {
        return id;
    }

    @Override
    public final boolean equals(Object o) {
        return o == this;
    }
}
