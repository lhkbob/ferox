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
import com.ferox.math.Matrix3;
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
import java.util.Collections;
import java.util.List;

import static com.ferox.input.logic.Predicates.*;

/**
 *
 */
public class AshikhminOptimizedDeferredShader extends ApplicationStub implements Task<Void> {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;

    private static final String DEFAULT_LLS_DIR = "/Users/mludwig/Desktop/LLS";
    private static final String DEFAULT_ENV_DIR = "/Users/mludwig/Desktop/";

    private static final int SAMPLES_PER_FRAME = 40;

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
    private final Shader tonemapShader;

    // environment mapping
    private final Geometry envCube;
    private volatile Environment environment;
    private boolean showCubeMap = true;
    private boolean showDiffMap = false;

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

    // geometry
    private final Geometry shape;

    private final Geometry xAxis;
    private final Geometry yAxis;
    private final Geometry zAxis;
    private boolean showAxis;

    // camera controls
    private final TrackBall modelTrackBall;
    private final TrackBall viewTrackBall;
    private final Frustum camera;
    private double cameraDist;
    private MoveState moveState;

    private volatile boolean invalidateGbuffer;
    private int specularSamplesLeft;

    private volatile int samplesPerFrame = 40;

    // input textures
    private volatile Material matA;
    private volatile Material matB;

    public static void main(String[] args) throws Exception {
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
        tonemapShader = loadShader(framework, "tonemap").bindColorBuffer("fColor", 0).build();
        gbufferShader = loadShader(framework, "ashik-gbuffer2")
                                .bindColorBuffer("fNormalTangent", GBuffer.NORMAL_TAN_IDX)
                                .bindColorBuffer("fTexCoordAndView", GBuffer.TC_VIEW_IDX).build();

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
        getFramework().invoke(this);
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

            r.setUniform(gbufferShader.getUniform("uNormalTexA"), matA.normal);
            r.setUniform(gbufferShader.getUniform("uNormalTexB"), matB.normal);

            r.setUniform(gbufferShader.getUniform("uNormalAlpha"), normalAlpha);

            r.setUniform(gbufferShader.getUniform("uTCScale"), texCoordAScale, texCoordBScale);

            r.bindAttribute(gbufferShader.getAttribute("aPos"), shape.getVertices());
            r.bindAttribute(gbufferShader.getAttribute("aNorm"), shape.getNormals());
            r.bindAttribute(gbufferShader.getAttribute("aTan"), shape.getTangents());
            r.bindAttribute(gbufferShader.getAttribute("aTC"), shape.getTextureCoordinates());

            r.setIndices(shape.getIndices());
            r.render(shape.getPolygonType(), shape.getIndexOffset(), shape.getIndexCount());
            ctx.flush();
            Profiler.pop();

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
            r.setUniform(lightingShader.getUniform("uDiffuseIrradiance"), environment.diffuseMap);

            Matrix3 invView = new Matrix3().setUpper(camera.getViewMatrix()).inverse();
            r.setUniform(lightingShader.getUniform("uInvView"), invView);

            r.setUniform(lightingShader.getUniform("uAlbedoA"), matA.diffuseAlbedo);
            r.setUniform(lightingShader.getUniform("uAlbedoB"), matB.diffuseAlbedo);
            r.setUniform(lightingShader.getUniform("uAlbedoAlpha"), diffuseAlbedoAlpha);

            r.setUniform(lightingShader.getUniform("uNormalAndTangent"), gbuffer.normalTangentXY);
            r.setUniform(lightingShader.getUniform("uTCAndView"), gbuffer.texCoordAndViewXY);
            r.setUniform(lightingShader.getUniform("uTCScale"), texCoordAScale, texCoordBScale);

            r.setUniform(lightingShader.getUniform("uProjection"),
                         fullscreenProjection.getProjectionMatrix());
            r.bindAttribute(lightingShader.getAttribute("aPos"), fullscreenQuad.getVertices());
            r.bindAttribute(lightingShader.getAttribute("aTC"), fullscreenQuad.getTextureCoordinates());
            r.setIndices(fullscreenQuad.getIndices());
            r.render(fullscreenQuad.getPolygonType(), fullscreenQuad.getIndexOffset(),
                     fullscreenQuad.getIndexCount());
            ctx.flush();
            Profiler.pop();

            specularSamplesLeft = environment.samples.size();
            invalidateGbuffer = false;
        }

        if (specularSamplesLeft > 0) {
            Profiler.push("specular");
            ctx = access.setActiveSurface(lightingSurface);
            if (ctx == null) {
                return null;
            }
            r = ctx.getGlslRenderer();

            if (invalidateGbuffer) {
                r.clear(true, false, false, new Vector4(0.0, 0.0, 0.0, 1.0), 1.0, 0);
            }
            // avoid the clear and just overwrite everything
            r.setDepthTest(Renderer.Comparison.ALWAYS);
            r.setDepthWriteMask(false);

            r.setBlendingEnabled(true);
            r.setBlendMode(Renderer.BlendFunction.ADD, Renderer.BlendFactor.ONE, Renderer.BlendFactor.ONE);

            r.setShader(lightingShader);
            r.setUniform(lightingShader.getUniform("uDiffuseMode"), false);
            r.setUniform(lightingShader.getUniform("uAlbedoA"), matA.specularAlbedo);
            r.setUniform(lightingShader.getUniform("uAlbedoB"), matB.specularAlbedo);
            r.setUniform(lightingShader.getUniform("uAlbedoAlpha"), specularAlbedoAlpha);

            r.setUniform(lightingShader.getUniform("uNormalAndTangent"), gbuffer.normalTangentXY);
            r.setUniform(lightingShader.getUniform("uTCAndView"), gbuffer.texCoordAndViewXY);
            r.setUniform(lightingShader.getUniform("uTCScale"), texCoordAScale, texCoordBScale);

            r.setUniform(lightingShader.getUniform("uShininessA"), matA.shininessXY);
            r.setUniform(lightingShader.getUniform("uShininessB"), matB.shininessXY);
            r.setUniform(lightingShader.getUniform("uShininessAlpha"), shininessAlpha);
            r.setUniform(lightingShader.getUniform("uShininessScale"), shininessXScale, shininessYScale);

            r.setUniform(lightingShader.getUniform("uProjection"),
                         fullscreenProjection.getProjectionMatrix());
            r.bindAttribute(lightingShader.getAttribute("aPos"), fullscreenQuad.getVertices());
            r.bindAttribute(lightingShader.getAttribute("aTC"), fullscreenQuad.getTextureCoordinates());
            r.setIndices(fullscreenQuad.getIndices());

            Vector3 lightDir = new Vector3();
            int numPasses = samplesPerFrame / 40;
            for (int i = 0; i < numPasses; i++) {
                Profiler.push("specular-sample");
                for (int j = 0; j < 40; j++) {
                    if (specularSamplesLeft >= 1) {
                        EnvironmentMap.Sample s = environment.samples.get(specularSamplesLeft - 1);
                        r.setUniformArray(lightingShader.getUniform("uLightDirection"), j,
                                          lightDir.transform(camera.getViewMatrix(), s.direction, 0)
                                                  .normalize());
                        r.setUniformArray(lightingShader.getUniform("uLightRadiance"), j, s.illumination);
                        specularSamplesLeft--;
                    } else {
                        // fill in with black
                        r.setUniformArray(lightingShader.getUniform("uLightDirection"), j, new Vector3());
                        r.setUniformArray(lightingShader.getUniform("uLightRadiance"), j, new Vector3());
                    }
                }

                r.render(fullscreenQuad.getPolygonType(), fullscreenQuad.getIndexOffset(),
                         fullscreenQuad.getIndexCount());
                Profiler.pop();
            }
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

        r.setShader(simpleShader);
        r.setUniform(simpleShader.getUniform("uProjection"), camera.getProjectionMatrix());
        r.setUniform(simpleShader.getUniform("uView"), camera.getViewMatrix());
        r.setUniform(simpleShader.getUniform("uUseEnvMap"), false);

        // axis rendering
        if (showAxis) {
            r.setUniform(simpleShader.getUniform("uModel"), new Matrix4().setIdentity());
            r.setUniform(simpleShader.getUniform("uSolidColor"), new Vector4(1, 0, 0, 1));

            r.bindAttribute(simpleShader.getAttribute("aPos"), xAxis.getVertices());

            r.setIndices(xAxis.getIndices());
            r.render(xAxis.getPolygonType(), xAxis.getIndexOffset(), xAxis.getIndexCount());

            r.setUniform(simpleShader.getUniform("uSolidColor"), new Vector4(0, 1, 0, 1));
            r.bindAttribute(simpleShader.getAttribute("aPos"), yAxis.getVertices());

            r.setIndices(yAxis.getIndices());
            r.render(yAxis.getPolygonType(), yAxis.getIndexOffset(), yAxis.getIndexCount());

            r.setUniform(simpleShader.getUniform("uSolidColor"), new Vector4(0, 0, 1, 1));
            r.bindAttribute(simpleShader.getAttribute("aPos"), zAxis.getVertices());

            r.setIndices(zAxis.getIndices());
            r.render(zAxis.getPolygonType(), zAxis.getIndexOffset(), zAxis.getIndexCount());
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
            r.setUniform(simpleShader.getUniform("uEnvMap"),
                         (showDiffMap ? environment.diffuseMap : environment.envMap));

            r.bindAttribute(simpleShader.getAttribute("aPos"), envCube.getVertices());

            r.setIndices(envCube.getIndices());
            r.render(envCube.getPolygonType(), envCube.getIndexOffset(), envCube.getIndexCount());
        }

        //        ctx.flush();
        Profiler.pop();
        Profiler.pop();
        return null;
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
                JFileChooser fc = new JFileChooser(DEFAULT_ENV_DIR);
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String file = fc.getSelectedFile().getAbsolutePath();
                    loadEnvironment(file);
                    envLabel.setText(fc.getSelectedFile().getName());
                    properties.pack();
                }
            }
        });

        JButton loadTexturesA = new JButton("Load Textures A");
        final JLabel texLabelA = new JLabel("None");
        loadTexturesA.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(DEFAULT_LLS_DIR);
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String dir = fc.getSelectedFile().getAbsolutePath();
                    loadTexturesA(dir);
                    texLabelA.setText(fc.getSelectedFile().getName());
                    properties.pack();
                }
            }
        });

        JButton loadTexturesB = new JButton("Load Textures B");
        final JLabel texLabelB = new JLabel("None");
        loadTexturesB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(DEFAULT_LLS_DIR);
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String dir = fc.getSelectedFile().getAbsolutePath();
                    loadTexturesB(dir);
                    texLabelB.setText(fc.getSelectedFile().getName());
                    properties.pack();
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

        JLabel tcALabel = new JLabel("TC A Scale");
        final JSlider tcASlider = createSlider(100, 100000);
        tcASlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                texCoordAScale = tcASlider.getValue() / 100.0;
                updateGBuffer();
            }
        });
        JLabel tcBLabel = new JLabel("TC B Scale");
        final JSlider tcBSlider = createSlider(100, 100000);
        tcBSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                texCoordBScale = tcBSlider.getValue() / 100.0;
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

        JLabel samplesLabel = new JLabel("Samples");
        final JSpinner samplesSlider = new JSpinner(new SpinnerNumberModel(samplesPerFrame, 10, 1000, 10));
        samplesSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                samplesPerFrame = (Integer) samplesSlider.getValue();
                updateGBuffer();
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
                                                        .addComponent(sensitivitySlider)
                                                        .addComponent(exposureSlider)
                                                        .addComponent(fstopSlider).addComponent(gammaSlider)
                                                        .addComponent(samplesSlider))
                                        .addGroup(layout.createParallelGroup().addComponent(envLabel)
                                                        .addComponent(texLabelA).addComponent(tcALabel)
                                                        .addComponent(texLabelB).addComponent(tcBLabel)
                                                        .addComponent(fullLabel).addComponent(normalLabel)
                                                        .addComponent(diffLabel).addComponent(specLabel)
                                                        .addComponent(shinyLabel).addComponent(expULabel)
                                                        .addComponent(expVLabel)
                                                        .addComponent(sensitivityLabel)
                                                        .addComponent(exposureLabel).addComponent(fstopLabel)
                                                        .addComponent(gammaLabel)
                                                        .addComponent(samplesLabel)));
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
                                                      .addComponent(expVLabel)).addGap(15)
                                      .addGroup(layout.createParallelGroup().addComponent(sensitivitySlider)
                                                      .addComponent(sensitivityLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(exposureSlider)
                                                      .addComponent(exposureLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(fstopSlider)
                                                      .addComponent(fstopLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(gammaSlider)
                                                      .addComponent(gammaLabel)).addGap(15)
                                      .addGroup(layout.createParallelGroup().addComponent(samplesSlider)
                                                      .addComponent(samplesLabel)));

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
        input.on(keyPress(KeyEvent.KeyCode.D)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                showDiffMap = !showDiffMap;
            }
        });
        input.on(keyPress(KeyEvent.KeyCode.S)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                System.out.println("Samples left: " + specularSamplesLeft);
                Profiler.getDataSnapshot().get(Thread.currentThread()).print(System.out);
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
        private static final int TC_VIEW_IDX = 1;

        private final TextureSurface gbuffer;
        private final Texture2D normalTangentXY; // RGBA16, eye space converted to [0, 1], so z is positive
        private final Texture2D texCoordAndViewXY; // RGBA16
        private final DepthMap2D depth; // DEPTH24

        public GBuffer(Framework f) {
            Texture2DBuilder b = f.newTexture2D();
            b.width(WIDTH).height(HEIGHT).rgba().mipmap(0).fromUnsignedNormalized((short[]) null);
            normalTangentXY = b.build();

            b = f.newTexture2D();
            b.width(WIDTH).height(HEIGHT).rgba().mipmap(0).fromUnsignedNormalized((short[]) null);
            texCoordAndViewXY = b.build();

            DepthMap2DBuilder d = f.newDepthMap2D();
            d.width(WIDTH).height(HEIGHT).depth().mipmap(0).fromUnsignedNormalized((int[]) null);
            depth = d.build();

            TextureSurfaceOptions o = new TextureSurfaceOptions().size(WIDTH, HEIGHT)
                                                                 .colorBuffers(normalTangentXY
                                                                                       .getRenderTarget(),
                                                                               texCoordAndViewXY
                                                                                       .getRenderTarget())
                                                                 .depthBuffer(depth.getRenderTarget());
            gbuffer = f.createSurface(o);
        }
    }

    private static class Material {
        private final Sampler specularAlbedo;
        private final Sampler diffuseAlbedo;
        private final Sampler normal;
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
        private final List<EnvironmentMap.Sample> samples;
        private final TextureCubeMap envMap;
        private final TextureCubeMap diffuseMap;

        public Environment() {
            samples = Collections.emptyList();
            envMap = null;
            diffuseMap = null;
        }

        public Environment(Framework f, File in) throws IOException {
            EnvironmentMap map = EnvironmentMap.loadFromFile(in);
            samples = map.getSamples();
            envMap = map.createEnvironmentMap(f);
            diffuseMap = map.createDiffuseMap(f);
        }
    }
}
