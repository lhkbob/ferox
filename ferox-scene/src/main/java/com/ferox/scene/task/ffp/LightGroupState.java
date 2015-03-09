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

import com.ferox.math.ColorRGB;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.scene.Light;
import com.ferox.scene.Transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LightGroupState implements State {
    private static final Vector4 BLACK = new Vector4(0.0, 0.0, 0.0, 1.0);

    private final Vector4 ambientColor;
    private final Set<Light> shadowCastingLights;
    private final LightBatch[] batches;

    public LightGroupState(Set<Light> lightGroup, Set<Light> shadowCastingLights, ColorRGB ambientColor,
                           int maxLights) {
        this.shadowCastingLights = shadowCastingLights;
        this.ambientColor = convertColor(ambientColor);

        List<LightBatch> states = new ArrayList<>();

        LightBatch currentState = new LightBatch(maxLights); // always have one state, even if empty
        states.add(currentState);

        ColorRGB color = new ColorRGB();
        Matrix4 transform = new Matrix4();
        int index = 0;
        for (Light light : lightGroup) {
            Transform t = light.getEntity().get(Transform.class);
            GLLight gl = new GLLight();

            double cutoff = light.getCutoffAngle();
            if (Double.isNaN(cutoff)) {
                gl.setDirectionLight(light, t.getMatrix(transform), color);
            } else {
                gl.setSpotOrPointLight(light, t.getMatrix(transform), color);
            }

            if (index >= maxLights) {
                // must move on to a new batch
                currentState = new LightBatch(maxLights);
                states.add(currentState);
                index = 0;
            }
            currentState.setLight(index++, gl);
        }

        batches = states.toArray(new LightBatch[states.size()]);
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
        // we only do blending overriding when accumulating into the previous
        // lights that were already rendered. If we're in the shadowing pass, we
        // know that only a single light is active so it doesn't matter if the index > 0,
        // we don't need to touch the blending
        boolean needsBlending = batches.length > 1 && !effects.isShadowBeingRendered();
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        // render first batch with regular blending
        AppliedEffects batchEffects = effects;
        batches[0].renderBatch(0, currentNode, batchEffects, access);

        if (needsBlending) {
            // update blending state
            batchEffects = effects.applyBlending(effects.getSourceBlendFactor(), BlendFactor.ONE);
            batchEffects.pushBlending(r);
        }

        // render all other batches
        for (int i = 1; i < batches.length; i++) {
            batches[i].renderBatch(i, currentNode, batchEffects, access);
        }

        if (needsBlending) {
            // overwrote blending state, so restore it
            effects.pushBlending(r);
        }
    }

    private class LightBatch {
        private final GLLight[] lights;

        public LightBatch(int maxLights) {
            lights = new GLLight[maxLights];
        }

        public void setLight(int light, GLLight glLight) {
            lights[light] = glLight;
        }

        public void renderBatch(int batch, StateNode currentNode, AppliedEffects effects,
                                HardwareAccessLayer access) {
            FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();
            int numLights = 0;

            if (!effects.isShadowBeingRendered() && batch == 0) {
                // use the defined ambient color during the main render stage,
                // but not when we're being accumulated for the shadow stage
                r.setGlobalAmbientLight(ambientColor);
                numLights++;
            } else {
                // default ambient color is black, and if we're in an SM pass,
                // we don't want to apply ambient light multiple times
                r.setGlobalAmbientLight(BLACK);
            }

            GLLight light;
            for (int i = 0; i < lights.length; i++) {
                light = lights[i];

                if (light != null) {
                    // check to see if this light should be used for the current
                    // stage of shadow mapping
                    boolean ifInSM = effects.isShadowBeingRendered() &&
                                     light.source == effects.getShadowMappingLight();
                    boolean notInSM =
                            !effects.isShadowBeingRendered() && !shadowCastingLights.contains(light.source);

                    if (ifInSM || notInSM) {
                        // enable and configure the light
                        r.setLightEnabled(i, true);
                        r.setLightPosition(i, light.position);
                        r.setLightColor(i, BLACK, light.color, light.color);

                        if (light.spotlightDirection != null) {
                            // configure additional spotlight parameters
                            r.setSpotlight(i, light.spotlightDirection, light.cutoffAngle, 0);
                            if (light.falloff >= 0) {
                                // the constant 15 was chosen through experimentation, basically
                                // a value that makes lights seem bright enough but still
                                // drop off pretty well by the desired radius
                                r.setLightAttenuation(i, 1.0, 0.0, (15.0 / (light.falloff * light.falloff)));
                            } else {
                                // disable attenuation
                                r.setLightAttenuation(i, 1.0, 0.0, 0.0);
                            }
                        }

                        numLights++;
                    } else {
                        // disable the light
                        r.setLightEnabled(i, false);
                    }
                } else {
                    // disable the light
                    r.setLightEnabled(i, false);
                }
            }

            if (numLights > 0) {
                // render because at least one new light is affecting the scene
                currentNode.visitChildren(effects, access);
            } else if (batch == 0 && !effects.isShadowBeingRendered()) {
                // there are no configured lights, but render the object's
                // silhouettes to make sure the depth buffer, etc. get filled
                // as expected
                currentNode.visitChildren(effects, access);
            }
        }
    }

    private static class GLLight {
        Vector4 color;
        Vector4 position;
        Vector3 spotlightDirection;
        double cutoffAngle;
        double falloff;

        Light source;

        public void setDirectionLight(Light light, Matrix4 transform, ColorRGB color) {
            position = new Vector4(-transform.m02, -transform.m12, -transform.m22, 0.0);
            this.color = convertColor(light.getColor(color));
            spotlightDirection = null;
            source = light;
        }

        public void setSpotOrPointLight(Light light, Matrix4 transform, ColorRGB color) {
            position = new Vector4(transform.m03, transform.m13, transform.m23, 1.0);
            this.color = convertColor(light.getColor(color));
            spotlightDirection = new Vector3(transform.m02, transform.m12, transform.m22);
            this.cutoffAngle = light.getCutoffAngle();
            this.falloff = light.getFalloffDistance();
            source = light;
        }
    }

    private static Vector4 convertColor(ColorRGB color) {
        return new Vector4(color.redHDR(), color.greenHDR(), color.blueHDR(), 1.0);
    }
}
