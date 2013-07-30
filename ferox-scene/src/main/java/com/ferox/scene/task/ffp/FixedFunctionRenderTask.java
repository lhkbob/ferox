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

import com.ferox.math.*;
import com.ferox.math.bounds.Frustum;
import com.ferox.math.bounds.Frustum.FrustumIntersection;
import com.ferox.renderer.*;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.geom.Geometry;
import com.ferox.scene.*;
import com.ferox.scene.task.PVSResult;
import com.ferox.scene.task.light.LightGroupResult;
import com.ferox.util.Bag;
import com.ferox.util.HashFunction;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.property.Clone;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.property.ObjectProperty;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.*;
import java.util.concurrent.Future;

public class FixedFunctionRenderTask implements Task, ParallelAware {
    private static final Set<Class<? extends Component>> COMPONENTS;

    static {
        Set<Class<? extends Component>> types = new HashSet<>();
        types.add(AmbientLight.class);
        types.add(Light.class);
        types.add(Renderable.class);
        types.add(Transform.class);
        types.add(LambertianDiffuseModel.class);
        types.add(OrenNayarDiffuseModel.class);
        types.add(BlinnPhongSpecularModel.class);
        types.add(CookTorrenceSpecularModel.class);
        types.add(EmittedColor.class);
        types.add(DiffuseColorMap.class);
        types.add(EmittedColorMap.class);
        types.add(DecalColorMap.class);
        types.add(Transparent.class);
        types.add(Camera.class);
        types.add(AtmosphericFog.class);
        types.add(InfluenceRegion.class);
        COMPONENTS = Collections.unmodifiableSet(types);
    }

    private final Framework framework;
    private final boolean flush;

    // alternating frame storage so one frame can be prepared while the
    // other is being rendering
    private final Frame frameA;
    private final Frame frameB;

    private final int shadowmapTextureUnit;

    // per-frame data
    private List<PVSResult> cameraPVS;
    private List<PVSResult> lightPVS;
    private LightGroupResult lightGroups;

    private Frame inuseFrame;
    private final Queue<Future<Void>> previousFrame;

    private Renderable renderable;
    private LambertianDiffuseModel diffuseColor;
    private BlinnPhongSpecularModel specularColor;
    private EmittedColor emittedColor;
    private DiffuseColorMap diffuseTexture;
    private EmittedColorMap emittedTexture;
    private DecalColorMap decalTexture;
    private Transparent transparent;
    //    types.add(OrenNayarDiffuseModel.class);
    //    types.add(CookTorrenceSpecularModel.class);

    public FixedFunctionRenderTask(Framework framework) {
        this(framework, 1024, true);
    }

    public FixedFunctionRenderTask(Framework framework, int shadowMapSize, boolean flush) {
        if (framework == null) {
            throw new NullPointerException("Framework cannot be null");
        }

        Capabilities caps = framework.getCapabilities();

        this.framework = framework;
        this.flush = flush;

        previousFrame = new ArrayDeque<>();

        ShadowMapCache shadowMapA;
        ShadowMapCache shadowMapB;

        boolean shadowsRequested = shadowMapSize > 0; // size is positive
        boolean shadowSupport = ((caps.getFBOSupport() || caps.getPBufferSupport()));

        if (shadowsRequested && shadowSupport) {
            // convert size to a power of two
            int sz = 1;
            while (sz < shadowMapSize) {
                sz = sz << 1;
            }
            shadowMapA = new ShadowMapCache(framework, sz, sz);
            shadowMapB = new ShadowMapCache(framework, sz, sz);

            shadowmapTextureUnit = 3;
        } else {
            shadowMapA = null;
            shadowMapB = null;
            shadowmapTextureUnit = -1;
        }

        frameA = new Frame(shadowMapA, 0, 1, 2);
        frameB = new Frame(shadowMapB, 0, 1, 2);
    }

    @Override
    public void reset(EntitySystem system) {
        if (frameA.atoms == null) {
            frameA.decorate(system);
            frameB.decorate(system);
        }

        lightGroups = null;
        cameraPVS = new ArrayList<>();
        lightPVS = new ArrayList<>();
    }


    private ComponentIterator getIterator(EntitySystem system, Bag<Entity> pvs) {
        ComponentIterator it = system.fastIterator(pvs);
        renderable = it.addRequired(Renderable.class);
        diffuseColor = it.addOptional(LambertianDiffuseModel.class);
        specularColor = it.addOptional(BlinnPhongSpecularModel.class);
        emittedColor = it.addOptional(EmittedColor.class);
        emittedTexture = it.addOptional(EmittedColorMap.class);
        diffuseTexture = it.addOptional(DiffuseColorMap.class);
        decalTexture = it.addOptional(DecalColorMap.class);
        transparent = it.addOptional(Transparent.class);
        return it;
    }

    public void report(PVSResult pvs) {
        if (pvs.getSource().getType().equals(Camera.class)) {
            cameraPVS.add(pvs);
        } else if (Light.class.isAssignableFrom(pvs.getSource().getType())) {
            lightPVS.add(pvs);
        }
    }

    public void report(LightGroupResult r) {
        lightGroups = r;
    }

    @Override
    public Set<Class<? extends Component>> getAccessedComponents() {
        return COMPONENTS;
    }

    @Override
    public boolean isEntitySetModified() {
        return false;
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        Profiler.push("render");
        Frame currentFrame = (inuseFrame == frameA ? frameB : frameA);

        Profiler.push("shadow-map-scene");
        currentFrame.shadowMap.reset();
        for (PVSResult light : lightPVS) {
            currentFrame.shadowMap.cacheShadowScene(light);
        }
        Profiler.pop();

        // synchronize render atoms for all visible entities
        Profiler.push("state-sync");
        if (currentFrame.needsReset()) {
            currentFrame.resetStates();
        }

        RenderAtom atom;
        for (PVSResult visible : cameraPVS) {
            ComponentIterator it = getIterator(system, visible.getPotentiallyVisibleSet());
            while (it.next()) {
                atom = (RenderAtom) currentFrame.atoms.get(renderable.getIndex());
                if (atom == null) {
                    atom = new RenderAtom();
                    currentFrame.atoms.set(renderable.getIndex(), atom);
                }

                syncEntityState(atom, currentFrame);
            }
        }
        Profiler.pop();

        // all visible atoms have valid states now, so we build a 
        // postman sorted state node tree used for rendering
        Profiler.push("build-state-tree");
        List<com.ferox.renderer.Task<Void>> thisFrame = new ArrayList<>();
        for (PVSResult visible : cameraPVS) {
            Camera camera = (Camera) visible.getSource();
            thisFrame.add(buildTree(system, visible.getPotentiallyVisibleSet(), visible.getFrustum(),
                                    currentFrame, camera.getSurface()));
        }
        Profiler.pop();

        // block until the previous frame has completed, so we no its data
        // structures are no longer in use and we can swap which frame is active
        Profiler.push("block-opengl");
        while (!previousFrame.isEmpty()) {
            Future<Void> f = previousFrame.poll();
            try {
                f.get();
            } catch (Exception e) {
                throw new RuntimeException("Previous frame failed", e);
            }
        }
        Profiler.pop();

        // activate frame and queue all tasks
        inuseFrame = currentFrame;
        for (com.ferox.renderer.Task<Void> rf : thisFrame) {
            previousFrame.add(framework.invoke(rf));
        }

        Profiler.pop();
        return null;
    }

    private StateNode getFogNode(EntitySystem system, Frustum frustum) {
        AxisAlignedBox bounds = new AxisAlignedBox();

        FogState bestState = null;
        double bestVolume = Double.POSITIVE_INFINITY; // select min
        double bestLuminance = 0; // select max

        ComponentIterator it = system.fastIterator();
        AtmosphericFog fog = it.addRequired(AtmosphericFog.class);
        Transform transform = it.addRequired(Transform.class);
        InfluenceRegion influence = it.addOptional(InfluenceRegion.class);

        while (it.next()) {
            double volume = Double.POSITIVE_INFINITY;
            if (influence.isAlive()) {
                bounds.set(influence.getBounds());
                if (transform.isAlive()) {
                    bounds.transform(transform.getMatrix());
                }

                FrustumIntersection fi = frustum.intersects(bounds, null);
                double boundVolume = (bounds.max.x - bounds.min.x) * (bounds.max.y - bounds.min.y) *
                                     (bounds.max.z - bounds.min.z);
                if (influence.isNegated()) {
                    if (fi == FrustumIntersection.INSIDE) {
                        // view is inside negated region
                        continue;
                    }

                    volume = Double.MAX_VALUE - boundVolume;
                } else {
                    if (fi == FrustumIntersection.OUTSIDE) {
                        // view is outside of region
                        continue;
                    }

                    volume = boundVolume;
                }
            }

            // compare
            if (bestState == null || volume < bestVolume ||
                (Math.abs(volume - bestVolume) < .001 && fog.getColor().luminance() > bestLuminance)) {
                bestState = new FogState(fog.getColor(), fog.getFalloff(), fog.getOpaqueDistance());
                bestVolume = volume;
                bestLuminance = fog.getColor().luminance();
            }
        }

        return (bestState == null ? null : new StateNode(bestState));
    }

    private ColorRGB getAmbientColor(EntitySystem system, Frustum frustum) {
        AxisAlignedBox bounds = new AxisAlignedBox();
        ColorRGB bestColor = new ColorRGB();
        double bestVolume = Double.POSITIVE_INFINITY; // select min

        ComponentIterator it = system.fastIterator();
        AmbientLight light = it.addRequired(AmbientLight.class);
        Transform transform = it.addRequired(Transform.class);
        InfluenceRegion influence = it.addOptional(InfluenceRegion.class);

        while (it.next()) {
            double volume = Double.POSITIVE_INFINITY;
            if (influence.isAlive()) {
                bounds.set(influence.getBounds());
                if (transform.isAlive()) {
                    bounds.transform(transform.getMatrix());
                }

                FrustumIntersection fi = frustum.intersects(bounds, null);
                double boundVolume = (bounds.max.x - bounds.min.x) * (bounds.max.y - bounds.min.y) *
                                     (bounds.max.z - bounds.min.z);
                if (influence.isNegated()) {
                    if (fi == FrustumIntersection.INSIDE) {
                        // view is inside negated region
                        continue;
                    }

                    volume = Double.MAX_VALUE - boundVolume;
                } else {
                    if (fi == FrustumIntersection.OUTSIDE) {
                        // view is outside of region
                        continue;
                    }

                    volume = boundVolume;
                }
            }

            // compare
            if (volume < bestVolume || (Math.abs(volume - bestVolume) < .001 &&
                                        light.getColor().luminance() > bestColor.luminance())) {
                bestColor.set(light.getColor());
                bestVolume = volume;
            }
        }

        return bestColor;
    }

    private com.ferox.renderer.Task<Void> buildTree(EntitySystem system, Bag<Entity> pvs,
                                                    final Frustum camera, Frame frame,
                                                    final Surface surface) {
        // static tree construction that doesn't depend on entities
        final StateNode root = new StateNode(new CameraState(camera)); // children = (opaque, trans) or fog
        StateNode fogNode = getFogNode(system, camera); // children = (opaque, trans) if non-null
        StateNode opaqueNode = new StateNode(TransparentState.OPAQUE);

        // opaque node must be an earlier child node than transparent so atoms
        // are rendered in the correct order
        if (fogNode == null) {
            root.addChild(opaqueNode);
        } else {
            root.addChild(fogNode);
            fogNode.addChild(opaqueNode);
        }

        // lit and unlit state nodes for opaque node
        StateNode litNode = new StateNode(LightingState.LIT); // child = shadowmap state
        StateNode unlitNode = new StateNode(LightingState.UNLIT); // child = texture states
        StateNode smNode = new StateNode(
                new ShadowMapState(frame.shadowMap, shadowmapTextureUnit)); // children = light groups

        opaqueNode.addChild(unlitNode);
        opaqueNode.addChild(litNode);
        litNode.addChild(smNode);

        // insert light group nodes
        ColorRGB ambientColor = getAmbientColor(system, camera);
        IntProperty groupAssgn = lightGroups.getAssignmentProperty();
        for (int i = 0; i < lightGroups.getGroupCount(); i++) {
            smNode.setChild(i, new StateNode(
                    new LightGroupState(lightGroups.getGroup(i), frame.shadowMap.getShadowCastingLights(),
                                        ambientColor, 8), frame.textureStates.count()));
        }

        // bag to collect transparent entities for sorting
        Bag<Entity> transparentEntities = new Bag<>();

        ComponentIterator it = system.fastIterator(pvs);
        Renderable renderable = it.addRequired(Renderable.class);
        Transform transform = it.addRequired(Transform.class);
        while (it.next()) {
            RenderAtom atom = (RenderAtom) frame.atoms.get(renderable.getIndex());

            if (atom.transparentVersion >= 0) {
                // has transparency, assume it must be rendered in depth correct order
                transparentEntities.add(renderable.getEntity());
                continue;
            }

            // first node is either the proper light group, or the unlit node
            StateNode firstNode = (atom.diffuseColorVersion >= 0 || atom.specularColorVersion >= 0 ? smNode
                    .getChild(groupAssgn.get(renderable.getIndex())) : unlitNode);
            addOpaqueAtom(transform, atom, firstNode, frame);
        }

        if (!transparentEntities.isEmpty()) {
            // now depth sort all transparent entities by depth
            Profiler.push("transparent-depth-sort");
            transparentEntities.sort(new HashFunction<Entity>() {
                @Override
                public int hashCode(Entity value) {
                    Matrix4 m = value.get(Transform.class).getMatrix();
                    Matrix4 v = camera.getViewMatrix();
                    // transformed z value is 3rd row of view dotted with 4th col of the model
                    double cameraDepth = m.m03 * v.m20 + m.m13 * v.m21 + m.m23 * v.m22 + m.m33 + v.m23;
                    return Functions.sortableFloatToIntBits((float) cameraDepth);
                }
            });
            Profiler.pop();

            // build subtree for lighting within transparent state
            Profiler.push("transparent-tree");
            if (!transparentEntities.isEmpty()) {
                StateNode transparentRoot = new StateNode(NullState.INSTANCE, transparentEntities.size());
                if (fogNode == null) {
                    root.addChild(transparentRoot);
                } else {
                    fogNode.addChild(transparentRoot);
                }

                boolean nodeIsAdditive = false;
                boolean nodeIsLit = false;
                StateNode lastNode = null;

                // insert sorted entities into the transparent node of the tree, but
                // they cannot share nodes so that their depth order is preserved
                it = system.fastIterator(transparentEntities);
                renderable = it.addRequired(Renderable.class);
                transform = it.addRequired(Transform.class);
                while (it.next()) {
                    RenderAtom atom = (RenderAtom) frame.atoms.get(renderable.getIndex());

                    // FIXME track lit state of atom better, especially once we get more color models in
                    boolean litUpdate = (nodeIsLit ? atom.diffuseColorVersion < 0 &&
                                                     atom.specularColorVersion < 0
                                                   : atom.diffuseColorVersion >= 0 ||
                                                     atom.specularColorVersion >= 0);
                    boolean blendUpdate = (nodeIsAdditive ? !atom.additiveBlending : atom.additiveBlending);
                    if (lastNode == null || litUpdate || blendUpdate) {
                        StateNode blendNode = new StateNode(
                                atom.additiveBlending ? TransparentState.ADDITIVE : TransparentState.NORMAL,
                                1);
                        transparentRoot.addChild(blendNode);

                        if (atom.diffuseColorVersion >= 0 || atom.specularColorVersion >= 0) {
                            StateNode lit = new StateNode(LightingState.LIT, 1);
                            blendNode.addChild(lit);
                            StateNode sm = new StateNode(
                                    smNode.getChild(groupAssgn.get(renderable.getIndex())).getState(), 1);
                            lit.addChild(sm);

                            lastNode = sm;
                        } else {
                            StateNode unlit = new StateNode(LightingState.UNLIT, 1);
                            blendNode.addChild(unlit);

                            lastNode = unlit;
                        }

                        nodeIsLit = atom.diffuseColorVersion >= 0 || atom.specularColorVersion >= 0;
                        nodeIsAdditive = atom.additiveBlending;
                    }

                    addTransparentAtom(transform, atom, renderable.getGeometry().getVertices(), lastNode,
                                       frame);
                }
            }
            Profiler.pop();
        }

        // every entity in the PVS has been put into the tree, which is automatically
        // clustered by the defined state hierarchy
        return new com.ferox.renderer.Task<Void>() {
            @Override
            public Void run(HardwareAccessLayer access) {
                Context ctx = access.setActiveSurface(surface);
                if (ctx != null) {
                    Profiler.push("render-tree");
                    FixedFunctionRenderer ffp = ctx.getFixedFunctionRenderer();
                    // FIXME clear color should be configurable somehow
                    ffp.clear(true, true, true, new Vector4(0, 0, 0, 1.0), 1, 0);
                    ffp.setDrawStyle(DrawStyle.SOLID, DrawStyle.SOLID);
                    root.visit(new AppliedEffects(), access);
                    Profiler.pop();

                    count++;
                    if (count > 100) {
                        Profiler.getDataSnapshot().print(System.out);
                        count = 0;
                    }
                    if (flush) {
                        ctx.flush();
                    }
                }
                return null;
            }
        };
    }

    static int count = 0;

    private void addTransparentAtom(Transform transform, RenderAtom atom, VertexAttribute vertices,
                                    StateNode firstNode, Frame frame) {
        // texture state
        StateNode texNode = new StateNode(frame.textureStates.getState(atom.textureStateIndex), 1);
        firstNode.addChild(texNode);

        // geometry state
        StateNode geomNode = new StateNode(frame.geometryStates.getState(atom.geometryStateIndex), 1);
        texNode.setChild(0, geomNode);

        // color state
        StateNode colorNode = new StateNode(frame.colorStates.getState(atom.colorStateIndex), 1);
        geomNode.setChild(0, colorNode);

        // transparent render state
        StateNode renderNode = new StateNode(frame.renderStates.getState(atom.renderStateIndex)
                                                  .newTransparentRenderState(vertices, frame.sortedIndices));
        colorNode.setChild(0, renderNode);

        // now record transform into the state
        ((RenderState) renderNode.getState()).add(transform.getMatrix());
    }

    private void addOpaqueAtom(Transform transform, RenderAtom atom, StateNode firstNode, Frame frame) {
        // texture state
        StateNode texNode = firstNode.getChild(atom.textureStateIndex);
        if (texNode == null) {
            texNode = new StateNode(frame.textureStates.getState(atom.textureStateIndex),
                                    frame.geometryStates.count());
            firstNode.setChild(atom.textureStateIndex, texNode);
        }

        // geometry state
        StateNode geomNode = texNode.getChild(atom.geometryStateIndex);
        if (geomNode == null) {
            geomNode = new StateNode(frame.geometryStates.getState(atom.geometryStateIndex),
                                     frame.colorStates.count());
            texNode.setChild(atom.geometryStateIndex, geomNode);
        }

        // color state
        StateNode colorNode = geomNode.getChild(atom.colorStateIndex);
        if (colorNode == null) {
            colorNode = new StateNode(frame.colorStates.getState(atom.colorStateIndex),
                                      frame.renderStates.count());
            geomNode.setChild(atom.colorStateIndex, colorNode);
        }

        // render state
        StateNode renderNode = colorNode.getChild(atom.renderStateIndex);
        if (renderNode == null) {
            // must clone the geometry since each node accumulates its own
            // packed transforms that must be rendered
            renderNode = new StateNode(
                    frame.renderStates.getState(atom.renderStateIndex).newOpaqueRenderState());
            colorNode.setChild(atom.renderStateIndex, renderNode);
        }

        // now record the transform into the render node's state
        ((RenderState) renderNode.getState()).add(transform.getMatrix());
    }

    private void syncEntityState(RenderAtom atom, Frame frame) {
        // sync render state and draw state
        int newRenderableVersion = renderable.getVersion();
        if (newRenderableVersion != atom.renderableVersion || atom.renderStateIndex < 0 ||
            atom.geometryStateIndex < 0) {
            atom.renderStateIndex = frame.getRenderState(renderable, atom.renderStateIndex);
            atom.geometryStateIndex = frame.getGeometryState(renderable, atom.geometryStateIndex);
        }

        // sync texture state
        int newDiffuseTexVersion = (diffuseTexture.isAlive() ? diffuseTexture.getVersion() : -1);
        int newDecalTexVersion = (decalTexture.isAlive() ? decalTexture.getVersion() : -1);
        int newEmittedTexVersion = (emittedTexture.isAlive() ? emittedTexture.getVersion() : -1);
        if (newDiffuseTexVersion != atom.diffuseTextureVersion ||
            newDecalTexVersion != atom.decalTextureVersion ||
            newEmittedTexVersion != atom.emittedTextureVersion ||
            atom.textureStateIndex < 0) {
            atom.textureStateIndex = frame
                    .getTextureState(diffuseTexture, decalTexture, emittedTexture, renderable,
                                     atom.textureStateIndex);
        }

        // sync color state
        int newTransparentVersion = (transparent.isAlive() ? transparent.getVersion() : -1);
        int newDiffuseColorVersion = (diffuseColor.isAlive() ? diffuseColor.getVersion() : -1);
        int newSpecularColorVersion = (specularColor.isAlive() ? specularColor.getVersion() : -1);
        int newEmittedColorVersion = (emittedColor.isAlive() ? emittedColor.getVersion() : -1);
        if (newTransparentVersion != atom.transparentVersion ||
            newDiffuseColorVersion != atom.diffuseColorVersion ||
            newSpecularColorVersion != atom.specularColorVersion ||
            newEmittedColorVersion != atom.emittedColorVersion ||
            atom.colorStateIndex < 0) {
            atom.colorStateIndex = frame.getColorState(diffuseColor, specularColor, emittedColor, transparent,
                                                       atom.colorStateIndex);
        }

        atom.additiveBlending = transparent.isAlive() && transparent.isAdditive();

        // record new versions
        atom.renderableVersion = newRenderableVersion;
        atom.diffuseTextureVersion = newDiffuseTexVersion;
        atom.decalTextureVersion = newDecalTexVersion;
        atom.emittedTextureVersion = newEmittedTexVersion;
        atom.transparentVersion = newTransparentVersion;
        atom.diffuseColorVersion = newDiffuseColorVersion;
        atom.specularColorVersion = newSpecularColorVersion;
        atom.emittedColorVersion = newEmittedColorVersion;
    }

    private static class Frame {
        final ShadowMapCache shadowMap;
        final ElementBuffer sortedIndices;

        StateCache<TextureState> textureStates;
        StateCache<GeometryState> geometryStates;
        StateCache<ColorState> colorStates;
        StateCache<IndexBufferState> renderStates;

        // per-entity tracking
        ObjectProperty atoms;

        private final int diffuseTextureUnit;
        private final int emissiveTextureUnit;
        private final int decalTextureUnit;

        Frame(ShadowMapCache map, int diffuseTextureUnit, int decalTextureUnit, int emissiveTextureUnit) {
            shadowMap = map;

            this.diffuseTextureUnit = diffuseTextureUnit;
            this.decalTextureUnit = decalTextureUnit;
            this.emissiveTextureUnit = emissiveTextureUnit;

            textureStates = new StateCache<>(TextureState.class);
            geometryStates = new StateCache<>(GeometryState.class);
            colorStates = new StateCache<>(ColorState.class);
            renderStates = new StateCache<>(IndexBufferState.class);

            //            sortedIndices = new VertexBufferObject(new BufferData(new int[1]), StorageMode.GPU_DYNAMIC);
            //            sortedIndices.setUpdatePolicy(UpdatePolicy.MANUAL);
            sortedIndices = null;
        }

        int getTextureState(DiffuseColorMap diffuse, DecalColorMap decal, EmittedColorMap emitted,
                            Renderable renderable, int oldIndex) {
            Texture diffuseTex = (diffuse.isAlive() ? diffuse.getTexture() : null);
            Texture decalTex = (decal.isAlive() ? decal.getTexture() : null);
            Texture emittedTex = (emitted.isAlive() ? emitted.getTexture() : null);
            VertexAttribute texCoord = renderable.getGeometry().getTextureCoordinates();

            TextureState state = new TextureState(diffuseTextureUnit, decalTextureUnit, emissiveTextureUnit);
            state.set(diffuseTex, decalTex, emittedTex, texCoord);

            return textureStates.getStateIndex(state, oldIndex);
        }

        int getGeometryState(Renderable renderable, int oldIndex) {
            Geometry g = renderable.getGeometry();
            GeometryState state = new GeometryState();
            state.set(g.getVertices(), g.getNormals(), renderable.getFrontDrawStyle(),
                      renderable.getBackDrawStyle());

            return geometryStates.getStateIndex(state, oldIndex);
        }

        int getColorState(LambertianDiffuseModel diffuse, BlinnPhongSpecularModel specular,
                          EmittedColor emitted, Transparent transparent, int oldIndex) {
            double alpha = (transparent.isAlive() ? transparent.getOpacity() : 1.0);
            double shininess = 0.0;
            ColorRGB d = (diffuse.isAlive() ? diffuse.getColor() : null);
            ColorRGB s = null;
            ColorRGB e = (emitted.isAlive() ? emitted.getColor() : null);

            if (specular.isAlive()) {
                shininess = specular.getShininess();
                s = specular.getColor();
            }

            ColorState state = new ColorState();
            state.set(d, s, e, alpha, shininess);

            return colorStates.getStateIndex(state, oldIndex);
        }

        int getRenderState(Renderable renderable, int oldIndex) {
            Geometry g = renderable.getGeometry();
            IndexBufferState state = new IndexBufferState();
            state.set(g.getPolygonType(), g.getIndices(), g.getIndexOffset(), g.getIndexCount());

            return renderStates.getStateIndex(state, oldIndex);
        }

        void resetStates() {
            textureStates = new StateCache<>(TextureState.class);
            geometryStates = new StateCache<>(GeometryState.class);
            colorStates = new StateCache<>(ColorState.class);
            renderStates = new StateCache<>(IndexBufferState.class);

            // clearing the render atoms effectively invalidates all of the
            // version tracking we do as well
            Arrays.fill(atoms.getIndexedData(), null);
        }

        boolean needsReset() {
            return textureStates.needsReset() || geometryStates.needsReset() ||
                   colorStates.needsReset() || renderStates.needsReset();
        }

        @SuppressWarnings("unchecked")
        void decorate(EntitySystem system) {
            atoms = system.decorate(Renderable.class, new ObjectProperty.Factory(Clone.Policy.DISABLE));
        }
    }

    private static class RenderAtom {
        // state indices
        int textureStateIndex = -1; // depends on the 3 texture versions
        int colorStateIndex = -1; // depends on blinnphong-material and 3 color versions
        int geometryStateIndex = -1; // depends on renderable, blinnphong-material
        int renderStateIndex = -1; // depends on indices within renderable

        // component versions
        int renderableVersion = -1;
        int diffuseColorVersion = -1;
        int emittedColorVersion = -1;
        int specularColorVersion = -1;
        int diffuseTextureVersion = -1;
        int emittedTextureVersion = -1;
        int decalTextureVersion = -1;
        int transparentVersion = -1;

        boolean additiveBlending = false;
    }
}
