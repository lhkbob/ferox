package com.ferox.scene.controller.ffp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Future;

import com.ferox.math.ColorRGB;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Framework;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Surface;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.AtmosphericFog;
import com.ferox.scene.BlinnPhongMaterial;
import com.ferox.scene.Camera;
import com.ferox.scene.DecalColorMap;
import com.ferox.scene.DiffuseColor;
import com.ferox.scene.DiffuseColorMap;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.EmittedColor;
import com.ferox.scene.EmittedColorMap;
import com.ferox.scene.Light;
import com.ferox.scene.PointLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.SpecularColor;
import com.ferox.scene.SpecularColorMap;
import com.ferox.scene.SpotLight;
import com.ferox.scene.Transform;
import com.ferox.scene.Transparent;
import com.ferox.scene.controller.PVSResult;
import com.ferox.scene.controller.light.LightGroupResult;
import com.ferox.util.Bag;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.property.ObjectProperty;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

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
    private Transparent transparent; // FIXME not fully implemented either

    // FIXME not implemented yet
    //    private AtmosphericFog fog;

    public FixedFunctionRenderTask(Framework framework) {
        this(framework, 1024, true);
    }

    public FixedFunctionRenderTask(Framework framework, int shadowMapSize, boolean flush) {
        if (framework == null) {
            throw new NullPointerException("Framework cannot be null");
        }

        RenderCapabilities caps = framework.getCapabilities();
        if (!caps.hasFixedFunctionRenderer()) {
            throw new IllegalArgumentException("Framework must support a FixedFunctionRenderer");
        }

        this.framework = framework;
        this.flush = flush;

        previousFrame = new ArrayDeque<Future<Void>>();

        ShadowMapCache shadowMapA;
        ShadowMapCache shadowMapB;

        int numTex = caps.getMaxFixedPipelineTextures();
        boolean shadowsRequested = shadowMapSize > 0; // size is positive
        boolean shadowSupport = ((caps.getFboSupport() || caps.getPbufferSupport()) && numTex > 1 && caps.getDepthTextureSupport());

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

        frameA = new Frame(shadowMapA,
                           diffuseTextureUnit,
                           decalTextureUnit,
                           emissiveTextureUnit);
        frameB = new Frame(shadowMapB,
                           diffuseTextureUnit,
                           decalTextureUnit,
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

        Profiler.push("build-state-tree");
        // all visible atoms have valid states now, so we build a 
        // postman sorted state node tree used for rendering
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

    private com.ferox.renderer.Task<Void> buildTree(Bag<Entity> pvs, Frustum camera,
                                                    Frame frame, final Surface surface) {
        // static tree construction that doesn't depend on entities
        final StateNode root = new StateNode(new CameraState(camera)); // children = lit, unlit
        StateNode litNode = new StateNode(frame.litState); // child = shadowmap state
        StateNode unlitNode = new StateNode(frame.unlitState); // child = texture states
        StateNode smNode = new StateNode(new ShadowMapState(frame.shadowMap,
                                                            shadowmapTextureUnit)); // children = light groups

        root.setChild(0, litNode);
        root.setChild(1, unlitNode);
        litNode.setChild(0, smNode);

        // insert light group nodes
        for (int i = 0; i < lightGroups.getGroupCount(); i++) {
            smNode.setChild(i,
                            new StateNode(new LightGroupState(lightGroups.getGroup(i),
                                                              frame.shadowMap.getShadowCastingLights(),
                                                              framework.getCapabilities()
                                                                       .getMaxActiveLights(),
                                                              directionLight,
                                                              spotLight,
                                                              pointLight,
                                                              ambientLight,
                                                              transform),
                                          frame.textureState.length));
        }

        IntProperty groupAssgn = lightGroups.getAssignmentProperty();

        RenderAtom atom;
        for (Entity e : pvs) {
            e.get(renderable);
            atom = frame.atoms.get(renderable.getIndex());

            StateNode firstNode;
            try {
                firstNode = (atom.lit ? smNode.getChild(groupAssgn.get(renderable.getIndex())) : unlitNode);
            } catch (NullPointerException e1) {
                System.out.println(atom + " " + groupAssgn);
                throw e1;
            }

            // texture state
            StateNode texNode = firstNode.getChild(atom.textureStateIndex);
            if (texNode == null) {
                texNode = new StateNode(frame.textureState[atom.textureStateIndex],
                                        frame.geometryState.length);
                firstNode.setChild(atom.textureStateIndex, texNode);
            }

            // geometry state
            StateNode geomNode = texNode.getChild(atom.geometryStateIndex);
            if (geomNode == null) {
                geomNode = new StateNode(frame.geometryState[atom.geometryStateIndex],
                                         frame.colorState.length);
                texNode.setChild(atom.geometryStateIndex, geomNode);
            }

            // color state
            StateNode colorNode = geomNode.getChild(atom.colorStateIndex);
            if (colorNode == null) {
                colorNode = new StateNode(frame.colorState[atom.colorStateIndex],
                                          frame.renderState.length);
                geomNode.setChild(atom.colorStateIndex, colorNode);
            }

            // render state
            StateNode renderNode = colorNode.getChild(atom.renderStateIndex);
            if (renderNode == null) {
                // must clone the geometry since each node accumulates its own
                // packed transforms that must be rendered
                renderNode = new StateNode(frame.renderState[atom.renderStateIndex].cloneGeometry());
                colorNode.setChild(atom.renderStateIndex, renderNode);
            }

            // now record the transform into the render node's state
            e.get(transform);
            ((RenderState) renderNode.getState()).add(transform.getMatrix());
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

    private void syncEntityState(Entity e, RenderAtom atom, Frame frame) {
        // sync render state
        e.get(renderable);
        boolean renderableChanged = atom.renderableVersion != renderable.getVersion();
        if (renderableChanged || atom.renderStateIndex < 0) {
            atom.renderableVersion = renderable.getVersion();
            if (atom.renderStateIndex >= 0) {
                frame.renderUsage[atom.renderStateIndex]--;
            }
            atom.renderStateIndex = frame.getRenderState(renderable);
            frame.renderUsage[atom.renderStateIndex]++;
        }

        // sync geometry state
        e.get(blinnPhong);
        boolean blinnChanged = (blinnPhong.isEnabled() ? atom.blinnPhongVersion != blinnPhong.getVersion() : atom.blinnPhongVersion >= 0);
        if (renderableChanged || blinnChanged || atom.geometryStateIndex < 0) {
            // renderable version already synced above
            atom.blinnPhongVersion = (blinnPhong.isEnabled() ? blinnPhong.getVersion() : -1);
            if (atom.geometryStateIndex >= 0) {
                frame.geometryUsage[atom.geometryStateIndex]--;
            }
            atom.geometryStateIndex = frame.getGeometryState(renderable, blinnPhong);
            frame.geometryUsage[atom.geometryStateIndex]++;
        }

        // sync texture state
        e.get(diffuseTexture);
        e.get(decalTexture);
        e.get(emittedTexture);
        boolean dftChanged = (diffuseTexture.isEnabled() ? atom.diffuseTextureVersion != diffuseTexture.getVersion() : atom.diffuseTextureVersion >= 0);
        boolean dctChanged = (decalTexture.isEnabled() ? atom.decalTextureVersion != decalTexture.getVersion() : atom.decalTextureVersion >= 0);
        boolean emtChanged = (emittedTexture.isEnabled() ? atom.emittedTextureVersion != emittedTexture.getVersion() : atom.emittedTextureVersion >= 0);
        if (dftChanged || dctChanged || emtChanged || atom.textureStateIndex < 0) {
            atom.diffuseTextureVersion = (diffuseTexture.isEnabled() ? diffuseTexture.getVersion() : -1);
            atom.decalTextureVersion = (decalTexture.isEnabled() ? decalTexture.getVersion() : -1);
            atom.emittedTextureVersion = (emittedTexture.isEnabled() ? emittedTexture.getVersion() : -1);

            if (atom.textureStateIndex >= 0) {
                frame.textureUsage[atom.textureStateIndex]--;
            }
            atom.textureStateIndex = frame.getTextureState(diffuseTexture, decalTexture,
                                                           emittedTexture);
            frame.textureUsage[atom.textureStateIndex]++;
        }

        // sync color state
        e.get(diffuseColor);
        e.get(specularColor);
        e.get(emittedColor);
        e.get(transparent);
        boolean dfcChanged = (diffuseColor.isEnabled() ? atom.diffuseColorVersion != diffuseColor.getVersion() : atom.diffuseColorVersion >= 0);
        boolean spcChanged = (specularColor.isEnabled() ? atom.specularColorVersion != specularColor.getVersion() : atom.specularColorVersion >= 0);
        boolean emcChanged = (emittedColor.isEnabled() ? atom.emittedColorVersion != emittedColor.getVersion() : atom.emittedColorVersion >= 0);
        boolean transparentChanged = (transparent.isEnabled() ? atom.transparentVersion != transparent.getVersion() : atom.transparentVersion >= 0);
        if (dfcChanged || spcChanged || emcChanged || blinnChanged || transparentChanged || atom.colorStateIndex < 0) {
            // blinn phong version already synced
            atom.diffuseColorVersion = (diffuseColor.isEnabled() ? diffuseColor.getVersion() : -1);
            atom.specularColorVersion = (specularColor.isEnabled() ? specularColor.getVersion() : -1);
            atom.emittedColorVersion = (emittedColor.isEnabled() ? emittedColor.getVersion() : -1);

            if (atom.colorStateIndex >= 0) {
                frame.colorUsage[atom.colorStateIndex]--;
            }
            atom.colorStateIndex = frame.getColorState(diffuseColor, specularColor,
                                                       emittedColor, transparent,
                                                       blinnPhong);
            frame.colorUsage[atom.colorStateIndex]++;
        }

        // lit state
        atom.lit = blinnPhong.isEnabled();
    }

    // FIXME pull Frame and RenderAtom out into package-private classes
    private static class Frame {
        final ShadowMapCache shadowMap;

        //FIXME        TransparentState[] transparentStates;
        final LightingState litState;
        final LightingState unlitState;

        final int diffuseTextureUnit;
        final int emissiveTextureUnit;
        final int decalTextureUnit;

        TextureState[] textureState;
        GeometryState[] geometryState;
        ColorState[] colorState;
        RenderState[] renderState;

        Map<TextureState, Integer> textureLookup;
        Map<GeometryState, Integer> geometryLookup;
        Map<ColorState, Integer> colorLookup;
        Map<RenderState, Integer> renderLookup;

        int[] textureUsage;
        int[] geometryUsage;
        int[] colorUsage;
        int[] renderUsage;

        // per-entity tracking
        ObjectProperty<RenderAtom> atoms;

        Frame(ShadowMapCache map, int diffuseTextureUnit, int decalTextureUnit,
              int emissiveTextureUnit) {
            shadowMap = map;

            this.diffuseTextureUnit = diffuseTextureUnit;
            this.decalTextureUnit = decalTextureUnit;
            this.emissiveTextureUnit = emissiveTextureUnit;

            litState = new LightingState(true);
            unlitState = new LightingState(false);

            textureState = new TextureState[0];
            geometryState = new GeometryState[0];
            colorState = new ColorState[0];
            renderState = new RenderState[0];

            textureUsage = new int[0];
            geometryUsage = new int[0];
            colorUsage = new int[0];
            renderUsage = new int[0];

            textureLookup = new HashMap<TextureState, Integer>();
            geometryLookup = new HashMap<GeometryState, Integer>();
            colorLookup = new HashMap<ColorState, Integer>();
            renderLookup = new HashMap<RenderState, Integer>();
        }

        int getTextureState(DiffuseColorMap diffuse, DecalColorMap decal,
                            EmittedColorMap emitted) {
            Texture diffuseTex = (diffuse.isEnabled() ? diffuse.getTexture() : null);
            VertexAttribute diffuseCoord = (diffuse.isEnabled() ? diffuse.getTextureCoordinates() : null);
            Texture decalTex = (decal.isEnabled() ? decal.getTexture() : null);
            VertexAttribute decalCoord = (decal.isEnabled() ? decal.getTextureCoordinates() : null);
            Texture emittedTex = (emitted.isEnabled() ? emitted.getTexture() : null);
            VertexAttribute emittedCoord = (emitted.isEnabled() ? emitted.getTextureCoordinates() : null);

            TextureState state = new TextureState(diffuseTextureUnit,
                                                  decalTextureUnit,
                                                  emissiveTextureUnit);
            state.set(diffuseTex, diffuseCoord, decalTex, decalCoord, emittedTex,
                      emittedCoord);

            Integer index = textureLookup.get(state);
            if (index == null) {
                // must create a new state
                index = textureState.length;
                textureState = Arrays.copyOf(textureState, textureState.length + 1);
                textureUsage = Arrays.copyOf(textureUsage, textureUsage.length + 1);
                textureState[index] = state;
                textureLookup.put(state, index);
            }

            return index;
        }

        int getGeometryState(Renderable renderable, BlinnPhongMaterial blinnPhong) {
            VertexAttribute verts = renderable.getVertices();
            VertexAttribute norms = (blinnPhong.isEnabled() ? blinnPhong.getNormals() : null);

            GeometryState state = new GeometryState();
            state.set(verts, norms);

            Integer index = geometryLookup.get(state);
            if (index == null) {
                // needs a new state
                index = geometryState.length;
                geometryState = Arrays.copyOf(geometryState, geometryState.length + 1);
                geometryUsage = Arrays.copyOf(geometryUsage, geometryUsage.length + 1);
                geometryState[index] = state;
                geometryLookup.put(state, index);
            }

            return index;
        }

        int getColorState(DiffuseColor diffuse, SpecularColor specular,
                          EmittedColor emitted, Transparent transparent,
                          BlinnPhongMaterial blinnPhong) {
            double alpha = (transparent.isEnabled() ? transparent.getOpacity() : 1.0);
            double shininess = (blinnPhong.isEnabled() ? blinnPhong.getShininess() : 0.0);
            ColorRGB d = (diffuse.isEnabled() ? diffuse.getColor() : null);
            ColorRGB s = (specular.isEnabled() ? specular.getColor() : null);
            ColorRGB e = (emitted.isEnabled() ? emitted.getColor() : null);

            ColorState state = new ColorState();
            state.set(d, s, e, alpha, shininess);

            Integer index = colorLookup.get(state);
            if (index == null) {
                // must form a new state
                index = colorState.length;
                colorState = Arrays.copyOf(colorState, colorState.length + 1);
                colorUsage = Arrays.copyOf(colorUsage, colorUsage.length + 1);
                colorState[index] = state;
                colorLookup.put(state, index);
            }

            return index;
        }

        int getRenderState(Renderable renderable) {
            // we can assume that the renderable is always valid, since
            // we're processing renderable entities
            RenderState state = new RenderState();
            state.set(renderable.getPolygonType(), renderable.getIndices(),
                      renderable.getIndexOffset(), renderable.getIndexCount());

            //            System.out.println("getting render state");
            Integer index = renderLookup.get(state);
            if (index == null) {
                //                System.out.println("new render state");
                // must form a new state
                index = renderState.length;
                renderState = Arrays.copyOf(renderState, renderState.length + 1);
                renderUsage = Arrays.copyOf(renderUsage, renderUsage.length + 1);
                renderState[index] = state;
                renderLookup.put(state, index);
            }

            return index;
        }

        void resetStates() {
            textureState = new TextureState[0];
            geometryState = new GeometryState[0];
            colorState = new ColorState[0];
            renderState = new RenderState[0];

            textureUsage = new int[0];
            geometryUsage = new int[0];
            colorUsage = new int[0];
            renderUsage = new int[0];

            textureLookup = new HashMap<TextureState, Integer>();
            geometryLookup = new HashMap<GeometryState, Integer>();
            colorLookup = new HashMap<ColorState, Integer>();
            renderLookup = new HashMap<RenderState, Integer>();

            // clearing the render atoms effectively invalidates all of the
            // version tracking we do as well
            Arrays.fill(atoms.getIndexedData(), null);
        }

        boolean needsReset() {
            int empty = 0;
            int total = textureUsage.length + geometryUsage.length + colorUsage.length + renderUsage.length;

            for (int i = 0; i < textureUsage.length; i++) {
                if (textureUsage[i] == 0) {
                    empty++;
                }
            }

            for (int i = 0; i < geometryUsage.length; i++) {
                if (geometryUsage[i] == 0) {
                    empty++;
                }
            }

            for (int i = 0; i < colorUsage.length; i++) {
                if (colorUsage[i] == 0) {
                    empty++;
                }
            }

            for (int i = 0; i < renderUsage.length; i++) {
                if (renderUsage[i] == 0) {
                    empty++;
                }
            }

            return empty / (double) total > .5;
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
        boolean lit = false;

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
    }
}
