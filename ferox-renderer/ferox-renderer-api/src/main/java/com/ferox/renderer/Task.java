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

/**
 * <p/>
 * Tasks are used to run arbitrary set of operations on the internal threads of a {@link Framework} with an
 * active {@link HardwareAccessLayer}. The context provides access to {@link Renderer renderers} and allows
 * for manual updating and disposing of {@link Resource resources}.
 * <p/>
 * Tasks are executed using {@link Framework#invoke(Task)}.
 *
 * @param <T>
 *
 * @author Michael Ludwig
 * @see HardwareAccessLayer
 * @see Context
 */
public interface Task<T> {
    /**
     * Perform operations of this task, using the provided HardwareAccessLayer. The access layer should only
     * be used within this method and should not be stored for later use as the Framework has full control
     * over when the context is valid and on which threads it may be used.
     *
     * @param access The access layer providing access to renderers and resource management
     *
     * @return Some response that will be returned with the Future created when the task was queued
     */
    public T run(HardwareAccessLayer access);
}
