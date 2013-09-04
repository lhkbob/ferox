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
package com.ferox.scene.task.ffp;

import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.*;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.builder.DepthMap2DBuilder;
import com.ferox.renderer.geom.Geometry;
import com.ferox.scene.Light;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.ferox.scene.Transparent;
import com.ferox.scene.task.PVSResult;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;

import java.util.*;

public class ShadowMapCache {
    private final TextureSurface shadowMap;
    private final DepthMap2D depthData;

    private Map<Light, ShadowMapScene> shadowScenes;
    private Set<Light> shadowLights;

    public ShadowMapCache(Framework framework, int width, int height) {
        DepthMap2DBuilder db = framework.newDepthMap2D();
        db.width(width).height(height).interpolated().borderDepth(1.0).wrap(Sampler.WrapMode.CLAMP_TO_BORDER)
          .depthComparison(Comparison.LEQUAL).depth().mipmap(0).fromUnsignedNormalized((int[]) null);
        depthData = db.build();
        shadowMap = framework.createSurface(
                new TextureSurfaceOptions().size(width, height).depthBuffer(depthData.getRenderTarget()));

        shadowScenes = new HashMap<>();
    }

    public void reset() {
        shadowScenes = new HashMap<>();
        shadowLights = Collections.unmodifiableSet(shadowScenes.keySet());
    }

    public Set<Light> getShadowCastingLights() {
        return shadowLights;
    }

    public Frustum getShadowMapFrustum(Light light) {
        ShadowMapScene scene = shadowScenes.get(light);
        if (scene == null) {
            throw new IllegalArgumentException("Light was not cached previously");
        }
        return scene.frustum;
    }

    public DepthMap2D getShadowMap(Light shadowLight, HardwareAccessLayer access) {
        ShadowMapScene scene = shadowScenes.get(shadowLight);
        if (scene == null) {
            throw new IllegalArgumentException("Light was not cached previously");
        }

        Surface origSurface = access.getCurrentContext().getSurface();
        ContextState<FixedFunctionRenderer> origState = access.getCurrentContext().getFixedFunctionRenderer()
                                                              .getCurrentState();

        FixedFunctionRenderer r = access.setActiveSurface(shadowMap).getFixedFunctionRenderer();

        r.clear(true, true, true);
        r.setColorWriteMask(false, false, false, false);

        // move everything backwards slightly to account for floating errors
        r.setDepthOffsets(0, 5);
        r.setDepthOffsetsEnabled(true);

        scene.scene.visit(new AppliedEffects(), access);

        // restore original surface and state
        access.setActiveSurface(origSurface);

        // FIXME race condition on shutdown exists if the orig surface is
        // destroyed while rendering to the shadow map, if the map is on a pbuffer
        access.getCurrentContext().getFixedFunctionRenderer().setCurrentState(origState);

        return depthData;
    }

    @SuppressWarnings("unchecked")
    public void cacheShadowScene(PVSResult pvs) {
        if (!Light.class.isAssignableFrom(pvs.getSource().getType())) {
            // only take PVS's produced by lights
            return;
        }

        EntitySystem system = pvs.getSource().getEntitySystem();

        GeometryState geom = new GeometryState();

        // build up required states and tree simultaneously
        StateNode root = new StateNode(new CameraState(pvs.getFrustum()));

        List<GeometryState> geomLookup = new ArrayList<>();

        ComponentIterator it = system.fastIterator(pvs.getPotentiallyVisibleSet());
        Renderable renderable = it.addRequired(Renderable.class);
        Transform transform = it.addRequired(Transform.class);
        Transparent transparent = it.addOptional(Transparent.class);

        Map<GeometryState, Integer> geomState = new HashMap<>();
        while (it.next()) {
            // skip transparent entities, as its somewhat physically plausible that
            // they'd cast fainter shadows, and with the quality of FFP shadow mapping,
            // being able to see the shadows the cast immediately underneath them is
            // very bad looking
            if (transparent.isAlive()) {
                continue;
            }

            Geometry geometry = renderable.getGeometry();

            // don't need normals, and use front style for back faces and disable
            // front faces so we only render those in the back
            geom.set(geometry.getVertices(), null, null, DrawStyle.NONE, renderable.getFrontDrawStyle(),
                     geometry.getPolygonType(), geometry.getIndices(), geometry.getIndexOffset(),
                     geometry.getIndexCount());

            Integer geomStateIndex = geomState.get(geom);
            if (geomStateIndex == null) {
                geomStateIndex = geomLookup.size();
                geomLookup.add(geom);
                geomState.put(geom, geomStateIndex);
                // create a new state so we don't mutate value stached in collection
                geom = new GeometryState();
            }

            RenderState leaf;
            StateNode geomNode = root.getChild(geomStateIndex);
            if (geomNode == null) {
                geomNode = new StateNode(geomLookup.get(geomStateIndex));
                root.setChild(geomStateIndex, geomNode);
                leaf = geomLookup.get(geomStateIndex).newOpaqueRenderState();
                geomNode.addChild(new StateNode(leaf));
            } else {
                leaf = (RenderState) geomNode.getChild(0).getState();
            }

            leaf.add(transform.getMatrix());
        }

        Light source = (Light) pvs.getSource();
        shadowScenes.put(source, new ShadowMapScene(pvs.getFrustum(), root));
    }

    private static class ShadowMapScene {
        private final Frustum frustum;
        private final StateNode scene;

        public ShadowMapScene(Frustum frustum, StateNode scene) {
            this.frustum = frustum;
            this.scene = scene;
        }
    }
}
