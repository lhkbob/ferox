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
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.builder.Builder;
import com.ferox.renderer.builder.ShaderBuilder;
import com.ferox.renderer.geom.Geometry;
import com.ferox.renderer.geom.Shapes;
import com.ferox.renderer.loader.TextureLoader;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CancellationException;

import static com.ferox.input.logic.Predicates.*;

/**
 *
 */
public class AshikhminShader implements Task<Void> {
    private static enum MoveState {
        NONE,
        CAMERA,
        OBJECT,
        LIGHT
    }

    private final Framework framework;
    private final OnscreenSurface window;
    private final InputManager input;
    private MoveState moveState;

    private final Geometry shape;
    private final Shader shader;

    private final Geometry xAxis;
    private final Geometry yAxis;
    private final Geometry zAxis;
    private boolean showAxis;

    private final Geometry lightCube;
    private boolean showLight;
    private final Vector4 light;

    private final TrackBall modelTrackBall;
    private final TrackBall viewTrackBall;
    private final Frustum camera;
    private double cameraDist;

    private volatile Sampler specularNormalTexA;
    private volatile Sampler specularAlbedoTexA;
    private volatile Sampler diffuseNormalTexA;
    private volatile Sampler diffuseAlbedoTexA;
    private volatile Sampler shininessTexA;

    private volatile Sampler specularNormalTexB;
    private volatile Sampler specularAlbedoTexB;
    private volatile Sampler diffuseNormalTexB;
    private volatile Sampler diffuseAlbedoTexB;
    private volatile Sampler shininessTexB;

    private volatile double normalAlpha;
    private volatile double specularAlbedoAlpha;
    private volatile double diffuseAlbedoAlpha;
    private volatile double shininessAlpha;

    private volatile double shininessXScale;
    private volatile double shininessYScale;
    private volatile double texCoordAScale;
    private volatile double texCoordBScale;

    private final JFrame properties;

    public static void main(String[] args) throws Exception {
        Framework framework = Framework.Factory.create();
        try {
            new AshikhminShader(framework).run();
        } finally {
            framework.destroy();
        }
    }

    public AshikhminShader(Framework f) throws Exception {
        modelTrackBall = new TrackBall(false);
        viewTrackBall = new TrackBall(true);

        shininessXScale = 1.0;
        shininessYScale = 1.0;
        texCoordAScale = 1.0;
        texCoordBScale = 1.0;

        light = new Vector4(1, 1, 0, 1);
        moveState = MoveState.NONE;

        this.framework = f;
        OnscreenSurfaceOptions opts = new OnscreenSurfaceOptions().withDepthBuffer(24).withMSAA(4)
                                                                  .windowed(800, 800).fixedSize();
        window = framework.createSurface(opts);
        window.setVSyncEnabled(true);
        window.setLocation(0, 0);

        camera = new Frustum(60, window.getWidth() / (float) window.getHeight(), 0.1, 25);
        cameraDist = 1.5;
        updateCamera();

        input = new InputManager();
        input.attach(window);
        input.on(keyPress(KeyEvent.KeyCode.ESCAPE)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                framework.destroy();
            }
        });
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
        input.on(and(and(mousePress(MouseEvent.MouseButton.LEFT), keyHeld(KeyEvent.KeyCode.LEFT_META)),
                     not(keyHeld(KeyEvent.KeyCode.LEFT_CONTROL)))).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                moveState = MoveState.LIGHT;
            }
        });
        input.on(mouseMove(true)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                if (moveState == MoveState.LIGHT) {
                    Vector4 projectedLight = new Vector4();
                    projectedLight.mul(camera.getViewMatrix(), light);
                    projectedLight.mul(camera.getProjectionMatrix(), projectedLight);
                    double w = projectedLight.w;
                    projectedLight.scale(1.0 / w);

                    projectedLight.x += (getNormalizedDeviceX(next.getMouseState().getX()) -
                                         getNormalizedDeviceX(prev.getMouseState().getX()));
                    projectedLight.y += (getNormalizedDeviceY(next.getMouseState().getY()) -
                                         getNormalizedDeviceY(prev.getMouseState().getY()));

                    Matrix4 inv = new Matrix4();
                    light.mul(inv.inverse(camera.getProjectionMatrix()), projectedLight.scale(w));
                    light.mul(inv.inverse(camera.getViewMatrix()), light);
                } else if (moveState == MoveState.CAMERA) {
                    viewTrackBall.drag(getNormalizedDeviceX(next.getMouseState().getX()),
                                       getNormalizedDeviceY(next.getMouseState().getY()),
                                       viewTrackBall.getRotation());
                    updateCamera();

                } else if (moveState == MoveState.OBJECT) {
                    modelTrackBall.drag(getNormalizedDeviceX(next.getMouseState().getX()),
                                        getNormalizedDeviceY(next.getMouseState().getY()),
                                        viewTrackBall.getRotation());
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
                showLight = !showLight;
            }
        });

        //        shape = Shapes.createSphere(framework, 0.5, 128);
        shape = Shapes.createTeapot(framework);

        showAxis = true;
        xAxis = Shapes.createCylinder(framework, new Vector3(1, 0, 0), new Vector3(1, 0, 0), 0.01, 0.5, 4);
        yAxis = Shapes.createCylinder(framework, new Vector3(0, 1, 0), new Vector3(0, 1, 0), 0.01, 0.5, 4);
        zAxis = Shapes.createCylinder(framework, new Vector3(0, 0, 1), new Vector3(0, 0, 1), 0.01, 0.5, 4);

        showLight = true;
        lightCube = Shapes.createBox(framework, 0.1);

        ShaderBuilder shaderBuilder = framework.newShader();

        try (BufferedReader vertIn = new BufferedReader(new InputStreamReader(getClass()
                                                                                      .getResourceAsStream("ashik.vert")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = vertIn.readLine()) != null) {
                sb.append(line).append('\n');
            }
            shaderBuilder.withVertexShader(sb.toString());
        } catch (IOException e) {
            throw new FrameworkException("Unable to load vertex shader", e);
        }

        try (BufferedReader fragIn = new BufferedReader(new InputStreamReader(getClass()
                                                                                      .getResourceAsStream("ashik.frag")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = fragIn.readLine()) != null) {
                sb.append(line).append('\n');
            }
            shaderBuilder.withFragmentShader(sb.toString());
        } catch (IOException e) {
            throw new FrameworkException("Unable to load fragment shader", e);
        }

        shaderBuilder.bindColorBuffer("fColor", 0);
        shader = shaderBuilder.build();

        properties = new JFrame("Properties");
        properties.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        JPanel pl = new JPanel();
        GroupLayout layout = new GroupLayout(pl);
        pl.setLayout(layout);
        properties.add(pl);

        JButton loadTexturesA = new JButton("Load Textures A");
        final JLabel texLabelA = new JLabel("None");
        loadTexturesA.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser("/Users/mludwig/Desktop/LLS");
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
                JFileChooser fc = new JFileChooser("/Users/mludwig/Desktop/LLS");
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
            }
        });
        JLabel expVLabel = new JLabel("Shiny V Scale");
        final JSlider expVSlider = createSlider(10, 1000);
        expVSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessYScale = expVSlider.getValue() / 10.0;
            }
        });

        JLabel tcALabel = new JLabel("TC A Scale");
        final JSlider tcASlider = createSlider(100, 10000);
        tcASlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                texCoordAScale = tcASlider.getValue() / 100.0;
            }
        });
        JLabel tcBLabel = new JLabel("TC B Scale");
        final JSlider tcBSlider = createSlider(100, 10000);
        tcBSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                texCoordBScale = tcBSlider.getValue() / 100.0;
            }
        });

        layout.setHorizontalGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup().addComponent(loadTexturesA)
                                                        .addComponent(tcASlider).addComponent(loadTexturesB)
                                                        .addComponent(tcBSlider).addComponent(fullSlider)
                                                        .addComponent(normalSlider)
                                                        .addComponent(diffAlbedoSlider)
                                                        .addComponent(specAlbedoSlider)
                                                        .addComponent(shininessSlider)
                                                        .addComponent(expUSlider).addComponent(expVSlider))
                                        .addGroup(layout.createParallelGroup().addComponent(texLabelA)
                                                        .addComponent(tcALabel).addComponent(texLabelB)
                                                        .addComponent(tcBLabel).addComponent(fullLabel)
                                                        .addComponent(normalLabel).addComponent(diffLabel)
                                                        .addComponent(specLabel).addComponent(shinyLabel)
                                                        .addComponent(expULabel).addComponent(expVLabel)));
        layout.setVerticalGroup(layout.createSequentialGroup()
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
                                                      .addComponent(expVLabel)));

        properties.pack();
        properties.setLocation(810, 0);
        properties.setVisible(true);
    }

    private static JSlider createSlider(int min, int max) {
        JSlider slider = new JSlider(min, max);
        slider.setPaintLabels(false);
        slider.setPaintTicks(false);
        slider.setSnapToTicks(true);
        slider.setValue(0);
        return slider;
    }

    public void run() throws Exception {
        while (!window.isDestroyed()) {
            try {
                framework.invoke(this).get();
            } catch (CancellationException e) {
                // ignore
            }
        }
        properties.setVisible(false);
        properties.dispose();
    }

    private double getNormalizedDeviceX(double windowX) {
        return 2.0 * windowX / window.getWidth() - 1.0;
    }

    private double getNormalizedDeviceY(double windowY) {
        return 2.0 * windowY / window.getHeight() - 1.0;
    }

    private void updateCamera() {
        Matrix4 camT = new Matrix4()
                               .lookAt(new Vector3(), new Vector3(0, 0, cameraDist), new Vector3(0, 1, 0));
        camT.mul(viewTrackBall.getTransform(), camT);
        camera.setOrientation(camT);
    }

    private void loadTexturesA(final String directory) {
        new Thread("texture loader") {
            @Override
            public void run() {
                try {
                    Builder<? extends Sampler> diffNormal = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "diffuseNormal.hdr"));
                    Builder<? extends Sampler> diffAlbedo = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "diffuseAlbedo.hdr"));
                    Builder<? extends Sampler> specNormal = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "specularNormal.hdr"));
                    Builder<? extends Sampler> specAlbedo = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "specularAlbedo.hdr"));
                    Builder<? extends Sampler> shininess = TextureLoader.readTexture(framework,
                                                                                     new File(directory +
                                                                                              File.separator +
                                                                                              "shininessXY.hdr"));

                    diffuseNormalTexA = diffNormal.build();
                    diffuseAlbedoTexA = diffAlbedo.build();
                    specularNormalTexA = specNormal.build();
                    specularAlbedoTexA = specAlbedo.build();
                    shininessTexA = shininess.build();
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
                    Builder<? extends Sampler> diffNormal = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "diffuseNormal.hdr"));
                    Builder<? extends Sampler> diffAlbedo = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "diffuseAlbedo.hdr"));
                    Builder<? extends Sampler> specNormal = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "specularNormal.hdr"));
                    Builder<? extends Sampler> specAlbedo = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "specularAlbedo.hdr"));
                    Builder<? extends Sampler> shininess = TextureLoader.readTexture(framework,
                                                                                     new File(directory +
                                                                                              File.separator +
                                                                                              "shininessXY.hdr"));

                    diffuseNormalTexB = diffNormal.build();
                    diffuseAlbedoTexB = diffAlbedo.build();
                    specularNormalTexB = specNormal.build();
                    specularAlbedoTexB = specAlbedo.build();
                    shininessTexB = shininess.build();
                } catch (IOException e) {
                    System.err.println("Error loading images:");
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public Void run(HardwareAccessLayer access) {
        input.process();

        Context ctx = access.setActiveSurface(window);
        if (ctx == null) {
            return null;
        }
        GlslRenderer r = ctx.getGlslRenderer();

        r.clear(true, true, true, new Vector4(0.5, 0.5, 0.5, 1.0), 1.0, 0);

        r.setShader(shader);

        // Main object rendering
        r.setUniform(shader.getUniform("uUseSolidColor"), false);
        r.setUniform(shader.getUniform("uLightPos"), light);
        r.setUniform(shader.getUniform("uProjection"), camera.getProjectionMatrix());
        r.setUniform(shader.getUniform("uView"), camera.getViewMatrix());
        r.setUniform(shader.getUniform("uCamPos"), camera.getLocation());

        r.setUniform(shader.getUniform("uSpecularNormalTexA"), specularNormalTexA);
        r.setUniform(shader.getUniform("uSpecularAlbedoTexA"), specularAlbedoTexA);
        r.setUniform(shader.getUniform("uDiffuseNormalTexA"), diffuseNormalTexA);
        r.setUniform(shader.getUniform("uDiffuseAlbedoTexA"), diffuseAlbedoTexA);
        r.setUniform(shader.getUniform("uShininessTexA"), shininessTexA);

        r.setUniform(shader.getUniform("uSpecularNormalTexB"), specularNormalTexB);
        r.setUniform(shader.getUniform("uSpecularAlbedoTexB"), specularAlbedoTexB);
        r.setUniform(shader.getUniform("uDiffuseNormalTexB"), diffuseNormalTexB);
        r.setUniform(shader.getUniform("uDiffuseAlbedoTexB"), diffuseAlbedoTexB);
        r.setUniform(shader.getUniform("uShininessTexB"), shininessTexB);

        r.setUniform(shader.getUniform("uNormalAlpha"), normalAlpha);
        r.setUniform(shader.getUniform("uSpecularAlpha"), specularAlbedoAlpha);
        r.setUniform(shader.getUniform("uDiffuseAlpha"), diffuseAlbedoAlpha);
        r.setUniform(shader.getUniform("uShininessAlpha"), shininessAlpha);

        r.setUniform(shader.getUniform("uShininessScale"), shininessXScale, shininessYScale);
        r.setUniform(shader.getUniform("uTCScale"), texCoordAScale, texCoordBScale);

        r.setUniform(shader.getUniform("uModel"), modelTrackBall.getTransform());

        r.bindAttribute(shader.getAttribute("aPos"), shape.getVertices());
        r.bindAttribute(shader.getAttribute("aNorm"), shape.getNormals());
        r.bindAttribute(shader.getAttribute("aTan"), shape.getTangents());
        r.bindAttribute(shader.getAttribute("aTC"), shape.getTextureCoordinates());

        r.setIndices(shape.getIndices());
        r.render(shape.getPolygonType(), shape.getIndexOffset(), shape.getIndexCount());

        // draw light
        if (showLight) {
            r.setUniform(shader.getUniform("uModel"), new Matrix4().setIdentity().setCol(3, light));

            r.setUniform(shader.getUniform("uUseSolidColor"), true);
            r.setUniform(shader.getUniform("uSolidColor"), new Vector4(1, 1, 0, 1));

            r.bindAttribute(shader.getAttribute("aPos"), lightCube.getVertices());
            r.bindAttribute(shader.getAttribute("aNorm"), lightCube.getNormals());
            r.bindAttribute(shader.getAttribute("aTan"), lightCube.getTangents());
            r.bindAttribute(shader.getAttribute("aTC"), lightCube.getTextureCoordinates());

            r.setIndices(lightCube.getIndices());
            r.render(lightCube.getPolygonType(), lightCube.getIndexOffset(), lightCube.getIndexCount());
        }

        // axis rendering
        if (showAxis) {
            r.setUniform(shader.getUniform("uModel"), new Matrix4().setIdentity());
            r.setUniform(shader.getUniform("uSolidColor"), new Vector4(1, 0, 0, 1));
            r.setUniform(shader.getUniform("uUseSolidColor"), true);

            r.bindAttribute(shader.getAttribute("aPos"), xAxis.getVertices());
            r.bindAttribute(shader.getAttribute("aNorm"), xAxis.getNormals());
            r.bindAttribute(shader.getAttribute("aTan"), xAxis.getTangents());
            r.bindAttribute(shader.getAttribute("aTC"), xAxis.getTextureCoordinates());

            r.setIndices(xAxis.getIndices());
            r.render(xAxis.getPolygonType(), xAxis.getIndexOffset(), xAxis.getIndexCount());

            r.setUniform(shader.getUniform("uSolidColor"), new Vector4(0, 1, 0, 1));
            r.bindAttribute(shader.getAttribute("aPos"), yAxis.getVertices());
            r.bindAttribute(shader.getAttribute("aNorm"), yAxis.getNormals());
            r.bindAttribute(shader.getAttribute("aTan"), yAxis.getTangents());
            r.bindAttribute(shader.getAttribute("aTC"), yAxis.getTextureCoordinates());

            r.setIndices(yAxis.getIndices());
            r.render(yAxis.getPolygonType(), yAxis.getIndexOffset(), yAxis.getIndexCount());

            r.setUniform(shader.getUniform("uSolidColor"), new Vector4(0, 0, 1, 1));
            r.bindAttribute(shader.getAttribute("aPos"), zAxis.getVertices());
            r.bindAttribute(shader.getAttribute("aNorm"), zAxis.getNormals());
            r.bindAttribute(shader.getAttribute("aTan"), zAxis.getTangents());
            r.bindAttribute(shader.getAttribute("aTC"), zAxis.getTextureCoordinates());

            r.setIndices(zAxis.getIndices());
            r.render(zAxis.getPolygonType(), zAxis.getIndexOffset(), zAxis.getIndexCount());
        }

        ctx.flush();
        return null;
    }
}
