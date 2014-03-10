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
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.builder.Builder;
import com.ferox.renderer.builder.DepthMap2DBuilder;
import com.ferox.renderer.builder.ShaderBuilder;
import com.ferox.renderer.builder.Texture2DBuilder;
import com.ferox.renderer.geom.Geometry;
import com.ferox.renderer.geom.Shapes;
import com.ferox.renderer.loader.TextureLoader;
import com.ferox.util.ApplicationStub;
import com.ferox.util.profile.Profiler;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
    private final JFrame properties;
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
    private final Shader tonemapShader;

    private volatile String lastLLSDirectory;
    private volatile String lastEnvDirectory;

    // environment mapping
    private final Geometry envCube;
    private volatile Environment environment;
    private boolean showCubeMap = true;
    private int envMode = 0;

    // tone mapping
    private volatile double exposure = 0.1;
    private volatile double sensitivity = 500;
    private volatile double fstop = 2.8;
    private volatile double gamma = 2.2;

    // blending
    private volatile double normalAlpha = 0.0;
    private volatile double specularAlbedoAlpha = 0.0;
    private volatile double diffuseAlbedoAlpha = 0.0;
    private volatile double shininessAlpha = 0.0;

    // appearance tweaks
    private volatile double shininessXScale = 1.0;
    private volatile double shininessYScale = 1.0;
    private volatile double texCoordAScale = 1.0;
    private volatile double texCoordBScale = 1.0;
    private volatile double shininessOverride = -1.0;

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
    private int sample;

    // input textures
    private volatile Material matA;
    private volatile Material matB;

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

        shininessXScale = 1.0;
        shininessYScale = 1.0;
        texCoordAScale = 1.0;
        texCoordBScale = 1.0;

        moveState = MoveState.NONE;

        camera = new Frustum(60, WIDTH / (float) HEIGHT, 0.1, 25);
        cameraDist = 1.5;

        gbuffer = new GBuffer(framework);

        fullscreenQuad = Shapes.createRectangle(framework, 0, WIDTH, 0, HEIGHT);
        fullscreenProjection = new Frustum(true, 0, WIDTH, 0, HEIGHT, -1.0, 1.0);

        environment = new Environment();
        envCube = Shapes.createBox(framework, 6.0);
        matA = new Material();
        matB = new Material();

        //        shape = Shapes.createSphere(framework, 0.5, 128);
        shape = Shapes.createTeapot(framework);

        showAxis = true;
        xAxis = Shapes.createCylinder(framework, new Vector3(1, 0, 0), new Vector3(1, 0, 0), 0.01, 0.5, 4);
        yAxis = Shapes.createCylinder(framework, new Vector3(0, 1, 0), new Vector3(0, 1, 0), 0.01, 0.5, 4);
        zAxis = Shapes.createCylinder(framework, new Vector3(0, 0, 1), new Vector3(0, 0, 1), 0.01, 0.5, 4);

        simpleShader = loadShader(framework, "simple").bindColorBuffer("fColor", 0).build();
        lightingShader = loadShader(framework, "ashik-lighting").bindColorBuffer("fColor", 0).build();
        completeShader = loadShader(framework, "ashik-complete").bindColorBuffer("fColor", 0).build();
        tonemapShader = loadShader(framework, "tonemap").bindColorBuffer("fColor", 0).build();
        gbufferShader = loadShader(framework, "ashik-gbuffer2")
                                .bindColorBuffer("fNormalTangent", GBuffer.NORMAL_TAN_IDX)
                                .bindColorBuffer("fShininessAndView", GBuffer.SHINE_VIEW_IDX)
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

        properties = new JFrame("Properties");
        configurePropertiesWindow();
        updateCamera();
    }

    private static JSlider createSlider(int min, int max) {
        JSlider slider = new JSlider(min, max);
        slider.setPaintLabels(false);
        slider.setPaintTicks(false);
        slider.setSnapToTicks(true);
        slider.setValue(0);
        return slider;
    }

    @Override
    public void run() {
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

            // fill gbuffer
            r.setShader(gbufferShader);
            r.setUniform(gbufferShader.getUniform("uProjection"), camera.getProjectionMatrix());
            r.setUniform(gbufferShader.getUniform("uView"), camera.getViewMatrix());
            r.setUniform(gbufferShader.getUniform("uModel"), modelTrackBall.getTransform());
            r.setUniform(gbufferShader.getUniform("uCamPos"), camera.getLocation());

            r.setUniform(gbufferShader.getUniform("uNormalTexA"), matA.normal);
            r.setUniform(gbufferShader.getUniform("uNormalTexB"), matB.normal);
            r.setUniform(gbufferShader.getUniform("uNormalAlpha"), normalAlpha);

            r.setUniform(gbufferShader.getUniform("uDiffuseAlbedoA"), matA.diffuseAlbedo);
            r.setUniform(gbufferShader.getUniform("uDiffuseAlbedoB"), matB.diffuseAlbedo);
            r.setUniform(gbufferShader.getUniform("uDiffuseAlbedoAlpha"), diffuseAlbedoAlpha);

            r.setUniform(gbufferShader.getUniform("uSpecularAlbedoA"), matA.specularAlbedo);
            r.setUniform(gbufferShader.getUniform("uSpecularAlbedoB"), matB.specularAlbedo);
            r.setUniform(gbufferShader.getUniform("uSpecularAlbedoAlpha"), specularAlbedoAlpha);

            r.setUniform(gbufferShader.getUniform("uShininessA"), matA.shininessXY);
            r.setUniform(gbufferShader.getUniform("uShininessB"), matB.shininessXY);
            r.setUniform(gbufferShader.getUniform("uShininessAlpha"), shininessAlpha);
            r.setUniform(gbufferShader.getUniform("uShininessScale"), shininessXScale, shininessYScale);
            r.setUniform(gbufferShader.getUniform("uShinyOverride"), shininessOverride);

            r.setUniform(gbufferShader.getUniform("uTCScale"), texCoordAScale, texCoordBScale);

            r.bindAttribute(gbufferShader.getAttribute("aPos"), shape.getVertices());
            r.bindAttribute(gbufferShader.getAttribute("aNorm"), shape.getNormals());
            r.bindAttribute(gbufferShader.getAttribute("aTan"), shape.getTangents());
            r.bindAttribute(gbufferShader.getAttribute("aTC"), shape.getTextureCoordinates());

            r.setIndices(shape.getIndices());
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

        if (renderApproximation) {
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
            r.setUniform(lightingShader.getUniform("uDiffuseMode"), true);
            r.setUniform(lightingShader.getUniform("uIrradianceMin"), environment.diffuseMap);

            r.setUniform(lightingShader.getUniform("uNormalAndTangent"), gbuffer.normalTangentXY);
            r.setUniform(lightingShader.getUniform("uShininessAndView"), gbuffer.shininessAndViewXY);
            r.setUniform(lightingShader.getUniform("uDiffuseAlbedo"), gbuffer.diffuseAlbedo);
            r.setUniform(lightingShader.getUniform("uSpecularAlbedo"), gbuffer.specularAlbedo);

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

            r.setUniform(completeShader.getUniform("uNormalAndTangent"), gbuffer.normalTangentXY);
            r.setUniform(completeShader.getUniform("uShininessAndView"), gbuffer.shininessAndViewXY);
            r.setUniform(completeShader.getUniform("uDiffuseAlbedo"), gbuffer.diffuseAlbedo);
            r.setUniform(completeShader.getUniform("uSpecularAlbedo"), gbuffer.specularAlbedo);

            r.setUniform(completeShader.getUniform("uProjection"), fullscreenProjection.getProjectionMatrix());
            r.bindAttribute(completeShader.getAttribute("aPos"), fullscreenQuad.getVertices());
            r.bindAttribute(completeShader.getAttribute("aTC"), fullscreenQuad.getTextureCoordinates());
            r.setIndices(fullscreenQuad.getIndices());

            r.setBlendingEnabled(true);
            r.setBlendMode(Renderer.BlendFunction.ADD, Renderer.BlendFactor.ONE, Renderer.BlendFactor.ONE);

            for (int p = 0; p < 1; p++) {
                for (int s = 0; s < 40; s++) {
                    if (sample + s < environment.samples.size()) {
                        EnvironmentMap.Sample smp = environment.samples.get(sample + s);
                        r.setUniformArray(completeShader.getUniform("uLightDirection"), s, smp.direction);
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
            r.setUniform(tonemapShader.getUniform("uGamma"), gamma);
            r.setUniform(tonemapShader.getUniform("uSensitivity"), sensitivity);
            r.setUniform(tonemapShader.getUniform("uExposure"), exposure);
            r.setUniform(tonemapShader.getUniform("uFstop"), fstop);

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
            r.setUniform(simpleShader.getUniform("uGamma"), gamma);
            r.setUniform(simpleShader.getUniform("uSensitivity"), sensitivity);
            r.setUniform(simpleShader.getUniform("uExposure"), exposure);
            r.setUniform(simpleShader.getUniform("uFstop"), fstop);

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
        generateRealProbe(v, n, tv, bv, shininessXScale, shininessYScale, false, 1.0);
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

    private void generateApproximateProbe(@Const Vector3 v, @Const Vector3 n, @Const Vector3 tv,
                                          @Const Vector3 bv) {
        Vector3 r = reflect(v, n);
        Vector3 rb = reflect(r, bv).scale(-1);
        Vector3 rt = reflect(r, tv).scale(-1);

        Vector3 t;
        Vector3 m;
        double lowExp;
        double shininess;
        if (shininessXScale > shininessYScale) {
            t = rb;
            m = bv;
            lowExp = shininessYScale;
            shininess = shininessXScale;
        } else {
            t = rt;
            m = tv;
            lowExp = shininessXScale;
            shininess = shininessYScale;
        }

        Vector3 l = new Vector3();
        Vector3 h = new Vector3();

        // NOTES very promising, mostly matches shape except at direct anisotropic view angles, we don't
        // rotate past the reflected axis yet, which we should
        /*for (int i = 0; i < 30; i++) {
            double a = (i + 1) / 30.0;
            // main band
            l.scale(r, a).addScaled(1 - a, t).normalize();
            double a2 = (1.0 - l.dot(n)) * (0.25 - Math.pow(a - 0.5, 2));
            l.scale(1 - a2).addScaled(a2, n).normalize();
            h.add(l, v).normalize();
            double magnitude = Math.pow(n.dot(h), lowExp);

            l.scale(magnitude).get(probeLines, i * 3 + SPEC_VECTOR_COUNT * 3);

            // band past the reflection vector
            l.scale(r, a).addScaled(a - 1, t).normalize();
            h.add(l, v).normalize();
            magnitude = Math.pow(n.dot(h), lowExp);
            l.scale(magnitude).get(probeLines, (i + 30) * 3 + SPEC_VECTOR_COUNT * 3);

            // band past the reflected t vector
            l.scale(r, -a).addScaled(1 - a, t).normalize();
            h.add(l, v).normalize();
            magnitude = Math.pow(n.dot(h), lowExp);
            l.scale(magnitude).get(probeLines, (i + 60) * 3 + SPEC_VECTOR_COUNT * 3);

        }
        for (int i = 90; i < SPHERE_PHI_RES * SPHERE_THETA_RES; i++) {
            l.set(0, 0, 0);
            l.get(probeLines, i * 3 + SPEC_VECTOR_COUNT * 3);
        }*/

        generateRealProbe(v, n, tv, bv, shininess, shininess, false, 1.0);

        if (t.dot(m) >= 0) {
            l.add(t, m).normalize();
            h.add(l, v).normalize();
            double weight = Math.pow(n.dot(h), lowExp);
            // and reflect back around normal to get view
            generateRealProbe(reflect(l, n), n, tv, bv, shininess, shininess, true, weight);

            l.scale(m, -1).add(t).normalize();
            h.add(l, v).normalize();
            weight = Math.pow(n.dot(h), lowExp);
            // and reflect back around normal to get view
            generateRealProbe(reflect(l, n), n, tv, bv, shininess, shininess, true, weight);
        } else {
            l.set(t).addScaled(-1, m).normalize();
            h.add(l, v).normalize();
            double weight = Math.pow(n.dot(h), lowExp);
            // and reflect back around normal to get view
            generateRealProbe(reflect(l, n), n, tv, bv, shininess, shininess, true, weight);

            l.add(t, m).normalize();
            h.add(l, v).normalize();
            weight = Math.pow(n.dot(h), lowExp);
            // and reflect back around normal to get view
            generateRealProbe(reflect(l, n), n, tv, bv, shininess, shininess, true, weight);
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

    private void updateGBuffer() {
        invalidateGbuffer = true;
    }

    private void updateCamera() {
        Matrix4 camT = new Matrix4()
                               .lookAt(new Vector3(), new Vector3(0, 0, cameraDist), new Vector3(0, 1, 0));
        camT.mul(viewTrackBall.getTransform(), camT);
        camera.setOrientation(camT);

        updateGBuffer();
    }

    private void loadTexturesA(final String directory) {
        new Thread("texture loader") {
            @Override
            public void run() {
                try {
                    matA = new Material(getFramework(), directory);
                    updateGBuffer();
                } catch (IOException e) {
                    System.err.println("Error loading images:");
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void loadTexturesB(final String directory) {
        new Thread("texture loader") {
            @Override
            public void run() {
                try {
                    matB = new Material(getFramework(), directory);
                    updateGBuffer();
                } catch (IOException e) {
                    System.err.println("Error loading images:");
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void loadEnvironment(final String file) {
        new Thread("env loader") {
            @Override
            public void run() {
                try {
                    environment = new Environment(getFramework(), new File(file));
                    updateGBuffer();
                } catch (IOException e) {
                    System.err.println("Error loading images:");
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void configurePropertiesWindow() {
        properties.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        JPanel pl = new JPanel();
        GroupLayout layout = new GroupLayout(pl);
        pl.setLayout(layout);
        properties.add(pl);

        JButton loadEnv = new JButton("Load Environment");
        final JLabel envLabel = new JLabel("None");
        loadEnv.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(lastEnvDirectory);
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String file = fc.getSelectedFile().getAbsolutePath();
                    loadEnvironment(file);
                    envLabel.setText(fc.getSelectedFile().getName());
                    properties.pack();
                    lastEnvDirectory = fc.getSelectedFile().getParentFile().getAbsolutePath();
                }
            }
        });

        JButton loadTexturesA = new JButton("Load Textures A");
        final JLabel texLabelA = new JLabel("None");
        loadTexturesA.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(lastLLSDirectory);
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String dir = fc.getSelectedFile().getAbsolutePath();
                    loadTexturesA(dir);
                    texLabelA.setText(fc.getSelectedFile().getName());
                    properties.pack();
                    lastLLSDirectory = fc.getSelectedFile().getParentFile().getAbsolutePath();
                }
            }
        });

        JButton loadTexturesB = new JButton("Load Textures B");
        final JLabel texLabelB = new JLabel("None");
        loadTexturesB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(lastLLSDirectory);
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String dir = fc.getSelectedFile().getAbsolutePath();
                    loadTexturesB(dir);
                    texLabelB.setText(fc.getSelectedFile().getName());
                    properties.pack();
                    lastLLSDirectory = fc.getSelectedFile().getParentFile().getAbsolutePath();
                }
            }
        });

        final JSlider fullSlider = createSlider(0, 1000);
        final JSlider normalSlider = createSlider(0, 1000);
        final JSlider specAlbedoSlider = createSlider(0, 1000);
        final JSlider diffAlbedoSlider = createSlider(0, 1000);
        final JSlider shininessSlider = createSlider(0, 1000);

        JLabel fullLabel = new JLabel("All");
        fullSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (fullSlider.getValueIsAdjusting()) {
                    // Update the other sliders
                    normalSlider.setValue(fullSlider.getValue());
                    specAlbedoSlider.setValue(fullSlider.getValue());
                    diffAlbedoSlider.setValue(fullSlider.getValue());
                    shininessSlider.setValue(fullSlider.getValue());
                }
            }
        });
        JLabel normalLabel = new JLabel("Normals");
        normalSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                normalAlpha = normalSlider.getValue() / 1000.0;
                updateGBuffer();

                if (normalSlider.getValueIsAdjusting()) {
                    fullSlider
                            .setValue((int) (1000 * (normalAlpha + specularAlbedoAlpha + diffuseAlbedoAlpha +
                                                     shininessAlpha) / 4.0));
                }
            }
        });
        JLabel specLabel = new JLabel("Specular");
        specAlbedoSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                specularAlbedoAlpha = specAlbedoSlider.getValue() / 1000.0;
                updateGBuffer();

                if (specAlbedoSlider.getValueIsAdjusting()) {
                    fullSlider
                            .setValue((int) (1000 * (normalAlpha + specularAlbedoAlpha + diffuseAlbedoAlpha +
                                                     shininessAlpha) / 4.0));
                }
            }
        });
        JLabel diffLabel = new JLabel("Diffuse");
        diffAlbedoSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                diffuseAlbedoAlpha = diffAlbedoSlider.getValue() / 1000.0;
                updateGBuffer();

                if (diffAlbedoSlider.getValueIsAdjusting()) {
                    fullSlider
                            .setValue((int) (1000 * (normalAlpha + specularAlbedoAlpha + diffuseAlbedoAlpha +
                                                     shininessAlpha) / 4.0));
                }
            }
        });
        JLabel shinyLabel = new JLabel("Shininess");
        shininessSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessAlpha = shininessSlider.getValue() / 1000.0;
                updateGBuffer();

                if (shininessSlider.getValueIsAdjusting()) {
                    fullSlider
                            .setValue((int) (1000 * (normalAlpha + specularAlbedoAlpha + diffuseAlbedoAlpha +
                                                     shininessAlpha) / 4.0));
                }
            }
        });

        JLabel expULabel = new JLabel("Shiny U Scale");
        final JSlider expUSlider = createSlider(10, 1000);
        expUSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessXScale = expUSlider.getValue() / 10.0;
                updateGBuffer();
            }
        });
        JLabel expVLabel = new JLabel("Shiny V Scale");
        final JSlider expVSlider = createSlider(10, 1000);
        expVSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessYScale = expVSlider.getValue() / 10.0;
                updateGBuffer();
            }
        });
        JLabel shinyOverLabel = new JLabel("Exponent Override");
        final JSpinner shinyOverSlider = new JSpinner(new SpinnerNumberModel(shininessOverride, -1.0,
                                                                             100000.0, 1.0));
        shinyOverSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessOverride = (Double) shinyOverSlider.getValue();
                updateGBuffer();
            }
        });

        JLabel tcALabel = new JLabel("TC A Scale");
        final JSlider tcASlider = createSlider(100, 2400);
        tcASlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                texCoordAScale = Math.pow(1.3, tcASlider.getValue() / 100.0);
                updateGBuffer();
            }
        });
        JLabel tcBLabel = new JLabel("TC B Scale");
        final JSlider tcBSlider = createSlider(100, 2400);
        tcBSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                texCoordBScale = Math.pow(1.3, tcBSlider.getValue() / 100.0);
                updateGBuffer();
            }
        });

        JLabel sensitivityLabel = new JLabel("Film ISO");
        final JSpinner sensitivitySlider = new JSpinner(new SpinnerNumberModel(sensitivity, 20, 8000, 10));
        sensitivitySlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                sensitivity = (Double) sensitivitySlider.getValue();
            }
        });
        JLabel exposureLabel = new JLabel("Shutter Speed");
        final JSpinner exposureSlider = new JSpinner(new SpinnerNumberModel(exposure, 0.00001, 30, 0.001));
        exposureSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                exposure = (Double) exposureSlider.getValue();
            }
        });
        JLabel fstopLabel = new JLabel("F-Stop");
        final JSpinner fstopSlider = new JSpinner(new SpinnerNumberModel(fstop, 0.5, 128, 0.1));
        fstopSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                fstop = (Double) fstopSlider.getValue();
            }
        });
        JLabel gammaLabel = new JLabel("Gamma");
        final JSpinner gammaSlider = new JSpinner(new SpinnerNumberModel(gamma, 0.0, 5, 0.01));
        gammaSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gamma = (Double) gammaSlider.getValue();
            }
        });

        layout.setHorizontalGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup().addComponent(loadEnv)
                                                        .addComponent(loadTexturesA).addComponent(tcASlider)
                                                        .addComponent(loadTexturesB).addComponent(tcBSlider)
                                                        .addComponent(fullSlider).addComponent(normalSlider)
                                                        .addComponent(diffAlbedoSlider)
                                                        .addComponent(specAlbedoSlider)
                                                        .addComponent(shininessSlider)
                                                        .addComponent(expUSlider).addComponent(expVSlider)
                                                        .addComponent(shinyOverSlider)
                                                        .addComponent(sensitivitySlider)
                                                        .addComponent(exposureSlider)
                                                        .addComponent(fstopSlider).addComponent(gammaSlider))
                                        .addGroup(layout.createParallelGroup().addComponent(envLabel)
                                                        .addComponent(texLabelA).addComponent(tcALabel)
                                                        .addComponent(texLabelB).addComponent(tcBLabel)
                                                        .addComponent(fullLabel).addComponent(normalLabel)
                                                        .addComponent(diffLabel).addComponent(specLabel)
                                                        .addComponent(shinyLabel).addComponent(expULabel)
                                                        .addComponent(expVLabel).addComponent(shinyOverLabel)
                                                        .addComponent(sensitivityLabel)
                                                        .addComponent(exposureLabel).addComponent(fstopLabel)
                                                        .addComponent(gammaLabel)));
        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addGroup(layout.createParallelGroup().addComponent(loadEnv)
                                                      .addComponent(envLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(loadTexturesA)
                                                      .addComponent(texLabelA))
                                      .addGroup(layout.createParallelGroup().addComponent(tcASlider)
                                                      .addComponent(tcALabel)).addGap(10)
                                      .addGroup(layout.createParallelGroup().addComponent(loadTexturesB)
                                                      .addComponent(texLabelB))
                                      .addGroup(layout.createParallelGroup().addComponent(tcBSlider)
                                                      .addComponent(tcBLabel)).addGap(15)
                                      .addGroup(layout.createParallelGroup().addComponent(fullSlider)
                                                      .addComponent(fullLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(normalSlider)
                                                      .addComponent(normalLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(diffAlbedoSlider)
                                                      .addComponent(diffLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(specAlbedoSlider)
                                                      .addComponent(specLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(shininessSlider)
                                                      .addComponent(shinyLabel)).addGap(15)
                                      .addGroup(layout.createParallelGroup().addComponent(expUSlider)
                                                      .addComponent(expULabel))
                                      .addGroup(layout.createParallelGroup().addComponent(expVSlider)
                                                      .addComponent(expVLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(shinyOverSlider)
                                                      .addComponent(shinyOverLabel)).addGap(15).addGroup(layout.createParallelGroup().addComponent(sensitivitySlider)
                                                      .addComponent(sensitivityLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(exposureSlider)
                                                      .addComponent(exposureLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(fstopSlider)
                                                      .addComponent(fstopLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(gammaSlider)
                                                      .addComponent(gammaLabel)).addGap(15)
                                      .addGroup(layout.createParallelGroup()));

        properties.pack();
        properties.setLocation(810, 0);

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
        private static final int SHINE_VIEW_IDX = 1;
        private static final int DIFFUSE_IDX = 2;
        private static final int SPECULAR_IDX = 3;

        private final TextureSurface gbuffer;
        private final Texture2D normalTangentXY; // encoded so we don't need Z
        private final Texture2D shininessAndViewXY;
        private final Texture2D diffuseAlbedo;
        private final Texture2D specularAlbedo;

        private final DepthMap2D depth; // DEPTH24

        public GBuffer(Framework f) {
            Texture2DBuilder b = f.newTexture2D();
            b.width(WIDTH).height(HEIGHT).rgba().mipmap(0).fromHalfFloats(null);
            normalTangentXY = b.build();

            b = f.newTexture2D();
            b.width(WIDTH).height(HEIGHT).rgba().mipmap(0).fromHalfFloats(null);
            shininessAndViewXY = b.build();

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
                                                                               shininessAndViewXY
                                                                                       .getRenderTarget(),
                                                                               diffuseAlbedo
                                                                                       .getRenderTarget(),
                                                                               specularAlbedo
                                                                                       .getRenderTarget())
                                                                 .depthBuffer(depth.getRenderTarget());
            gbuffer = f.createSurface(o);
        }
    }

    private static class Material {
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
    }

    private static class Environment {
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
    }
}
