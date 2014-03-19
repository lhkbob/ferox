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
package com.ferox.renderer;

import com.ferox.input.KeyEvent;
import com.ferox.input.MouseEvent;
import com.ferox.input.logic.Action;
import com.ferox.input.logic.InputManager;
import com.ferox.input.logic.InputState;
import com.ferox.math.*;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.builder.Builder;
import com.ferox.renderer.builder.DepthMap2DBuilder;
import com.ferox.renderer.builder.ShaderBuilder;
import com.ferox.renderer.builder.Texture2DBuilder;
import com.ferox.renderer.geom.Geometry;
import com.ferox.renderer.geom.Shapes;
import com.ferox.renderer.loader.GeometryLoader;
import com.ferox.renderer.loader.TextureLoader;
import com.ferox.util.ApplicationStub;
import com.ferox.util.profile.Profiler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import static com.ferox.input.logic.Predicates.*;

/**
 *
 */
public class AshikhminOptimizedDeferredShader extends ApplicationStub implements Task<Void> {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;

    private static final int SPHERE_THETA_RES = 128;
    private static final int SPHERE_PHI_RES = 128;
    private static final int SPEC_VECTOR_COUNT = 5; // origin, view, reflection, reflected over t, reflected over b

    // framework
    private final ControlPanel properties;
    private InputManager input;
    private OnscreenSurface window;

    // deferred rendering
    private final Frustum fullscreenProjection;
    private final Geometry fullscreenQuad;

    private final GBuffer gbuffer;

    private final TextureSurface lightingSurface;
    private final Texture2D lighting;

    private final Shader simpleShader;
    private final Shader gbufferShader;
    private final Shader lightingShader;
    private final Shader completeShader;
    private final Shader sampleShader;
    private final Shader tonemapShader;

    // environment mapping
    private final Geometry envCube;
    private boolean showCubeMap = true;
    private int envMode = 0;

    // geometry
    private final Geometry shape;

    private final Geometry xAxis;
    private final Geometry yAxis;
    private final Geometry zAxis;
    private boolean showAxis;

    private boolean showProbe;
    private boolean saveCamPos;
    private boolean approxProbe;
    private final Vector3 probeCamPos = new Vector3();

    private final float[] probeLines;
    private final VertexAttribute probeVBO;
    private final VertexAttribute probeColors;
    private final ElementBuffer probeIndices;

    // camera controls
    private final TrackBall modelTrackBall;
    private final TrackBall viewTrackBall;
    private final Frustum camera;
    private double cameraDist;
    private MoveState moveState;

    private volatile boolean invalidateGbuffer;
    private long gbufferTimeStamp;
    private boolean renderApproximation;
    private boolean renderSampledMethod;
    private int sample;

    // settings and persistent state
    private final Properties settings;

    public static void main(String[] args) throws Exception {
        Framework.Factory.enableDebugMode();
        Framework framework = Framework.Factory.create();
        try {
            new AshikhminOptimizedDeferredShader(framework).run();
        } finally {
            framework.destroy();
        }
    }

    public AshikhminOptimizedDeferredShader(Framework framework) throws Exception {
        super(framework, new OnscreenSurfaceOptions().withDepthBuffer(24).windowed(WIDTH, HEIGHT).fixedSize(),
              false);
        modelTrackBall = new TrackBall(false);
        viewTrackBall = new TrackBall(true);

        moveState = MoveState.NONE;

        camera = new Frustum(60, WIDTH / (float) HEIGHT, 0.1, 25);
        cameraDist = 1.5;

        gbuffer = new GBuffer(framework);

        fullscreenQuad = Shapes.createRectangle(framework, 0, WIDTH, 0, HEIGHT);
        fullscreenProjection = new Frustum(true, 0, WIDTH, 0, HEIGHT, -1.0, 1.0);

        envCube = Shapes.createBox(framework, 6.0);

        //        shape = Shapes.createSphere(framework, 0.5, 128);
        //        shape = Shapes.createTeapot(framework);
        shape = GeometryLoader.readGeometry(framework, AshikhminOptimizedDeferredShader.class
                                                               .getResource("suitcase.ply"));

        showAxis = false;
        xAxis = Shapes.createCylinder(framework, new Vector3(1, 0, 0), new Vector3(1, 0, 0), 0.01, 0.5, 4);
        yAxis = Shapes.createCylinder(framework, new Vector3(0, 1, 0), new Vector3(0, 1, 0), 0.01, 0.5, 4);
        zAxis = Shapes.createCylinder(framework, new Vector3(0, 0, 1), new Vector3(0, 0, 1), 0.01, 0.5, 4);

        simpleShader = loadShader(framework, "simple").bindColorBuffer("fColor", 0).build();
        lightingShader = loadShader(framework, "ashik-lighting").bindColorBuffer("fColor", 0).build();
        completeShader = loadShader(framework, "ashik-complete").bindColorBuffer("fColor", 0).build();
        sampleShader = loadShader(framework, "ashik-sample").bindColorBuffer("fColor", 0).build();
        tonemapShader = loadShader(framework, "tonemap").bindColorBuffer("fColor", 0).build();
        gbufferShader = loadShader(framework, "ashik-gbuffer2")
                                .bindColorBuffer("fNormalTangentXY", GBuffer.NORMAL_TAN_IDX)
                                .bindColorBuffer("fShininessAndNTZ", GBuffer.SHINE_NTZ_IDX)
                                .bindColorBuffer("fDiffuseAlbedo", GBuffer.DIFFUSE_IDX)
                                .bindColorBuffer("fSpecularAlbedo", GBuffer.SPECULAR_IDX).build();

        showProbe = false;
        saveCamPos = false;
        probeLines = new float[SPHERE_PHI_RES * SPHERE_THETA_RES * 3 + SPEC_VECTOR_COUNT * 3];
        float[] probeColorData = new float[SPHERE_PHI_RES * SPHERE_THETA_RES * 4 + SPEC_VECTOR_COUNT * 4];

        VertexBuffer vbo = framework.newVertexBuffer().from(probeLines).build();
        probeVBO = new VertexAttribute(vbo, 3);

        int[] indices = new int[SPHERE_PHI_RES * SPHERE_THETA_RES * 2 + (SPEC_VECTOR_COUNT - 1) * 2];
        // origin
        probeColorData[0] = 0.5f;
        probeColorData[1] = 0.5f;
        probeColorData[2] = 0.5f;
        probeColorData[3] = 1.0f;
        // view vector
        indices[0] = 0;
        indices[1] = 1;
        probeColorData[4] = 1.0f;
        probeColorData[5] = 1.0f;
        probeColorData[6] = 0.0f;
        probeColorData[7] = 1.0f;
        // reflection vector
        indices[2] = 0;
        indices[3] = 2;
        probeColorData[8] = 0.0f;
        probeColorData[9] = 1.0f;
        probeColorData[10] = 1.0f;
        probeColorData[11] = 1.0f;
        // reflect over t vector
        indices[4] = 0;
        indices[5] = 3;
        probeColorData[12] = 1.0f;
        probeColorData[13] = 0.0f;
        probeColorData[14] = 0.0f;
        probeColorData[15] = 1.0f;
        // reflect over b vector
        indices[6] = 0;
        indices[7] = 4;
        probeColorData[16] = 0.0f;
        probeColorData[17] = 0.0f;
        probeColorData[18] = 1.0f;
        probeColorData[19] = 1.0f;

        for (int i = 0; i < SPHERE_PHI_RES * SPHERE_THETA_RES; i++) {
            indices[i * 2 + (SPEC_VECTOR_COUNT - 1) * 2] = 0;
            indices[i * 2 + (SPEC_VECTOR_COUNT - 1) * 2 + 1] = i + SPEC_VECTOR_COUNT;

            probeColorData[i * 4 + SPEC_VECTOR_COUNT * 4] = 1.0f;
            probeColorData[i * 4 + SPEC_VECTOR_COUNT * 4 + 1] = 1.0f;
            probeColorData[i * 4 + SPEC_VECTOR_COUNT * 4 + 2] = 1.0f;
            probeColorData[i * 4 + SPEC_VECTOR_COUNT * 4 + 3] = 1.0f;
        }
        probeColors = new VertexAttribute(framework.newVertexBuffer().from(probeColorData).build(), 4);

        probeIndices = framework.newElementBuffer().fromUnsigned(indices).build();

        Texture2DBuilder b = framework.newTexture2D();
        b.width(WIDTH).height(HEIGHT).rgb().mipmap(0).from((float[]) null);
        lighting = b.build();
        TextureSurfaceOptions gOpts = new TextureSurfaceOptions().size(WIDTH, HEIGHT)
                                                                 .colorBuffers(lighting.getRenderTarget());
        lightingSurface = framework.createSurface(gOpts);

        settings = new Properties();
        properties = new ControlPanel(this);
    }

    public synchronized void setLastLLSDir(String dir) {
        settings.setProperty("last_lls_dir", dir);
        savePreferences();
    }

    public synchronized String getLastLLSDir() {
        return settings.getProperty("last_lls_dir");
    }

    public synchronized void setLastEnvDir(String dir) {
        settings.setProperty("last_env_dir", dir);
        savePreferences();
    }

    public synchronized String getLastEnvDir() {
        return settings.getProperty("last_env_dir");
    }

    public synchronized String getLastMorphDir() {
        return settings.getProperty("last_morph_dir");
    }

    public synchronized void setLastMorphDir(String dir) {
        settings.setProperty("last_morph_dir", dir);
        savePreferences();
    }

    public synchronized void setDefaultEnv(String env) {
        settings.setProperty("default_env", env);
        savePreferences();
    }

    public synchronized String getDefaultEnv() {
        return settings.getProperty("default_env");
    }

    public synchronized void setDefaultMatA(String mat) {
        settings.setProperty("default_mat_a", mat);
        savePreferences();
    }

    public synchronized String getDefaultMatA() {
        return settings.getProperty("default_mat_a");
    }

    public synchronized void setDefaultMatB(String mat) {
        settings.setProperty("default_mat_b", mat);
        savePreferences();
    }

    public synchronized String getDefaultMatB() {
        return settings.getProperty("default_mat_b");
    }

    public synchronized Environment.Settings getDefaultTonemapping(String env) {
        return new Environment.Settings(env, settings);
    }

    public synchronized void setDefaultTonemapping(String env, Environment.Settings tonemapping) {
        tonemapping.store(env, settings);
        savePreferences();
    }

    public synchronized Material.Settings getDefaultMatSettings(String mat) {
        return new Material.Settings(mat, settings);
    }

    public synchronized void setDefaultMatSettings(String mat, Material.Settings settings) {
        settings.store(mat, this.settings);
        savePreferences();
    }

    private void savePreferences() {
        try (FileOutputStream out = new FileOutputStream("ashikhmin.properties")) {
            settings.store(out, "Auto-generated, do not edit by hand");
        } catch (IOException e) {
            System.err.println("Saving preferences failed");
            e.printStackTrace();
        }
    }

    private void loadPreferences() {
        try (FileInputStream in = new FileInputStream("ashikhmin.properties")) {
            settings.load(in);
        } catch (FileNotFoundException e) {
            // ignore
        } catch (IOException e) {
            System.err.println("Loading preferences failed");
            e.printStackTrace();
        }

        String defaultEnv = getDefaultEnv();
        if (defaultEnv != null) {
            properties.getEnvTab().loadEnvironment(defaultEnv);
        }
        String defaultMatA = getDefaultMatA();
        if (defaultMatA != null) {
            properties.getMatATab().loadTexturesA(defaultMatA, getDefaultMatSettings(defaultMatA));
        }
        String defaultMatB = getDefaultMatB();
        if (defaultMatB != null) {
            properties.getMatBTab().loadTexturesA(defaultMatB, getDefaultMatSettings(defaultMatB));
        }
    }

    public void loadMorph(String morphFile) {
        try (FileInputStream in = new FileInputStream(morphFile)) {
            Properties props = new Properties();
            props.load(in);

            String matA = props.getProperty("mat_a");
            properties.getMatATab().loadTexturesA(matA, new Material.Settings(matA, props));
            String matB = props.getProperty("mat_b");
            properties.getMatBTab().loadTexturesA(matB, new Material.Settings(matB, props));
            properties.getMorphTab().updateFromSettings(props);
        } catch (IOException e) {
            System.err.println("Unable to load morph");
            e.printStackTrace();
        }
    }

    public void saveMorph(String morphFile) {
        try (FileOutputStream out = new FileOutputStream(morphFile)) {
            Properties props = new Properties();
            String matA = properties.getMatATab().getMatFolder();
            properties.getMatATab().getAsSettings().store(matA, props);
            props.setProperty("mat_a", matA);

            String matB = properties.getMatBTab().getMatFolder();
            properties.getMatBTab().getAsSettings().store(matB, props);
            properties.getMorphTab().saveToSettings(props);
            props.setProperty("mat_b", matB);

            props.store(out, "Auto-generated, do not edit by hand");
        } catch (IOException e) {
            System.err.println("Unable to save morph");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        loadPreferences();
        updateCamera();
        properties.pack();
        properties.setVisible(true);
        super.run();
        properties.setVisible(false);
        properties.dispose();
    }

    @Override
    protected void renderFrame(OnscreenSurface surface) {
        try {
            getFramework().invoke(this).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void run(HardwareAccessLayer access) {
        input.process();

        Context ctx;
        GlslRenderer r;

        Profiler.push("render");

        Material matA = properties.getMatATab().getMaterial();
        Material matB = properties.getMatBTab().getMaterial();
        Environment environment = properties.getEnvTab().getEnvironment();

        // if we've moved the camera or modelview, then the gbuffer is invalidated, so
        // fill in the gbuffer and the diffuse lighting
        if (invalidateGbuffer) {
            Profiler.push("fill-gbuffer");
            ctx = access.setActiveSurface(gbuffer.gbuffer);
            if (ctx == null) {
                return null;
            }

            r = ctx.getGlslRenderer();
            r.clear(true, true, false, new Vector4(), 1.0, 0);

            // fill gbuffer with first material
            r.setShader(gbufferShader);
            r.setUniform(gbufferShader.getUniform("uProjection"), camera.getProjectionMatrix());
            r.setUniform(gbufferShader.getUniform("uView"), camera.getViewMatrix());
            r.setUniform(gbufferShader.getUniform("uModel"), modelTrackBall.getTransform());

            r.bindAttribute(gbufferShader.getAttribute("aPos"), shape.getVertices());
            r.bindAttribute(gbufferShader.getAttribute("aNorm"), shape.getNormals());
            r.bindAttribute(gbufferShader.getAttribute("aTan"), shape.getTangents());
            r.bindAttribute(gbufferShader.getAttribute("aTC"), shape.getTextureCoordinates());

            r.setIndices(shape.getIndices());

            // first material
            r.setUniform(gbufferShader.getUniform("uNormalTex"), matA.normal);
            r.setUniform(gbufferShader.getUniform("uNormalWeight"),
                         1.0 - properties.getMorphTab().getNormalAlpha());

            r.setUniform(gbufferShader.getUniform("uDiffuseAlbedo"), matA.diffuseAlbedo);
            r.setUniform(gbufferShader.getUniform("uDiffuseScale"),
                         properties.getMatATab().getDiffuseScale());
            r.setUniform(gbufferShader.getUniform("uDiffuseAlbedoWeight"),
                         1.0 - properties.getMorphTab().getDiffuseAlpha());

            r.setUniform(gbufferShader.getUniform("uSpecularAlbedo"), matA.specularAlbedo);
            r.setUniform(gbufferShader.getUniform("uSpecularScale"),
                         properties.getMatATab().getSpecularScale());
            r.setUniform(gbufferShader.getUniform("uSpecularAlbedoWeight"),
                         1.0 - properties.getMorphTab().getSpecularAlpha());

            r.setUniform(gbufferShader.getUniform("uShininess"), matA.shininessXY);
            r.setUniform(gbufferShader.getUniform("uShininessWeight"),
                         1.0 - properties.getMorphTab().getShininessAlpha());
            r.setUniform(gbufferShader.getUniform("uShininessScale"),
                         properties.getMatATab().getShininessXScale(),
                         properties.getMatATab().getShininessYScale());
            r.setUniform(gbufferShader.getUniform("uShinyOverride"),
                         properties.getMatATab().getShininessOverride());

            r.setUniform(gbufferShader.getUniform("uTCScale"), properties.getMatATab().getTexCoordScale());
            r.render(shape.getPolygonType(), shape.getIndexOffset(), shape.getIndexCount());

            r.setBlendMode(Renderer.BlendFunction.ADD, Renderer.BlendFactor.ONE, Renderer.BlendFactor.ONE);
            r.setBlendingEnabled(true);
            r.setDepthTest(Renderer.Comparison.LEQUAL);

            // now add second material
            r.setUniform(gbufferShader.getUniform("uNormalTex"), matB.normal);
            r.setUniform(gbufferShader.getUniform("uNormalWeight"),
                         properties.getMorphTab().getNormalAlpha());

            r.setUniform(gbufferShader.getUniform("uDiffuseAlbedo"), matB.diffuseAlbedo);
            r.setUniform(gbufferShader.getUniform("uDiffuseScale"),
                         properties.getMatBTab().getDiffuseScale());
            r.setUniform(gbufferShader.getUniform("uDiffuseAlbedoWeight"),
                         properties.getMorphTab().getDiffuseAlpha());

            r.setUniform(gbufferShader.getUniform("uSpecularAlbedo"), matB.specularAlbedo);
            r.setUniform(gbufferShader.getUniform("uSpecularScale"),
                         properties.getMatBTab().getSpecularScale());

            r.setUniform(gbufferShader.getUniform("uSpecularAlbedoWeight"),
                         properties.getMorphTab().getSpecularAlpha());

            r.setUniform(gbufferShader.getUniform("uShininess"), matB.shininessXY);
            r.setUniform(gbufferShader.getUniform("uShininessWeight"),
                         properties.getMorphTab().getShininessAlpha());
            r.setUniform(gbufferShader.getUniform("uShininessScale"),
                         properties.getMatBTab().getShininessXScale(),
                         properties.getMatBTab().getShininessYScale());
            r.setUniform(gbufferShader.getUniform("uShinyOverride"),
                         properties.getMatBTab().getShininessOverride());

            r.setUniform(gbufferShader.getUniform("uTCScale"), properties.getMatBTab().getTexCoordScale());


            r.render(shape.getPolygonType(), shape.getIndexOffset(), shape.getIndexCount());
            ctx.flush();
            Profiler.pop();

            invalidateGbuffer = false;
            gbufferTimeStamp = System.currentTimeMillis();
            renderApproximation = true;
        } else {
            if (System.currentTimeMillis() - gbufferTimeStamp > 2000) {
                if (renderApproximation) {
                    renderApproximation = false;
                    sample = 0;
                }
            }
        }

        Matrix4 invProj = new Matrix4().inverse(camera.getProjectionMatrix());
        Matrix3 view = new Matrix3().setUpper(camera.getViewMatrix());
        Matrix3 invView = new Matrix3().inverse(view);

        if (renderSampledMethod) {
            Profiler.push("sampled");
            // accumulate lighting into another texture (linear pre gamma correction)
            ctx = access.setActiveSurface(lightingSurface);
            if (ctx == null) {
                return null;
            }
            r = ctx.getGlslRenderer();
            // avoid the clear and just overwrite everything
            r.setDepthTest(Renderer.Comparison.ALWAYS);
            r.setDepthWriteMask(false);

            r.setShader(sampleShader);

            r.setUniform(sampleShader.getUniform("uInvProj"), invProj);
            r.setUniform(sampleShader.getUniform("uInvView"), invView);

            r.setUniform(sampleShader.getUniform("uDiffuseIrradiance"), environment.diffuseMap);
            r.setUniform(sampleShader.getUniform("uSpecularRadiance"),
                         environment.envMap); //environment.specularMaps[environment.specularMaps.length - 1]);

            r.setUniform(sampleShader.getUniform("uNormalAndTangent"), gbuffer.normalTangentXY);
            r.setUniform(sampleShader.getUniform("uShininessAndNTZ"), gbuffer.shininessAndNTZ);
            r.setUniform(sampleShader.getUniform("uDiffuseAlbedo"), gbuffer.diffuseAlbedo);
            r.setUniform(sampleShader.getUniform("uSpecularAlbedo"), gbuffer.specularAlbedo);
            r.setUniform(sampleShader.getUniform("uDepth"), gbuffer.depth);

            r.setUniform(sampleShader.getUniform("uProjection"), fullscreenProjection.getProjectionMatrix());
            r.bindAttribute(sampleShader.getAttribute("aPos"), fullscreenQuad.getVertices());
            r.bindAttribute(sampleShader.getAttribute("aTC"), fullscreenQuad.getTextureCoordinates());
            r.setIndices(fullscreenQuad.getIndices());
            Random rand = new Random(24545243L);
            //            for (int i = 0; i < 64; i++) {
            //                r.setUniformArray(sampleShader.getUniform("u1"), i, rand.nextDouble());
            //                r.setUniformArray(sampleShader.getUniform("u2"), i, rand.nextDouble());
            //            }
            r.render(fullscreenQuad.getPolygonType(), fullscreenQuad.getIndexOffset(),
                     fullscreenQuad.getIndexCount());

            ctx.flush();
            Profiler.pop();
        } else if (renderApproximation) {
            Profiler.push("diffuse");
            // accumulate lighting into another texture (linear pre gamma correction)
            ctx = access.setActiveSurface(lightingSurface);
            if (ctx == null) {
                return null;
            }
            r = ctx.getGlslRenderer();
            // avoid the clear and just overwrite everything
            r.setDepthTest(Renderer.Comparison.ALWAYS);
            r.setDepthWriteMask(false);

            r.setShader(lightingShader);

            r.setUniform(lightingShader.getUniform("uInvProj"), invProj);
            r.setUniform(lightingShader.getUniform("uInvView"), invView);

            r.setUniform(lightingShader.getUniform("uDiffuseMode"), true);
            r.setUniform(lightingShader.getUniform("uIrradianceMin"), environment.diffuseMap);

            r.setUniform(lightingShader.getUniform("uNormalAndTangent"), gbuffer.normalTangentXY);
            r.setUniform(lightingShader.getUniform("uShininessAndNTZ"), gbuffer.shininessAndNTZ);
            r.setUniform(lightingShader.getUniform("uDiffuseAlbedo"), gbuffer.diffuseAlbedo);
            r.setUniform(lightingShader.getUniform("uSpecularAlbedo"), gbuffer.specularAlbedo);
            r.setUniform(lightingShader.getUniform("uDepth"), gbuffer.depth);

            r.setUniform(lightingShader.getUniform("uProjection"),
                         fullscreenProjection.getProjectionMatrix());
            r.bindAttribute(lightingShader.getAttribute("aPos"), fullscreenQuad.getVertices());
            r.bindAttribute(lightingShader.getAttribute("aTC"), fullscreenQuad.getTextureCoordinates());
            r.setIndices(fullscreenQuad.getIndices());
            r.render(fullscreenQuad.getPolygonType(), fullscreenQuad.getIndexOffset(),
                     fullscreenQuad.getIndexCount());
            Profiler.pop();

            Profiler.push("specular");

            r.setBlendingEnabled(true);
            r.setBlendMode(Renderer.BlendFunction.ADD, Renderer.BlendFactor.ONE, Renderer.BlendFactor.ONE);

            r.setShader(lightingShader);
            r.setUniform(lightingShader.getUniform("uDiffuseMode"), false);
            for (int i = 0; i < environment.specularMaps.length - 1; i++) {
                r.setUniform(lightingShader.getUniform("uShininessMin"), EnvironmentMap.SPEC_EXP[i]);
                r.setUniform(lightingShader.getUniform("uShininessMax"), EnvironmentMap.SPEC_EXP[i + 1]);
                r.setUniform(lightingShader.getUniform("uIrradianceMin"), environment.specularMaps[i]);
                r.setUniform(lightingShader.getUniform("uIrradianceMax"), environment.specularMaps[i + 1]);

                r.render(fullscreenQuad.getPolygonType(), fullscreenQuad.getIndexOffset(),
                         fullscreenQuad.getIndexCount());
            }

            ctx.flush();
            Profiler.pop();
        } else if (sample < environment.samples.size()) {
            Profiler.push("complete");
            // accumulate lighting into another texture (linear pre gamma correction)
            ctx = access.setActiveSurface(lightingSurface);
            if (ctx == null) {
                return null;
            }
            r = ctx.getGlslRenderer();
            if (sample == 0) {
                // must clear the first time
                r.clear(true, true, false);
            }

            r.setDepthTest(Renderer.Comparison.ALWAYS);
            r.setDepthWriteMask(false);

            r.setShader(completeShader);

            r.setUniform(completeShader.getUniform("uInvProj"), invProj);

            r.setUniform(completeShader.getUniform("uNormalAndTangent"), gbuffer.normalTangentXY);
            r.setUniform(completeShader.getUniform("uShininessAndNTZ"), gbuffer.shininessAndNTZ);
            r.setUniform(completeShader.getUniform("uDiffuseAlbedo"), gbuffer.diffuseAlbedo);
            r.setUniform(completeShader.getUniform("uSpecularAlbedo"), gbuffer.specularAlbedo);
            r.setUniform(completeShader.getUniform("uDepth"), gbuffer.depth);

            r.setUniform(completeShader.getUniform("uProjection"),
                         fullscreenProjection.getProjectionMatrix());
            r.bindAttribute(completeShader.getAttribute("aPos"), fullscreenQuad.getVertices());
            r.bindAttribute(completeShader.getAttribute("aTC"), fullscreenQuad.getTextureCoordinates());
            r.setIndices(fullscreenQuad.getIndices());

            r.setBlendingEnabled(true);
            r.setBlendMode(Renderer.BlendFunction.ADD, Renderer.BlendFactor.ONE, Renderer.BlendFactor.ONE);

            Vector3 l = new Vector3();
            for (int p = 0; p < 1; p++) {
                for (int s = 0; s < 40; s++) {
                    if (sample + s < environment.samples.size()) {
                        EnvironmentMap.Sample smp = environment.samples.get(sample + s);
                        l.mul(view, smp.direction);
                        r.setUniformArray(completeShader.getUniform("uLightDirection"), s, l);
                        r.setUniformArray(completeShader.getUniform("uLightRadiance"), s, smp.illumination);
                    } else {
                        r.setUniformArray(completeShader.getUniform("uLightDirection"), s, new Vector3());
                        r.setUniformArray(completeShader.getUniform("uLightRadiance"), s, new Vector3());
                    }
                }
                sample += 40;

                r.render(fullscreenQuad.getPolygonType(), fullscreenQuad.getIndexOffset(),
                         fullscreenQuad.getIndexCount());
            }

            System.out.println("Samples left: " + Math.max(0, (environment.samples.size() - sample)));
            ctx.flush();
            Profiler.pop();
        }

        Profiler.push("window");

        // display everything to the window
        ctx = access.setActiveSurface(window);
        if (ctx == null) {
            return null;
        }
        r = ctx.getGlslRenderer();
        r.clear(true, true, false, new Vector4(0.5, 0.5, 0.5, 1.0), 1.0, 0);
        if (!showProbe) {
            r.setShader(tonemapShader);
            r.setUniform(tonemapShader.getUniform("uProjection"), fullscreenProjection.getProjectionMatrix());
            r.setUniform(tonemapShader.getUniform("uHighRange"), lighting);
            r.setUniform(tonemapShader.getUniform("uDepth"), gbuffer.depth);
            r.setUniform(tonemapShader.getUniform("uGamma"), properties.getEnvTab().getGamma());
            r.setUniform(tonemapShader.getUniform("uSensitivity"), properties.getEnvTab().getSensitivity());
            r.setUniform(tonemapShader.getUniform("uExposure"), properties.getEnvTab().getExposure());
            r.setUniform(tonemapShader.getUniform("uFstop"), properties.getEnvTab().getFStop());

            r.bindAttribute(tonemapShader.getAttribute("aPos"), fullscreenQuad.getVertices());
            r.bindAttribute(tonemapShader.getAttribute("aTC"), fullscreenQuad.getTextureCoordinates());
            r.setIndices(fullscreenQuad.getIndices());
            r.render(fullscreenQuad.getPolygonType(), fullscreenQuad.getIndexOffset(),
                     fullscreenQuad.getIndexCount());
        }

        r.setShader(simpleShader);
        r.setUniform(simpleShader.getUniform("uProjection"), camera.getProjectionMatrix());
        r.setUniform(simpleShader.getUniform("uView"), camera.getViewMatrix());
        r.setUniform(simpleShader.getUniform("uUseEnvMap"), false);

        // axis rendering
        if (showAxis) {
            r.setUniform(simpleShader.getUniform("uModel"), new Matrix4().setIdentity());
            r.setAttribute(simpleShader.getAttribute("aColor"), new Vector4(1, 0, 0, 1));

            r.bindAttribute(simpleShader.getAttribute("aPos"), xAxis.getVertices());

            r.setIndices(xAxis.getIndices());
            r.render(xAxis.getPolygonType(), xAxis.getIndexOffset(), xAxis.getIndexCount());

            r.setAttribute(simpleShader.getAttribute("aColor"), new Vector4(0, 1, 0, 1));
            r.bindAttribute(simpleShader.getAttribute("aPos"), yAxis.getVertices());

            r.setIndices(yAxis.getIndices());
            r.render(yAxis.getPolygonType(), yAxis.getIndexOffset(), yAxis.getIndexCount());

            r.setAttribute(simpleShader.getAttribute("aColor"), new Vector4(0, 0, 1, 1));
            r.bindAttribute(simpleShader.getAttribute("aPos"), zAxis.getVertices());

            r.setIndices(zAxis.getIndices());
            r.render(zAxis.getPolygonType(), zAxis.getIndexOffset(), zAxis.getIndexCount());
        }

        if (showProbe) {
            // first compute new probe
            Vector3 n = new Vector3(0, 1, 0);
            Vector3 tv = new Vector3(1, 0, 0);
            Vector3 bv = new Vector3().cross(n, tv).normalize();
            Vector3 v = new Vector3((saveCamPos ? probeCamPos : camera.getLocation())).normalize();
            generateReflectionAxis(v, n, tv, bv);
            if (approxProbe) {
                generateApproximateProbe(v, n, tv, bv);
            } else {
                generateRealProbe(v, n, tv, bv);
            }

            access.refresh(probeVBO.getVBO());

            r.setUniform(simpleShader.getUniform("uModel"), new Matrix4().setIdentity());

            r.bindAttribute(simpleShader.getAttribute("aColor"), probeColors);
            r.bindAttribute(simpleShader.getAttribute("aPos"), probeVBO);

            r.setIndices(probeIndices);
            r.setLineSize(1.0);
            r.render(Renderer.PolygonType.LINES, 0, probeIndices.getLength());
        }

        if (showCubeMap) {
            // draw environment map
            r.setShader(simpleShader);
            r.setDrawStyle(Renderer.DrawStyle.NONE, Renderer.DrawStyle.SOLID);
            r.setUniform(simpleShader.getUniform("uGamma"), properties.getEnvTab().getGamma());
            r.setUniform(simpleShader.getUniform("uSensitivity"), properties.getEnvTab().getSensitivity());
            r.setUniform(simpleShader.getUniform("uExposure"), properties.getEnvTab().getExposure());
            r.setUniform(simpleShader.getUniform("uFstop"), properties.getEnvTab().getFStop());

            r.setUniform(simpleShader.getUniform("uModel"), new Matrix4().setIdentity());
            r.setUniform(simpleShader.getUniform("uUseEnvMap"), true);

            int totalEnv = 2 + environment.specularMaps.length; // env + diff + spec
            int realMode = envMode % totalEnv;
            if (realMode == 0) {
                r.setUniform(simpleShader.getUniform("uEnvMap"), environment.envMap);
            } else if (realMode == 1) {
                r.setUniform(simpleShader.getUniform("uEnvMap"), environment.diffuseMap);
            } else {
                r.setUniform(simpleShader.getUniform("uEnvMap"), environment.specularMaps[realMode - 2]);
            }

            r.bindAttribute(simpleShader.getAttribute("aPos"), envCube.getVertices());

            r.setIndices(envCube.getIndices());
            r.render(envCube.getPolygonType(), envCube.getIndexOffset(), envCube.getIndexCount());
        }

        Profiler.pop();
        Profiler.pop();
        return null;
    }

    private void generateRealProbe(@Const Vector3 v, @Const Vector3 n, @Const Vector3 tv, @Const Vector3 bv) {
        generateRealProbe(v, n, tv, bv, properties.getMatATab().getShininessXScale(),
                          properties.getMatATab().getShininessYScale(), false, 1.0);
    }

    private void generateRealProbe(@Const Vector3 v, @Const Vector3 n, @Const Vector3 tv, @Const Vector3 bv,
                                   double shineX, double shineY, boolean add, double weight) {
        Vector3 l = new Vector3();
        Vector3 h = new Vector3();
        for (int t = 0; t < SPHERE_THETA_RES; t++) {
            for (int p = 0; p < SPHERE_PHI_RES; p++) {
                double theta = 2 * t / (double) SPHERE_THETA_RES * Math.PI;
                double phi = p / (double) SPHERE_PHI_RES * Math.PI / 2.0;

                l.set(Math.cos(theta) * Math.sin(phi), Math.cos(phi), Math.sin(theta) * Math.sin(phi));
                h.add(l, v).normalize();
                double th = tv.dot(h);
                double bh = bv.dot(h);
                double nh = n.dot(h);
                double magnitude = Math.pow(nh, (shineX * th * th + shineY * bh * bh) / (1.0 - nh * nh));

                l.scale(weight * magnitude);

                if (add) {
                    h.set(probeLines, t * SPHERE_PHI_RES * 3 + p * 3 + SPEC_VECTOR_COUNT * 3);
                    h.add(l).get(probeLines, t * SPHERE_PHI_RES * 3 + p * 3 + SPEC_VECTOR_COUNT * 3);
                } else {
                    l.get(probeLines, t * SPHERE_PHI_RES * 3 + p * 3 + SPEC_VECTOR_COUNT * 3);
                }
            }
        }
    }

    private static Vector3 reflect(@Const Vector3 v, @Const Vector3 n) {
        return new Vector3(n).scale(2 * n.dot(v)).sub(v).normalize();
    }

    private double samplePhi(double u, double nx, double ny) {
        return Math.atan(Math.sqrt((nx + 1) / (ny + 1)) * Math.tan(Math.PI * u / 2.0));
    }

    private void generateApproximateProbe(@Const Vector3 v, @Const Vector3 n, @Const Vector3 tv,
                                          @Const Vector3 bv) {
        Random r = new Random(23423423L);

        Matrix3 basis = new Matrix3();
        basis.setCol(0, tv).setCol(1, bv).setCol(2, n);

        Vector3 h = new Vector3();

        int numSamples = 128;
        double nx = properties.getMatATab().getShininessXScale();
        double ny = properties.getMatATab().getShininessYScale();
        for (int i = 0; i < numSamples; i++) {
            double u1 = r.nextDouble();
            double u2 = r.nextDouble();

            double phi;
            if (u1 < 0.25) {
                u1 = 4 * u1;
                phi = samplePhi(u1, nx, ny);
            } else if (u1 < 0.5) {
                u1 = 4 * (0.5 - u1);
                phi = Math.PI - samplePhi(u1, nx, ny);
            } else if (u1 < 0.75) {
                u1 = 4 * (u1 - 0.5);
                phi = samplePhi(u1, nx, ny) + Math.PI;
            } else {
                u1 = 4 * (1.0 - u1);
                phi = 2 * Math.PI - samplePhi(u1, nx, ny);
            }

            double cosTheta = Math.pow((1 - u2), 1.0 / (nx * Math.cos(phi) * Math.cos(phi) +
                                                        ny * Math.sin(phi) * Math.sin(phi) + 1));
            double radius = Math.sin(Math.acos(cosTheta));

            h.set(Math.cos(phi) * radius, Math.sin(phi) * radius, cosTheta);
            h.mul(basis, h);

            Vector3 l = reflect(v, h);

            double th = tv.dot(h);
            double bh = bv.dot(h);
            double nh = n.dot(h);
            double magnitude = Math.pow(nh, (nx * th * th + ny * bh * bh) / (1.0 - nh * nh));

            l.scale(magnitude);
            l.get(probeLines, i * 3 + SPEC_VECTOR_COUNT * 3);
        }

        for (int i = SPEC_VECTOR_COUNT * 3 + numSamples * 3; i < probeLines.length; i++) {
            probeLines[i] = 0.0f;
        }
    }

    private void generateReflectionAxis(@Const Vector3 v, @Const Vector3 n, @Const Vector3 tv,
                                        @Const Vector3 bv) {
        Vector3 reflected = reflect(v, n);

        // reflection vector reflected over plane formed by t and n
        reflect(reflected, bv).scale(-1).normalize().scale(4).get(probeLines, 9);
        // reflection over plane by b and n
        reflect(reflected, tv).scale(-1).normalize().scale(4).get(probeLines, 12);
        // view vector
        new Vector3(v).scale(4).get(probeLines, 3);

        // main reflection vector
        reflected.scale(4).get(probeLines, 6);
    }

    @Override
    protected void init(OnscreenSurface surface) {
        surface.setVSyncEnabled(true);
        window = surface;
        // everything is already constructed or will be loaded dynamically
    }

    private double getNormalizedDeviceX(double windowX) {
        return 2.0 * windowX / window.getWidth() - 1.0;
    }

    private double getNormalizedDeviceY(double windowY) {
        return 2.0 * windowY / window.getHeight() - 1.0;
    }

    public void updateGBuffer() {
        invalidateGbuffer = true;
    }

    private void updateCamera() {
        Matrix4 camT = new Matrix4()
                               .lookAt(new Vector3(), new Vector3(0, 0, cameraDist), new Vector3(0, 1, 0));
        camT.mul(viewTrackBall.getTransform(), camT);
        camera.setOrientation(camT);

        updateGBuffer();
    }

    @Override
    protected void installInputHandlers(InputManager input) {
        this.input = input;

        input.on(or(mouseRelease(MouseEvent.MouseButton.LEFT), mouseRelease(MouseEvent.MouseButton.RIGHT)))
             .trigger(new Action() {
                 @Override
                 public void perform(InputState prev, InputState next) {
                     moveState = MoveState.NONE;
                 }
             });
        input.on(and(and(mousePress(MouseEvent.MouseButton.LEFT), not(keyHeld(KeyEvent.KeyCode.LEFT_META))),
                     not(keyHeld(KeyEvent.KeyCode.LEFT_CONTROL)))).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                moveState = MoveState.CAMERA;
                viewTrackBall.startDrag(getNormalizedDeviceX(next.getMouseState().getX()),
                                        getNormalizedDeviceY(next.getMouseState().getY()));
            }
        });
        input.on(mouseMove(true)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                if (moveState == MoveState.CAMERA) {
                    viewTrackBall.drag(getNormalizedDeviceX(next.getMouseState().getX()),
                                       getNormalizedDeviceY(next.getMouseState().getY()),
                                       viewTrackBall.getRotation());
                    updateCamera();

                } else if (moveState == MoveState.OBJECT) {
                    modelTrackBall.drag(getNormalizedDeviceX(next.getMouseState().getX()),
                                        getNormalizedDeviceY(next.getMouseState().getY()),
                                        viewTrackBall.getRotation());
                    updateGBuffer();
                }
            }
        });
        input.on(or(mousePress(MouseEvent.MouseButton.RIGHT),
                    and(mousePress(MouseEvent.MouseButton.LEFT), keyHeld(KeyEvent.KeyCode.LEFT_CONTROL))))
             .trigger(new Action() {
                 @Override
                 public void perform(InputState prev, InputState next) {
                     moveState = MoveState.OBJECT;
                     modelTrackBall.startDrag(getNormalizedDeviceX(next.getMouseState().getX()),
                                              getNormalizedDeviceY(next.getMouseState().getY()));
                 }
             });
        input.on(forwardScroll()).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                cameraDist = Math.max(.2, cameraDist - 0.02);
                updateCamera();
            }
        });
        input.on(backwardScroll()).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                cameraDist = Math.min(20.0, cameraDist + 0.02);
                updateCamera();
            }
        });
        input.on(keyPress(KeyEvent.KeyCode.A)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                showAxis = !showAxis;
            }
        });
        input.on(keyPress(KeyEvent.KeyCode.E)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                showCubeMap = !showCubeMap;
            }
        });
        input.on(keyPress(KeyEvent.KeyCode.Z)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                showProbe = !showProbe;
            }
        });
        input.on(keyPress(KeyEvent.KeyCode.X)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                saveCamPos = !saveCamPos;
                if (saveCamPos) {
                    probeCamPos.set(camera.getLocation());
                }
            }
        });
        input.on(keyPress(KeyEvent.KeyCode.C)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                approxProbe = !approxProbe;
            }
        });
        input.on(keyPress(KeyEvent.KeyCode.D)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                envMode++;
            }
        });
        input.on(keyPress(KeyEvent.KeyCode.SPACE)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                renderSampledMethod = !renderSampledMethod;
            }
        });
    }

    private static ShaderBuilder loadShader(Framework framework, String root) throws Exception {
        ShaderBuilder shaderBuilder = framework.newShader();

        try (BufferedReader vertIn = new BufferedReader(new InputStreamReader(AshikhminOptimizedDeferredShader.class
                                                                                      .getResourceAsStream(root +
                                                                                                           ".vert")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = vertIn.readLine()) != null) {
                sb.append(line).append('\n');
            }
            shaderBuilder.withVertexShader(sb.toString());
        } catch (IOException e) {
            throw new FrameworkException("Unable to load vertex shader", e);
        }

        try (BufferedReader fragIn = new BufferedReader(new InputStreamReader(AshikhminOptimizedDeferredShader.class
                                                                                      .getResourceAsStream(root +
                                                                                                           ".frag")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = fragIn.readLine()) != null) {
                sb.append(line).append('\n');
            }
            shaderBuilder.withFragmentShader(sb.toString());
        } catch (IOException e) {
            throw new FrameworkException("Unable to load fragment shader", e);
        }

        return shaderBuilder;
    }

    private static enum MoveState {
        NONE,
        CAMERA,
        OBJECT
    }

    private static class GBuffer {
        private static final int NORMAL_TAN_IDX = 0;
        private static final int SHINE_NTZ_IDX = 1;
        private static final int DIFFUSE_IDX = 2;
        private static final int SPECULAR_IDX = 3;

        private final TextureSurface gbuffer;
        private final Texture2D normalTangentXY;
        private final Texture2D shininessAndNTZ;
        private final Texture2D diffuseAlbedo;
        private final Texture2D specularAlbedo;

        private final DepthMap2D depth; // DEPTH24

        public GBuffer(Framework f) {
            Texture2DBuilder b = f.newTexture2D();
            b.width(WIDTH).height(HEIGHT).rgba().mipmap(0).fromHalfFloats(null);
            normalTangentXY = b.build();

            b = f.newTexture2D();
            b.width(WIDTH).height(HEIGHT).rgba().mipmap(0).fromHalfFloats(null);
            shininessAndNTZ = b.build();

            b = f.newTexture2D();
            b.width(WIDTH).height(HEIGHT).rgba().mipmap(0).fromHalfFloats(null);
            diffuseAlbedo = b.build();

            b = f.newTexture2D();
            b.width(WIDTH).height(HEIGHT).rgba().mipmap(0).fromHalfFloats(null);
            specularAlbedo = b.build();

            DepthMap2DBuilder d = f.newDepthMap2D();
            d.width(WIDTH).height(HEIGHT).depth().mipmap(0).fromUnsignedNormalized((int[]) null);
            depth = d.build();

            TextureSurfaceOptions o = new TextureSurfaceOptions().size(WIDTH, HEIGHT)
                                                                 .colorBuffers(normalTangentXY
                                                                                       .getRenderTarget(),
                                                                               shininessAndNTZ
                                                                                       .getRenderTarget(),
                                                                               diffuseAlbedo
                                                                                       .getRenderTarget(),
                                                                               specularAlbedo
                                                                                       .getRenderTarget())
                                                                 .depthBuffer(depth.getRenderTarget());
            gbuffer = f.createSurface(o);
        }
    }

    public static class Material {
        private final Sampler specularAlbedo;
        private final Sampler diffuseAlbedo;
        private final Sampler normal; // FIXME could pack diffuse normal into alpha of diffuse/specular and then see if that fixes missing vector info?
        private final Sampler shininessXY;

        public Material() {
            specularAlbedo = null;
            diffuseAlbedo = null;
            normal = null;
            shininessXY = null;
        }

        public Material(Framework f, String directory) throws IOException {
            Builder<? extends Sampler> diffAlbedo = TextureLoader.readTexture(f, new File(directory +
                                                                                          File.separator +
                                                                                          "diffuseAlbedo.hdr"));
            Builder<? extends Sampler> specNormal = TextureLoader.readTexture(f, new File(directory +
                                                                                          File.separator +
                                                                                          "specularNormal.hdr"));
            Builder<? extends Sampler> specAlbedo = TextureLoader.readTexture(f, new File(directory +
                                                                                          File.separator +
                                                                                          "specularAlbedo.hdr"));
            Builder<? extends Sampler> shininess = TextureLoader.readTexture(f, new File(directory +
                                                                                         File.separator +
                                                                                         "shininessXY.hdr"));

            diffuseAlbedo = diffAlbedo.build();
            normal = specNormal.build();
            specularAlbedo = specAlbedo.build();
            shininessXY = shininess.build();
        }

        public static class Settings {
            public final double exposureOverride;
            public final double texCoordScale;
            public final double shinyXScale;
            public final double shinyYScale;

            public final double diffuseRScale;
            public final double diffuseGScale;
            public final double diffuseBScale;

            public final double specularRScale;
            public final double specularGScale;
            public final double specularBScale;

            public Settings(double exposureOverride, double texCoordScale, double shinyXScale,
                            double shinyYScale, double diffuseRScale, double diffuseGScale,
                            double diffuseBScale, double specularRScale, double specularGScale,
                            double specularBScale) {
                this.exposureOverride = exposureOverride;
                this.texCoordScale = texCoordScale;
                this.shinyXScale = shinyXScale;
                this.shinyYScale = shinyYScale;
                this.diffuseRScale = diffuseRScale;
                this.diffuseGScale = diffuseGScale;
                this.diffuseBScale = diffuseBScale;
                this.specularRScale = specularRScale;
                this.specularGScale = specularGScale;
                this.specularBScale = specularBScale;
            }

            public Settings(String matFolder, Properties props) {
                exposureOverride = Double.parseDouble(props.getProperty(matFolder + "_exposureOverride",
                                                                        "-1.0"));
                texCoordScale = Double.parseDouble(props.getProperty(matFolder + "_texCoordScale", "1.0"));
                shinyXScale = Double.parseDouble(props.getProperty(matFolder + "_shinyXScale", "14.15"));
                shinyYScale = Double.parseDouble(props.getProperty(matFolder + "_shinyYScale", "14.15"));
                diffuseRScale = Double.parseDouble(props.getProperty(matFolder + "_diffuseRScale", "1.0"));
                diffuseGScale = Double.parseDouble(props.getProperty(matFolder + "_diffuseGScale", "1.0"));
                diffuseBScale = Double.parseDouble(props.getProperty(matFolder + "_diffuseBScale", "1.0"));
                specularRScale = Double.parseDouble(props.getProperty(matFolder + "_specularRScale", "1.0"));
                specularGScale = Double.parseDouble(props.getProperty(matFolder + "_specularGScale", "1.0"));
                specularBScale = Double.parseDouble(props.getProperty(matFolder + "_specularBScale", "1.0"));
            }

            private void store(String envFile, Properties props) {
                props.setProperty(envFile + "_exposureOverride", Double.toString(exposureOverride));
                props.setProperty(envFile + "_texCoordScale", Double.toString(texCoordScale));
                props.setProperty(envFile + "_shinyXScale", Double.toString(shinyXScale));
                props.setProperty(envFile + "_shinyYScale", Double.toString(shinyYScale));
                props.setProperty(envFile + "_diffuseRScale", Double.toString(diffuseRScale));
                props.setProperty(envFile + "_diffuseGScale", Double.toString(diffuseGScale));
                props.setProperty(envFile + "_diffuseBScale", Double.toString(diffuseBScale));
                props.setProperty(envFile + "_specularRScale", Double.toString(specularRScale));
                props.setProperty(envFile + "_specularGScale", Double.toString(specularGScale));
                props.setProperty(envFile + "_specularBScale", Double.toString(specularBScale));
            }
        }
    }

    public static class Environment {
        private final TextureCubeMap envMap;
        private final TextureCubeMap diffuseMap;
        private final TextureCubeMap[] specularMaps;
        private final List<EnvironmentMap.Sample> samples;

        public Environment() {
            envMap = null;
            diffuseMap = null;
            specularMaps = new TextureCubeMap[1];
            samples = new ArrayList<>();
        }

        public Environment(Framework f, File in) throws IOException {
            EnvironmentMap map = EnvironmentMap.loadFromFile(in);
            specularMaps = map.createSpecularMaps(f);
            envMap = map.createEnvironmentMap(f);
            diffuseMap = map.createDiffuseMap(f);
            samples = map.getSamples();
        }


        public static class Settings {
            public final double gamma;
            public final double fstop;
            public final double exposure;
            public final double sensitivity;

            public Settings(double gamma, double fstop, double exposure, double sensitivity) {
                this.gamma = gamma;
                this.fstop = fstop;
                this.exposure = exposure;
                this.sensitivity = sensitivity;
            }

            public Settings(String envFile, Properties props) {
                gamma = Double.parseDouble(props.getProperty(envFile + "_gamma", "2.2"));
                fstop = Double.parseDouble(props.getProperty(envFile + "_fstop", "2.0"));
                exposure = Double.parseDouble(props.getProperty(envFile + "_exposure", "0.5"));
                sensitivity = Double.parseDouble(props.getProperty(envFile + "_sensitivity", "500"));
            }

            private void store(String envFile, Properties props) {
                props.setProperty(envFile + "_gamma", Double.toString(gamma));
                props.setProperty(envFile + "_fstop", Double.toString(fstop));
                props.setProperty(envFile + "_exposure", Double.toString(exposure));
                props.setProperty(envFile + "_sensitivity", Double.toString(sensitivity));
            }
        }
    }
}
