package com.ferox.scene.controller.ffp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
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
import com.ferox.scene.SpotLight;
import com.ferox.scene.Transform;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Entity;

public class LightGroupState implements State {
    private static final Vector4 BLACK = new Vector4(0.0, 0.0, 0.0, 1.0);

    private final Set<Component<? extends Light<?>>> shadowCastingLights;
    private final LightBatch[] batches;

    public LightGroupState(Set<Component<? extends Light<?>>> lightGroup,
                           Set<Component<? extends Light<?>>> shadowCastingLights,
                           int maxLights, DirectionLight dl, SpotLight sl, PointLight pl,
                           AmbientLight al, Transform t) {
        this.shadowCastingLights = shadowCastingLights;

        List<LightBatch> states = new ArrayList<LightBatch>();
        List<Component<?>> unassignedAmbientLights = new ArrayList<Component<?>>();

        LightBatch currentState = new LightBatch(maxLights); // always have one state, even if empty
        states.add(currentState);

        int index = 0;
        for (Component<? extends Light<?>> light : lightGroup) {
            Entity e = light.getEntity();
            GLLight gl = null;

            if (light.getType().equals(DirectionLight.class) && e.get(dl)) {
                gl = new GLLight();
                e.get(t);
                gl.setDirectionLight(light, dl.getColor(), t.getMatrix());
            } else if (light.getType().equals(SpotLight.class) && e.get(sl)) {
                gl = new GLLight();
                e.get(t);
                gl.setSpotLight(light, sl.getColor(), sl.getCutoffAngle(),
                                sl.getFalloffDistance(), t.getMatrix());
            } else if (light.getType().equals(PointLight.class) && e.get(pl)) {
                gl = new GLLight();
                e.get(t);
                gl.setPointLight(light, pl.getColor(), pl.getFalloffDistance(),
                                 t.getMatrix());
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
                    currentState = new LightBatch(maxLights);
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
                    LightBatch state = new LightBatch(0);
                    state.setAmbientLight(al.getColor());
                    states.add(state);
                }
            }
        }

        batches = states.toArray(new LightBatch[states.size()]);
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
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
            batchEffects = effects.applyBlending(effects.getSourceBlendFactor(),
                                                 BlendFactor.ONE);
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
        private Vector4 ambientColor;

        private final GLLight[] lights;

        public LightBatch(int maxLights) {
            ambientColor = null;
            lights = new GLLight[maxLights];
        }

        public void setAmbientLight(@Const ColorRGB color) {
            ambientColor = convertColor(color);
        }

        public void setLight(int light, GLLight glLight) {
            lights[light] = glLight;
        }

        public void renderBatch(int batch, StateNode currentNode, AppliedEffects effects,
                                HardwareAccessLayer access) {
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
                                                      1.0,
                                                      0.0,
                                                      (15.0 / (light.falloff * light.falloff)));
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

    private static Vector4 convertColor(ColorRGB color) {
        return new Vector4(color.redHDR(), color.greenHDR(), color.blueHDR(), 1.0);
    }
}
