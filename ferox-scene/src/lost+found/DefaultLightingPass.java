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

import java.util.BitSet;

import com.ferox.entity2.Component;
import com.ferox.entity2.ComponentId;
import com.ferox.entity2.Entity;
import com.ferox.math.bounds.ReadOnlyAxisAlignedBox;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.Surface;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.scene.Transform;
import com.ferox.scene.ShadowReceiver;
import com.ferox.scene.Transparent;
import com.ferox.util.Bag;

/**
 * DefaultLightingPass is a {@link RenderPass} implementation that renders the
 * contents of a {@link RenderConnection} such that its entities are lit without
 * shadows using standard OpenGL lighting. This is the base lighting pass within
 * the shadow mapping algorithm before the shadowed lighting is applied.
 * 
 * @author Michael Ludwig
 */
public class DefaultLightingPass extends AbstractFixedFunctionRenderPass {
    private static final ComponentId<Transform> SE_ID = Component.getComponentId(Transform.class);
    private static final ComponentId<ShadowReceiver> SR_ID = Component.getComponentId(ShadowReceiver.class);
    private static final ComponentId<Transparent> T_ID = Component.getComponentId(Transparent.class);
    
    private final Component[] lightMap;
    private final ReadOnlyAxisAlignedBox[] boundMap;
    private final BitSet activeLights;
    
    public DefaultLightingPass(RenderConnection connection, int maxNumLights, int maxMaterialTexUnits,
                               String vertexBinding, String normalBinding, String texCoordBinding) {
        super(connection, maxMaterialTexUnits, vertexBinding, normalBinding, texCoordBinding);
        
        lightMap = new Component[maxNumLights];
        boundMap = new ReadOnlyAxisAlignedBox[maxNumLights];
        activeLights = new BitSet(maxNumLights);
    }
    
    @Override
    protected void render(FixedFunctionRenderer ffp) {
        Bag<Component> lightAtoms = connection.getLights();
        Bag<ReadOnlyAxisAlignedBox> boundAtoms = connection.getLightBounds();
        Bag<Entity> renderAtoms = connection.getRenderedEntities();
        
        // setup state for handling blending when needed
        ffp.setBlendMode(BlendFunction.ADD, BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
        
        Entity atom;
        int renderCount = renderAtoms.size();
        for (int ri = 0; ri < renderCount; ri++) {
            atom = renderAtoms.get(ri);
            // configure lighting for the render atom
            assignLights(atom, lightAtoms, boundAtoms);
            
            // handle transparency, since this is the only pass that needs it
            if (atom.get(T_ID) != null) {
                ffp.setBlendingEnabled(true);
                ffp.setDepthWriteMask(false);
            } else {
                ffp.setBlendingEnabled(false);
                ffp.setDepthWriteMask(true);
            }

            // render it, AbstractFixedFunctionRenderPass takes care of state setting
            render(atom);
        }
        
        // clear light map
        for (int i = 0; i < lightMap.length; i++) {
            lightMap[i] = null;
            boundMap[i] = null;
        }
        activeLights.clear();
    }
    
    private void assignLights(Entity ra, Bag<Component> lights, Bag<ReadOnlyAxisAlignedBox> bounds) {
        Transform se = ra.get(SE_ID);
        ReadOnlyAxisAlignedBox aabb = (se == null ? null : se.getWorldBounds());
        boolean receivesShadow = ra.get(SR_ID) != null;
        
        // first check all active lights to see which continue influencing the render atom
        Component la;
        for (int i = activeLights.nextSetBit(0); i >= 0; i = activeLights.nextSetBit(i + 1)) {
            if (!enableLight(i, aabb, receivesShadow, lightMap[i], boundMap[i])) {
                // we're not reusing this light so clear it
                lightMap[i] = null;
                boundMap[i] = null;
                activeLights.clear(i);
            }
        }
        
        // go through all lights that aren't active and apply them
        int nextLightIndex = activeLights.nextClearBit(0);
        int lightCount = lights.size();
        for (int i = 0; i < lightCount; i++) {
            if (nextLightIndex < 0)
                break; // no more room for lights in gl state
            
            la = lights.get(i);
            if (lightMap[i] != la && enableLight(nextLightIndex, aabb, receivesShadow, la, bounds.get(i))) {
                // light was applied, so advance the nextLightIndex
                nextLightIndex = activeLights.nextClearBit(nextLightIndex + 1);
            }
        }
        
        // disable all lights that don't have a mapped atom, just in case
        for (int i = 0; i < lightMap.length; i++) {
            if (lightMap[i] == null)
                setLight(i, null);
        }
    }
    
    private boolean enableLight(int light, ReadOnlyAxisAlignedBox rBounds, boolean receivesShadow, 
                                Component la, ReadOnlyAxisAlignedBox lBounds) {
        if (rBounds == null || lBounds.intersects(rBounds)) {
            if (lightMap[light] != la || la == connection.getShadowCastingLight()) {
                if (la != connection.getShadowCastingLight() || !receivesShadow)
                    setLight(light, la);
                else
                    setLight(light, null);
            }

            lightMap[light] = la;
            boundMap[light] = lBounds;
            activeLights.set(light);
            
            return true;
        } else
            return false;
    }

    @Override
    protected Frustum getFrustum() {
        return connection.getViewFrustum();
    }

    @Override
    protected void configureViewport(FixedFunctionRenderer renderer, Surface surface) {
        renderer.setViewport(connection.getViewportLeft(), connection.getViewportBottom(), 
                             connection.getViewportRight() - connection.getViewportLeft(), 
                             connection.getViewportTop() - connection.getViewportBottom());
        renderer.clear(true, true, true, surface.getClearColor(), surface.getClearDepth(), surface.getClearStencil());
    }

    @Override
    protected void notifyPassBegin() {
        connection.notifyBaseLightingPassBegin();
    }

    @Override
    protected void notifyPassEnd() {
        connection.notifyBaseLightingPassEnd();
    }
}
