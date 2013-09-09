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
package com.ferox.physics.dynamics;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.entreri.Vector3Property.DefaultVector3;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.SharedInstance;

/**
 * Gravity is a component that can be added to a rigid body entity to give it a unique gravity acceleration
 * that is different from the system's default (which is by default configured through the {@link
 * com.ferox.physics.task.IntegrationTask}.
 * <p/>
 * The gravity components records the acceleration so that it is not dependent on the mass of the entity.
 *
 * @author Michael Ludwig
 */
public interface Gravity extends Component {
    /**
     * @return The current custom gravity acceleration vector
     */
    @Const
    @SharedInstance
    @DefaultVector3(x = 0.0, y = -9.8, z = 0.0)
    public Vector3 getGravity();

    /**
     * Set the custom gravity acceleration vector.
     *
     * @param gravity The new gravity vector
     *
     * @return This component
     */
    public Gravity setGravity(@Const Vector3 gravity);
}
