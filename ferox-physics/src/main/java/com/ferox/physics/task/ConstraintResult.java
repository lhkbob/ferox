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
package com.ferox.physics.task;

import com.ferox.physics.dynamics.LinearConstraintPool;
import com.lhkbob.entreri.task.Result;

/**
 * ConstraintResult is a result that can be used to deliver {@link LinearConstraintPool constraints} to a task
 * that can then solve them when appropriate. Currently this is the aptly named {@link
 * ConstraintSolvingTask}.
 *
 * @author Michael Ludwig
 */
public class ConstraintResult extends Result {
    private final LinearConstraintPool group;

    /**
     * Create a new result that wraps the given constraint pool
     *
     * @param group The pool to use
     *
     * @throws NullPointerException if group is null
     */
    public ConstraintResult(LinearConstraintPool group) {
        if (group == null) {
            throw new NullPointerException("LinearConstraintPool cannot be null");
        }
        this.group = group;
    }

    /**
     * @return The constraint pool in the result
     */
    public LinearConstraintPool getConstraints() {
        return group;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
