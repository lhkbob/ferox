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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ferox.math.ColorRGB;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.Light;
import com.ferox.scene.PointLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.SpotLight;
import com.ferox.scene.Transform;
import com.ferox.scene.controller.light.LightGroupResult;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.property.IntProperty;

public class LightGroupFactory implements StateGroupFactory {
    private static final Vector4 BLACK = new Vector4(0.0, 0.0, 0.0, 1.0);

    private final StateGroupFactory child;

    private final Set<Component<? extends Light<?>>> shadowCastingLights;

    private final State[][] groups;
    private final IntProperty groupAssignment;

    // components for access
    private final Renderable renderable;

    public LightGroupFactory(EntitySystem system, LightGroupResult lightGroups,
                             Set<Component<? extends Light<?>>> shadowCastingLights,
                             int maxLights, StateGroupFactory child) {
        this.child = child;
        this.shadowCastingLights = shadowCastingLights;

        groupAssignment = lightGroups.getAssignmentProperty();
        groups = new State[lightGroups.getGroupCount()][];
        renderable = system.createDataInstance(Renderable.class);

        // components for access
        DirectionLight dl = system.createDataInstance(DirectionLight.class);
        SpotLight sl = system.createDataInstance(SpotLight.class);
        PointLight pl = system.createDataInstance(PointLight.class);
        AmbientLight al = system.createDataInstance(AmbientLight.class);
        Transform t = system.createDataInstance(Transform.class);

        Matrix4 identity = new Matrix4().setIdentity();

        for (int i = 0; i < groups.length; i++) {
            Set<Component<? extends Light<?>>> group = lightGroups.getGroup(i);

            List<LightState> states = new ArrayList<LightState>();
            List<Component<?>> unassignedAmbientLights = new ArrayList<Component<?>>();

            LightState currentState = new LightState(maxLights); // always have one state, even if empty
            states.add(currentState);

            int index = 0;
            for (Component<? extends Light<?>> light : group) {
                Entity e = light.getEntity();
                GLLight gl = null;

                if (light.getType().equals(DirectionLight.class) && e.get(dl)) {
                    gl = new GLLight();
                    gl.setDirectionLight(light, dl.getColor(),
                                         (e.get(t) ? t.getMatrix() : identity));
                } else if (light.getType().equals(SpotLight.class) && e.get(sl)) {
                    gl = new GLLight();
                    gl.setSpotLight(light, sl.getColor(), sl.getCutoffAngle(),
                                    sl.getFalloffDistance(),
                                    (e.get(t) ? t.getMatrix() : identity));
                } else if (light.getType().equals(PointLight.class) && e.get(pl)) {
                    gl = new GLLight();
                    gl.setPointLight(light, pl.getColor(), pl.getFalloffDistance(),
                                     (e.get(t) ? t.getMatrix() : identity));
                } else if (light.getType().equals(AmbientLight.class)) {
                    if (currentState.ambientColor == null) {
                        // merge ambient light with this state group
                        e.get(al);
                        currentState.setAmbientLight(al.getColor());
                    } else {
                        // store the ambient light for later
                        unassignedAmbientLights.add(light);
                    }
                }

                if (gl != null) {
                    // have a light to store in the current state
                    if (index >= maxLights) {
                        // must move on to a new state
                        currentState = new LightState(maxLights);
                        states.add(currentState);
                        index = 0;
                    }
                    currentState.setLight(index++, gl);
                }
            }

            // process any ambient lights that need to go into a state group
            if (!unassignedAmbientLights.isEmpty()) {
                for (int j = 0; j < states.size() && !unassignedAmbientLights.isEmpty(); j++) {
                    if (states.get(j).ambientColor == null) {
                        // this state can take an ambient light color
                        Component<?> light = unassignedAmbientLights.remove(unassignedAmbientLights.size() - 1);
                        if (light.getEntity().get(al)) {
                            states.get(j).setAmbientLight(al.getColor());
                        }
                    }
                }

                // if there are still ambient lights, we need one state for
                // each ambient light without any other configuration
                while (!unassignedAmbientLights.isEmpty()) {
                    Component<?> light = unassignedAmbientLights.remove(unassignedAmbientLights.size() - 1);
                    if (light.getEntity().get(al)) {
                        LightState state = new LightState(0);
                        state.setAmbientLight(al.getColor());
                        states.add(state);
                    }
                }
            }

            groups[i] = states.toArray(new LightState[states.size()]);
        }
    }

    private static Vector4 convertColor(ColorRGB color) {
        return new Vector4(color.redHDR(), color.greenHDR(), color.blueHDR(), 1.0);
    }

    @Override
    public StateGroup newGroup() {
        return new LightGroup();
    }

    private class LightGroup implements StateGroup {
        private final List<StateNode> nodes;

        public LightGroup() {
            nodes = new ArrayList<StateNode>();
            for (int i = 0; i < groups.length; i++) {
                nodes.add(new StateNode((child == null ? null : child.newGroup()),
                                        groups[i]));
            }
        }

        @Override
        public StateNode getNode(Entity e) {
            if (e.get(renderable)) {
                int group = groupAssignment.get(renderable.getIndex());
                return nodes.get(group);
            } else {
                // shouldn't happen
                return null;
            }
        }

        @Override
        public List<StateNode> getNodes() {
            return nodes;
        }

        @Override
        public AppliedEffects applyGroupState(HardwareAccessLayer access,
                                              AppliedEffects effects) {
            // do nothing
            return effects;
        }

        @Override
        public void unapplyGroupState(HardwareAccessLayer access, AppliedEffects effects) {
            // do nothing
        }
    }

    private class LightState implements State {
        private Vector4 ambientColor;

        private final GLLight[] lights;

        public LightState(int maxLights) {
            ambientColor = null;
            lights = new GLLight[maxLights];
        }

        public void setAmbientLight(ColorRGB color) {
            ambientColor = convertColor(color);
        }

        public void setLight(int light, GLLight glLight) {
            lights[light] = glLight;
        }

        @Override
        public void add(Entity e) {
            // do nothing
        }

        @Override
        public AppliedEffects applyState(HardwareAccessLayer access,
                                         AppliedEffects effects, int index) {
            FixedFunctionRenderer r = access.getCurrentContext()
                                            .getFixedFunctionRenderer();
            int numLights = 0;

            if (!effects.isShadowBeingRendered() && ambientColor != null) {
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
                    boolean ifInSM = effects.isShadowBeingRendered() && light.source == effects.getShadowMappingLight();
                    boolean notInSM = !effects.isShadowBeingRendered() && !shadowCastingLights.contains(light.source);

                    if (ifInSM || notInSM) {
                        // enable and configure the light
                        r.setLightEnabled(i, true);
                        r.setLightPosition(i, light.position);
                        r.setLightColor(i, BLACK, light.color, light.color);

                        if (light.spotlightDirection != null) {
                            // configure additional spotlight parameters
                            r.setSpotlight(i, light.spotlightDirection, light.cutoffAngle);
                            if (light.falloff >= 0) {
                                // the constant 15 was chosen through experimentation, basically
                                // a value that makes lights seem bright enough but still
                                // drop off pretty well by the desired radius
                                r.setLightAttenuation(i,
                                                      1.0f,
                                                      0.0f,
                                                      (15.0 / (light.falloff * light.falloff)));
                            } else {
                                // disable attenuation
                                r.setLightAttenuation(i, 1.0f, 0.0f, 0.0f);
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
                // update blending state
                if (index > 0 && !effects.isShadowBeingRendered()) {
                    // update blending state, we only do this when accumulating into the previous
                    // lights that were already rendered. If we're in the shadowing pass, we
                    // know that only a single light is active so it doesn't matter if the index > 0,
                    // we don't touch the blending
                    effects = effects.applyBlending(effects.getSourceBlendFactor(),
                                                    BlendFactor.ONE);
                    effects.pushBlending(r);
                }

                return effects;
            } else {
                // if there aren't any configured lights, no need to render everything
                return null;
            }
        }

        @Override
        public void unapplyState(HardwareAccessLayer access, AppliedEffects effects,
                                 int index) {
            FixedFunctionRenderer r = access.getCurrentContext()
                                            .getFixedFunctionRenderer();
            if (index > 0 && !effects.isShadowBeingRendered()) {
                // these effects were the original, so we restore the blend state
                effects.pushBlending(r);
            }
        }
    }

    private static class GLLight {
        Vector4 color;
        Vector4 position;
        Vector3 spotlightDirection;
        double cutoffAngle;
        double falloff;

        Component<? extends Light<?>> source;

        public void setDirectionLight(Component<? extends Light<?>> light,
                                      ColorRGB color, Matrix4 transform) {
            position = new Vector4(-transform.m02, -transform.m12, -transform.m22, 0.0);
            this.color = convertColor(color);
            spotlightDirection = null;
            source = light;
        }

        public void setSpotLight(Component<? extends Light<?>> light, ColorRGB color,
                                 double cutoffAngle, double falloff, Matrix4 transform) {
            position = new Vector4(transform.m03, transform.m13, transform.m23, 1.0);
            this.color = convertColor(color);
            spotlightDirection = new Vector3(transform.m02, transform.m12, transform.m22);
            this.cutoffAngle = cutoffAngle;
            this.falloff = falloff;
            source = light;
        }

        public void setPointLight(Component<? extends Light<?>> light, ColorRGB color,
                                  double falloff, Matrix4 transform) {
            position = new Vector4(transform.m03, transform.m13, transform.m23, 1.0);
            this.color = convertColor(color);
            spotlightDirection = new Vector3(0.0, 0.0, 1.0); // any non-null direction is fine
            this.cutoffAngle = 180.0;
            this.falloff = falloff;
            source = light;
        }
    }
}
