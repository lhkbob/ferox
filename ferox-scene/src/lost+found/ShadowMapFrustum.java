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
package com.ferox.scene.controller.ffp;

import com.ferox.entity2.Component;
import com.ferox.entity2.TypedComponent;
import com.ferox.math.bounds.Frustum;

/**
 * ShadowMapFrustum is a meta-Component that associates the shadow casting
 * information needed for shadow-mapping for a Camera. This information is
 * computed by a {@link ShadowMapFrustumController} and is used by the
 * {@link FixedFunctionRenderController} to render scenes with shadows.
 * 
 * @author Michael Ludwig
 */
public class ShadowMapFrustum extends TypedComponent<ShadowMapFrustum> {
    private Frustum frustum;
    private Component shadowLight;

    /**
     * Create a ShadowMapFrustum that stores the given Frustum and light. See
     * {@link #setFrustum(Frustum)} and {@link #setLight(Component)}
     * 
     * @param f
     * @param light
     * @throws NullPointerException if f and light are null
     */
    public ShadowMapFrustum(Frustum f, Component light) {
        super(ShadowMapFrustum.class);
        setFrustum(f);
        setLight(light);
    }

    /**
     * Set the light Component that represents the shadow-casting light.
     * 
     * @param light The new shadow-casting light
     * @throws NullPointerException if light is null
     */
    public void setLight(Component light) {
        if (light == null)
            throw new NullPointerException("Light cannot be null");
        
        shadowLight = light;
    }
    
    /**
     * @return The shadow-casting light component, either a DirectionLight or a
     *         SpotLight.
     */
    public Component getLight() {
        return shadowLight;
    }

    /**
     * @return A Frustum that represents the projection information to render a
     *         depth-image from the perspective of the light returned by
     *         {@link #getLight()}
     */
    public Frustum getFrustum() {
        return frustum;
    }

    /**
     * Set the Frustum that stores the shadow-mapping projection to use.
     * 
     * @param f The new shadow-mapping frustum
     * @throws NullPointerException if f is null
     */
    public void setFrustum(Frustum f) {
        if (f == null)
            throw new NullPointerException("Frustum cannot be null");
        frustum = f;
    }
}