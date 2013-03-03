package com.ferox.scene.task.ffp;

import com.ferox.math.*;
import com.ferox.math.bounds.Frustum;
import com.ferox.math.bounds.Frustum.FrustumIntersection;
import com.ferox.renderer.*;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.resource.BufferData;
import com.ferox.resource.Resource.UpdatePolicy;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.VertexBufferObject.StorageMode;
import com.ferox.scene.*;
import com.ferox.scene.task.PVSResult;
import com.ferox.scene.task.light.LightGroupResult;
import com.ferox.util.Bag;
import com.ferox.util.HashFunction;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.property.ObjectProperty;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.*;
import java.util.concurrent.Future;

public class FixedFunctionRenderTask implements Task, ParallelAware {
    private static final Set<Class<? extends ComponentData<?>>> COMPONENTS;

    static {
        Set<Class<? extends ComponentData<?>>> types = new HashSet<Class<? extends ComponentData<?>>>();
        types.add(AmbientLight.class);
        types.add(DirectionLight.class);
        types.add(SpotLight.class);
        types.add(PointLight.class);
        types.add(Renderable.class);
        types.add(Transform.class);
        types.add(BlinnPhongMaterial.class);
        types.add(DiffuseColor.class);
        types.add(EmittedColor.class);
        types.add(SpecularColor.class);
        types.add(DiffuseColorMap.class);
        types.add(EmittedColorMap.class);
        types.add(SpecularColorMap.class);
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

    // cached local instances
    private Camera camera;
    private Transform transform;

    private AmbientLight ambientLight;
    private DirectionLight directionLight;
    private SpotLight spotLight;
    private PointLight pointLight;

    private Renderable renderable;
    private BlinnPhongMaterial blinnPhong;
    private DiffuseColor diffuseColor;
    private SpecularColor specularColor;
    private EmittedColor emittedColor;
    private DiffuseColorMap diffuseTexture;
    private DecalColorMap decalTexture;
    private EmittedColorMap emittedTexture;
    private Transparent transparent;
    private AtmosphericFog fog;
    private InfluenceRegion influence;

    private ComponentIterator fogIterator;

    public FixedFunctionRenderTask(Framework framework) {
        this(framework, 1024, true);
    }

    public FixedFunctionRenderTask(Framework framework, int shadowMapSize,
                                   boolean flush) {
        if (framework == null) {
            throw new NullPointerException("Framework cannot be null");
        }

        RenderCapabilities caps = framework.getCapabilities();
        if (!caps.hasFixedFunctionRenderer()) {
            throw new IllegalArgumentException(
                    "Framework must support a FixedFunctionRenderer");
        }

        this.framework = framework;
        this.flush = flush;

        previousFrame = new ArrayDeque<Future<Void>>();

        ShadowMapCache shadowMapA;
        ShadowMapCache shadowMapB;

        int numTex = caps.getMaxFixedPipelineTextures();
        boolean shadowsRequested = shadowMapSize > 0; // size is positive
        boolean shadowSupport = ((caps.getFboSupport() || caps.getPbufferSupport()) &&
                                 numTex > 1 && caps.getDepthTextureSupport());

        if (shadowsRequested && shadowSupport) {
            // convert size to a power of two
            int sz = 1;
            while (sz < shadowMapSize) {
                sz = sz << 1;
            }
            shadowMapA = new ShadowMapCache(framework, sz, sz);
            shadowMapB = new ShadowMapCache(framework, sz, sz);

            // use the 4th unit if available, or the last unit if we're under
            shadowmapTextureUnit = Math.max(numTex, 3) - 1;
            // reserve one unit for the shadow map
            numTex--;
        } else {
            shadowMapA = null;
            shadowMapB = null;
            shadowmapTextureUnit = -1;
        }

        int diffuseTextureUnit, emissiveTextureUnit, decalTextureUnit;
        if (numTex >= 3) {
            diffuseTextureUnit = 0;
            emissiveTextureUnit = 2;
            decalTextureUnit = 1;
        } else if (numTex == 2) {
            diffuseTextureUnit = 0;
            // merge emissive and decal units
            emissiveTextureUnit = 1;
            decalTextureUnit = 1;
        } else if (numTex == 1) {
            // merge all units
            diffuseTextureUnit = 0;
            emissiveTextureUnit = 0;
            decalTextureUnit = 0;
        } else {
            // disable texturing
            diffuseTextureUnit = -1;
            emissiveTextureUnit = -1;
            decalTextureUnit = -1;
        }

        frameA = new Frame(shadowMapA, diffuseTextureUnit, decalTextureUnit,
                           emissiveTextureUnit);
        frameB = new Frame(shadowMapB, diffuseTextureUnit, decalTextureUnit,
                           emissiveTextureUnit);
    }

    @Override
    public void reset(EntitySystem system) {
        if (transform == null) {
            transform = system.createDataInstance(Transform.class);
            camera = system.createDataInstance(Camera.class);

            ambientLight = system.createDataInstance(AmbientLight.class);
            directionLight = system.createDataInstance(DirectionLight.class);
            spotLight = system.createDataInstance(SpotLight.class);
            pointLight = system.createDataInstance(PointLight.class);

            renderable = system.createDataInstance(Renderable.class);
            blinnPhong = system.createDataInstance(BlinnPhongMaterial.class);
            diffuseColor = system.createDataInstance(DiffuseColor.class);
            specularColor = system.createDataInstance(SpecularColor.class);
            emittedColor = system.createDataInstance(EmittedColor.class);
            diffuseTexture = system.createDataInstance(DiffuseColorMap.class);
            decalTexture = system.createDataInstance(DecalColorMap.class);
            emittedTexture = system.createDataInstance(EmittedColorMap.class);
            transparent = system.createDataInstance(Transparent.class);
            fog = system.createDataInstance(AtmosphericFog.class);
            influence = system.createDataInstance(InfluenceRegion.class);

            fogIterator = new ComponentIterator(system).addRequired(fog)
                                                       .addOptional(transform)
                                                       .addOptional(influence);

            frameA.decorate(system);
            frameB.decorate(system);
        }

        lightGroups = null;
        cameraPVS = new ArrayList<PVSResult>();
        lightPVS = new ArrayList<PVSResult>();
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
    public Set<Class<? extends ComponentData<?>>> getAccessedComponents() {
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
            for (Entity e : visible.getPotentiallyVisibleSet()) {
                e.get(renderable); // guaranteed that this one is present
                atom = currentFrame.atoms.get(renderable.getIndex());

                if (atom == null) {
                    atom = new RenderAtom();
                    currentFrame.atoms.set(atom, renderable.getIndex());
                }

                syncEntityState(e, atom, currentFrame);
            }
        }
        Profiler.pop();

        // all visible atoms have valid states now, so we build a 
        // postman sorted state node tree used for rendering
        Profiler.push("build-state-tree");
        List<com.ferox.renderer.Task<Void>> thisFrame = new ArrayList<com.ferox.renderer.Task<Void>>();
        for (PVSResult visible : cameraPVS) {
            visible.getSource().getEntity().get(camera);
            thisFrame.add(buildTree(visible.getPotentiallyVisibleSet(),
                                    visible.getFrustum(), currentFrame,
                                    camera.getSurface()));
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
            previousFrame.add(framework.queue(rf));
        }

        Profiler.pop();
        return null;
    }

    private StateNode getFogNode(Frustum frustum) {
        AxisAlignedBox bounds = new AxisAlignedBox();

        FogState bestState = null;
        double bestVolume = Double.POSITIVE_INFINITY; // select min
        double bestLuminance = 0; // select max

        fogIterator.reset();
        while (fogIterator.next()) {
            double volume = Double.POSITIVE_INFINITY;
            if (influence.isEnabled()) {
                bounds.set(influence.getBounds());
                if (transform.isEnabled()) {
                    bounds.transform(transform.getMatrix());
                }

                FrustumIntersection fi = frustum.intersects(bounds, null);
                double boundVolume =
                        (bounds.max.x - bounds.min.x) * (bounds.max.y - bounds.min.y) *
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
                (Math.abs(volume - bestVolume) < .001 &&
                 fog.getColor().luminance() > bestLuminance)) {
                bestState = new FogState(fog.getColor(), fog.getFalloff(),
                                         fog.getOpaqueDistance());
                bestVolume = volume;
                bestLuminance = fog.getColor().luminance();
            }
        }

        return (bestState == null ? null : new StateNode(bestState));
    }

    private com.ferox.renderer.Task<Void> buildTree(Bag<Entity> pvs, final Frustum camera,
                                                    Frame frame, final Surface surface) {
        // static tree construction that doesn't depend on entities
        final StateNode root = new StateNode(
                new CameraState(camera)); // children = (opaque, trans) or fog
        StateNode fogNode = getFogNode(camera); // children = (opaque, trans) if non-null
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
        StateNode unlitNode = new StateNode(
                LightingState.UNLIT); // child = texture states
        StateNode smNode = new StateNode(new ShadowMapState(frame.shadowMap,
                                                            shadowmapTextureUnit)); // children = light groups

        opaqueNode.addChild(unlitNode);
        opaqueNode.addChild(litNode);
        litNode.addChild(smNode);

        // insert light group nodes
        IntProperty groupAssgn = lightGroups.getAssignmentProperty();
        for (int i = 0; i < lightGroups.getGroupCount(); i++) {
            smNode.setChild(i, new StateNode(new LightGroupState(lightGroups.getGroup(i),
                                                                 frame.shadowMap
                                                                      .getShadowCastingLights(),
                                                                 framework
                                                                         .getCapabilities()
                                                                         .getMaxActiveLights(),
                                                                 directionLight,
                                                                 spotLight, pointLight,
                                                                 ambientLight, transform),
                                             frame.textureStates.count()));
        }

        // bag to collect transparent entities for sorting
        Bag<Entity> transparentEntities = new Bag<Entity>();

        RenderAtom atom;
        for (Entity e : pvs) {
            e.get(renderable);
            atom = frame.atoms.get(renderable.getIndex());

            if (atom.transparentVersion >= 0) {
                // has transparency, assume it must be rendered in depth correct order
                transparentEntities.add(e);
                continue;
            }

            // first node is either the proper light group, or the unlit node
            StateNode firstNode = (atom.blinnPhongVersion >= 0 ? smNode
                    .getChild(groupAssgn.get(renderable.getIndex())) : unlitNode);
            addOpaqueAtom(e, atom, firstNode, frame, groupAssgn);
        }

        if (!transparentEntities.isEmpty()) {
            // now depth sort all transparent entities by depth
            Profiler.push("transparent-depth-sort");
            transparentEntities.sort(new HashFunction<Entity>() {
                @Override
                public int hashCode(Entity value) {
                    value.get(transform);
                    Matrix4 m = transform.getMatrix();
                    Matrix4 v = camera.getViewMatrix();
                    // transformed z value is 3rd row of view dotted with 4th col of the model
                    double cameraDepth =
                            m.m03 * v.m20 + m.m13 * v.m21 + m.m23 * v.m22 + m.m33 + v.m23;
                    return Functions.sortableFloatToIntBits((float) cameraDepth);
                }
            });
            Profiler.pop();

            // build subtree for lighting within transparent state
            Profiler.push("transparent-tree");
            StateNode transparentRoot = new StateNode(NullState.INSTANCE,
                                                      transparentEntities.size());
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
            for (Entity e : transparentEntities) {
                e.get(renderable);
                atom = frame.atoms.get(renderable.getIndex());

                boolean litUpdate = (nodeIsLit ? atom.blinnPhongVersion < 0
                                               : atom.blinnPhongVersion >= 0);
                boolean blendUpdate = (nodeIsAdditive ? !atom.additiveBlending
                                                      : atom.additiveBlending);
                if (lastNode == null || litUpdate || blendUpdate) {
                    StateNode blendNode = new StateNode(
                            atom.additiveBlending ? TransparentState.ADDITIVE
                                                  : TransparentState.NORMAL, 1);
                    transparentRoot.addChild(blendNode);

                    if (atom.blinnPhongVersion >= 0) {
                        StateNode lit = new StateNode(LightingState.LIT, 1);
                        blendNode.addChild(lit);
                        StateNode sm = new StateNode(
                                smNode.getChild(groupAssgn.get(renderable.getIndex()))
                                      .getState(), 1);
                        lit.addChild(sm);

                        lastNode = sm;
                    } else {
                        StateNode unlit = new StateNode(LightingState.UNLIT, 1);
                        blendNode.addChild(unlit);

                        lastNode = unlit;
                    }

                    nodeIsLit = atom.blinnPhongVersion >= 0;
                    nodeIsAdditive = atom.additiveBlending;
                }

                addTransparentAtom(e, atom, renderable.getVertices(), lastNode, frame,
                                   groupAssgn);
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

    private void addTransparentAtom(Entity e, RenderAtom atom, VertexAttribute vertices,
                                    StateNode firstNode, Frame frame,
                                    IntProperty groupAssgn) {
        // texture state
        StateNode texNode = new StateNode(
                frame.textureStates.getState(atom.textureStateIndex), 1);
        firstNode.addChild(texNode);

        // geometry state
        StateNode geomNode = new StateNode(
                frame.geometryStates.getState(atom.geometryStateIndex), 1);
        texNode.setChild(0, geomNode);

        // color state
        StateNode colorNode = new StateNode(
                frame.colorStates.getState(atom.colorStateIndex), 1);
        geomNode.setChild(0, colorNode);

        // transparent render state
        StateNode renderNode = new StateNode(
                frame.renderStates.getState(atom.renderStateIndex)
                     .newTransparentRenderState(vertices, frame.sortedIndices));
        colorNode.setChild(0, renderNode);

        // now record transform into the state
        e.get(transform);
        ((RenderState) renderNode.getState()).add(transform.getMatrix());
    }

    private void addOpaqueAtom(Entity e, RenderAtom atom, StateNode firstNode,
                               Frame frame, IntProperty groupAssgn) {

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
            geomNode = new StateNode(
                    frame.geometryStates.getState(atom.geometryStateIndex),
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
            renderNode = new StateNode(frame.renderStates.getState(atom.renderStateIndex)
                                            .newOpaqueRenderState());
            colorNode.setChild(atom.renderStateIndex, renderNode);
        }

        // now record the transform into the render node's state
        e.get(transform);
        ((RenderState) renderNode.getState()).add(transform.getMatrix());
    }

    private void syncEntityState(Entity e, RenderAtom atom, Frame frame) {
        // sync render state and draw state
        int newRenderableVersion = (e.get(renderable) ? renderable.getVersion() : -1);
        if (newRenderableVersion != atom.renderableVersion || atom.renderStateIndex < 0) {
            atom.renderStateIndex = frame
                    .getRenderState(renderable, atom.renderStateIndex);
        }

        // sync geometry state
        int newBlinnPhongVersion = (e.get(blinnPhong) ? blinnPhong.getVersion() : -1);
        if (atom.blinnPhongVersion != newBlinnPhongVersion ||
            atom.renderableVersion != newRenderableVersion ||
            atom.geometryStateIndex < 0) {
            atom.geometryStateIndex = frame
                    .getGeometryState(renderable, blinnPhong, atom.geometryStateIndex);
        }

        // sync texture state
        int newDiffuseTexVersion = (e.get(diffuseTexture) ? diffuseTexture.getVersion()
                                                          : -1);
        int newDecalTexVersion = (e.get(decalTexture) ? decalTexture.getVersion() : -1);
        int newEmittedTexVersion = (e.get(emittedTexture) ? emittedTexture.getVersion()
                                                          : -1);
        if (newDiffuseTexVersion != atom.diffuseTextureVersion ||
            newDecalTexVersion != atom.decalTextureVersion ||
            newEmittedTexVersion != atom.emittedTextureVersion ||
            atom.textureStateIndex < 0) {
            atom.textureStateIndex = frame
                    .getTextureState(diffuseTexture, decalTexture, emittedTexture,
                                     atom.textureStateIndex);
        }

        // sync color state
        int newTransparentVersion = (e.get(transparent) ? transparent.getVersion() : -1);
        int newDiffuseColorVersion = (e.get(diffuseColor) ? diffuseColor.getVersion()
                                                          : -1);
        int newSpecularColorVersion = (e.get(specularColor) ? specularColor.getVersion()
                                                            : -1);
        int newEmittedColorVersion = (e.get(emittedColor) ? emittedColor.getVersion()
                                                          : -1);
        if (newTransparentVersion != atom.transparentVersion ||
            newDiffuseColorVersion != atom.diffuseColorVersion ||
            newSpecularColorVersion != atom.specularColorVersion ||
            newEmittedColorVersion != atom.emittedColorVersion ||
            atom.colorStateIndex < 0) {
            atom.colorStateIndex = frame
                    .getColorState(diffuseColor, specularColor, emittedColor, transparent,
                                   blinnPhong, atom.colorStateIndex);
        }

        atom.additiveBlending = transparent.isEnabled() && transparent.isAdditive();

        // record new versions
        atom.renderableVersion = newRenderableVersion;
        atom.blinnPhongVersion = newBlinnPhongVersion;
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
        final VertexBufferObject sortedIndices;

        StateCache<TextureState> textureStates;
        StateCache<GeometryState> geometryStates;
        StateCache<ColorState> colorStates;
        StateCache<IndexBufferState> renderStates;

        // per-entity tracking
        ObjectProperty<RenderAtom> atoms;

        private final int diffuseTextureUnit;
        private final int emissiveTextureUnit;
        private final int decalTextureUnit;

        Frame(ShadowMapCache map, int diffuseTextureUnit, int decalTextureUnit,
              int emissiveTextureUnit) {
            shadowMap = map;

            this.diffuseTextureUnit = diffuseTextureUnit;
            this.decalTextureUnit = decalTextureUnit;
            this.emissiveTextureUnit = emissiveTextureUnit;

            textureStates = new StateCache<TextureState>(TextureState.class);
            geometryStates = new StateCache<GeometryState>(GeometryState.class);
            colorStates = new StateCache<ColorState>(ColorState.class);
            renderStates = new StateCache<IndexBufferState>(IndexBufferState.class);

            sortedIndices = new VertexBufferObject(new BufferData(new int[1]),
                                                   StorageMode.GPU_DYNAMIC);
            sortedIndices.setUpdatePolicy(UpdatePolicy.MANUAL);
        }

        int getTextureState(DiffuseColorMap diffuse, DecalColorMap decal,
                            EmittedColorMap emitted, int oldIndex) {
            Texture diffuseTex = null;
            Texture decalTex = null;
            Texture emittedTex = null;

            VertexAttribute diffuseCoord = null;
            VertexAttribute decalCoord = null;
            VertexAttribute emittedCoord = null;

            if (diffuse.isEnabled()) {
                diffuseTex = diffuse.getTexture();
                diffuseCoord = diffuse.getTextureCoordinates();
            }
            if (decal.isEnabled()) {
                decalTex = decal.getTexture();
                decalCoord = decal.getTextureCoordinates();
            }
            if (emitted.isEnabled()) {
                emittedTex = emitted.getTexture();
                emittedCoord = emitted.getTextureCoordinates();
            }

            TextureState state = new TextureState(diffuseTextureUnit, decalTextureUnit,
                                                  emissiveTextureUnit);
            state.set(diffuseTex, diffuseCoord, decalTex, decalCoord, emittedTex,
                      emittedCoord);

            return textureStates.getStateIndex(state, oldIndex);
        }

        int getGeometryState(Renderable renderable, BlinnPhongMaterial blinnPhong,
                             int oldIndex) {
            // we can assume that the renderable is always valid, since
            // we're processing renderable entities
            VertexAttribute norms = (blinnPhong.isEnabled() ? blinnPhong.getNormals()
                                                            : null);

            GeometryState state = new GeometryState();
            state.set(renderable.getVertices(), norms, renderable.getFrontDrawStyle(),
                      renderable.getBackDrawStyle());

            return geometryStates.getStateIndex(state, oldIndex);
        }

        int getColorState(DiffuseColor diffuse, SpecularColor specular,
                          EmittedColor emitted, Transparent transparent,
                          BlinnPhongMaterial blinnPhong, int oldIndex) {
            double alpha = (transparent.isEnabled() ? transparent.getOpacity() : 1.0);
            double shininess = (blinnPhong.isEnabled() ? blinnPhong.getShininess() : 0.0);
            ColorRGB d = (diffuse.isEnabled() ? diffuse.getColor() : null);
            ColorRGB s = (specular.isEnabled() ? specular.getColor() : null);
            ColorRGB e = (emitted.isEnabled() ? emitted.getColor() : null);

            ColorState state = new ColorState();
            state.set(d, s, e, alpha, shininess);

            return colorStates.getStateIndex(state, oldIndex);
        }

        int getRenderState(Renderable renderable, int oldIndex) {
            // we can assume that the renderable is always valid, since
            // we're processing renderable entities
            IndexBufferState state = new IndexBufferState();
            state.set(renderable.getPolygonType(), renderable.getIndices(),
                      renderable.getIndexOffset(), renderable.getIndexCount());

            return renderStates.getStateIndex(state, oldIndex);
        }

        void resetStates() {
            textureStates = new StateCache<TextureState>(TextureState.class);
            geometryStates = new StateCache<GeometryState>(GeometryState.class);
            colorStates = new StateCache<ColorState>(ColorState.class);
            renderStates = new StateCache<IndexBufferState>(IndexBufferState.class);

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
            atoms = system.decorate(Renderable.class, new ObjectProperty.Factory(null));
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
        int blinnPhongVersion = -1;
        int transparentVersion = -1;

        boolean additiveBlending = false;
    }
}
